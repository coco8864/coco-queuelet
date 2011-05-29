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
			//method uri�͂��̂܂ܗ��p����
			requestParser.setReqHttpVersion("HTTP/1.0");
			//proxy�T�[�o��Keep-Alive�����Ȃ�
			requestParser.setHeader("Proxy-Connection", "close");
		}else{
			URL url=new URL(requestUri);
			String query=url.getQuery();
			String path=url.getPath();
			if( query!=null){
				path=url.getPath() +"?"+ query;
			}
			//method�͂��̂܂ܗ��p����
			requestParser.setUri(path);
			requestParser.setReqHttpVersion("HTTP/1.0");
			
			//Web�T�[�o��Keep-Alive�����Ȃ�
			requestParser.setHeader("Connection", "close");
		}
		String clientIp=httpContext.getClientIp();
		if(configuration.getBoolean(clientIp+".deleteIfModifiedSince",false)){
			//�u���E�U�L���b�V�����g�킹�Ȃ�
			requestParser.removeHeader(HttpContext.IF_MODIFIED_SINCE_HEADER);
			requestParser.removeHeader("If-None-Match");
		}
		
		//�F�؃w�b�_��ǉ�
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
		//���N�G�X�g��Peek���邩�ۂ��������Ō��߂�
		OutputStream os=socket.getOutputStream();
		requestParser.writeSeriarizeHeader(os);//���N�G�X�g�w�b�_�̑��M
		
		//Body���L�^���邩�ǂ����𔻒f
		long contentLength=requestParser.getContentLength();
		String contentType=requestParser.getContentType();
		String method=requestParser.getMethod();
		if( AccessLog.isRecodeBody(method, contentType, contentLength) ){
			//Body���L�^����
			AccessLog accessLog=httpContext.getAccessLog();
			StringBuffer peekBody=new StringBuffer();
			requestParser.writeBody(os,peekBody);//���N�G�X�g�{�f�B�̑��M
			accessLog.setRequestBody(peekBody.toString());
		}else{
			requestParser.writeBody(os);//���N�G�X�g�{�f�B�̑��M
		}
		os.flush();
		httpContext.checkPoint();
		httpContext.setResponseStream(new BufferedInputStream(socket.getInputStream()));//���X�|���X�̎�M
		httpContext.checkPoint();
	}
	
	protected boolean sendResponse(HttpContext httpContext){
		logger.debug(httpContext.getRequestUri());
		
		/* domain����proxy���邩�ۂ��𔻒� */
		String targetServer=httpContext.getRequestServer();
		int targetPort=httpContext.getRequestServerPort();
		boolean useNextProxy=config.isUseProxy(targetServer);
		if(useNextProxy){
			targetServer=proxyServer;
			targetPort=proxyPort;
		}
		
		/* ���N�G�X�g���\�z */
		HttpParser proxyRequest;
		try {
			proxyRequest = nextRequestHeader(httpContext,useNextProxy);
		} catch (MalformedURLException e) {
			String errMsg="fail to proxy crate next header:" + httpContext.getRequestUri();
			logger.error(errMsg,e);
			httpContext.registerResponse("500",errMsg);
			return false;
		}
		
		/* ���N�G�X�g�����s�A���X�|���X���󂯎�� */
		try {
			requestNext(httpContext,targetServer,targetPort,proxyRequest);
		} catch (IOException e) {
			String errMsg="fail to proxy next server.server:" + targetServer + " port:"+targetPort;
			logger.error(errMsg,e);
			httpContext.registerResponse("500",errMsg);
			return false;
		}
		
		/* ���X�|���X�����s */
		//�u���E�U��Keep-Alive�����Ȃ��Ώ����s��
		httpContext.setResponseHeader("Proxy-Connection", "close");
		httpContext.registerResponse();
		return true;
	}
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		boolean rc=false;//����n�́Atrue,�ُ�n�́Afalse
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
