package naru.queuelet.startup;

//import java.io.ByteArrayInputStream;
import java.io.File;
//import java.io.IOException;
//import java.io.ObjectInputStream;

import junit.framework.TestCase;

public class StartupLoaderTest extends TestCase {
	
	public void testLoader() {
		File[] classpaths=new File[1];
		classpaths[0]=new File("../queuelet-system/target/classes");
		StartupLoader sl=new StartupLoader("test",classpaths,null,null);
		try {
			Class cl=sl.loadClass("naru.queuelet.core.Container");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
//			fail();
		}
	}
	
	public void testLoader2() {
		File[] classpaths=new File[1];
		classpaths[0]=new File("E:/ggproj/queuelet/queuelet-boot/target/test-classes");
		StartupLoader sl=new StartupLoader("test",classpaths,null,null);
		byte[]b=null;
		try {
			Class cl=sl.loadClass("naru.queuelet.startup.Dummy");
//			Class cl=sl.loadClass("java.lang.String");
			Object obj=cl.newInstance();
			b=ObjectIoUtil.objectTobytes(obj);
			System.out.println("obj:"+obj);
			System.out.println("obj.getClass().getName():"+obj.getClass().getName());
			System.out.println("b.length:"+b.length);
			System.out.println("b:"+b);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
//			fail();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		StartupLoader sl2=new StartupLoader("test2",classpaths,null,null);
//		ByteArrayInputStream bais=new ByteArrayInputStream(b);
		try {
//			ObjectInputStream ois=new ObjectInputStream(bais);
//			Object obj=ois.readObject();
			Object obj=ObjectIoUtil.bytesToObject(b,sl2);
			System.out.println("obj:"+obj);
			System.out.println("obj.getClass().getName():"+obj.getClass().getName());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
