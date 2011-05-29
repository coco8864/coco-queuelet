/*
 * Created on 2004/10/20
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

/**
 * @author NARU
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ProxyResponse implements Queuelet {
	static private Logger logger=Logger.getLogger(ProxyResponse.class);
	static private String authHead;
	static private String authFoot;
	
	static{
		InputStream is=null;
		int readLength;
		byte[] buff=new byte[1024];
		try {
			is=ReverseProxyResponse.class.getResourceAsStream("authHead.txt");
			readLength=is.read(buff);
			authHead=new String(buff,0,readLength);
			is=ReverseProxyResponse.class.getResourceAsStream("authFoot.txt");
			readLength=is.read(buff);
			authFoot=new String(buff,0,readLength);
		} catch (IOException e) {
			logger.error("read error authHade authFoot",e);
		}
	}
	
	protected QueueletContext context;
	protected String close;
	protected String error;
	//protected boolean allowKeepAlive=false;
	
	private Map targetUrls=Collections.synchronizedMap(new HashMap());
//	private String targetServer;
//	private int targetPort;
	private String clientIp;//ここからのリクエストだけに対応する
//	private String authHeader=null;//="Authorization: Basic eXXXXXw==";
	
	private void notfound(HttpData httpData) throws IOException{
		logger.debug("not found");
		httpData.addResponseHeadr("Connection","close");
		httpData.writeResponse(404,"Not found");
		context.enque(httpData,close);
	}
	
	private void forbidden(HttpData httpData) throws IOException{
		logger.debug("forbidden");
		httpData.addResponseHeadr("Connection","close");
		httpData.writeResponse(403,"Forbidden");
		context.enque(httpData,close);
	}
	
	private void unauthorized(HttpData httpData,String targetURL) throws IOException{
		logger.debug("unauthorized");
		httpData.addResponseHeadr("WWW-Authenticate", "Basic realm=\"" + targetURL +"\"");
		httpData.addResponseHeadr("Connection","close");
		httpData.writeResponse(401,"Unauthorized:" + targetURL);
		context.enque(httpData,close);
	}
	
	private void ok(HttpData httpData) throws IOException{
		logger.debug("ok");
		httpData.addResponseHeadr("Connection","close");
		StringBuffer sb=new StringBuffer(authHead);
		Iterator itr=targetUrls.keySet().iterator();
		while(itr.hasNext()){
			String url=(String)itr.next();
			sb.append("<A href='/authDel?targetUrl=");
			sb.append(URLEncoder.encode(url, "utf-8"));
			sb.append("'>delete</A> ");
			sb.append(url);
			sb.append(" <BR/>");
		}
		sb.append(authFoot);
		httpData.writeResponse(200,sb.toString(),"text/html");
		context.enque(httpData,close);
	}
	
	private String getTargetUrl(String requestUrl) throws UnsupportedEncodingException{
		int pos=requestUrl.indexOf("targetUrl=");
		if( pos<0 ){
			return null;
		}
		String targetUrl=URLDecoder.decode(requestUrl.substring(pos+"targetUrl=".length()),"utf-8");
		return targetUrl;
	}
	
	private boolean checkTargetUrl(HttpData httpData,String targetUrl,String authValue) throws IOException{
		try{
			URL url=new URL(targetUrl);
			HttpURLConnection huc=(HttpURLConnection)url.openConnection();
			huc.setRequestMethod("HEAD");
			huc.addRequestProperty("Authorization", authValue);
			int code=huc.getResponseCode();
			if( code==401){
				unauthorized(httpData,targetUrl);
				return false;
			}else if(code!=200){
				forbidden(httpData);
				return false;
			}
		}catch(Throwable e){
			logger.debug("targetUrl check error:"+ targetUrl,e);
			forbidden(httpData);
			return false;
		}
		return true;
	}
	
	private boolean webServerResponse(HttpData httpData) throws IOException{
		String requestUri=httpData.getRequestUri();
		String targetUrl=getTargetUrl(requestUri);
		if(requestUri.startsWith("/authAdd")){
			if(targetUrl==null){
				ok(httpData);
				return true;
			}
			String authValue=httpData.getRequestHeader("Authorization");
			if( authValue==null){
				unauthorized(httpData,targetUrl);
				return true;
			}
			//チェック
//			URL url=new URL("http://" + targetServer + ":" + targetPort);
			if( checkTargetUrl(httpData,targetUrl,authValue)==false ){
				return true;
			}
			String authHeader="Authorization: " + authValue;
			targetUrls.put(targetUrl, authHeader);
		}else if(requestUri.startsWith("/authDel")){
			if(targetUrl!=null && targetUrls.containsKey(targetUrl)){
				targetUrls.remove(targetUrl);
			}
		}else if(requestUri.startsWith("/authStat")){
			if(targetUrl==null || !targetUrls.containsKey(targetUrl)){
				notfound(httpData);
			}
		}
		ok(httpData);
		return true;
	}
	
	private String getAuthHeader(String requestUrl){
		Iterator itr=targetUrls.keySet().iterator();
		while(itr.hasNext()){
			String url=(String)itr.next();
			if( requestUrl.startsWith(url) ){
				return (String)targetUrls.get(url);
			}
		}
		return null;
	}
	
	protected boolean sendResponse(HttpData httpData) throws IOException{
//		boolean rc=false;//正常系は、true,異常系は、false
		logger.debug(httpData.getRequestUri());
		
		/* 指定クライアント以外からのリクエストは、拒否 */
		if( clientIp!=null){
			InetAddress remote=httpData.getSocket().getInetAddress();
			if( !clientIp.equals(remote.getHostAddress())){
				forbidden(httpData);
				return true;
			}
		}
		
		String requestUri=httpData.getRequestUri();
		/* Webサーバへのリクエスト */
		if( requestUri.startsWith("/") ){
			return webServerResponse(httpData);
		}
		
		/* Proxyへのリクエスト */
		String method=httpData.getMethod();
		
		//当初GETだけを対象にしていたがボディの処理を追加
		if(!requestUri.startsWith("http://")){
			forbidden(httpData);
			return true;
		}
		
		/*http://直後の"/"を探す */
		URL url=new URL(requestUri);
		String server=url.getHost();
		int port=url.getPort();
		if(port<0){
			port=80;
		}
		String query=url.getQuery();
		String path=url.getPath();
		if( query!=null){
			path=url.getPath() +"?"+ query;
		}
		
		logger.debug("server:" + server);
		logger.debug("port:" + port);
		logger.debug("path:" + path);
		
		/* 対象のホスト：ポートのみをproxyする */
