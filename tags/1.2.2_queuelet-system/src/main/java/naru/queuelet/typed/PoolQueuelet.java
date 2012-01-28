/*
 * Created on 2004/10/23
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.queuelet.typed;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.log4j.Logger;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

/**
 * @author NARU
 *
 * このQueueletを囲むTerminalは、threadCount=0である事が必須
 * Pool対象は、パラメタなしもしくはString１つを受け取るコントラクタを持つ事
 * poolCount:Pool数
 * ClassName:Pool対象クラス
 * arg:コンストラクタパラメタ
 */
public class PoolQueuelet implements Queuelet {
	static private Logger logger=Logger.getLogger(PoolQueuelet.class);

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		throw new RuntimeException("never called");
	}

	private Object instantiate(ClassLoader loader,Class types[],Object args[],String className,String factoryClassName,String factoryMethodName) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException{
		/* classNameが指定された場合 */
		if(className!=null){
			Class poolClass=loader.loadClass(className);
			Constructor constructor=poolClass.getConstructor(types);
			return constructor.newInstance(args);
		}
		Class factoryClass=loader.loadClass(factoryClassName);
		Method method = factoryClass.getMethod(factoryMethodName, types);
		return method.invoke(null, args);
	}
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletContext, java.util.Map)
	 */
	public void init(QueueletContext context, Map param) {
		String poolCountString=(String)param.get("poolCount");
		String arg=(String)param.get("arg");
		String className=(String)param.get("className");
		String factoryClassName=(String)param.get("factoryClassName");
		String factoryMethodName=(String)param.get("factoryMethodName");
		
		String thisTerminal=(String)param.get("thisTerminal");
		ClassLoader loader=(ClassLoader)param.get("QueueletLoader");
		if( loader==null){
			loader=getClass().getClassLoader();
		}

		Class[] types=null;
		Object[] args=null;
		if(arg==null){
			types=new Class[0];
		}else{
			types=new Class[1];
			args=new Object[1];
			try {
				int argInt=Integer.parseInt(arg);
				types[0]=int.class;
				args[0]=new Integer(argInt);
			} catch (NumberFormatException e) {
				types[0]=String.class;
				args[0]=arg;
			}
		}
		try {
			int poolCount=Integer.parseInt(poolCountString);
			for(int i=0;i<poolCount;i++){
				Object obj=instantiate(loader,types,args,className,factoryClassName,factoryMethodName);
				context.enque(obj,thisTerminal);
			}
		} catch (Exception e) {
			logger.error("PoolQueuelet#init error.",e);
		}
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}
