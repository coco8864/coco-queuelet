/*
 * Created on 2004/10/20
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.web;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

/**
 * @author NARU
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Proxy implements Queuelet {
	static private Logger logger=Logger.getLogger(Proxy.class);
	
	private QueueletContext context;
	private Config config;
	private Configuration configuration;
	private String proxyServer;
	private int proxyPort;
//	private String authHeader=null;//="Authorization: Basic eXXXXXw==";
//	private Map authHeaders;
//	private List exceptProxyDomains;
	
	private HttpParser nextRequestHeader(HttpContext httpContext,boolean useNextProxy) throws MalformedURLException{
		HttpParser requestParser=httpContext.getRequestParser();
		String requestUri=requestParser.getUri();
		if(useNextProxy){
			//method uriはそのまま利用する
			requestParser.setReqHttpVersion("HTTP/1.0");
			//proxyサーバにKeep-Aliveさせない
			requestParser.setHeader("Proxy-Connection", "close");
		}else{
			URL url=new URL(requestUri);
			String query=url.getQuery();
			String path=url.getPath();
			if( query!=null){
				path=url.getPath() +"?"+ query;
			}
			//methodはそのまま利用する
			requestParser.setUri(path);
			requestParser.setReqHttpVersion("HTTP/1.0");
			
			//WebサーバにKeep-Aliveさせない
			requestParser.setHeader("Connection", "close");
		}
		String clientIp=httpContext.getClientIp();
		if(configuration.getBoolean(clientIp+".deleteIfModifiedSince",false)){
			//ブラウザキャッシュを使わせない
			requestParser.removeHeader(HttpContext.IF_MODIFIED_SINCE_HEADER);
			requestParser.removeHeader("If-None-Match");
		}
		
		//認証ヘッダを追加
		String authHeader=config.getAuthHeader(requestUri);
		if(authHeader!=null){
			requestParser.addRawHeader(authHeader);
		}
		return requestParser;
	}
	
	private void requestNext(HttpContext httpContext,String server,int port,HttpParser requestParser) throws IOException{
		Socket socket =new Socket(server,port);
		httpContext.setSecondSocket(socket);
		socket.setSoTimeout(configuration.getInt("socketReadTimeout", 10000));
		//リクエストをPeekするか否かをここで決める
		OutputStream os=socket.getOutputStream();
		requestParser.writeSeriarizeHeader(os);//リクエストヘッダの送信
		
		//Bodyを記録するかどうかを判断
		long contentLength=requestParser.getContentLength();
		String contentType=requestParser.getContentType();
		String method=requestParser.getMethod();
		if( AccessLog.isRecodeBody(method, contentType, contentLength) ){
			//Bodyを記録する
			AccessLog accessLog=httpContext.getAccessLog();
			StringBuffer peekBody=new StringBuffer();
			requestParser.writeBody(os,peekBody);//リクエストボディの送信
			accessLog.setRequestBody(peekBody.toString());
		}else{
			requestParser.writeBody(os);//リクエストボディの送信
		}
		os.flush();
		httpContext.checkPoint();
		httpContext.setResponseStream(new BufferedInputStream(socket.getInputStream()));//レスポンスの受信
		httpContext.checkPoint();
	}
	
	protected boolean sendResponse(HttpContext httpContext){
		logger.debug(httpContext.getRequestUri());
		
		/* domainからproxyするか否かを判定 */
		String targetServer=httpContext.getRequestServer();
		int targetPort=httpContext.getRequestServerPort();
		boolean useNextProxy=config.isUseProxy(targetServer);
		if(useNextProxy){
			targetServer=proxyServer;
			targetPort=proxyPort;
		}
		
		/* リクエストを構築 */
		HttpParser proxyRequest;
		try {
			proxyRequest = nextRequestHeader(httpContext,useNextProxy);
		} catch (MalformedURLException e) {
			String errMsg="fail to proxy crate next header:" + httpContext.getRequestUri();
			logger.error(errMsg,e);
			httpContext.registerResponse("500",errMsg);
			return false;
		}
		
		/* リクエストを実行、レスポンスを受け取る */
		try {
			requestNext(httpContext,targetServer,targetPort,proxyRequest);
		} catch (IOException e) {
			String errMsg="fail to proxy next server.server:" + targetServer + " port:"+targetPort;
			logger.error(errMsg,e);
			httpContext.registerResponse("500",errMsg);
			return false;
		}
		
		/* レスポンスを実行 */
		//ブラウザにKeep-Aliveさせない対処を行う
		httpContext.setResponseHeader("Proxy-Connection", "close");
		httpContext.registerResponse();
		return true;
	}
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		boolean rc=false;//正常系は、true,異常系は、false
		HttpContext httpContext=(HttpContext)req;
		httpContext.passQueue(AccessLog.QUEUE_PROXY);
		
		httpContext.checkPoint();
		try {
			rc=sendResponse(httpContext);
		}finally{
			httpContext.checkPoint();
			httpContext.startResponse();
		}
		return rc;
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletContext, java.util.Map)
	 */
	public void init(QueueletContext context, Map param) {
		this.config=Config.getInstance();
		this.configuration=config.getConfiguration();
		this.context=context;
		this.proxyServer=configuration.getString("proxyServer");
		if(proxyServer!=null){
			this.proxyPort=configuration.getInt("proxyPort");
		}
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}
