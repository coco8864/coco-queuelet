/*
 * Created on 2004/11/11
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.queuelet.typed;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Map;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

/**
 * @author naru
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TimerQueuelet implements Queuelet,Runnable {
	private QueueletContext context;
	private int interval;
	
	private String pool=null;
	private Constructor constructor=null;
	private Object[] args=null;
	
	private boolean stop=false;
	private Thread timerThread;
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		return false;
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletContext, java.util.Map)
	 */
	public void init(QueueletContext context, Map param) {
		this.context=context;
		String intervalString=(String)param.get("interval");
		this.interval=Integer.parseInt(intervalString);

		pool=(String)param.get("queuePool");
		String queueClassName=(String)param.get("queueClassName");
		
		if( queueClassName!=null){
			String arg=(String)param.get("constructorArg");
			Class[] type=null;
			if(arg==null){
				type=new Class[0];
			}else{
				type=new Class[1];
				type[0]=String.class;
				args=new Object[1];
				args[0]=arg;
			}
			ClassLoader loader=(ClassLoader)param.get("QueueletLoader");
			if( loader==null){
				loader=getClass().getClassLoader();
			}
			try {
				Class queueClass=loader.loadClass(queueClassName);
				constructor=queueClass.getConstructor(type);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		timerThread=new Thread(this);
		timerThread.start();
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
		stop=true;
		timerThread.interrupt();
		try {
			timerThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private Object getQueueObject(){
		if( pool!=null ){
			return context.deque(pool);
		}else if(constructor!=null){
			try {
				return constructor.newInstance(args);
			} catch (InstantiationException e) {
			} catch (IllegalAccessException e) {
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return new Date();
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
			} catch (InterruptedException ignore) {
//				e.printStackTrace();
			}
			//前回投げたリクエストを処理しきれていない場合、新規に投げる必要なし
			if( context.queueLength()==0 ){
				context.enque(getQueueObject());
			}
		}
	}

}
