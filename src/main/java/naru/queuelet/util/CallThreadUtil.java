/*
 * �쐬��: 2004/08/04
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
package naru.queuelet.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.log4j.Logger;

/**
 * @author naru
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
public class CallThreadUtil implements Runnable{
	static private Logger logger=Logger.getLogger(CallThreadUtil.class.getName());
	
	/* �����o���\�b�h�Ăяo���p */
	private Object obj=null;

	/* �X�^�e�B�b�N���\�b�h�Ăяo���p */
	private Class clazz=null;
	private Class[] paramTypes;
	
	private String methodName;
	private Object[] paramValues;

	public static void callASync(Object obj,String methodName){
		callASync(obj,methodName,new Object[0]);
	}

	public static void callASync(Object obj,String methodName,Object o){
		Object[] params=new Object[1];
		params[0]=o;
		callASync(obj,methodName,params);
	}
	
	public static void callASync(Object obj,String methodName,Object[] paramValue){
		CallThreadUtil ct=new CallThreadUtil(obj,methodName,paramValue);
		Thread t=new Thread(ct);
		t.start();
	}

	public static void callStaticASync(Class clazz,String methodName,Class[] paramTypes,Object[] paramValues){
		CallThreadUtil ct=new CallThreadUtil(clazz,methodName,paramTypes,paramValues);
		Thread t=new Thread(ct);
		t.start();
	}
	
	private CallThreadUtil(Object obj,String methodName,Object[] paramValues){
		this.obj=obj;
		this.methodName=methodName;
		this.paramValues=paramValues;	
	}

	private CallThreadUtil(Class clazz,String methodName,Class[] paramTypes,Object[] paramValues){
		this.clazz=clazz;
		this.methodName=methodName;
		this.paramTypes=paramTypes;
		this.paramValues=paramValues;	
	}

	/* (�� Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			logger.debug("run start");
			if(obj!=null){
				MethodUtils.invokeMethod(obj,methodName,paramValues);
			}else if( clazz!=null){
				Method method =	clazz.getMethod(methodName, paramTypes);
				method.invoke(null, paramValues);
			}
		} catch (NoSuchMethodException e) {
			logger.error("CallThread fail to run.",e);
		} catch (IllegalAccessException e) {
			logger.error("CallThread fail to run.",e);
		} catch (InvocationTargetException e) {
			logger.error("CallThread fail to run.",e);
		}
		logger.debug("run end");
	}

}
