/*
 * �쐬��: 2004/08/04
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
package naru.queuelet.typed;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import naru.queuelet.util.CallStackUtil;
import naru.queuelet.Queuelet;
import naru.queuelet.QueueletCallInfo;
import naru.queuelet.QueueletContext;
import naru.queuelet.loader.MethodHooker;

/**
 * @author naru
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
public class AsyncCallQueuelet implements Queuelet {
	private QueueletContext context;

	/* (�� Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		QueueletCallInfo qci=(QueueletCallInfo)req;
		qci.setShortcut(true);/* �����Ăяo�����́A�V���[�g�J�b�g���� */
		int methodNumber=qci.getMagic();
		MethodHooker hooker=MethodHooker.getMethodHooker(methodNumber);
		Method method=hooker.getMethod();
		
//		System.out.println("hooker.getReturnValue()="+ hooker.getReturnValue());
		if( hooker.getReturnValue()==null ){
			Class clazz=method.getReturnType();
			Object returnObject=null;
			/* �ÓI���A�錾���Ȃ��ꍇ�́A�����I�u�W�F�N�g���쐬 */
			if( clazz.isPrimitive() ){
				if(Boolean.TYPE.equals(clazz)){
					returnObject=new Boolean(false);
				}else if(Byte.TYPE.equals(clazz)){
					returnObject=new Byte((byte)0);
				}else if(Short.TYPE.equals(clazz)){
					returnObject=new Short((short)0);
				}else if(Integer.TYPE.equals(clazz)){
					returnObject=new Integer(0);
				}else if(Long.TYPE.equals(clazz)){
					returnObject=new Long((long)0);
				}else if(Character.TYPE.equals(clazz)){
					returnObject=new Character('\0');
				}else if(Float.TYPE.equals(clazz)){
					returnObject=new Float((float)0);
				}else if(Double.TYPE.equals(clazz)){
					returnObject=new Double((double)0);
				}
			}else{
				/* �I�u�W�F�N�g�̏ꍇ�́A�f�t�H���g�R���X�g���N�^ */
				try {
					returnObject=clazz.newInstance();
				} catch (InstantiationException e1) {
					e1.printStackTrace();
				} catch (IllegalAccessException e1) {
					e1.printStackTrace();
				}
			}
			/* ���A�l�����I�Ɍ��܂�ꍇ�́A�����Ăяo�������҂��Ă��� */
			qci.setReturnValue(returnObject);
			synchronized (qci) {
				qci.notify();
			}
		}
		
		CallStackUtil.setAsync(qci);
		try {
			Object rc=method.invoke(qci.getThiz(),qci.getArgs());
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			qci.setReturnThrowable(e.getCause());
		}finally{
		}
		return true;
	}

	/* (�� Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletCommand, java.util.Map)
	 */
	public void init(QueueletContext command, Map param) {
		this.context=command;
	}

	/* (�� Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}
