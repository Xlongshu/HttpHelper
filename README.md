#HttpHelper
##Http通信使用帮助工具

###CookieHelper
可以轻松获取服务器端保持会话的SESSION,
某域名的Cookie,以及以字符串的形式.
###HttpMultipartEntity
可以轻松获的与服务端发送Multipart的表单数据
例如上传文件等操作.
HttpMultipartEntity和CookieHelper可以同时配合使用

使用高效通用的URLConnection进行网络交互.

附带一个EncryptDecrypt.java加密解密工具
SHA1 MD5 BASE64 AES 加密解密,JavaSE/JavaEE/Android都适用


CookieHelper示例:

```java
CookieHelper cookieHelper;

public String loginByGet(String username, String password){
	String path="http://10.0.2.2:8080/LoginServlet?username={0}&password={1}";
	path = MessageFormat.format(path, username, password);
	try {
			URL url = new URL(path);
			//初始化CookieHelper
			if (cookieHelper == null) {
				cookieHelper = new CookieHelper(new URL("http://10.0.2.2:8080/index.jsp").openConnection(), null);
				System.out.println(cookieHelper.getSessionCookie(url));
			}
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(2000);

			// 发送设置cookie
			if (cookieHelper != null) {
				System.out.println(cookieHelper.getAllCookiesStr());
				conn.setRequestProperty("Cookie", cookieHelper.getAllCookiesStr());
			}

			int code = conn.getResponseCode();
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
			throw new RuntimeException(e);
		}
}

```

HttpMultipartEntity示例:

```java

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

```
