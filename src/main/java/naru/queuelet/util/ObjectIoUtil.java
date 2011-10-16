package naru.queuelet.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public class ObjectIoUtil extends ObjectInputStream {
	private ClassLoader cl;
	protected ObjectIoUtil(InputStream is,ClassLoader cl) throws IOException, SecurityException {
		super(is);
		this.cl=cl;
	}
	
	protected Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
		return cl.loadClass(desc.getName());
	}

	public static byte[] objectTobytes(Object obj) throws IOException{
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		ObjectOutputStream oos=new ObjectOutputStream(baos);
		oos.writeObject(obj);
		oos.close();
		return baos.toByteArray();
	}
	
	public static Object bytesToObject(byte[] b,ClassLoader cl) throws SecurityException, IOException, ClassNotFoundException{
		ByteArrayInputStream bais=new ByteArrayInputStream(b);
		ObjectIoUtil oiu=new ObjectIoUtil(bais,cl);
		return oiu.readObject();
	}

}
