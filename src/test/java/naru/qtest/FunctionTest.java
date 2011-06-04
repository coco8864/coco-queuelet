package naru.qtest;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


import naru.queuelet.Queuelet;
import naru.queuelet.QueueletCallInfo;
import naru.queuelet.QueueletContext;
import naru.queuelet.test.TestBase;

public class FunctionTest extends TestBase {
	@BeforeClass
	public static void beforClass() throws IOException {
		System.out.println(Thread.currentThread().getName() + ":setupContainer:"+System.getProperty("QueueletTestStartMark"));
		FunctionTest.setupContainer();
	}
	@AfterClass
	public static void afterClass() {
	}
	@Before
	public void setup(){
		System.out.println(Thread.currentThread().getName() + ":startContainer:"+System.getProperty("QueueletTestStartMark"));
		startContainer("FunctionTest.xml");
	}
	@After
	public void after(){
		System.out.println(Thread.currentThread().getName() + ":stopContainer:"+System.getProperty("QueueletTestStartMark"));
		FunctionTest.stopContainer();
	}
	private static QueueletCallInfo callInfo;
	private static Queuelet q;
	
	public static Queuelet getQueuelet() {
		q=new Q();
		return q;
	}

	public String func1(String param) {
		System.out.println(Thread.currentThread().getName() +":"+ this.getClass().getName()+":func1");
		return "func1ReturnRaw";
	}
	public String func2(String param) {
		System.out.println(Thread.currentThread().getName() +":"+ this.getClass().getName()+":func2");
		return "func2ReturnRaw";
	}
	public String func3(String param) {
		System.out.println(Thread.currentThread().getName() +":"+ this.getClass().getName()+":func3");
		return "func3ReturnRaw";
	}
	public String func4(String param) {
		System.out.println(Thread.currentThread().getName() +":"+ this.getClass().getName()+":func4");
		return "func4ReturnRaw";
	}
	
	private int maxCounter=0;
	private int counter;
	public String func5(String param) {
		System.out.println(Thread.currentThread().getName() + ":func5");
		synchronized(this){
			counter++;
			if(maxCounter<counter){
				maxCounter=counter;
			}
			try {
				wait(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			counter--;
		}
		return "func5ReturnRaw";
	}
	
	@Test
	public void testFunc1() throws Throwable{
		callTest("qtestFunc1");
	}
	public void qtestFunc1() {
		System.out.println("qtestFunc1");
		//<methodHooker name="func1" before="async" returnValue='"func1ReturnRewrite"' />
		assertEquals("即値書き換え","func1ReturnRewrite", func1("func1Param")); 
	}
	
	
	@Test
	public void testFunc2() throws Throwable{
		callTest("qtestFunc2");
	}
	public void qtestFunc2() {
		System.out.println("qtestFunc2");
		synchronized(q){
			callInfo=null;
			//<methodHooker name="func2" before="funcQueue" />
			assertEquals("queulet書き換え","queueletReturn", func2("func2Param"));
			try {
				q.wait();
				assertEquals("呼び出し確認","func2", callInfo.getMethodName());
				assertEquals("呼び出し確認","func2Param", callInfo.getArgs()[0]);
			} catch (InterruptedException e) {
			}
		}
	}

	@Test
	public void testFunc3() throws Throwable{
		callTest("qtestFunc3");
	}
	public void qtestFunc3() {
		System.out.println("qtestFunc3");
		synchronized(q){
			callInfo=null;
			//<methodHooker name="func3" after="funcQueue"/>
			assertEquals("呼び出し通知","func3ReturnRaw", func3("func3Param"));
			try {
				q.wait();
				assertEquals("呼び出し確認","func3", callInfo.getMethodName());
				assertEquals("呼び出し確認","func3Param", callInfo.getArgs()[0]);
			} catch (InterruptedException e) {
			}
		}
	}

	@Test
	public void testFunc4() throws Throwable{
		callTest("qtestFunc4");
	}
	public void qtestFunc4() {
		System.out.println("qtestFunc4");
		//<methodHooker name="func4" before="async" returnValue='$1'/>
		assertEquals("即値書き換え","func4Param", func4("func4Param")); 
	}
	
	@Test
	public void testFunc5() throws Throwable{
		callTest("qtestFunc5");
	}
	public void qtestFunc5() {
		System.out.println("qtestFunc5");
		//<methodHooker name="func5" concurrence="3"/>
		Thread ts[]=new Thread[20];
		for(int i=0;i<ts.length;i++){
			ts[i]=new Thread(new T(this));
			ts[i].start();
		}
		for(int i=0;i<ts.length;i++){
			try {
				ts[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		assertEquals("同時処理数",3,maxCounter); 
	}
	
	private static class T implements Runnable{
		private T(FunctionTest ft){
			this.ft=ft;
		}
		FunctionTest ft;
		public void run() {
			ft.func5("xx");
		}
	}
	
	
	private static class Q implements Queuelet {
		public void init(QueueletContext context, Map param) {
			System.out.println("Q init:" + this.getClass().getClassLoader());
		}
		public boolean service(Object req) {
			System.out.println(Thread.currentThread().getName() + ":Q service:" + req);
			QueueletCallInfo info=(QueueletCallInfo)req;
			System.out.println(Thread.currentThread().getName() + ":Q service:" + info.getMethodName());
			info.setReturnValue("queueletReturn");
			synchronized(req){
				callInfo=info;
				req.notify();
			}
			synchronized(this){
				notify();
			}
			return false;
		}

		public void term() {
			System.out.println("Q term");
		}
	}
}