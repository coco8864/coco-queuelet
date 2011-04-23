/*
 * Created on 2004/10/20
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.queuelet.manager;

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
public class HttpData{
	static private Logger logger=Logger.getLogger(HttpData.class);
	private static final String ENCODE="ISO8859_1";
	private byte buffer[];
	private int posBuffer;
	private int status;
	private InetAddress remote;
	private Socket socket;
	private int statusCode;
	private HashMap requestHeaders=new HashMap();
	private HashMap responseHeaders=new HashMap();

	private StringBuffer requestString=new StringBuffer();
	private String method;
	private String requestUri;
	private String requestProtocol;
//	private ArrayList terminalPath=new ArrayList();
	
	static private Properties reasonPhaseProperties;
	static{
		InputStream is=null;
		try {
			reasonPhaseProperties=new Properties();
			is=HttpData.class.getResourceAsStream("reasonPhase.properties");
			reasonPhaseProperties.load(is);
		} catch (IOException e) {
		}finally{
			if(is!=null){
				try {
					is.close();
				} catch (IOException ignore) {
				}
			}
		}
	}
	
	/**
	 * @param bufferSize
	 */
	public HttpData(String bufferSizeString) {
		int bufferSize=Integer.parseInt(bufferSizeString);
		buffer=new byte[bufferSize];
	}

	public String toString(){
		StringBuffer sb=null;
		if( remote!=null){
			sb=new StringBuffer(remote.toString());
		}else{
			sb=new StringBuffer("romote is null");
		}
		sb.append(" ");
		sb.append(method);
		sb.append(" ");
		sb.append(requestUri);
		sb.append(" ");
		sb.append(statusCode);
		sb.append(" ");
		sb.append(responseHeaders.get("Content-length"));
		return sb.toString();
	}

	public void recycle(){
		requestString.setLength(0);
		responseHeaders.clear();
		requestHeaders.clear();
		method=null;
		requestUri=null;
		requestProtocol=null;
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
	
	private boolean keepAlive=false;
	
	public boolean canKeepAlive(){
		return keepAlive;
	}

	private void parseRequestLine(String requestLine){
		StringTokenizer st=new StringTokenizer(requestLine," ");
		method=st.nextToken();
		requestUri=st.nextToken();
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
		
		InputStream is;
		is=socket.getInputStream();
		length=is.read(buffer);
		if(length<=0){
			/* ‰ñü’f */
			closeSocket(true);
			return false;
		}
		req=new String(buffer,0,length,ENCODE);
		requestString.append(req);
		int pos=requestString.indexOf("\r\n\r\n");
		if( pos<0){
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

	public boolean writeResponseBody(byte[] response) throws IOException {
		if(response==null){
			return true;
		}
		OutputStream os=socket.getOutputStream();
		os.write(response,0,response.length);
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
