/*
 * �쐬��: 2004/09/10
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
package naru.test;

import java.net.URL;

/**
 * @author naru
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
public class TestLoader3 extends TestLoader2 {

	protected String Test;

	/**
	 * @param arg0
	 */
	public TestLoader3(URL[] arg0) {
		super(arg0);
		// TODO �����������ꂽ�R���X�g���N�^�[�E�X�^�u
	}
	
	

	public static void main(String[] args) {
	}
	/* (�� Javadoc)
	 * @see java.lang.ClassLoader#findClass(java.lang.String)
	 */
	protected Class findClass(String arg0) throws ClassNotFoundException {
		return super.findClass(arg0);
	}

}
