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
import java.net.Socket;
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
public class ReverseProxyResponse implements Queuelet {
	static private Logger logger=Logger.getLogger(ReverseProxyResponse.class);
	protected QueueletContext context;
//	protected File docRoot;
//	protected String keepAlive;
	protected String close;
	protected String error;
	//	protected boolean allowKeepAlive=false;
	
	private String targetServer;
	private int targetPort;
	
	static protected Properties contentTypeProperties;
	static{
		InputStream is=null;
		try {
			contentTypeProperties=new Properties();
			is=ReverseProxyResponse.class.getResourceAsStream("contentType.properties");
			contentTypeProperties.load(is);
		} catch (IOException e) {
		}finally{
			if(is!=null){
				try {
					is.close();
				} catch (IOException ignore) {
				}
			}
		}
	}
	
	
	protected boolean sendResponse(HttpData1 httpData) throws IOException{
//		boolean rc=false;//ê≥èÌånÇÕÅAtrue,àŸèÌånÇÕÅAfalse
		logger.debug(httpData.getRequestUri());
		
		String method=httpData.getMethod();
		String nextTerminal=close;
		
		if(!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)){
			httpData.setStatusCode(403);
			httpData.addResponseHeader("Content-length","0");
			httpData.writeResponseHeader();
			context.enque(httpData,nextTerminal);
			return true;
		}
		StringBuffer sb=new StringBuffer();
		sb.append(method);
		sb.append(" ");
		String requestUri=httpData.getRequestUri();
		if( requestUri.startsWith("http://")){
			int pos=requestUri.indexOf("/","http://".length());
			if( pos>0 ){
				requestUri=requestUri.substring(pos);
			}
		}
		logger.debug("requestUri:" + requestUri);
		sb.append(requestUri);
		sb.append(" HTTP/1.0\r\n");
		
		HashMap requests=httpData.getRequestHeaders();
		Iterator itr=requests.keySet().iterator();
		int requestLength=0;
		while(itr.hasNext()){
			String key=(String)itr.next();
			if( key.equalsIgnoreCase("Connection") ){
				continue;//Keep-AliveÇ≥ÇπÇ»Ç¢
			}
			if( key.equalsIgnoreCase("Host") ){
				sb.append(key);
				sb.append(": ");
				sb.append(targetServer);
				sb.append("\r\n");
				continue;//HostÉwÉbÉ_ÇÃèëÇ´ä∑Ç¶
			}
			String value=(String)requests.get(key);
			logger.debug(key + ": " + value);
			sb.append(key);
			sb.append(": ");
			sb.append(value);
			sb.append("\r\n");
			if( key.equalsIgnoreCase("Content-length") ){
				requestLength=Integer.parseInt(value);
			}
		}
		sb.append("Connection: close\r\n");
		//ÉwÉbÉ_Çí«â¡
		sb.append("\r\n\r\n");
		
		
		
		
		Socket socket =new Socket(targetServer,targetPort);
		OutputStream os=socket.getOutputStream();
		os.write(sb.toString().getBytes());
		
		logger.debug("read start");
		os.flush();
		InputStream is=socket.getInputStream();
		httpData.writeResponseBody(is);
		logger.debug("read end");
		os.close();
		is.close();
		
		httpData.closeSocket();
		httpData.acclog();
		context.enque(httpData,nextTerminal);
		return true;
	}
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		boolean rc=false;//ê≥èÌånÇÕÅAtrue,àŸèÌånÇÕÅAfalse
		HttpData1 httpData=(HttpData1)req;
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
		
		this.targetServer=(String)param.get("targetServer");
		String targetPort=(String)param.get("targetPort");
		this.targetPort=Integer.parseInt(targetPort);
		
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}
