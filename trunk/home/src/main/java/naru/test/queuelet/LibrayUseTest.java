package naru.test.queuelet;

import java.io.File;
import java.io.IOException;

import naru.web.AccessLog;

public class LibrayUseTest {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		File tmp=new File("F:\\aa\test.log");
		File pass=new File("F:\\aa\test2.log");
		tmp.renameTo(pass);
	}
}
