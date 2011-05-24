/*
 * �쐬��: 2004/07/22
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
package naru.queuelet;

import java.util.Properties;

/**
 * @author naru
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
public interface QueueletContext {
	int queueLength();
	int queueLength(String terminal);
	
	void enque(Object req);
	void enque(Object req,String terminal);
	Object deque();
	Object deque(String terminal);
	void finish();
	void finish(boolean restart);//restart ��true�̏ꍇ�A�ċN��
	void finish(boolean restart,int xmx,String vmoption);//�ċN�����̃�����(M)��vmoption���w��
	/**
	 * @param string
	 * @param file
	 */
	void callASync(String method,Object param);
	void callASync(String method);
	
	/* ${xxx}�`����property�l���������� */
	String resolveProperty(String value,Properties prop);
}
