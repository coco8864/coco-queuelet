package naru.web;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SslClient {
/*
 * トラストストアに登録する方法
	keytool -import -alias opensvn_ca -file opensvn.cer -keypass changeit -storetype JKS -keystore cacerts -storepass changeit
	
	ポイント
	１）-aliasは、ストア内で一意
	２）-fileは、IEからexportした証明書(Base 64 encoded X.509(CER)(S))
*/
	
	
	
	/**
	 * @param args
	 * @throws KeyManagementException 
	 * @throws NoSuchAlgorithmException 
	 * @throws IOException 
	 * @throws UnknownHostException 
	 * @throws UnrecoverableKeyException 
	 * @throws KeyStoreException 
	 * @throws CertificateException 
	 */
	public static void main(String[] args) throws KeyManagementException, NoSuchAlgorithmException, UnknownHostException, IOException, KeyStoreException, UnrecoverableKeyException, CertificateException {
		// トラストストア設定
		System.setProperty("javax.net.ssl.trustStore" , "f:/cer/cacerts" );
		System.setProperty("javax.net.ssl.trustStorePassword","changeit"  );
		
		KeyStore ks = KeyStore.getInstance ( "JKS" );
		char[] keystorePass ="changeit".toCharArray();
		ks.load ( new FileInputStream( "f:/cer/cacerts" ) , keystorePass );
		
		KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
		kmf.init(ks , keystorePass  );
		
		SSLContext ctx = SSLContext.getInstance ( "TLS" );
		ctx.init( kmf.getKeyManagers()  , null , null );
		
		//SSL proxyを使う場合
		Socket socket=new Socket("proxy.soft.fujitsu.com",8080);
		OutputStream pos=socket.getOutputStream();
		pos.write("CONNECT opensvn.csie.org:443 HTTP/1.0\r\n".getBytes());
		pos.write("\r\n".getBytes());
		InputStream pis=socket.getInputStream();
		byte[] buf=new byte[4096];
		while(true){
			int readlen=pis.read(buf);
			if(readlen<=0){
				break;
			}
			String sss=new String(buf,0,readlen);
			System.out.println(sss);
			if(sss.indexOf("\r\n\r\n")>0){
				break;
			}
		}
		
		// SSLSocket生成
		SSLSocketFactory factory  = ctx.getSocketFactory();
		
		//直接通信する場合
//		SSLSocket s = (SSLSocket)factory.createSocket( "esupport.fujitsu.com" , 443 );
//		SSLSocket s = (SSLSocket)factory.createSocket( "vf750f.soft.fujitsu.com" , 443 );
		Socket s=factory.createSocket(socket,"opensvn.csie.org",443,true);
		OutputStream os=s.getOutputStream();
		os.write("GET / HTTP/1.0\r\n".getBytes());
		os.write("\r\n".getBytes());
		InputStream is=s.getInputStream();
//		buf=new byte[4096];
		while(true){
			int readlen=is.read(buf);
			if(readlen<=0){
				break;
			}
			System.out.println(new String(buf,0,readlen));
		}
		s.close();

	}

}
