package longshu.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;

import longshu.http.HttpMultipartEntity;
import longshu.http.cookie.CookieHelper;

public class Demo {

	static CookieHelper cookieHelper;
	static int timeoutMillis = 2000;

	public static void main(String[] args) throws IOException {
		// String base64 = EncryptDecrypt.encodeBASE64("longshu");
		// System.out.println("encodeBASE64: " + base64);
		// System.out.println("decodeBASE64: " +
		// EncryptDecrypt.decodeBASE64(base64));
		// System.out.println();
		// String result = loginByGet("test", "test");
		// System.out.println("result:\n" + result);
		// System.out.println(loginByPost("longshu","longshu"));
		uploadFile();
	}

	public static void uploadFile() throws IOException {
		URL url = new URL("http://127.0.0.1:8080/AndroidWeb/UploadFileServlet");
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setRequestMethod("POST");
		httpConn.setDoOutput(true);// 设置该连接允许写入
		httpConn.setUseCaches(false);
		httpConn.setConnectTimeout(timeoutMillis);
		httpConn.setReadTimeout(timeoutMillis);
		httpConn.setRequestProperty("Charset", "UTF-8");

		HttpMultipartEntity multipart = new HttpMultipartEntity();
		multipart.addPart("username", "龙叔");
		multipart.addPart("password", "longshu");
		File uploadfile = new File("upload.txt");
		multipart.addPart("uploadfile", uploadfile, "text/plain");

		httpConn.setRequestProperty("Content-Type", multipart.getContentType());
		httpConn.setRequestProperty("Content-Length", multipart.getContentLength() + "");
		System.out.println("Content-Type=" + multipart.getContentType());
		System.out.println("Content-Length=" + multipart.getContentLength());
		OutputStream os = httpConn.getOutputStream();
		multipart.writeTo(os);
		multipart.free();
		int code = httpConn.getResponseCode();
		System.out.println("code:" + code);
		InputStream is = httpConn.getInputStream();
		String result = readInputStream(is);
		System.out.println(result);
	}

	public static String loginByPost(String username, String password) {
		String path = "http://127.0.0.1:8080/AndroidWeb/LoginServlet";
		try {
			URL url = new URL(path);
			if (cookieHelper == null) {
				cookieHelper = new CookieHelper(new URL("http://127.0.0.1:8080/index.jsp").openConnection(), null);
				System.out.println(cookieHelper.getSessionCookie(url));
			}
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(2000);
			conn.setReadTimeout(3000);
			conn.setRequestMethod("POST");
			// post 的方式是想服务端流写数据
			conn.setDoOutput(true);
			// 准备数据
			String data = "username=" + URLEncoder.encode(username, "UTF-8") + "&password="
					+ URLEncoder.encode(password, "UTF-8");
			conn.setRequestProperty("Content-Length", data.length() + "");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			// 发送设置cookie
			if (cookieHelper != null) {
				System.out.println(cookieHelper.getAllCookiesStr());
				conn.setRequestProperty("Cookie", cookieHelper.getAllCookiesStr());
			}

			OutputStream os = conn.getOutputStream();
			os.write(data.getBytes());
			int code = conn.getResponseCode();
			System.out.println("code:" + code);
			if (200 == code) {
				cookieHelper.addCookie(conn);// 保存服务端返回的Cookie
				System.out.println(cookieHelper.getAllCookies());
				InputStream is = conn.getInputStream();
				String result = readInputStream(is);
				return result;
			} else {
				System.out.println("loginByGet fail");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String loginByGet(String username, String password) {
		String path = "http://120.27.122.59/blog/UserServlet?method=login&email={0}&password={1}&verifyCode=";
		path = MessageFormat.format(path, username, password);
		System.out.println("path:\n" + path);
		try {
			URL url = new URL(path);
			// 初始化CookieHelper
			if (cookieHelper == null) {
				cookieHelper = new CookieHelper(new URL("http://120.27.122.59/blog/index.jsp").openConnection(), null);
				System.out.println("get: " + cookieHelper.getSessionCookie(url));
			}
			HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
			httpConn.setConnectTimeout(2000);

			// 发送设置cookie
			if (cookieHelper != null) {
				System.out.println("send " + cookieHelper.getSessionCookieStr(url));
				httpConn.setRequestProperty("Cookie", cookieHelper.getSessionCookieStr(url));
			}

			int code = httpConn.getResponseCode();
			System.out.println("code:" + code);
			if (200 == code) {
				cookieHelper.addCookie(httpConn);// 保存服务端返回的Cookie
				System.out.println("add: " + cookieHelper.getAllCookies());
				InputStream is = httpConn.getInputStream();
				String result = readInputStream(is);
				return result;
			} else {
				System.out.println("loginByGet fail");
				return null;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String readInputStream(InputStream is) {
		int len = 0;
		byte[] buffer = new byte[1024];
		String temp = null;
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			while (-1 != (len = is.read(buffer))) {
				os.write(buffer, 0, len);
			}
			is.close();
			temp = new String(os.toByteArray());
			if (temp.contains("charset=gb")) {
				temp = new String(os.toByteArray(), "GBK");
			}
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
			return temp;
		}
		return temp;
	}

}
