package naru.qtest;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;
import naru.queuelet.test.TestBase;

public class LoaderTest extends TestBase {
	private static Object callMethodReturnValue(Object obj,String methodName,Class types[],Object args[]) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException{
		Method method=null;
		method=obj.getClass().getMethod(methodName, types);
		return method.invoke(obj, args);
	}
	
	@BeforeClass
	public static void beforClass() throws IOException {
		System.out.println(Thread.currentThread().getName() + ":setupContainer:"+System.getProperty("QueueletTestStartMark"));
		LoaderTest.setupContainer();
	}
	@AfterClass
	public static void afterClass() {
	}
	@Before
	public void setup(){
		System.out.println(Thread.currentThread().getName() + ":startContainer:"+System.getProperty("QueueletTestStartMark"));
		startContainer("LoaderTest.xml");
	}
	@After
	public void after(){
		System.out.println(Thread.currentThread().getName() + ":stopContainer:"+System.getProperty("QueueletTestStartMark"));
		LoaderTest.stopContainer();
	}
	
	public static Queuelet getQ1() {
		return new Q1();
	}
	
	public static Queuelet getQ2() {
		return new Q2();
	}
	
	@Test
	public void testLoader1() throws Throwable{
		callTest("qtestLoader1");
	}
	public void qtestLoader1() {
		assertEquals(q1.getClass(), Q1.class);
		assertEquals(q2.getClass().getName(), Q2.class.getName());
		assertNotSame(q2.getClass(), Q2.class);
	}
	
	@Test
	public void testLoader2() throws Throwable{
		callTest("qtestLoader2");
	}
	public void qtestLoader2() {
		try {
			URL url=new URL("file:target/test-classes/test/");
			URLClassLoader ucl=new URLClassLoader(new URL[]{url},null);
			Class clazz=ucl.loadClass("naru.qtest.Dummy");
			Object ret=callMethodReturnValue(clazz.newInstance(),"func1",new Class[]{"".getClass()},new Object[]{"param"});
			assertEquals("直接URLClassLoaderを直接使った場合は書き換えできない","func1ReturnRaw",ret);
		} catch (Throwable e) {
			fail("直接URLClassLoaderを直接使った場合は書き換えできない");
			e.printStackTrace();
		}
	}
	
	private static class MyLoader extends URLClassLoader{
		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			// TODO Auto-generated method stub
			return super.findClass(name);
		}

		public MyLoader(URL[] urls, ClassLoader parent) {
			super(urls, parent);
		}
	}
	
	@Test
	public void testLoader3() throws Throwable{
		callTest("qtestLoader3");
	}
	public void qtestLoader3() {
		try {
			URL url=new URL("file:target/test-classes/test/");
			MyLoader ucl=new MyLoader(new URL[]{url},null);
			Class clazz=ucl.loadClass("naru.qtest.Dummy");
			Object ret=callMethodReturnValue(clazz.newInstance(),"func1",new Class[]{"".getClass()},new Object[]{"param"});
			assertEquals("直接URLClassLoaderをextendsしたloaderは書き換えられる","func1ReturnRewrite",ret);
		} catch (Throwable e) {
			fail("直接URLClassLoaderをextendsしたloaderは書き換えられる");
			e.printStackTrace();
		}
	}
	
	private static Q1 q1;//main　loader
	private static Object q2;//sub　loader
	
	private static class Q1 implements Queuelet {
		public void init(QueueletContext context, Map param) {
			System.out.println("Q1 init:" + this.getClass().getClassLoader());
			q1=this;
		}

		public boolean service(Object req) {
			System.out.println(Thread.currentThread().getName() + ":" + req.getClass().getName());
			q2=req;
			return false;
		}

		public void term() {
			System.out.println("Q1 term");
		}
	}
	
	private static class Q2 implements Queuelet {
		public void init(QueueletContext context, Map param) {
			System.out.println("Q2 init:" + this.getClass().getClassLoader());
			context.enque(this);
		}

		public boolean service(Object req) {
			return false;
		}

		public void term() {
			System.out.println("Q2 term");
		}
	}
	
}