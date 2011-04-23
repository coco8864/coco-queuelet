package naru.queuelet.startup;

import java.io.FileNotFoundException;

import naru.queuelet.Container;

import junit.framework.TestCase;

public class StartupTest extends TestCase {
	public void testMain1() {
		String args[]=new String[0];
		args=new String[1];
		args[0]="test.xml";
		try {
			Container container=new Container("test.xml");
			container.waitForRealContainer();
		} catch (Throwable e) {
			fail("test.xml error");
		}
	}
}
