package naru.web;

import org.apache.commons.codec.binary.Base64;

public class Base64Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		byte[] a=Base64.decodeBase64("dGVzdDp0ZXN0".getBytes());
		System.out.println(new String(a));
		
		byte[] b=Base64.encodeBase64("yf911074:NARU3NARU3".getBytes());
		System.out.println(new String(b));
		
	}

}
