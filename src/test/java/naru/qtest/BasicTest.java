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
import naru.queuelet.QueueletContext;
import naru.queuelet.test.TestBase;

public class BasicTest extends TestBase {
	@BeforeClass
	public static void beforClass() throws IOException {
		System.out.println(Thread.currentThread().getName() + ":setupContainer:"+getTestContainerStatus());
		setupContainer();
	}
	@AfterClass
	public static void afterClass() {
	}
	@Before
	public void setup(){
		System.out.println(Thread.currentThread().getName() + ":startContainer:"+getTestContainerStatus());
		startContainer("BasicTest.xml");
	}
	@After
	public void after(){
		System.out.println(Thread.currentThread().getName() + ":stopContainer:"+getTestContainerStatus());
		stopContainer();
	}
	
	public static Queuelet getQueuelet() {
		return new Q();
	}

	public int func1() {
		System.out.println(Thread.currentThread().getName() + ":func1");
		return 1234;
	}

	public String func2() {
		System.out.println(Thread.currentThread().getName() + ":func2");
		return "abcd";
	}
	
	public String func3(String p1,int p2,Date p3) {
		System.out.println(Thread.currentThread().getName() + ":func3");
		return "p1:"+p1+":p2:"+p2+":p3:"+p3;
	}
	
	
	@Test
	public void testBasic1() throws Throwable{
		callTest("qtestBasic1");
	}
	public void qtestBasic1() {
		long start=System.currentTimeMillis();
		for(int i=0;i<1000;i++){ 
			 Long time=System.currentTimeMillis();
			 synchronized(time){
				 enque(time,"testQueue");
				 try {
					time.wait();
				} catch (InterruptedException e) {
					fail("InterruptedException");
				}
			}
		}
		System.out.println("erapse time:"+(System.currentTimeMillis()-start));
		
		 
	}

	@Test
	public void testBasic2() throws Throwable {
		callTest("qtestBasic2");
	}
	public void qtestBasic2() {
		System.out.println("qtestBasic2");
		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			enque(System.currentTimeMillis(),"testQueue");
			fail("既に停止しているので例外が発生するはず");
		} catch (IllegalStateException e) {
//			e.printStackTrace();
		}
		//この後stopContainerも失敗しstackTraceするが例外とはならない
	}
	
	@Test
	public void testBasic3() throws Throwable{
		callTest("qtestBasic3");
	}
	public void qtestBasic3() {
		/*
		 * try { Thread.sleep(10000); } catch (InterruptedException e) {
		 * e.printStackTrace(); }
		 */
		System.out.println("qtestBasic1");
		System.out.println("func1:" + func1());
		System.out.println("func2:" + func2());
		//basicTest.xmlにて
		//<methodHooker name="func1"  before="async" returnValue="4321+10000" />
		assertEquals("復帰値書き換え",14321, func1()); 
		
		//<methodHooker name="func2"  before="async" returnValue='"dcba".toUpperCase()' />
		assertEquals("復帰値書き換え","DCBA", func2());

		//<methodHooker name="func3"  before="async" returnValue='$1+":addString"' />
		assertEquals("復帰値書き換え","abcd:addString", func3("abcd",1234,new Date()));
		
	}

	private static class Q implements Queuelet {
		public void init(QueueletContext context, Map param) {
			System.out.println("Q init:" + this.getClass().getClassLoader());
		}

		public boolean service(Object req) {
			System.out.println(Thread.currentThread().getName() + ":Q service:"
					+ (System.currentTimeMillis() - ((Long) req).longValue()));
			synchronized(req){
				req.notify();
			}
			return false;
		}

		public void term() {
			System.out.println("Q term");
		}
	}
}