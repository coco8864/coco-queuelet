/*
 * 作成日: 2004/07/21
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;
import naru.queuelet.util.CallThreadUtil;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class QueueletWrapper implements QueueletContext{
	static private Logger logger=Logger.getLogger(QueueletWrapper.class);
	
	static private Properties typedClasses;
	static{
		InputStream is=null;
		try {
			typedClasses=new Properties();
			is=QueueletWrapper.class.getResourceAsStream("typedClasses.properties");
			typedClasses.load(is);
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
	
	public QueueletWrapper(){
		logger.debug("new QueletWrapper");
	}
	
	private String type;
	private String className;
	private String factoryClassName;
	private String factoryMethodName;
	
	private String nextTerminalName;
	private String memberClassName;
	private String loaderName;
	
	
	private Map param=new HashMap();

	private Container container;
	private Class memberClass=null;
	private Queuelet quelet=null;
	private Terminal nextTerminal=null;
	private Terminal terminal=null;
	private ClassLoader loader=null;

	public void putParam(String name,String value){
		param.put(name,value);
	}

	/**
	 * @return
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * @return
	 */
	public String getMemberClassName() {
		return memberClassName;
	}

	/**
	 * @return
	 */
	public String getNextTerminal() {
		return nextTerminalName;
	}

	/**
	 * @return
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param string
	 */
	public void setClassName(String string) {
		className = string;
	}

	/**
	 * @param string
	 */
	public void setMemberClassName(String string) {
		memberClassName = string;
	}

	/**
	 * @param string
	 */
	public void setNextTerminal(String string) {
		nextTerminalName = string;
	}

	/**
	 * @param string
	 */
	public void setType(String string) {
		type = string;
	}

	/**
	 * @param string
	 */
	public void setLoaderName(String string) {
		loaderName = string;
	}

	/**
	 * @param Terminal
	 */
	public void setTerminal(Terminal terminal){
		this.terminal=terminal;
	}
	
	public void setFactoryClassName(String factoryClassName) {
		this.factoryClassName = factoryClassName;
	}

	public void setFactoryMethodName(String factoryMethodName) {
		this.factoryMethodName = factoryMethodName;
	}
	

	private Queuelet instantiateQueuelet() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException{
		/* classNameが指定された場合 */
		if(className!=null){
			if(loader==null || type!=null){
				return (Queuelet)Class.forName(className).newInstance();
			}else{
				Class cl=loader.loadClass(className);
				ClassLoader cloader=cl.getClassLoader();
				return (Queuelet)cl.newInstance();
			}
		}
		Class factoryClass=null;
		if(loader==null){
			factoryClass=Class.forName(factoryClassName);
		}else{
			factoryClass=loader.loadClass(factoryClassName);
		}
		Class[] paramTypes = new Class[0];
		Object[] paramValues = new Object[0];
		Method method = factoryClass.getMethod(factoryMethodName, paramTypes);
		return (Queuelet)method.invoke(null, paramValues);
	}
	
	public void init(Container container) {
		logger.debug("QueletWrapper init in.type:"+ type +",className:"+className);
		this.container=container;
		this.loader=container.getLoader(loaderName);
		if(nextTerminalName!=null){
			this.nextTerminal=container.getTerminal(nextTerminalName);
		}else{
			//nestTreaminalを省略した場合は自termainalを設定
			this.nextTerminal=this.terminal;
		}

		/* paramのvalue側を property変換 */
		Iterator itr=param.keySet().iterator();
		while(itr.hasNext()){
			Object key=itr.next();
			String value=(String)param.get(key);
			String convValue=container.resolveProperty(value);
			param.put(key,convValue);
		}
		
		if( type!=null){
			className=(String)typedClasses.get(type);
		}else{
			className=container.resolveProperty(className);
		}
		factoryClassName=container.resolveProperty(factoryClassName);
		factoryMethodName=container.resolveProperty(factoryMethodName);
		
		param.put("QueueletDaemon",container.getQueueletDaemon());
		param.put("QueueletLoader",loader);
		param.put("QueueletArgs",container.getArgs());
		if(terminal!=null){
			param.put("thisTerminal",terminal.getName());
		}
		
		/* コンテキストクラスローダを変更する */
		ClassLoader orgCl=null;
		Thread curThread=Thread.currentThread();
		if( loader!=null){
			orgCl=curThread.getContextClassLoader();
			curThread.setContextClassLoader(loader);
		}
		
		try {
			quelet=instantiateQueuelet();
			if(memberClassName!=null){
				if(loader==null){
					memberClass=Class.forName(memberClassName);
				}else{
					memberClass=loader.loadClass(memberClassName);
				}
			}
		} catch (Exception e) {
			logger.error("fail to init QueueletWrapper memberClassName:"+memberClassName,e);
			throw new IllegalStateException("fail to init QueueletWrapper memberClassName:"+memberClassName);
		}
		
		try{
			quelet.init(this,param);
		}catch(Throwable t){
			String terminalName="[root]";
			if(terminal!=null){
				terminalName=terminal.getName();
			}
			//TODO initに失敗(例外)した場合、containerの起動を失敗させた方がよいのではないか？
			logger.warn("init exception:terminal="+ terminalName ,t);
		}finally{
			if( orgCl!=null){
				curThread.setContextClassLoader(orgCl);
			}
		}
	}
	
	/**
	 * 
	 */
	public void term(){
		/* コンテキストクラスローダを変更する */
		ClassLoader orgCl=null;
		Thread curThread=Thread.currentThread();
		if( loader!=null){
			orgCl=curThread.getContextClassLoader();
			curThread.setContextClassLoader(loader);
		}
		try{
			quelet.term();
		}catch(Throwable t){
			String tName="[no terminal]";
			if(terminal!=null){
				tName=terminal.getName();
			}
			logger.warn("term exception:terminal="+ tName ,t);
		}finally{
			if( orgCl!=null){
				curThread.setContextClassLoader(orgCl);
			}
		}
	}

	/**
	 * 
	 */
	public boolean service(Object req) {
		logger.debug("service req:"+req.getClass().getName());
		if( req!=null && 
			memberClass!=null && 
			memberClass.isInstance(req)==false ){
			logger.debug("skip");
			return true;
		}
		logger.debug("call quelet service");
		
		Thread curThread=Thread.currentThread();
		ClassLoader orgCl=curThread.getContextClassLoader();
		curThread.setContextClassLoader(loader);
		boolean rc=false;
		try{
			rc=quelet.service(req);
		}finally{
			curThread.setContextClassLoader(orgCl);
		}
		logger.debug("return quelet service");
		return rc;
	}
	
	/* QueletCommand interface */
	/* (非 Javadoc)
	 * @see naru.quelet.QueletCommand#enqueNext(java.lang.Object)
	 */
	public void enque(Object req) {
		if( nextTerminal!=null ){
			if(terminal!=null){
				terminal.addTotalOutCount();
			}
			nextTerminal.enque(req);
		}
	}

	/* (非 Javadoc)
	 * @see naru.quelet.QueletCommand#finish()
	 */
	public void finish() {
		finish(false,-1,null);
	}
	public void finish(boolean restart) {
		finish(restart,-1,null);
	}
	public void finish(boolean restart, int xmx, String vmoption) {
		if(terminal!=null){
			if(restart==true){
				logger.warn("finish restart parameter was ignored.");
			}
			terminal.finishQulet();
		}else{
			container.stop();
		}
	}

	/* (非 Javadoc)
	 * @see naru.quelet.QueletCommand#enque(java.lang.Object, java.lang.String)
	 */
	public void enque(Object req, String terminalName) {
		if(terminal!=null){
			terminal.addTotalOutCount();
		}
		container.enque(req,terminalName);
	}

	/* (非 Javadoc)
	 * @see naru.quelet.QueletCommand#deque()
	 */
	public Object deque() {
		return terminal.deque();
	}

	/* (非 Javadoc)
	 * @see naru.quelet.QueletCommand#deque(java.lang.String)
	 */
	public Object deque(String terminal) {
		return container.deque(terminal);
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.QueueletContext#queueLength()
	 */
	public int queueLength() {
		return nextTerminal.getQueLength();
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.QueueletContext#queueLength(java.lang.String)
	 */
	public int queueLength(String terminal) {
		return 0;
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.QueueletContext#callASync(java.lang.String, java.lang.Object)
	 */
	public void callASync(String method, Object param) {
		CallThreadUtil.callASync(quelet,method,param);
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.QueueletContext#callASync(java.lang.String)
	 */
	public void callASync(String method) {
		CallThreadUtil.callASync(quelet,method);
	}
	
	public String resolveProperty(String value,Properties prop) {
		return container.getQueueletProperties().resolveProperty(value,prop);
	}

}
