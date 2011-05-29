/*
 * Created on 2004/10/20
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;


/**
 * @author NARU
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class HttpReceiveData{
	static private Logger logger=Logger.getLogger(HttpReceiveData.class);
	private static final String ENCODE="ISO8859_1";
	private byte buffer[];
	private int posBuffer=0;
	private InetAddress remote;
	private HashMap headers=new HashMap();
	private boolean endHeader=false;//header‚ð‚·‚×‚Ä“Ç‚ñ‚¾‚©”Û‚©
	private Socket socket;
	
	/**
	 * @param bufferSize
	 */
	public HttpReceiveData(String bufferSizeString) {
		int bufferSize=Integer.parseInt(bufferSizeString);
		buffer=new byte[bufferSize];
	}

	public void recycle(){
	}
	
	public void closeSocket(){
		closeSocket(false);
	}

	public void closeSocket(boolean real){
		if(socket==null){
			return;
		}
		if(!real){
			return;
		}
//		terminalPath.clear();
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.socket=null;
	}
	
	public void setSocket(Socket socket){
		try {
			socket.setTcpNoDelay(true);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		this.remote=socket.getInetAddress();
		this.socket=socket;
	}
	
	public Socket getSocket(){
		return socket;
	}
	
	public HashMap getHeaders(){
		return headers;
	}
	
	private void skipLN(){
		/* “ª‚Ì'\n'‚Í“Ç‚Ý”ò‚Î‚· */
		if(buffer[posBuffer]=='\n'){
			posBuffer++;
		}
	}
	
	private int lineLength(){
		int i=posBuffer;
		while(true){
			if(buffer[i]=='\r'){
				int length=i-posBuffer;
				skipLN();
				return length;
			}
			i++;
		}
	}
	
	private void parseRequestLine(byte[] requestLineByes){
		
		
		String requestLine=new String(requestLineByes,);
		StringTokenizer st=new StringTokenizer(requestLine," ");
		if(!st.hasMoreTokens()){
			logger.error("RequestLine error:" + requestLine);
		}
		method=st.nextToken();
		if(!st.hasMoreTokens()){
			logger.error("RequestLine error:" + requestLine);
		}
		requestUri=st.nextToken();
		if(!st.hasMoreTokens()){
			logger.error("RequestLine error:" + requestLine);
		}
		requestProtocol=st.nextToken();

		if("HTTP/1.1".equalsIgnoreCase(requestProtocol) ){
			keepAlive=true;
		}else{
			keepAlive=false;
		}
	}

	private void parseRequestHeader(String headerLine){
		int pos=headerLine.indexOf(": ");
		String name=headerLine.substring(0,pos);
		String value=headerLine.substring(pos+2);
		requestHeaders.put(name,value);

		if("Connection".equalsIgnoreCase(name)){
			if("Keep-Alive".equalsIgnoreCase(value)){
				keepAlive=true;
			}else if("close".equalsIgnoreCase(value)){
				keepAlive=false;
			}
		}
	}
	
	public boolean readRequestHeader() throws IOException{
		int length=0;
		String req="";
		
		starttime=System.currentTimeMillis();
		
		InputStream is;
		is=socket.getInputStream();
		length=is.read(buffer);
		if(length<=0){
			/* ‰ñü’f */
			logger.error("shutdown error");
			closeSocket(true);
			return false;
		}
		req=new String(buffer,0,length,ENCODE);
		requestString.append(req);
		int pos=requestString.indexOf("\r\n\r\n");
		if( pos<0){
			logger.error("too short header error");
			return false;
		}
		StringTokenizer st=new StringTokenizer(requestString.toString(),"\r\n");
		parseRequestLine(st.nextToken());
		while(st.hasMoreTokens()){
			parseRequestHeader(st.nextToken());
		}
		if( logger.isDebugEnabled() ){
			logger.debug(
					"\r\nrequest start------------------------------\r\n" 
					+requestString.toString()+
					"\r\nrequest end--------------------------------\r\n");
			logger.debug(
					"\r\nparse start--------------------------------\r\n" 
					+ requestHeaders.toString() +
					"\r\nparse end----------------------------------\r\n");
		}
		return true;
	}
	
	private void bufferFlush(OutputStream os) throws IOException{
		os.write(buffer,0,posBuffer);
		if( logger.isDebugEnabled() ){
			logger.debug(
				"\r\nresponse start------------------------------\r\n" 
				+new String(buffer,0,posBuffer,ENCODE)+
				"\r\nresponse end--------------------------------\r\n");
			
		}
		posBuffer=0;
	}
	
	private void bufferWrite(OutputStream os,String str) throws IOException{
		byte[] b=str.getBytes(ENCODE);
		if( (posBuffer+b.length)>=buffer.length){
			bufferFlush(os);
		}
		System.arraycopy(b,0,buffer,posBuffer,b.length);
		posBuffer+=b.length;
	}
	
	private void writeHeader(OutputStream os,String name,String value) throws IOException{
		bufferWrite(os,name);
		if( value!=null){
			bufferWrite(os,": ");
			bufferWrite(os,value);
		}
		bufferWrite(os,"\r\n");
	}
	
	public boolean writeResponseHeader() throws IOException {
		String statusCodeString=Integer.toString(statusCode);
		String reasonPhase=reasonPhaseProperties.getProperty(statusCodeString);
		if(reasonPhase==null){
			reasonPhase="Unkown reason Phase:" + statusCode;
		}
		String responseLine="HTTP/1.0 " + statusCode + " " + reasonPhase;

		OutputStream os;
		os=socket.getOutputStream();
		posBuffer=0;
		writeHeader(os,responseLine,null);
		writeHeader(os,"Date",new Date().toString());
		writeHeader(os,"Server","QueueletHttpServer/0.1");
		Iterator itr=responseHeaders.keySet().iterator();
		while(itr.hasNext()){
			String name=(String)itr.next();
			String value=(String)responseHeaders.get(name);
			writeHeader(os,name,value);
		}
//		writeHeader(os,"Connection", "Keep-Alive");
//		writeHeader(os,"Connection", "close");
		bufferWrite(os,"\r\n");
		bufferFlush(os);
		os.flush();
		return true;
	}

	public boolean writeResponseBody(InputStream response) throws IOException {
		if(response==null){
			return true;
		}
		try {
			OutputStream os=socket.getOutputStream();
			while(true){
				int len=response.read(buffer);
				if(len<=0){
					break;
				}
				os.write(buffer,0,len);
				os.flush();
			}
		}finally{
			try {
				response.close();
			} catch (IOException ignore) {
			}
		}
		return true;
	}
	
	/* ƒŒƒXƒ|ƒ“ƒX‚ÉŠÖ‚·‚éî•ñÝ’è */
	/**
	 * @param statusCode The statusCode to set.
	 */
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	
	public void addResponseHeadr(String name,String value){
		responseHeaders.put(name,value);
	}

	/**
	 * @return Returns the method.
	 */
	public String getMethod() {
		return method;
	}
	/**
	 * @return Returns the requestUri.
	 */
	public String getRequestUri() {
		return requestUri;
	}
}
