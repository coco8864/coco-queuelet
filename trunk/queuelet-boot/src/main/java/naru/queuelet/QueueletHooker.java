/*
 * �쐬��: 2004/09/01
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
public interface QueueletHooker {
	public boolean callMethod(QueueletCallInfo req);
	public QueueletCallInfo returnMethod(Object returnValue,Throwable returnThrowable);
	public byte[] getByteCode(String className);
	public byte[] getByteCode(String className,byte[] b,int off,int len);
	public void registerClass(String className,Class clazz);
}
