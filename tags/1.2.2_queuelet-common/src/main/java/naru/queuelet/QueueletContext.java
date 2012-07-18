/*
 * �쐬��: 2004/07/22
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
package naru.queuelet;

import java.util.Properties;

import naru.queuelet.watch.StartupInfo;

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
	/**
	 * terminal�z���ł͂Ȃ��AQueuelet�Ŏw��ł���B
	 * isForceEnd:����̃V�[�P���X���o���Ƀf�[�������狭���I�������݂�B�i����̏I���V�[�P���X�����s������j
	 * isRestart:�I�����ɍċN�����邩�ۂ����w��,false�̏ꍇ�ȍ~�͖��������B
	 * javaHeap:�ċN�����Ɏw�肷��-XmxSS(m)�̒l���w��A���̒l�̏ꍇ�A����l
	 * javaVmOptions:�ċN�����Ɏw�肷��java VM�I�v�V�������w��Anull�̏ꍇ�A����l
	 * args:�ċN�����Ɏw�肷��������w��Anull�̏ꍇ�A����l
	 */
	void finish(boolean isForceEnd,boolean isRestart,StartupInfo startupInfo);
	
	/**
	 * �����M���𑗐M,������init�����s����ƊĎ��^�C���A�E�g�ɂ�����̂����
	 */
	boolean heatBeat();
	
	/**
	 * @param string
	 * @param file
	 */
	void callASync(String method,Object param);
	void callASync(String method);
	
	/* ${xxx}�`����property�l���������� */
	String resolveProperty(String value,Properties prop);
}
