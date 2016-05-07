package longshu.http.cookie;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cookie获取帮助类.
 * 可以轻松获取服务器端保持会话的SESSION,
 * 某域名的Cookie,以及以字符串的形式. <br/>
 * 即使是访问不同网站只要有SESSION就可以保持会话.<br/>
 * <pre>
 * 注意:创建对象时确保请求中包含SESSIONID的Cookie,
 * 或者是请求了动态页面JSP而不是Servlet,因为Servlet是不会返回JSESSIONID的.
 * 例如:CookieHelper cookieHelper = new CookieHelper(new URL("http://192.168.1.100:8080/web/index.jsp").openConnection());
 * </pre>
 * 当然也可以使用 {@link #addCookie(URLConnection)}来完成添加.
 * @author longshu 2016年4月14日
 */
public class CookieHelper {
	// 设置Cookie的响应头
	public final static String SET_COOKIE = "Set-Cookie";
	public final static String SET_COOKIE2 = "Set-Cookie2";
	// 管理Cookie,static保证使用的是相同的CookieStore
	private static CookieManager cookieManager = new CookieManager();
	private CookieStore cookieStore;
	// 服务器端保持会话的SESSION,保存下来方便获取
	private Map<URI, HttpCookie> sessionCookies;// 保存不同网站的SESSION
	private String sessionid = "JSESSIONID";// 默认的sessionid键,以javaEE服务端为例,PHP为PHPSESSID
	private Map<URI, String> sessionids;/// 保存不同网站的SESSION的KEY的名称
	private boolean findSession;

	private CookieHelper() {
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		cookieStore = cookieManager.getCookieStore();
		sessionCookies = new HashMap<URI, HttpCookie>();
		sessionids = new HashMap<URI, String>();
	}

	/**
	 * @param url 
	 * 如果不为空将会使用url.getHost(),作为存cookie的uri;如果为空uri为 "" .
	 * @param cookiesHeader 
	 * 来自URLConnection.getHeaderFields().get("Set-Cookie") ,或者是Set-Cookie2 .
	 * @param sessionid javaEE服务端为JSESSIONID(如果为空默认),PHP为PHPSESSID .
	 */
	public CookieHelper(URL url, List<String> cookiesHeader, String sessionid) {
		this();
		URI uri = urlToURI(url);
		if (null == sessionid || sessionid.isEmpty()) {
			sessionids.put(uri, this.sessionid);// 使用默认
		} else {
			sessionids.put(uri, sessionid);
		}
		/*
		 * 保存所有Cookie
		 */
		add(url, cookiesHeader);
		if (!findSession) {
			findSession = saveSessionCookie(uri, getCookies(url));// 获取sessionCookie
		}
		System.out.println("CookieHelper--> Size:" + getAllCookies().size() + ";Session:" + findSession);
	}

	/**
	 * 使用URLConnection直接获取Cookie
	 * @param connection 已经完成connection.setXXX;已发送connect(),或者将要发送connect().
	 * @param sessionid {@link #CookieHelper(URL, List, String)}
	 */
	public CookieHelper(URLConnection connection, String sessionid) {
		this(connection.getURL(), connection.getHeaderFields().get(SET_COOKIE), sessionid);
	}

	/**
	 * 添加所有Cookie
	 * @param connection
	 * @return 添加cookie的个数
	 */
	public int addCookie(URLConnection connection) {
		List<String> cookiesHeader = connection.getHeaderFields().get(SET_COOKIE);
		// cookiesHeader.addAll(connection.getHeaderFields().get(SET_COOKIE2));
		return add(connection.getURL(), cookiesHeader);
	}

	/**
	 * 添加你需要的Cookie
	 * @param url {@link #CookieHelper(URL, List, String)}
	 * @param cookiesHeader {@link #CookieHelper(URL, List, String)}
	 * @return 添加cookie的个数
	 */
	public int addCookie(URL url, List<String> cookiesHeader) {
		return add(url, cookiesHeader);
	}

	/**
	 * 添加网站的SESSION的KEY的名称
	 * @param url
	 * @param sessionid 如: JSESSIONID
	 */
	public void addSessionID(URL url, String sessionid) {
		sessionids.put(urlToURI(url), sessionid);
	}

	/**
	 * 返回某网站的SESSION的KEY名称
	 * @param url
	 * @return 如: JSESSIONID
	 */
	public String getSessionID(URL url) {
		return sessionids.get(urlToURI(url));
	}

	/**
	 * 返回所有网站的SESSION的KEY名称
	 * @return
	 */
	public List<String> getAllSessionID() {
		return new ArrayList<String>(sessionids.values());
	}

	/**
	 * 替换某网站的SESSION的Cookie
	 * @param url
	 * @param sessionHeader
	 * @return
	 */
	/*
	public boolean replaceSessionCookie(URL url, String sessionHeader) {
		URI uri = urlToURI(url);
		boolean haveSession = false;
		if (!sessionids.isEmpty()) {
			if (sessionids.get(uri).contains(sessionHeader))
				haveSession = true;
		} else if (sessionid.contains(sessionHeader))
			haveSession = true;
		// 如果不存在SESSION
		if (!haveSession)
			return false;
	
		List<String> session = new ArrayList<String>();
		session.add(sessionHeader);
		int r = add(url, session);
		if (r < 1) {
			return false;
		}
		sessionCookies.remove(uri);// 移除原有的
		return saveSessionCookie(uri, HttpCookie.parse(sessionHeader));
	}
	*/

	/**
	 * URL转换成URI
	 * @param url
	 * @return
	 */
	public static URI urlToURI(URL url) {
		URI uri;
		if (null == url) {
			uri = URI.create("");
		} else {
			uri = URI.create(url.getHost());
		}
		return uri;
	}

	/**
	 * 获取内存中的所有Cookie
	 * @return 已保存的Cookie
	 */
	public List<HttpCookie> getAllCookies() {
		return cookieStore.getCookies();
	}

	/**
	 * 获取所有Cookie的字符串
	 * @return 如: user=longshu;password=123456;
	 */
	public String getAllCookiesStr() {
		List<HttpCookie> cookies = cookieStore.getCookies();
		return getCookiesStr(cookies);
	}

	/**
	 * 获取指定url下的Cookie的字符串
	 * @param url {@link #getCookies(URL)}
	 * @return 如: user=longshu;password=123456;
	 */
	public String getCookiesStr(URL url) {
		List<HttpCookie> cookies = getCookies(url);
		return getCookiesStr(cookies);
	}

	/**
	 * 获取指定url下的cookies
	 * @param url
	 * 如果不为空将会使用url.getHost(),作为获取cookie的uri;如果为空uri为 "" .
	 * @return 已保存的并且URI为url的Cookie .
	 */
	public List<HttpCookie> getCookies(URL url) {
		URI uri = null;
		uri = urlToURI(url);
		return cookieStore.get(uri);
	}

	/**
	 * 服务器端保持会话的Cookie,如JSESSIONID
	 * @param url
	 * @return
	 */
	public HttpCookie getSessionCookie(URL url) {
		return sessionCookies.get(urlToURI(url));
	}

	/**
	 * 返回SESSION的字符串
	 * @param url
	 * @return
	 */
	public String getSessionCookieStr(URL url) {
		if (sessionCookies.isEmpty())
			return "";
		return sessionCookies.get(urlToURI(url)).toString();
	}

	/**
	 * 记录保持会话的SESSION
	 * @param cookies 所有
	 * @return 是否获取到SESSION
	 */
	private boolean saveSessionCookie(URI uri, List<HttpCookie> cookies) {
		boolean findSession = false;
		for (HttpCookie cookie : cookies) {
			if (cookie.getName().equals(sessionids.get(uri))) {// 保存SESSION
				sessionCookies.put(uri, cookie);
				findSession = true;
				// break;
			}
		}
		return findSession;
	}

	/**
	 * 将Cookie添加到cookieManager来管理
	 * @param url
	 * @param cookiesHeader 包含Cookie的集合
	 * @return 保存cookie的个数
	 */
	private int add(URL url, List<String> cookiesHeader) {
		int count = 0;
		if (null == cookiesHeader || cookiesHeader.isEmpty()) {
			return count;
		}
		URI uri = null;
		uri = urlToURI(url);
		System.out.println("CookieHelper--> add uri:" + uri);

		for (String cookieHead : cookiesHeader) {
			List<HttpCookie> cookies = HttpCookie.parse(cookieHead);
			for (HttpCookie cookie : cookies) {
				cookieStore.add(uri, cookie);
				++count;
			}
			// 如果存在Session就保存,会替换原有的
			if (cookiesHeader.toString().contains(sessionids.get(uri))) {
				findSession = saveSessionCookie(uri, cookies);
			}
		}
		return count;
	}

	private String getCookiesStr(List<HttpCookie> cookies) {
		if (null == cookies || cookies.isEmpty()) {
			return "";
		}
		StringBuffer sb = new StringBuffer();
		for (HttpCookie cookie : cookies) {
			sb.append(cookie.toString() + ";");
		}
		return sb.toString();
	}

}
