/*
 * Created on 2004/06/09
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package naru.queuelet.core;

import org.apache.log4j.Logger;

/**
 * @author naru
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ServiceThread implements Runnable {
	static private Logger logger=Logger.getLogger(ServiceThread.class.getName());

	private Terminal terminal;
	private Thread thread;
	
	public ServiceThread(Terminal terminal){
		this.terminal=terminal;
	}
	
	public void start(){
		thread=new Thread(this);
		int priority=terminal.getPriority();
		if(priority>Thread.MAX_PRIORITY){
			priority=Thread.MAX_PRIORITY;
		}
		if(priority<Thread.MIN_PRIORITY){
			priority=Thread.MIN_PRIORITY;
		}
		thread.setPriority(priority);
		String threadName="thread-" +terminal.getName() + ":" + terminal.getThreadId();
		thread.setName(threadName);
		logger.debug("start thread.name:"+threadName + ":priority:"+priority);
		thread.start();
	}
	
	public void suspend(){
		if( Thread.currentThread()==thread ){
			logger.warn("can not suspend this thread",new Exception());
			return;
		}
		thread.suspend();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		logger.debug("start");
		terminal.registerServiceThread(this);
		while(true){
			/* リクエスト待ち */
			Object req=terminal.deque();
			if(req==null){
				break;
			}
			try{
				/* 主処理 */
				terminal.service(req);
			}catch(Throwable serviceException){
				logger.warn("service exception:req="+req +",terminal="+ terminal.getName() ,serviceException);
			}finally{
				/* 必ず実行する後処理 */
			}
		}
		terminal.deregisterServiceThread(this);
		logger.debug("end");
	}
	
	public void dump(){
		if(!logger.isDebugEnabled()){
			return;
		}
		StackTraceElement[] traces=thread.getStackTrace();
		StringBuilder sb=new StringBuilder("###:"+thread.getName());
		for(int i=0;i<traces.length;i++){
			sb.append("\r\nat ");
			sb.append(traces[i]);
		}
		logger.debug(sb.toString());
	}

}
