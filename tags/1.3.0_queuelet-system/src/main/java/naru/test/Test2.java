/*
 * �쐬��: 2004/10/04
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
package naru.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Category;

/**
 * @author naru
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
public class Test2 extends Category{
/**
	 * @param arg0
	 */
	protected Test2(String arg0) {
		super(arg0);
		// TODO �����������ꂽ�R���X�g���N�^�[�E�X�^�u
	}

	//	static private Category logger=Category.getInstance(Test2.class);
	private static Log LOG = LogFactory.getLog(Test2.class);

	static{
//		logger.info("aaa");
		LOG.info("bbb");
	}
}
