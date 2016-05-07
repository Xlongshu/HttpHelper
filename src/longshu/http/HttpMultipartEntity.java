package longshu.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 使用URLConnection向服务端提交multipart的表单数据
 * <pre>
....
URL url = new URL("http://127.0.0.1:8080/AndroidWeb/UploadFileServlet");
HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
httpConn.setRequestMethod("POST");
httpConn.setDoOutput(true);// 设置该连接允许写入

HttpMultipartEntity multipart = new HttpMultipartEntity();
multipart.addPart("username", "longshu");//普通表单
multipart.addPart("password", "longshu");
File uploadfile = new File("upload.txt");
multipart.addPart("uploadfile", uploadfile, "text/plain");//文件表单
httpConn.setRequestProperty("Content-Type", multipart.getContentType());
httpConn.setRequestProperty("Content-Length", multipart.getContentLength() + "");

multipart.writeTo(httpConn.getOutputStream());//写数据
multipart.free();//释放资源
....
 * </pre>
 * 参考来自 com.loopj.android.http.SimpleMultipartEntity
 * @author longshu 2016年5月6日
 */
public class HttpMultipartEntity {
	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
	public static final String TRANSFER_ENCODING_BINARY = "Content-Transfer-Encoding: binary\r\n";
	public static final String HEADER_CONTENT_TYPE = "Content-Type";
	// public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	// public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
	public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
	public static final String ENCTYPE = "multipart/form-data";
	public static final String LINE = "--";
	public static final String CRLF = "\r\n";
	public static final int BUFFER_SIZE = 4096;

	public ByteArrayOutputStream outBuf;// 所有要发送的Byte数据
	private List<FilePart> fileParts; // 文件数据
	private String charset = "UTF-8";// 与服务端交互的编码
	public String boundary;// 分界
	private String boundaryLine;// 分界包含换行回车
	private String boundaryEnd;// 分界结束
	private long totalSize;// 总数据大小

