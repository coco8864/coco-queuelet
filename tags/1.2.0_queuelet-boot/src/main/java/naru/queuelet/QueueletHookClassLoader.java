/*
 * �쐬��: 2004/09/18
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
package naru.queuelet;

/**
 * @author naru
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
public class QueueletHookClassLoader extends ClassLoader {
	private static ClassLoader __queueletRootClassLoader;

	protected static ClassLoader queuelet_getParentClassLoader(ClassLoader cl){
		ClassLoader system=ClassLoader.getSystemClassLoader();
		if(cl==null || cl.equals(system)){
			return __queueletRootClassLoader;
		}
		return cl;
	}

	public static void queuelet_setParentClassLoader(ClassLoader cl){
		__queueletRootClassLoader=cl;
	}

	/**
	 * 
	 */
	public QueueletHookClassLoader() {
		super(__queueletRootClassLoader);
//		System.out.println("QueueletHookClassLoader():"+ __queueletRootClassLoader);
	}

	/**
	 * @param arg0
	 */
	public QueueletHookClassLoader(ClassLoader arg0) {
		super(queuelet_getParentClassLoader(arg0));
//		System.out.println("QueueletHookClassLoader(arg0):"+ arg0 + ":" + queuelet_getParentClassLoader(arg0));
	}
}
