/*
 * �쐬��: 2004/09/10
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
package naru.test;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author naru
 *
 * ���̐������ꂽ�R�����g�̑}�������e���v���[�g��ύX���邽��
 * �E�B���h�E > �ݒ� > Java > �R�[�h���� > �R�[�h�ƃR�����g
 */
public class TestLoader2 extends URLClassLoader {

	protected String Test;

	/**
	 * @param arg0
	 */
	public TestLoader2(URL[] arg0) {
		this(arg0,"zzz");
	}
	
	private static ClassLoader conv(ClassLoader cl){
		return cl;
	}
	
	public TestLoader2(URL[] arg0,ClassLoader parent){
		this(arg0,conv(parent),"zzz");
	}
	public TestLoader2(URL[] arg0,ClassLoader parent,String zzz){
		super(arg0,parent);
		ClassLoader.getSystemClassLoader();
	}
	
	public TestLoader2(URL[] arg0,String zzz){
		super(arg0);
		Test="aaa";
	}

	public static void main(String[] args) {
	}
	/* (�� Javadoc)
	 * @see java.lang.ClassLoader#findClass(java.lang.String)
	 */
	protected Class findClass(String arg0) throws ClassNotFoundException {
		Class c=super.findClass(arg0);
		return c;
	}
	
	private void aaa() throws ClassNotFoundException{
		TestLoader2 a=new TestLoader2(null);
		
		super.loadClass("a.b.c");
		defineClass("a.b.c",null,0,0);
		loadClass("a.b.c");
		a.loadClass("xxxx");
		a.findClass("a.b.c.d");
	}

}
