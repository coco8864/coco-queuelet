/*
 * Created on 2004/10/20
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.queuelet.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;
import naru.queuelet.core.QueueletDaemon;

/**
 * @author NARU
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Response implements Queuelet {
	static private Logger logger=Logger.getLogger(Response.class);
	protected QueueletContext context;
	protected File docRoot;
	protected boolean allowKeepAlive=false;
	
	static protected Properties contentTypeProperties;
	static{
		InputStream is=null;
		try {
			contentTypeProperties=new Properties();
			is=Response.class.getResourceAsStream("contentType.properties");
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
	
	protected boolean sendResponse(HttpData httpData) throws IOException{
		boolean rc=false;//ê≥èÌånÇÕÅAtrue,àŸèÌånÇÕÅAfalse
		String method=httpData.getMethod();
		if( allowKeepAlive && httpData.canKeepAlive()){
			httpData.addResponseHeadr("Connection","Keep-Alive");
		}
		if(!"GET".equalsIgnoreCase(method)){
			httpData.setStatusCode(403);
			httpData.addResponseHeadr("Content-length","0");
			httpData.writeResponseHeader();
			return true;
		}
		String requestUri=httpData.getRequestUri();
		if( requestUri.startsWith("/stop")){
			logger.info("manager receive container stop.");
			queueletDaemon.stopRequest();
			
			httpData.setStatusCode(200);
			httpData.addResponseHeadr("Content-Type","text/html; charset=shift_jis");
			httpData.writeResponseHeader();
			String status=queueletDaemon.getStatusString();
			httpData.writeResponseBody(status.getBytes("MS932"));
			return true;
		}else if(true){
			httpData.setStatusCode(200);
			httpData.addResponseHeadr("Content-Type","text/html; charset=shift_jis");
			httpData.writeResponseHeader();
			String status=queueletDaemon.getStatusString();
			httpData.writeResponseBody(status.getBytes("MS932"));
			return true;
		}
		
		File file=new File(docRoot,requestUri);
		if( file.canRead() && file.isFile() ){
			httpData.setStatusCode(200);
			httpData.addResponseHeadr("Content-length",Long.toString(file.length()));
			
			String contentType="text/html";
			int pos=requestUri.indexOf(".");
			if( pos>0 ){
				String ext=requestUri.substring(pos+1);
				String ct=contentTypeProperties.getProperty(ext);
				if( ct!=null){
					contentType=ct;
				}
			}
			httpData.addResponseHeadr("Content-Type",contentType);
			httpData.writeResponseHeader();
			
			try {
				httpData.writeResponseBody(new FileInputStream(file));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}else{
			httpData.setStatusCode(404);
			httpData.addResponseHeadr("Content-length","0");
			httpData.writeResponseHeader();
		}
		httpData.closeSocket();
		return true;
	}
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		boolean rc=false;//ê≥èÌånÇÕÅAtrue,àŸèÌånÇÕÅAfalse
		HttpData httpData=(HttpData)req;
		try {
			rc=sendResponse(httpData);
		} catch (IOException e) {
			logger.error("Response error.",e);
			return false;
		}
		httpData.closeSocket(true);
		return rc;
	}

	
	private QueueletDaemon queueletDaemon;
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletContext, java.util.Map)
	 */
	public void init(QueueletContext context, Map param) {
		this.context=context;
		String docRootString=(String)param.get("docRoot");
		if(docRootString!=null){
			this.docRoot=new File(docRootString);
			if( !docRoot.exists() ){
				throw new IllegalArgumentException("docRoot not exists.docRoot:"+docRoot);
			}
		}
		String allowKeepAliveString=(String)param.get("allowKeepAlive");
		if( allowKeepAliveString!=null){
			this.allowKeepAlive=allowKeepAliveString.equalsIgnoreCase("true");
		}
		this.queueletDaemon=(QueueletDaemon)param.get("QueueletDaemon");
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}