//		if( !targetServer.equals(server) || targetPort!=port){
//			forbidden(httpData);
//			return true;
//		}
		
		StringBuffer sb=new StringBuffer();
		sb.append(method);
		sb.append(" ");
		sb.append(path);
		sb.append(" HTTP/1.0\r\n");
		
		Iterator itr=httpData.getRawRequestHeaders();
		while(itr.hasNext()){
			String header=(String)itr.next();
			logger.debug("request header:" + header);
			if( header.toUpperCase().startsWith("Connection:") ){
				continue;//Keep-Aliveさせない
			}
			sb.append(header);
			sb.append("\r\n");
		}
//		sb.append("Connection: close\r\n");
		
		//認証ヘッダを追加
		String authHeader=getAuthHeader(requestUri);
		if(authHeader!=null){
			sb.append(authHeader);
			sb.append("\r\n");
		}
		sb.append("\r\n");
		
		String targetServer=httpData.getRequestServer();
		int targetPort=httpData.getRequestServerPort();
		Socket socket =new Socket(targetServer,targetPort);
		OutputStream os=socket.getOutputStream();
		os.write(sb.toString().getBytes());
		logger.debug("sendHeader"+sb.toString());
		
		//InputStream bodyStream=httpData.getRequestBodyStream();
		httpData.sendRequestBody(os);
		
		logger.debug("read start");
		os.flush();
		InputStream is=socket.getInputStream();
		httpData.responseStream(is);
		logger.debug("read end");
		
		os.close();
		is.close();
		httpData.closeSocket();
		context.enque(httpData,close);
		return true;
	}
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		boolean rc=false;//正常系は、true,異常系は、false
		HttpData httpData=(HttpData)req;
		try {
			rc=sendResponse(httpData);
		} catch (IOException e) {
			logger.error("Response IO error.",e);
			context.enque(httpData,close);
			return false;
		} catch (Exception e) {
			logger.error("Response error.",e);
			context.enque(httpData,close);
			return false;
		}
		return rc;
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletContext, java.util.Map)
	 */
	public void init(QueueletContext context, Map param) {
		this.context=context;
		this.close=(String)param.get("close");
		this.error=(String)param.get("error");
		
		//this.targetServer=(String)param.get("targetServer");
		//String targetPort=(String)param.get("targetPort");
		//this.targetPort=Integer.parseInt(targetPort);
		this.clientIp=(String)param.get("clientIp");
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}
