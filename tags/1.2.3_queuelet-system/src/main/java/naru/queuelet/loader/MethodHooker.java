/*
 * Created on 2004/08/04
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package naru.queuelet.loader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;

/**
 * @author NARU
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class MethodHooker implements Cloneable {
	static private Logger logger=Logger.getLogger(MethodHooker.class.getName());

	private int methodNumber;//magic
	private String name;
	private String signature;

	private int concurrence; /* 多重度 */

	private String returnValue;/* 静的復帰値 */

	/* 呼び出し前に通知するターミナル */	
	private String before;

	/* 呼び出し後に通知するターミナル */	
	private String after;
	
	/* 呼び出し情報に呼び出しスタックを含めるか否か */	
	private String callStack;/* 無指定を知るためにString */
	
	/* 呼び出し時に通知、復帰値を計算もしくは、既存処理にスルー */
	/* 既存処理にスルーした場合は、処理終了時にも通知 */
	
//	private Class clazz;
	private CtMethod ctMethod;
	private Method method=null;
	
	private ClassHooker classHooker;
	
	static int sequence=0;
	static List methodHookers=new ArrayList();
	
	public static String getIdString(MethodHooker mh){
		if(mh.signature==null){
			return mh.name;
		}
		return mh.name + ":" + mh.signature;
	}
	
	public MethodHooker(){
		logger.debug("MethodHooker create");
		synchronized(methodHookers){
			methodNumber=sequence;
			methodHookers.add(methodNumber,this);
			sequence++;
		}
	}
	
	public MethodHooker copy(){
		try {
			MethodHooker result=(MethodHooker)clone();
			synchronized(methodHookers){
				result.methodNumber=sequence;
				methodHookers.add(result.methodNumber,result);
				sequence++;
			}
			return result;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static MethodHooker getMethodHooker(int methodNumger){
		if( methodHookers.size()<=methodNumger ){
			return null;
		}
		return (MethodHooker)methodHookers.get(methodNumger);
	}
	
	public void setCtMethod(CtMethod ctMethod){
		this.ctMethod=ctMethod;
	}
	
	
	private String getArraySigniture(CtClass ctClass) throws NotFoundException, ClassNotFoundException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
		if(ctClass.isPrimitive()){
			if( CtClass.booleanType.equals(ctClass)){
				return "Z";
			}else if(CtClass.byteType.equals(ctClass)){
				return "B";
			}else if(CtClass.charType.equals(ctClass)){
				return "C";
			}else if(CtClass.doubleType.equals(ctClass)){
				return "D";
			}else if(CtClass.floatType.equals(ctClass)){
				return "F";
			}else if(CtClass.intType.equals(ctClass)){
				return "I";
			}else if(CtClass.longType.equals(ctClass)){
				return "J";
			}else if(CtClass.shortType.equals(ctClass)){
				return "S";
			}else{				
			}
		}else if(ctClass.isArray()){
			CtClass member=ctClass.getComponentType();
			String sig=getArraySigniture(member);
			return "[" +sig;
		}else{
			return "L" + ctClass.getName()+";";
		}
		return null;
	}
	
	
	public Method getMethod() {
		try {
			if( method!=null){
				return method;
			}
			Class clazz=classHooker.getClazz();
			if( clazz==null ){
				logger.info("getMeghod.clazz is null."+ getClassName() + "#" + getName());
				return null;
			}
			CtClass[] ctParamTypes=ctMethod.getParameterTypes();
			Class[] paramTypes=new Class[ctParamTypes.length];
			for(int i=0;i<ctParamTypes.length;i++){
				if(ctParamTypes[i].isPrimitive()){
					CtPrimitiveType pt=(CtPrimitiveType)ctParamTypes[i];
					Class wrapper=Class.forName(pt.getWrapperName());
					Field typeField=wrapper.getField("TYPE");
					paramTypes[i]=(Class)typeField.get(null);
				}else if(ctParamTypes[i].isArray()){
					String sig=getArraySigniture(ctParamTypes[i]);
					paramTypes[i]=Class.forName(sig);
				}else{
					String name=ctParamTypes[i].getName();
					paramTypes[i]=Class.forName(name);
				}
			}
			method=clazz.getMethod(name,paramTypes);
			return method;
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}


	/**
	 * @return
	 */
	public String getClassName() {
		return classHooker.getName();
	}

	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return
	 */
	public String getSignature() {
		return signature;
	}


	/**
	 * @param string
	 */
	public void setName(String string) {
		name = string;
	}

	/**
	 * @param string
	 */
	public void setSignature(String string) {
		signature = string;
	}

	/**
	 * @return
	 */
	public int getMethodNumber() {
		return methodNumber;
	}

	/**
	 * @param i
	 */
	public void setMethodNumber(int i) {
		methodNumber = i;
	}

	/**
	 * @return
	 */
	public int getConcurrence() {
		return concurrence;
	}

	/**
	 * @param i
	 */
	public void setConcurrence(int i) {
		concurrence = i;
		syncInfo=new SyncCallInfo(concurrence);
	}
	
	/**
	 * @param string
	 */
	public void setCallStack(String string) {
		callStack = string;
	}

	/* 同期呼び出しの多重度制限を処理します */
	private class SyncCallInfo{
		int count;
		SyncCallInfo(int maxCount){
			this.count=maxCount;			
		}
	}
	private SyncCallInfo syncInfo;
	public void syncCall(){
		if(syncInfo==null){
			logger.debug("syncInfo==null");
			return;
		}
		synchronized (syncInfo) {
		while(true){
			if( syncInfo.count>0 ){
				syncInfo.count--;
				return;
			}
			try {
				syncInfo.wait();
			} catch (InterruptedException e) {
				logger.error("syncCall error.",e);
			}
		}
		}
	}
	
	public void syncReturn(){
		if(syncInfo==null){
			return;
		}
		synchronized (syncInfo) {
			syncInfo.count++;
			syncInfo.notify();
		}
	}
	/**
	 * @return
	 */
	public ClassHooker getClassHooker() {
		return classHooker;
	}

	/**
	 * @param hooker
	 */
	public void setClassHooker(ClassHooker hooker) {
		classHooker = hooker;
	}

	/* (非 Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if( !(obj instanceof MethodHooker) ){
			return false;
		}
		MethodHooker mh=(MethodHooker)obj;
		if( name.equals(mh.name) && 
			signature.equals(mh.signature) &&
			classHooker.equals(mh.classHooker) ){
			return true;				
		}
		return false;
	}

	/* (非 Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		int hash=0;
		if(name!=null){
			hash+=name.hashCode();
		}
		if(signature!=null){
			hash+=signature.hashCode();
		}
		if(classHooker!=null){
			hash+=classHooker.hashCode();
		}
		return hash;
	}

	/**
	 * @return
	 */
	public String getAfter() {
		if(after!=null)
			return after;
		return classHooker.getAfter();
	}

	/**
	 * @return
	 */
	public String getBefore() {
		if(before!=null)
			return before;
		return classHooker.getBefore();
	}

	/**
	 * @param string
	 */
	public void setAfter(String string) {
		after = string;
	}

	/**
	 * @param string
	 */
	public void setBefore(String string) {
		before = string;
	}

	/**
	 * @return
	 */
	public boolean isDetectCallStack() {
		if(callStack!=null){
			return ("true".equalsIgnoreCase(callStack));
		}
		String cs=classHooker.getCallStack();
		return ("true".equalsIgnoreCase(cs));
	}

	/**
	 * @return
	 */
	public String getCallStack() {
		return callStack;
	}

	/**
	 * @return
	 */
	public String getReturnValue() {
		if(returnValue!=null)
			return returnValue;
		return classHooker.getReturnValue();
	}

	/**
	 * @param string
	 */
	public void setReturnValue(String string) {
		returnValue = string;
	}
}
