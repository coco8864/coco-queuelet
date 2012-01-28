/*
 * �쐬��: 2004/08/10
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
package naru.queuelet.util;

import java.util.Stack;

import naru.queuelet.QueueletCallInfo;

/**
 * @author naru
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
public class CallStackUtil extends ThreadLocal{
	private static CallStackUtil callStackThreadLocal=new CallStackUtil();
	private static ThreadLocal callCheckThreadLocal=new ThreadLocal();

	/* (�� Javadoc)
	 * @see java.lang.ThreadLocal#initialValue()
	 */
	protected Object initialValue() {
		return new Stack();
	}
	
	public static QueueletCallInfo pop(){
		Stack callStack=(Stack)callStackThreadLocal.get();
		return (QueueletCallInfo)callStack.pop();
	}

	public static void push(QueueletCallInfo info){
		Stack callStack=(Stack)callStackThreadLocal.get();
		callStack.push(info);
	}
	
	public static QueueletCallInfo peek(){
		Stack callStack=(Stack)callStackThreadLocal.get();
		return (QueueletCallInfo)callStack.peek();
	}

	/*�@�񓯊��Ăяo�����ɂQ�d�ɃL���[���Ȃ��d�g�� */
	public static Object isAsync(){
		Object o=callCheckThreadLocal.get();
		callCheckThreadLocal.set(null);
		return o;
	}
	
	public static void setAsync(Object obj){
		if( obj==null ){
			throw new NullPointerException(CallStackUtil.class.getName() +"#setAsync");
		}
		callCheckThreadLocal.set(obj);
	}
}