	public HttpMultipartEntity() {
		outBuf = new ByteArrayOutputStream();
		fileParts = new ArrayList<FilePart>();
		buildBoundary();
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	/**
	 * 普通表单内容
	 * @param key 表单name
	 * @param value name对应的值
	 * @param contentType 类型一般为空
	 */
	public void addPart(final String key, final String value, final String contentType) {
		try {
			outBuf.write(boundaryLine.getBytes());
			outBuf.write(createContentDisposition(key).getBytes());
			if (null != contentType) {
				outBuf.write(createContentType(contentType).getBytes());
			}
			outBuf.write(CRLF.getBytes());
			outBuf.write(value.getBytes());// URLEncoder.encode(value, charset) 表单值用URL编码
			outBuf.write(CRLF.getBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@link #addPart(String, String, String)} 
	 */
	public void addPart(final String key, final String value) {
		addPart(key, value, null);
	}

	/**
	 * {@link #addPart(String, File, String, String)}
	 */
	public void addPart(String key, File file) {
		addPart(key, file, null);
	}

	/**
	 * {@link #addPart(String, File, String, String)}
	 */
	public void addPart(String key, File file, String type) {
		fileParts.add(new FilePart(key, file, normalizeContentType(type)));
	}

	/**
	 * 添加文件
	 * @param key 文件的表单字段name的值
	 * @param file 上传的文件文件
	 * @param type 文件类型: text/plain image/jpeg
	 * @param customFileName 指定文件名
	 */
	public void addPart(String key, File file, String type, String customFileName) {
		fileParts.add(new FilePart(key, file, normalizeContentType(type), customFileName));
	}

	/**
	 * 从inputStream向服务端写数据
	 * @param key
	 * @param streamName
	 * @param inputStream
	 * @param type
	 * @throws IOException
	 */
	public void addPart(String key, String streamName, InputStream inputStream, String type) throws IOException {
		outBuf.write(boundaryLine.getBytes());

		// Headers
		outBuf.write(createContentDisposition(key, streamName).getBytes());
		outBuf.write(createContentType(type).getBytes());
		outBuf.write(TRANSFER_ENCODING_BINARY.getBytes());
		outBuf.write(CRLF.getBytes());

		// Stream (file)
		final byte[] tmp = new byte[BUFFER_SIZE];
		int len;
		while ((len = inputStream.read(tmp)) != -1) {
			outBuf.write(tmp, 0, len);
		}

		outBuf.write(CRLF.getBytes());
		outBuf.flush();
	}

	/**
	 * 表单分界标识
	 * @return
	 */
	public String buildBoundary() {
		if (boundary == null) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 10; i++) {
				sb.append(LINE);
			}
			sb.append(Long.toHexString(System.currentTimeMillis()));// FormBoundary
			boundary = sb.toString();
		}
		// boundary = "-----------------------------7e030c42037e";
		boundaryLine = LINE + boundary + CRLF;
		boundaryEnd = LINE + boundary + LINE + CRLF;
		return boundary;
	}

	/**
	 * conn.setRequestProperty("Content-Type", getContentType());
	 * @return
	 */
	public String getContentType() {
		return (ENCTYPE + "; boundary=" + boundary);
	}

	/**
	 * 获取multipart的总大小
	 * @return
	 */
	public long getContentLength() {
		totalSize = outBuf.size();
		for (FilePart filePart : fileParts) {
			long len = filePart.getTotalLength();
			// if (len < 0) {
			// return -1;
			// }
			totalSize += len;
		}
		totalSize += boundaryEnd.getBytes().length;
		return totalSize;
	}

	/**
	 * 向服务端的OutputStream流写数据
	 * @param outstream URLConnection.getOutputStream()
	 * @throws IOException
	 */
	public void writeTo(final OutputStream outstream) throws IOException {
		outBuf.writeTo(outstream);
		for (FilePart filePart : fileParts) {
			filePart.writeTo(outstream);
		}
		outstream.write(boundaryEnd.getBytes());
		outBuf.close();
	}

	/**
	 * 释放资源
	 * @throws IOException
	 */
	public void free() throws IOException {
		if (outBuf != null)
			outBuf.close();
	}

	private String normalizeContentType(String type) {
		return type == null ? APPLICATION_OCTET_STREAM : type;
	}

	/**
	 * Content-Type
	 * @param type
	 * @return
	 */
	private String createContentType(String type) {
		return (HEADER_CONTENT_TYPE + ": " + normalizeContentType(type) + CRLF);
	}

	/**
	 * {@link #createContentDisposition(String, String)}
	 * @param key
	 * @return
	 */
	private String createContentDisposition(String key) {
		return (HEADER_CONTENT_DISPOSITION + ": form-data; name=\"" + key + "\"" + CRLF);
	}

	/**
	 * Content-Disposition: form-data; name="uploadfile"; filename="test.txt" 
	 * @param key
	 * @param fileName
	 * @return
	 */
	private String createContentDisposition(String key, String fileName) {
		return (HEADER_CONTENT_DISPOSITION + ": form-data; name=\"" + key + "\"" + "; filename=\"" + fileName + "\""
				+ CRLF);
	}

	/**
	 * 上传的文件内容
	 */
	private class FilePart {
		public File file;
		public byte[] header;

		public FilePart(String key, File file, String type) {
			header = createHeader(key, file.getName(), type);
			this.file = file;
		}

		public FilePart(String key, File file, String type, String customFileName) {
			if (null == customFileName || customFileName.trim().isEmpty()) {
				customFileName = file.getName();
			}
			header = createHeader(key, customFileName, type);
			this.file = file;
		}

		private byte[] createHeader(String key, String filename, String type) {
			ByteArrayOutputStream headerStream = new ByteArrayOutputStream();
			try {
				headerStream.write(boundaryLine.getBytes());
				// Headers
				headerStream.write(createContentDisposition(key, filename).getBytes());
				headerStream.write(createContentType(type).getBytes());
				headerStream.write(TRANSFER_ENCODING_BINARY.getBytes());
				headerStream.write(CRLF.getBytes());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return headerStream.toByteArray();
		}

		/**
		 * 文件信息总长度
		 * @return
		 */
		public long getTotalLength() {
			long streamLength = file.length();
			return header.length + streamLength;
		}

		/**
		 * {@link HttpMultipartEntity#writeTo(OutputStream)}
		 * @param out
		 * @throws IOException
		 */
		public void writeTo(final OutputStream out) throws IOException {
			out.write(header);

			FileInputStream inputStream = new FileInputStream(file);
			final byte[] tmp = new byte[BUFFER_SIZE];
			int len;
			while ((len = inputStream.read(tmp)) != -1) {
				out.write(tmp, 0, len);
			}
			out.write(CRLF.getBytes());
			out.flush();
			try {
				inputStream.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
