/*
 * Created on 2004/10/20
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.web;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

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
public class Reverse implements Queuelet {
	static private Logger logger=Logger.getLogger(Reverse.class);
	
	private QueueletContext context;
	private Config config;
	private Configuration configuration;
	private String proxyServer;
	private int proxyPort;
	private SSLSocketFactory sslSocketFactory;
	private String sslProxyServer;
	private int sslProxyPort;
	
//	private String authHeader=null;//="Authorization: Basic eXXXXXw==";
	
	private HttpParser nextRequestHeader(HttpContext httpContext,boolean useHttpProxy,String requestUri,String hostHeader) throws MalformedURLException{
		HttpParser requestParser=httpContext.getRequestParser();
		if(useHttpProxy){
			//method はそのまま利用する
			requestParser.setUri(requestUri);
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
		//HOSTヘッダ書き換え
		requestParser.setHeader("Host", hostHeader);
		
		String clientIp=httpContext.getClientIp();
		if(configuration.getBoolean(clientIp+".deleteIfModifiedSince",false)){
			//ブラウザキャッシュを使わせない
			requestParser.removeHeader(HttpContext.IF_MODIFIED_SINCE_HEADER);
		}
		if(configuration.getBoolean(clientIp+".deleteReferer",false)){
			//リファラーヘッダを削除
			requestParser.removeHeader("Referler");
		}
		
		//認証ヘッダを追加
		String authHeader=config.getAuthHeader(requestUri);
		if(authHeader!=null){
			requestParser.addRawHeader(authHeader);
		}
		return requestParser;
	}
	
	private void requestNext(HttpContext httpContext,Socket secondSocket,HttpParser requestParser) throws IOException{
		httpContext.setSecondSocket(secondSocket);
		secondSocket.setSoTimeout(configuration.getInt("socketReadTimeout", 10000));
		//リクエストをPeekするか否かをここで決める
		OutputStream os=secondSocket.getOutputStream();
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
		httpContext.setResponseStream(new BufferedInputStream(secondSocket.getInputStream()));//レスポンスの受信
		httpContext.checkPoint();
	}
	
	protected boolean sendResponse(HttpContext httpContext){
		boolean useSsl=false;//SSL通信か？
		boolean useProxy=false;//Proxyを使用するか？
		boolean useHttpProxy=false;//HTTP Proxyを使用するか？
		
		logger.debug(httpContext.getRequestUri());
		MappingEntry mappingEntry=(MappingEntry)httpContext.getAttribute(HttpContext.ATTRIBUTE_MAPPING_ENTRY);
		String url=httpContext.getRequestUri();
		String fullUri=mappingEntry.getFullPath(url);
		
		URI uri=(URI)mappingEntry.getDestinationObject();
		/* domainからproxyするか否かを判定 */
		String targetServer=uri.getHost();
		useProxy=config.isUseProxy(targetServer);
		if( "https".equals(uri.getScheme())){
			useSsl=true;
		}else{
			useHttpProxy=useProxy;
		}
		int targetPort=uri.getPort();
		if(targetPort<0){
			if(useSsl){
				targetPort=443;
			}else{
				targetPort=80;
			}
		}
		
		//redirectを書き戻すために保存
		String orgHostHeader=httpContext.getRequestHeader("Host");
		
		/* リクエストを構築 */
		HttpParser proxyRequest;
		try {
			//ホストヘッダの書き換え
			proxyRequest = nextRequestHeader(httpContext,useHttpProxy,fullUri,targetServer + ":" + targetPort);
		} catch (MalformedURLException e) {
			String errMsg="fail to proxy crate next header:" + httpContext.getRequestUri();
			logger.error(errMsg,e);
			httpContext.registerResponse("500",errMsg);
			return false;
		}
		
		/* リクエストを実行、レスポンスを受け取る */
		try {
			Socket secondSocket=createSocket(targetServer,targetPort,useSsl,useProxy,httpContext.getResponseParser());
			requestNext(httpContext,secondSocket,proxyRequest);
		} catch (IOException e) {
			String errMsg="fail to request server." + e.toString()+" server:"  + targetServer + " port:"+targetPort;
			logger.error(errMsg,e);
			httpContext.registerResponse("500",errMsg);
			return false;
		}
		
		//リダイレクトの場合、自分に帰ってくるようにlocationヘッダを設定
		HttpParser responseParser=httpContext.getResponseParser();
		String statusCode=responseParser.getStatusCode();
		if("302".equals(statusCode)||"301".equals(statusCode)){
			String location=responseParser.getHeader(HttpContext.LOCATION_HEADER);
			String destination=mappingEntry.getDestination();
			String source=mappingEntry.getSourcePath();
			String newLocation=location.replaceFirst("^" + destination, "http://" + orgHostHeader +source);
			if(!location.equals(newLocation)){
				responseParser.setHeader(HttpContext.LOCATION_HEADER, newLocation);
			}
		}
		//else if("200".equals(statusCode))
		//本当は、コンテンツも書き換えたいが・・・・
		
		/* レスポンスを実行 */
		//if(useSsl)
		//TODO Set-Cookieヘッダのsecure属性を削除
		
		//ブラウザにKeep-Aliveさせない対処を行う
		httpContext.setResponseHeader("Proxy-Connection", "close");
		httpContext.registerResponse();
		return true;
	}
	
	private Socket createSocket(String targetServer, int targetPort,
			boolean useSsl, boolean useProxy,HttpParser responsParser) throws IOException {
		if(!useSsl && !useProxy ){//直接Webサーバと会話
			return new Socket(targetServer,targetPort);
		}else if(!useSsl && useProxy ){//HTTP Proxy経由でWebサーバと会話
			return new Socket(proxyServer,proxyPort);
		}else if(useSsl&&!useProxy){//直接HTTPSサーバと会話
			 return sslSocketFactory.createSocket( targetServer , targetPort );
		}
		//HTTPS proxy経由でHTTPSサーバと会話
		Socket socket=new Socket(sslProxyServer,sslProxyPort);
		OutputStream pos=socket.getOutputStream();
		pos.write(("CONNECT " + targetServer + ":" + targetPort + " HTTP/1.0\r\n").getBytes());
		pos.write("\r\n".getBytes());
		InputStream is=socket.getInputStream();
		responsParser.parse(is);//proxyからのレスポンスを受信
		is=responsParser.getBodyStream();
		responsParser.recycle();
		
		return sslSocketFactory.createSocket(socket,targetServer,targetPort,true);		
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		boolean rc=false;//正常系は、true,異常系は、false
		HttpContext httpContext=(HttpContext)req;
		httpContext.passQueue(AccessLog.QUEUE_REVERSE);
		
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
		this.context=context;
		this.config=Config.getInstance();
		this.configuration=config.getConfiguration();
		this.context=context;
		this.proxyServer=configuration.getString("proxyServer");
		if(proxyServer!=null){
			this.proxyPort=configuration.getInt("proxyPort");
		}
		this.sslProxyServer=configuration.getString("sslProxyServer");
		if(sslProxyServer!=null){
			this.sslProxyPort=configuration.getInt("sslProxyPort");
		}
		
		String trustStore=(String)param.get("trustStore");
		if(trustStore==null){//SSL定義が無い場合は、ここでおしまい
			return;
		}
		String trustStorePassword=(String)param.get("trustStorePassword");
		
		// トラストストア設定
		System.setProperty("javax.net.ssl.trustStore" , trustStore );
		System.setProperty("javax.net.ssl.trustStorePassword",trustStorePassword  );
		try {
			KeyStore ks = KeyStore.getInstance ( "JKS" );
			char[] keystorePass =trustStorePassword.toCharArray();
			ks.load ( new FileInputStream( trustStore ) , keystorePass );
			KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
			kmf.init(ks , keystorePass  );
			SSLContext ctx = SSLContext.getInstance ( "TLS" );
			ctx.init( kmf.getKeyManagers()  , null , null );
			// SSLSocket生成
			sslSocketFactory  = ctx.getSocketFactory();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}
