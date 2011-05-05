/*
 * Created on 2004/11/19
 *
 * Container終了、状態監視デーモン
 * コンテナ機能の追い出し
 */
package naru.queuelet.core;

import java.util.Date;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * @author naru
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class QueueletDaemon implements Runnable {
	static private Logger logger=Logger.getLogger(QueueletDaemon.class);
	private Thread thread;
	private long endTime;
	private Container container;
	private int checkInterval;
	private String stopMode;
	private boolean stopCheck=false;
	private boolean stopManager=false;

	private boolean stopFlag = false;

	public void stopRequest() {
		logger.info("receive stopRequest");
		synchronized(this){
			stopFlag = true;
			notify();
		}
	}

	public boolean isStop() {
		return stopFlag;
	}
	
	QueueletDaemon(Container container){
		this.container=container;
		this.checkInterval=container.getCheckInterval();
		this.stopMode=container.getStopMode();
		logger.info("stopMode:"+stopMode);
		if( "check".equalsIgnoreCase(stopMode)){
			stopCheck=true;
			endTime=Long.MAX_VALUE;
		}else if( "manager".equalsIgnoreCase(stopMode)){
			endTime=Long.MAX_VALUE;
		}else if("infinity".equalsIgnoreCase(stopMode)){
			endTime=Long.MAX_VALUE;
		}else{
			int stopTime=Integer.parseInt(stopMode);
			if( checkInterval>=stopTime){
				checkInterval=stopTime;
			}
			endTime=System.currentTimeMillis()+(stopTime*1000);
			logger.info("stopMode time:"+stopTime+"s:" +new Date(endTime));
		}
	}
	
	public void start(){
		this.thread=new Thread(this);
		thread.setName("QueueletDaemon");
		thread.start();
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while(true){
			synchronized(this){
			if(isStop()){
				logger.info("manager stop");
				stopFlag = true;
				break;
			}
			try {
				wait(checkInterval*1000);
			} catch (InterruptedException e) {
				logger.warn("sleep interrupted.",e);
			}
			status();
			long now=System.currentTimeMillis();
			if( now>=endTime ){
				logger.info("time out stop");
				stopFlag = true;
				break;
			}
			if( stopCheck && stopCheck() ){
				logger.info("check stop");
				stopFlag = true;
				break;
			}
			}
		}
		container.stop();
	}
	
	public void terminalStatus(Terminal terminal, StringBuffer sb) {
		sb.append(",").append(terminal.getName()).append(":");//ターミナル名
		sb.append(terminal.getTotalInCount()).append("|");//enqueueトータル数
		sb.append(terminal.getTotalOutCount()).append("|");//dequeueトータル数
		sb.append(terminal.getMaxDelay()).append("|");//最大滞留時間
		sb.append(terminal.getQueLength()).append("|");//現在Queue長
		sb.append(terminal.getWaitThreadCount()).append("|");//待ちスレッド数
		sb.append(terminal.getThreadCount());//全スレッド数
	}

	public String getStatusString() {
		StringBuffer sb = new StringBuffer("Terminal status:");
		Iterator itr = container.getTermianlNameIterator();
		while (itr.hasNext()) {
			String name = (String) itr.next();
			Terminal terminal = container.getTerminal(name);
			terminalStatus(terminal, sb);
		}
		Runtime rt = Runtime.getRuntime();
		sb.append("[").append(rt.freeMemory()).append("/");
		sb.append(rt.totalMemory()).append("]");
		return sb.toString();
	}

	public void status() {
		logger.info(getStatusString());
	}
	
	private int inCountBefore=-1;
	private int outCountBefore=-1;
	public boolean stopCheck(){
		Iterator itr = container.getTermianlNameIterator();
		int inCountSum=0;
		int outCountSum=0;
		while (itr.hasNext()) {
			String name = (String) itr.next();
			Terminal terminal = container.getTerminal(name);
			inCountSum+=terminal.getTotalInCount();
			outCountSum+=terminal.getTotalOutCount();
		}
		if( inCountBefore==inCountSum && outCountBefore==outCountSum){
			return true;
		}
		inCountBefore=inCountSum;
		outCountBefore=outCountSum;
		return false;
	}
}
