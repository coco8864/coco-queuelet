/*
 * Created on 2004/10/20
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.web;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

/**
 * @author NARU
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class VelocityPage implements Queuelet {
	static private Logger logger=Logger.getLogger(VelocityPage.class);
	private static String CONTENT_TYPE="text/html; charset=utf-8";
	private QueueletContext context;
	private Config config;
	private File controllerDocRoot;
	
	private VelocityContext createVeloContext(HttpContext httpContext){
		VelocityContext veloContext=new VelocityContext();
		veloContext.put("httpContext", httpContext);
		veloContext.put("config", config);
		veloContext.put("configuration", config.getConfiguration());
		Iterator itr=httpContext.getAttributeNames();
		while(itr.hasNext()){
			String key=(String)itr.next();
			Object value=httpContext.getAttribute(key);
			veloContext.put(key, value);
		}
		return veloContext;
	}
	
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		HttpContext httpContext=(HttpContext)req;
		httpContext.passQueue(AccessLog.QUEUE_VELOPAGE);
		
		httpContext.setResponseHeader("Connection","close");//Keep-Aliveしない
		httpContext.setNoCacheResponseHeaders();//動的コンテンツなのでキャッシュさせない
		
		String veloPage=(String)httpContext.getAttribute(HttpContext.ATTRIBUTE_VELOCITY_PAGE);
		File file=new File(controllerDocRoot,veloPage);
		if(!file.exists()||!file.canRead()){
			httpContext.registerResponse("404","Not Found."+veloPage);
			httpContext.startResponse();//レスポンス開始
			return false;
		}
		VelocityContext veloContext=createVeloContext(httpContext);
		Writer out=httpContext.getResponseOut("200",CONTENT_TYPE);
		if(out==null){
			return false;
		}
		
		try {
			Velocity.mergeTemplate(veloPage, "utf-8", veloContext, out);
		} catch (ResourceNotFoundException e) {
			logger.error("Velocity.mergeTemplate ResourceNotFoundException." + veloPage,e);
		} catch (ParseErrorException e) {
			logger.error("Velocity.mergeTemplate ParseErrorException." + veloPage,e);
		} catch (MethodInvocationException e) {
			logger.error("Velocity.mergeTemplate MethodInvocationException." + veloPage,e);
		} catch (Exception e) {
			logger.error("Velocity.mergeTemplate Exception." + veloPage,e);
		}finally{
			try {
				out.close();
			} catch (IOException ignore) {
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletContext, java.util.Map)
	 */
	public void init(QueueletContext context, Map param) {
		this.context=context;
		this.config=Config.getInstance();
		String velocityRootString=config.getConfiguration().getString("controllerDocRoot");
		if(velocityRootString!=null){
			this.controllerDocRoot=new File(velocityRootString);
			if( !controllerDocRoot.exists() ){
				throw new IllegalArgumentException("controllerDocRoot not exists.docRoot:"+controllerDocRoot);
			}
		}else{
			throw new IllegalArgumentException("controllerDocRoot is null");
		}
		try {
			Velocity.setProperty("file.resource.loader.path", velocityRootString);
			Velocity.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.SimpleLog4JLogSystem");
			Velocity.setProperty("runtime.log.logsystem.log4j.category", "velocity");
			Velocity.init();
		} catch (Exception e) {
			logger.error("Velocity.init error",e);
			throw new IllegalArgumentException("Velocity.init error");
		}
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}
}
