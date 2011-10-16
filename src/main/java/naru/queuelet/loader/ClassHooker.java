/*
 * �쐬��: 2004/09/14
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
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
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
public class ClassHooker {
	static private Logger logger=Logger.getLogger(ClassHooker.class.getName());

	private String name;
	
	private Class clazz;
	private CtClass ctClass;
	
	private boolean hookAll=false;
	private boolean resolved=false;
	
	private LoaderWrapper loaderWrapper;

	/* �ÓI���A�l �i�N���X�P�ʂɎw�肷��ꍇ) */	
	private String returnValue;
	
	/* �Ăяo���O�ɒʒm����^�[�~�i���i�N���X�P�ʂɎw�肷��ꍇ) */	
	private String before;

	/* �Ăяo����ɒʒm����^�[�~�i���i�N���X�P�ʂɎw�肷��ꍇ) */	
	private String after;
	
	/* �Ăяo�����ɌĂяo���X�^�b�N���܂߂邩�ۂ��i�N���X�P�ʂɎw�肷��ꍇ) */	
	private String callStack;/* ���w���m�邽�߂�String */
	
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
					/* ���N���X��T���Ȃ� ...�������\�b�h���̃V�O�j�`���Ⴂ��������������܂���...���ȁHfix ms*/
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
