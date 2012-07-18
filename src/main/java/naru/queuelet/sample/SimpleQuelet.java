/*
 * �쐬��: 2004/07/22
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
package naru.queuelet.sample;

import java.util.Map;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;


/**
 * @author naru
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
public class SimpleQuelet implements Queuelet {
	private QueueletContext command;

	/* (�� Javadoc)
	 * @see naru.quelet.Quelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		System.out.println("this:" + this.toString() +" req:"+ req);
//		command.enqueNext(this.toString() + req);
//		command.enqueNext(req);
		command.enque("stop");
		command.finish();
		return true;
	}

	/* (�� Javadoc)
	 * @see naru.quelet.Quelet#init(naru.quelet.QueletCommand, java.util.Map)
	 */
	public void init(QueueletContext command, Map param) {
		this.command=command;
	}

	/* (�� Javadoc)
	 * @see naru.quelet.Quelet#term()
	 */
	public void term() {
	}

}
