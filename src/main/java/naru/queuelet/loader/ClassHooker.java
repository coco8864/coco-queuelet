/*
 * 作成日: 2004/09/14
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet.loader;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class ClassHooker {
	static private Logger logger=Logger.getLogger(ClassHooker.class.getName());

	private String name;
	
	private Class clazz;
	private CtClass ctClass;
	
	private boolean hookAll=false;
	private boolean resolved=false;
	
	private LoaderWrapper loaderWrapper;

	/* 静的復帰値 （クラス単位に指定する場合) */	
	private String returnValue;
	
	/* 呼び出し前に通知するターミナル（クラス単位に指定する場合) */	
	private String before;

	/* 呼び出し後に通知するターミナル（クラス単位に指定する場合) */	
	private String after;
	
	/* 呼び出し情報に呼び出しスタックを含めるか否か（クラス単位に指定する場合) */	
	private String callStack;/* 無指定を知るためにString */
	
	private Map unResolvedMethodHookers=new HashMap();
	private Map methodHookers=new HashMap();

	public MethodHooker[] getMethodHookers(Map Hookers){
		Collection c=Hookers.values();
		
		MethodHooker[] result=(MethodHooker[])c.toArray(new MethodHooker[c.size()]);
		return result;
	}
	
	public MethodHooker[] getMethodHookers(){
		return getMethodHookers(methodHookers);
	}
	
	public MethodHooker addMethodHooker(MethodHooker hooker){
		String methodName=hooker.getName();
		hooker.setClassHooker(this);
		if( "*".equals(methodName)){
			hookAll=true;
		}
		String idString=MethodHooker.getIdString(hooker);
		unResolvedMethodHookers.put(idString,hooker);
		return hooker;
	}

	private void resoleveAllMethods(){
		MethodHooker wildHooker=(MethodHooker)unResolvedMethodHookers.get("*");
		CtMethod[] cms=ctClass.getDeclaredMethods();
		for(int i=0;i<cms.length;i++){
			CtMethod cm=cms[i];
			logger.debug("hoook method(*) loader:" + loaderWrapper + " name:" + cm.getName() + " signature:" + cm.getSignature());
			MethodHooker hooker=(MethodHooker)wildHooker.copy();
			
			hooker.setName(cm.getName());
			hooker.setSignature(cm.getSignature());
			hooker.setCtMethod(cm);
			hooker.setClassHooker(this);
			String idString=MethodHooker.getIdString(hooker);
			methodHookers.put(idString,hooker);
		}
	}

	
	public void resolve(CtClass cc){
		logger.debug("resolve:class:" +cc.getName());
		if(resolved){
			return;
		}
		this.ctClass=cc;
		if(hookAll){
			resoleveAllMethods();
			resolved=true;
			return;
		}

		MethodHooker[] mhs=getMethodHookers(unResolvedMethodHookers);
		for(int i=0;i<mhs.length;i++){
			MethodHooker hooker=mhs[i];
			CtMethod cm=null;
			String methodName=hooker.getName();
			logger.debug("resolve:methodName:" +methodName);
			try {
				if( hooker.getSignature()!=null){
					cm=cc.getMethod(methodName,hooker.getSignature());
					logger.debug("hoook method(signature) loader:" + loaderWrapper + " name:" + cm.getName() + " signature:" + cm.getSignature());
				}else{
					/* 基底クラスを探さない ...同じメソッド名のシグニチャ違いが複数あったらまずい...かな？fix ms*/
					cm=cc.getDeclaredMethod(methodName);
					hooker.setSignature(cm.getSignature());
					logger.debug("hoook method(name) loader:" + loaderWrapper + " name:" + cm.getName() + " signature:" + cm.getSignature());
				}
				hooker.setCtMethod(cm);
				hooker.setClassHooker(this);
				String idString=MethodHooker.getIdString(hooker);
				methodHookers.put(idString,hooker);
			} catch (NotFoundException e) {
				logger.debug("resolve:"+ name + ":" + methodName,e);
			}
		}
		resolved=true;
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
	public Class getClazz() {
		return clazz;
	}

	/**
	 * @return
	 */
	public String getReturnValue() {
		if( returnValue!=null)
			return returnValue;
		return loaderWrapper.getReturnValue();
	}

	/**
	 * @param string
	 */
	public void setName(String string) {
		name = string;
	}

	/**
	 * @param class1
	 */
	public void setClazz(Class class1) {
		clazz = class1;
	}

	/**
	 * @param string
	 */
	public void setReturnValue(String string) {
		returnValue = string;
	}

	/**
	 * @return
	 */
	public String getAfter() {
		if( after!=null)
			return after;
		return loaderWrapper.getAfter();
	}

	/**
	 * @return
	 */
	public String getBefore() {
		if( before!=null)
			return before;
		return loaderWrapper.getBefore();
	}

	/**
	 * @return
	 */
	public String getCallStack() {
		if( callStack!=null)
			return callStack;
		return loaderWrapper.getCallStack();
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
	 * @param string
	 */
	public void setCallStack(String string) {
		callStack = string;
	}

	/**
	 * @return
	 */
	public LoaderWrapper getLoaderWrapper() {
		return loaderWrapper;
	}

	/**
	 * @param wrapper
	 */
	public void setLoaderWrapper(LoaderWrapper wrapper) {
		loaderWrapper = wrapper;
	}

}
