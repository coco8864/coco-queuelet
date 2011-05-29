/*
 * Created on 2004/10/20
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.http;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import naru.util.HttpParser;

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
	private InetAddress remote;
	private Socket socket;
	private int statusCode;
//	private HashMap requestHeaders=new HashMap();
	private HttpParser requestParser=new HttpParser();
	
	//自分でレスポンスを組み立てる場合に使用
	private HashMap responseHeaders=new HashMap();
	
	//レスポンスストリームを貰う場合に使用
	private HttpParser responseParser=new HttpParser();
	
	private long responseLength;
	private long requestLength;
	
	private long startTime;
	
	//requestHeader,RequestBody,ResponseHeader,ResponseBodyを別ファイルに格納する
	private ZipOutputStream logStream;

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
		this(Integer.parseInt(bufferSizeString));
	}
	public HttpData(int bufferSize) {
		buffer=new byte[bufferSize];
	}

	private void acclog(){
		StringBuffer sb=new StringBuffer(remote.getHostAddress());
		sb.append(" ");
		sb.append(requestParser.getMethod());
		sb.append(" ");
		sb.append(requestParser.getUri());
		sb.append(" ");
		sb.append(statusCode);
		sb.append(" ");
		sb.append(responseLength);
		sb.append(" ");
		sb.append(System.currentTimeMillis()-startTime);
		logger.info(sb.toString());
	}

	public void recycle(){
		requestParser.recycle();
		responseHeaders.clear();
		responseParser.recycle();
		
		/*
		StringBuffer logNameSb=new StringBuffer("");
		logNameSb.append(System.currentTimeMillis());
		logNameSb.append(".zip");
		String logName=logNameSb.toString();
		logger.debug("logName:"+logName);
		try {
			logStream=new ZipOutputStream(new FileOutputStream(logName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		*/
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
	
	public String getRequestHeader(String name){
		return requestParser.getHeader(name);
	}
	
	public Iterator getRawRequestHeaders(){
		return requestParser.getRawHeaders();
	}
	
	public InputStream getRequestBodyStream(){
		return requestParser.getBodyStream();
	}
	
	// +リクエストbodyをosに出力する
	public void sendRequestBody(OutputStream os) throws IOException{
		requestLength=0;
		String headerRequestLengthString=requestParser.getHeader("Content-length");
		if( headerRequestLengthString==null ){
			return;
		}
		int headerRequestLength=Integer.parseInt(headerRequestLengthString);
		if( headerRequestLength==0 ){
			return;
		}
		//入出力データをログする場合
		if(logStream!=null){
			ZipEntry ze=new ZipEntry("RequestBody");
			logStream.putNextEntry(ze);
		}
		InputStream bodyStream=getRequestBodyStream();
		while(true){
			int len=bodyStream.read(buffer);
			if(len<=0){
				break;
			}
			requestLength+=len;
			os.write(buffer,0,len);
			if( logStream!=null){
				logStream.write(buffer,0,len);
			}
			if( headerRequestLength<=requestLength ){
				break;
			}
		}
		os.flush();
	}
	
	private String requestUri;
	private String requestServer;
	private int requestPort;
	
	/* +ブラウザからのリクエストデータを読み込む */
	public boolean parseRequestHeader() throws IOException{
		startTime=System.currentTimeMillis();
		
		InputStream is;
		is=socket.getInputStream();
		//入出力データをログする場合
		if(logStream!=null){
			ZipEntry ze=new ZipEntry("RequestHeader");
			logStream.putNextEntry(ze);
		}
		requestParser.parse(is,logStream);//+
		requestUri=requestParser.getUri();
		if( requestUri.startsWith("http://")){
			try {
				URL url = new URL(requestUri);
				requestServer=url.getHost();
				requestPort=url.getPort();
				if(requestPort<0){
					requestPort=80;
				}
			} catch (MalformedURLException e) {
				logger.error("URL error:" + getRequestUri(),e);
				throw new RuntimeException("URL error:" + getRequestUri(),e);
			}
		}else{
			requestServer=requestParser.getHeader("Host");
		}
		String version=requestParser.getReqHttpVersion();
		if("HTTP/1.1".equalsIgnoreCase(version) ){
			keepAlive=true;
		}else{
			keepAlive=false;
		}
		
		String connection=requestParser.getHeader("Connection");
		if("Keep-Alive".equalsIgnoreCase(connection)){
			keepAlive=true;
		}else if("close".equalsIgnoreCase(connection)){
			keepAlive=false;
		}
		
		if( logger.isDebugEnabled() ){
			logger.debug("requestLine:" + requestParser.getRequestLine());
			logger.debug("requestHeader:");
			Iterator itr=requestParser.getRawHeaders();
			while(itr.hasNext()){
				logger.debug(itr.next());
			}
			logger.debug("requestHeader END:");
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
		bufferWrite(os,responseLine);
		bufferWrite(os,"\r\n");
		writeHeader(os,"Date",new Date().toString());
		writeHeader(os,"Server","QueueletHttpServer/0.2");
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
	
//	public boolean writeResponseBody(String contents) throws IOException {
//		return writeResponseBody(new ByteArrayInputStream(contents.getBytes()));
//	}

	//+ responseには、レスポンスBodyだけが入ってくる
	public boolean writeResponseBody(InputStream response) throws IOException {
		if(logStream!=null){
			ZipEntry ze=new ZipEntry("ResponseBody");
			logStream.putNextEntry(ze);
		}
		responseLength=0;
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
				responseLength+=len;
				os.write(buffer,0,len);
				if(logStream!=null){
					logStream.write(buffer,0,len);
				}
			}
			os.flush();
		}finally{
			try {
				response.close();//ここでクローズしてよいのか？
			} catch (IOException ignore) {
			}
		}
		acclog();
		if(logStream!=null){
			logStream.close();
			logStream=null;
		}
		return true;
	}
	
	public boolean writeResponse(int statusCode,String text) throws IOException {
		return writeResponse(statusCode,text,"text/plain");
	}
	
	public boolean writeResponse(int statusCode,String text,String contentType) throws IOException {
		byte[] contents=text.getBytes(ENCODE);
		setStatusCode(statusCode);
		addResponseHeadr("Content-length",String.valueOf(contents.length));
		addResponseHeadr("Content-type",contentType);
		writeResponseHeader();
		writeResponseBody(new ByteArrayInputStream(contents));
		return true;
	}
	
	/* レスポンスに関する情報設定 */
	/**
	 * @param statusCode The statusCode to set.
	 */
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	
	public void addResponseHeadr(String name,String value){
		responseHeaders.put(name,value);
	}
	
	//+ isには、レスポンスHeaderとBodyが両方入ってくる
	public void responseStream(InputStream is) throws IOException{
		//入出力データをログする場合
		if(logStream!=null){
			ZipEntry ze=new ZipEntry("ResponseHeader");
			logStream.putNextEntry(ze);
		}
		responseParser.parse(is,logStream);//+ここでHeader部分を読み込む
		setStatusCode(Integer.parseInt(responseParser.getStatusCode()));
		OutputStream os;
		os=socket.getOutputStream();
		posBuffer=0;
		bufferWrite(os,responseParser.getStatusLine());
		bufferWrite(os,"\r\n");
		Iterator itr=responseParser.getRawHeaders();
		while(itr.hasNext()){
			bufferWrite(os,(String)itr.next());
			bufferWrite(os,"\r\n");
		}
		bufferWrite(os,"\r\n");
		bufferFlush(os);
		os.flush();
		writeResponseBody(responseParser.getBodyStream());//+ここでBody部分を読み込む
	}

	/**
	 * @return Returns the method.
	 */
	public String getMethod() {
		return requestParser.getMethod();
	}
	/**
	 * @return Returns the requestUri.
	 */
	public String getRequestUri() {
		return requestUri;
	}
	
	public String getRequestServer(){
		return requestServer;
	}
	
	public int getRequestServerPort(){
		return requestPort;
	}
	
}
