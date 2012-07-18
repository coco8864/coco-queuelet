/*
 * Created on 2004/11/20
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.queuelet.store;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import naru.queuelet.core.QueueletProperties;
import naru.queuelet.core.Terminal;

import org.apache.log4j.Logger;

/**
 * @author NARU
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class QueueletStore implements Runnable{
	static private Logger logger=Logger.getLogger(QueueletStore.class.getName());
	private QueueletProperties queueletProperties;
	private String jdbcDriver="org.hsqldb.jdbcDriver";
	private String jdbcUrl="jdbc:hsqldb:data/queueStore";
	private String jdbcUser="sa";
	private String jdbcPassword="";
	private QueueStore queueStore;
	private ClassLoader loader=null;
	
	private boolean startupServer=false;
	private String type=null;
	private String port=null;
	private String host="localhost";
	private boolean refresh;
	private int interval;
	private String loaderName=null;

	
	/**
	 * @param loaderName to set.
	 */
	public void setLoaderName(String loaderName) {
		this.loaderName = loaderName;
	}
	
	/**
	 * @return Returns the loaderName.
	 */
	public String getLoaderName() {
		return loaderName;
	}
	
	/**
	 * @param interval The interval to set.
	 */
	public void setInterval(int interval) {
		this.interval = interval;
	}
	/**
	 * @return Returns the host.
	 */
	public String getHost() {
		return host;
	}
	/**
	 * @param host The host to set.
	 */
	public void setHost(String host) {
		this.host = host;
	}
	/**
	 * @return Returns the port.
	 */
	public String getPort() {
		return port;
	}
	/**
	 * @param port The port to set.
	 */
	public void setPort(String port) {
		this.port = port;
	}
	/**
	 * @return Returns the refresh.
	 */
	public boolean isRefresh() {
		return refresh;
	}
	/**
	 * @param refresh The refresh to set.
	 */
	public void setRefresh(boolean refresh) {
		this.refresh = refresh;
	}
	/**
	 * @return Returns the starupServer.
	 */
	public boolean isStartupServer() {
		return startupServer;
	}
	/**
	 * @param starupServer The starupServer to set.
	 */
	public void setStartupServer(boolean starupServer) {
		this.startupServer = starupServer;
	}
	/**
	 * @return Returns the type.
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type The type to set.
	 */
	public void setType(String type) {
		this.type = type;
	}

	public void init(QueueletProperties queueletProperties){
		init(queueletProperties,null);
	}
	
	
	public void init(QueueletProperties queueletProperties,Terminal terminal){
		this.queueletProperties=queueletProperties;
		this.terminal=terminal;
		if( loaderName!=null ){
			this.loader=terminal.getContainer().getLoader(loaderName);
		}else{
			this.loader=QueueletStore.class.getClassLoader();
		}

		/* 自力でDBを立ち上げた場合 Hostは、自分、typeは、Server */
		if( isStartupServer() ){
			setHost("127.0.0.1");
			setType("Server");
		}
		
		if( terminal!=null){
			this.terminalName=terminal.getName();
			this.maxMemQueueLength=terminal.getMaxQueueLength();
		}
		Properties runtime=new Properties();
		if( isStartupServer() ){
			if( port!=null ){
				queueletProperties.setSysProperty("queuelet.store.serverPort",port);
			}
			if( host!=null){
				queueletProperties.setSysProperty("queuelet.host.serverPort",host);
			}
		}
		if("server".equalsIgnoreCase(type)){
			if( port!=null ){
				runtime.setProperty("queuelet.store.serverPort",port);
			}
			if( host!=null){
				runtime.setProperty("queuelet.host.serverPort",host);
			}
			this.jdbcDriver=queueletProperties.getProperty("store.server.driver",runtime);
			this.jdbcUrl=queueletProperties.getProperty("store.server.url",runtime);
			this.jdbcUser=queueletProperties.getProperty("store.server.user",runtime);
			this.jdbcPassword=queueletProperties.getProperty("store.server.password",runtime);
		}else{//"inProcess"の場合
			this.jdbcDriver=queueletProperties.getProperty("store.inProcess.driver");
			this.jdbcUrl=queueletProperties.getProperty("store.inProcess.url");
			this.jdbcUser=queueletProperties.getProperty("store.inProcess.user");
			this.jdbcPassword=queueletProperties.getProperty("store.inProcess.password");
		}
		queueStore=new QueueStore(queueletProperties);
		queueStore.init(jdbcDriver,jdbcUrl,jdbcUser,jdbcPassword,loader);
	}
	
	public void term(){
		if( queueStore==null ){
			return;
		}
		queueStore.term();
		queueStore=null;
	}
	
	/*------ Teaminalから使用される場合 -------------*/
	private Terminal terminal;
	private String terminalName;
	private boolean stop=false;
	private Thread timer;
	
	private int maxMemQueueLength;
	
	public void start(){
		if( isRefresh() ){
			queueStore.drop(terminalName);
			queueStore.create(terminalName);
		}else if( !queueStore.check(terminalName) ){
			queueStore.create(terminalName);
		}
		storeQueueLength=queueStore.getLength(terminalName);
		
		/* メモリにQueueできないなら監視の必要もない */
		if( maxMemQueueLength==0 ){
			return;
		}
		this.stop=false;
		this.timer=new Thread(this);
		this.timer.start();
	}

	public void stop(){
		if( timer==null ){
			return;
		}
		this.stop=true;
		this.timer.interrupt();
		try {
			this.timer.join();
		} catch (InterruptedException e) {
			logger.error("join Interrupted.",e);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while(true){
			if(stop){
				break;
			}
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				logger.debug("sleep Interrupted.",e);
			}
			flushQueue();
		}
	}
	
	/* 現在storeに幾つQueueされているか？inProcessの場合有効 */
	private int storeQueueLength=0;
	public synchronized void enqueStore(Object queEntry) {
		if( maxMemQueueLength==0 ){
			queueStore.enque(queEntry,terminalName);
			return;
		}
		int curQueueLength=flushQueue();
		if( curQueueLength>= maxMemQueueLength){/* 余裕がないので蓄積する */
			queueStore.enque(queEntry,terminalName);
			storeQueueLength++;
		}else{/* 余裕があるのでそのままenqueする */
			terminal.enqueMem(queEntry);
		}
	}
	
	public synchronized int flushQueue(){
		int memQueueLength=0;
		memQueueLength=terminal.getQueLength();
		logger.debug("in flushQueue.memQueueLength="+ memQueueLength);
		
		int dequeCount=0;
		if( memQueueLength<maxMemQueueLength){
			dequeCount=maxMemQueueLength-memQueueLength;
			if( storeQueueLength < dequeCount ){
				dequeCount=storeQueueLength;
			}
			logger.debug("flushQueue.dequeCount="+ dequeCount);
			/* 蓄積からenque */
			List queueList=queueStore.deque(dequeCount,terminalName);
			Iterator itr=queueList.iterator();
			while(itr.hasNext()){
				storeQueueLength--;
				terminal.enqueMem(itr.next());
				memQueueLength++;
			}
		}
		logger.debug("out flushQueue.memQueueLength="+ memQueueLength);
		return memQueueLength;/* 現在のQueue長を返す */
	}
	
	
	/*------ Conteainrから使用される場合 -------------*/
	/** 
	 *  リクエスト受付 
	 */
	public synchronized void enque(Object queEntry,String terminal) {
		queueStore.enque(queEntry,terminal);
	}
	
	/* Containerから定義なしTerminalからの読み込みを要求された場合 */
	public synchronized Object deque(String terminal) {
		Object queEntry=null;
		while(true){
			List list=queueStore.deque(1,terminal);
			if( list.size()==0 ){
				queEntry=list.get(0);
				break;
			}
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				logger.warn("sleep interrupt.",e);
			}
		}
		return queEntry;
	}
	
	public boolean shutdownSore(){
		return queueStore.shutdown();
	}
}
