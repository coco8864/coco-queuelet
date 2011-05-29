/*
 * Created on 2004/10/20
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.web;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;
import naru.util.HibernateUtil;

/**
 * @author NARU
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Replay implements Queuelet {
	static private Logger logger=Logger.getLogger(Replay.class);
	
	private QueueletContext context;
	private Config config;
//	private Configuration config;
	private File traceBaseDir;
//	private Map replayHistory;
	
	private AccessLog searchAccessLog(String method,String uri, String body,Set history){
		Session session=null;
		try {
			session=HibernateUtil.currentSession();
			Query query=null;
			if( body!=null){
				//2097152=AccessLog.QUEUE_REPLAY(0x00200000)を通過したリクエストは、対象にしない。
				//1179648=AccessLog.QUEUE_PROXY(0x00020000)|AccessLog.QUEUE_REVERSE(0x00100000)を通過したリクエストを、対象にする。
				query=session.createQuery("from AccessLog where responseFile is not null and BITAND(processQueue,1179648)!=0 and requestLine like :request and requestBody=:body order by id");
				query.setString("body", body);
			}else{
				query=session.createQuery("from AccessLog where responseFile is not null and BITAND(processQueue,1179648)!=0 and requestLine like :request and (requestBody is null or requestBody='') order by id");
			}
			query.setString("request", method + " " + uri + " %");
			Iterator itr=query.iterate();
			AccessLog log=null;
			while(itr.hasNext()){
				log=(AccessLog)itr.next();
				if( history.contains(log.getId()) ){
					continue;
				}
				history.add(log.getId());
				return log; 
			}
			return log;
		} catch (HibernateException e) {
			logger.error("fail to deleteAll",e);
			return null;
		}finally{
			if(session!=null){
				HibernateUtil.clearSession();
			}
		}
	}
	
	protected boolean sendResponse(HttpContext httpContext) throws IOException{
		logger.debug(httpContext.getRequestUri());

		//Bodyを検索するかどうかを判断
		HttpParser requestParser=httpContext.getRequestParser();
		long contentLength=requestParser.getContentLength();
		String contentType=requestParser.getContentType();
		String method=requestParser.getMethod();
		String uri=requestParser.getUri();
		String body=null;
		if( AccessLog.isRecodeBody(method, contentType, contentLength) ){
			AccessLog accessLog=httpContext.getAccessLog();
			StringBuffer bodyBuffer=new StringBuffer();
			requestParser.writeBody(null,bodyBuffer);
			body=bodyBuffer.toString();
			accessLog.setRequestBody(body);
		}else{
			//requestBodyの読み飛ばし
			requestParser.writeBody(null,null);
		}
		
		Set history=config.getReplayHistory(httpContext.getClientIp());
		if( history==null ){
			history=new HashSet();
			config.addReplayHistory(httpContext.getClientIp(), history);
		}
		AccessLog recodeLog=searchAccessLog(method,uri,body,history);
		if(recodeLog==null){
			httpContext.registerResponse("404","Replay not found:"+ uri);
			return false;
		}
		AccessLog accessLog=httpContext.getAccessLog();
		accessLog.setResponseFile("@"+recodeLog.getResponseFile());
		
		File responseFile=new File(traceBaseDir,recodeLog.getResponseFile());
		httpContext.setResponseStream(new BufferedInputStream(new FileInputStream(responseFile)));
		httpContext.registerResponse();
		
		/* レスポンスを実行 */
		return true;
	}
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		boolean rc=false;//正常系は、true,異常系は、false
		HttpContext httpContext=(HttpContext)req;
		httpContext.passQueue(AccessLog.QUEUE_REPLAY);
		
		try {
			rc=sendResponse(httpContext);
		} catch (IOException e) {
			logger.error("Replay IO error",e);
			httpContext.registerResponse("500","Replay IO error");			
		}finally{
			httpContext.startResponse();
		}
		return rc;
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletContext, java.util.Map)
	 */
	public void init(QueueletContext context, Map param) {
		this.config=Config.getInstance();
		this.context=context;
		this.traceBaseDir=new File(config.getConfiguration().getString("traceBaseDir"));
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}
