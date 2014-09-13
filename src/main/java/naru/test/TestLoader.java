/*
 * 作成日: 2004/09/01
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class TestLoader extends URLClassLoader {
	private ClassPool cp;
	
	private TestLoader(int a){
		super(null,null);
	}

	private TestLoader(ClassLoader parent){
		super(new URL[0],parent);
		cp=ClassPool.getDefault();
		cp.appendSystemPath();
	}
	
	private void aaa() throws ClassNotFoundException{
		TestLoader a=new TestLoader(null);
		
		super.loadClass("a.b.c");
		defineClass("a.b.c",null,0,0);
		loadClass("a.b.c");
		a.loadClass("xxxx");

	}
//	public Class loadClass(String className) throws ClassNotFoundException {
//		return findClass(className);
//	}

	/* (非 Javadoc)
	 * @see java.lang.ClassLoader#loadClass(java.lang.String)
	 */
	public Class loadClass(String className) throws ClassNotFoundException {
//	public Class findClass(String className) throws ClassNotFoundException {
		System.out.println("className=" + className);
		if( className.startsWith("java")){
			return super.loadClass(className);
		}
		if( !className.equals("naru.test.Test2") ){
			try {
				return super.loadClass(className);
//				TestLoader tl=new TestLoader(null);
//				CtClass cc=cp.get(className);
//				byte[] bc=cc.toBytecode();
//				return tl.defineClass(className,bc,0,bc.length);
			} catch (ClassFormatError e1) {
				// TODO 自動生成された catch ブロック
				e1.printStackTrace();
			} catch (Throwable e1) {
				e1.printStackTrace();
			}
		}
		try {
			CtClass cc=cp.get(className);
			byte[] bc=cc.toBytecode();
//			defineClass(className,bc,0,bc.length);
			return defineClass(className,bc,0,bc.length);
		} catch (NotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (CannotCompileException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return super.loadClass(className);
	}
	
	public static void main(String[] args) {
		ClassLoader classL1=ClassLoader.getSystemClassLoader();
		ClassLoader classL2=TestLoader.class.getClassLoader();

		if( classL1.equals(classL2)){
			System.out.println("same");
		}else{
			System.out.println("not same");
		}



		ClassLoader system=ClassLoader.getSystemClassLoader();
		TestLoader tl=new TestLoader(system);
		try {
//			Class cl2=tl.loadClass("org.apache.log4j.Category");
			Class cl=tl.loadClass("naru.test.Test2");
//			Class cl3=tl.loadClass("naru.test.Test2");
			System.out.println(cl.getClassLoader().toString());
			Class cl1=system.loadClass("naru.test.Test2");
			System.out.println(cl1.getClassLoader().toString());
			cl.newInstance();
			System.out.println(cl.getName());
		} catch (ClassNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}
}
