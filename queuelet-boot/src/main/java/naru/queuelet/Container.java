package naru.queuelet;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import naru.queuelet.startup.Startup;

public class Container {
	private Object container;
//	private Class containerClass;
	
	private static boolean callMethod(Object obj,String methodName,Class types[],Object args[]) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException{
		Method method=null;
		try {
			method=obj.getClass().getMethod(methodName, types);
		} catch (NoSuchMethodException e) {
			return false;
		}
		method.invoke(obj, args);
		return true;
	}
	
	private static Object callMethodReturnValue(Object obj,String methodName,Class types[],Object args[]) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException{
		Method method=null;
		method=obj.getClass().getMethod(methodName, types);
		return method.invoke(obj, args);
	}
	
	public Container(String confXml) throws FileNotFoundException{
		container=Startup.startup(confXml);
		ClassLoader systemLoader=container.getClass().getClassLoader();
//		try {
//			containerClass=systemLoader.loadClass("naru.queuelet.core.Container");
//		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
//		}
	}
	
	public void waitForRealContainer(){
		synchronized(container){
			try {
				container.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void stop(){
		synchronized(container){
			Startup.stop();
			try {
				container.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
//		Startup.stop();
	}
	
	public void enque(Object obj,String terminalName){
		
		try {
			if( callMethod(container,"enque",
					new Class[]{Object.class,String.class},
					new Object[]{obj,terminalName})==false){
				throw new IllegalStateException("not found enque in Container");
			}
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Object deque(String terminalName){
		try {
			Object obj =callMethodReturnValue(container,"deque",
					new Class[]{String.class},
					new Object[]{terminalName});
			return obj;
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public String resolveProperty(String value){
		try {
			Object obj =callMethodReturnValue(container,"resolveProperty",
					new Class[]{String.class},
					new Object[]{value});
			return (String)obj;
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
