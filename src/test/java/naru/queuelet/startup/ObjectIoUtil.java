package naru.queuelet.startup;

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

	public static byte[] objectTobytes(Object obj){
		try {
			ByteArrayOutputStream baos=new ByteArrayOutputStream();
			ObjectOutputStream oos=new ObjectOutputStream(baos);
			oos.writeObject(obj);
			oos.close();
			return baos.toByteArray();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static Object bytesToObject(byte[] b,ClassLoader cl){
		ByteArrayInputStream bais=new ByteArrayInputStream(b);
		try {
			ObjectIoUtil oiu=new ObjectIoUtil(bais,cl);
			return oiu.readObject();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
