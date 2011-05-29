/*
 * Created on 2004/10/20
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.web;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

/**
 * @author NARU
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class HttpContext{
	private static Logger logger=Logger.getLogger(HttpContext.class);
	private static Configuration config = Config.getInstance().getConfiguration();
	
	public static String ATTRIBUTE_VELOCITY_PAGE="velocityPage";
	public static String ATTRIBUTE_MAPPING_ENTRY="mappingEntry";
	public static String ATTRIBUTE_RESPONSE_FILE="responseFile";
	public static String ATTRIBUTE_RESPONSE_FILE_OFFSET="responseFileOffset";
	public static String ATTRIBUTE_RESPONSE_FILE_LENGTH="responseFileLength";
	public static String ATTRIBUTE_RESPONSE_CONTENT_TYPE="responseContentType";
	public static String ATTRIBUTE_RESPONSE_CONTENT_DISPOSITION="responseContentDisposition";
	
	public static String ATTRIBUTE_REVERSE_SOURCEPATH="reverseSourcePath";
	public static String ATTRIBUTE_REVERSE_DESTINATIONURI="reverseDestinationUri";
	
	public static String CONTENT_LENGTH_HEADER="Content-length";
	public static String CONTENT_TYPE_HEADER="Content-type";
	public static String CONTENT_DISPOSITION_HEADER="Content-disposition";
	public static String IF_MODIFIED_SINCE_HEADER="If-Modified-Since";
	public static String LAST_MODIFIED_HEADER="Last-Modified";
	public static String LOCATION_HEADER="Location";
	public static String COOKIE_HEADER="Cookie";//リクエストヘッダ
	public static String SET_COOKIE_HEADER="Set-Cookie";//レスポンスヘッダ
	public static String PROXY_AUTHORIZATION_HEADER="Proxy-Authorization";//リクエストヘッダ
	public static String PROXY_AUTHENTICATE_HEADER="Proxy-Authenticate";//レスポンスヘッダ
	
	
	private String parameterEncoding="utf-8";
	
	private byte buffer[];
	private InetAddress remote;
	private Socket socket;
	private InputStream requestStream;
	private OutputStream responseStream;
	
	//close時に一緒にcloseするSocket,proxyの場合closeしないとCLOSE_WAITが残る
	private Socket secondSocket;
	
	//private String statusCode;
	private HttpParser requestParser=new HttpParser();
	private String requestMethod;
	private String requestUri;
	private String requestPath;
	private String requestFile;
	private String requestQuery;
	private String requestServer;
	private int requestPort;
	private Map parameter=new HashMap();
	private boolean parameterSetup=false;
	private boolean proxyRequest=false;
	
	//レスポンス用バッファ,再利用するためにここに持つ
	private HttpParser responseParser=new HttpParser();
	private long responseLength;
	private List timeCheckPoints=new ArrayList();
	
	private boolean keepAlive=false;
	
	//リクエストに付随する属性
	private Map attribute=new HashMap();
	
	//ResponseFactoryからResponseQueueに依頼
	private PipedOutputStream responsHeaderOutputStream;

	//ResponseQueueからresponseStreamに出力
	private PipedInputStream responsHeaderInputStream;
	
	//アクセス情報を記録するためのオブジェクト
	private AccessLog accessLog;
	
	/**
	 * @param bufferSize
	 */
	public HttpContext(String bufferSizeString) {
		this(Integer.parseInt(bufferSizeString));
	}
	public HttpContext(int bufferSize) {
		buffer=new byte[bufferSize];
		headerDateFormat=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",Locale.US);
		headerDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public void checkPoint(){
		timeCheckPoints.add(new Long(System.currentTimeMillis()));
	}
	
	public void recycle(){
		requestParser.recycle();
		responseParser.recycle();
		timeCheckPoints.clear();
		attribute.clear();
		parameter.clear();
		parameterSetup=false;
		secondSocket=null;
	}
	
	public void closeSocket(boolean real){
		if(secondSocket!=null){
			try {
				secondSocket.close();
			} catch (IOException ignore) {
				logger.debug("secondSocket close error",ignore);
			}
		}
		secondSocket=null;
		
		if(socket==null){
			logger.debug("socket is null.");
			return;
		}
		try {
			if( requestStream!=null){
				requestStream.close();
			}
		} catch (IOException ignore) {
			logger.debug("requestStream close error",ignore);
		}
		this.requestStream=null;
		try {
			if( responseStream!=null ){
				responseStream.close();
			}
		} catch (IOException ignore) {
			logger.debug("responseStream close error",ignore);
		}
		this.responseStream=null;
		
		//realがfalseの時は、Keep-Aliveのためにsocketを残したいのだが実装できていない
		if(!real){
			return;
		}
		try {
			socket.close();
		} catch (IOException ignore) {
			logger.debug("socket close error",ignore);
		}
		this.socket=null;
	}
	
	public void setSecondSocket(Socket secodSocket) {
		this.secondSocket=secodSocket;
	}
	
	public void setSocket(Socket socket) {
		try {
			socket.setTcpNoDelay(true);
			socket.setSoTimeout(config.getInt("socketReadTimeout", 10000));
		} catch (SocketException e) {
			logger.error("fail to set socket option.",e);
			throw new RuntimeException("fail to set socket option.",e);
		}
		this.remote=socket.getInetAddress();
		this.socket=socket;
		this.requestStream=null;
		this.responseStream=null;
		
		//レスポンス実行(response)とレスポンスコンテンツ生成(ResponseFactory)が別スレッドで動作するため
		this.responsHeaderOutputStream=new PipedOutputStream();
		try {
			this.responsHeaderInputStream=new PipedInputStream(this.responsHeaderOutputStream);
		} catch (IOException e) {
			//こんなところで例外になる訳がない、されても困る
			logger.error("Internal error",e);
			throw new RuntimeException("Internal error.",e);
		}
	}
	
	public void setupAccessLog(AccessLog accessLog){
		accessLog.setStartTime(new Date());
		accessLog.setIp(getClientIp());
		this.accessLog=accessLog;
	}
	
	//ヘッダが読み込まれ、かつボディが読み込まれていない状態で呼び出される事が前提
	public void peekRequest(PeekStream requestPeeker) throws IOException{
		requestStream=requestPeeker.peekInputStream(getRequestStream());
		//requestParserの入力ストリームは、既に設定済みであるが、ピークするために再度設定する。
		requestParser.setBodyStream(requestStream);
	}
	
	//レスポンスについては、一切出力していない事が前提
	public void peekResponse(PeekStream requestPeeker) throws IOException{
		responseStream=requestPeeker.peekOutputStream(getResponseStream());
	}
	
	private InputStream getRequestStream() throws IOException{
		if(requestStream==null){
			requestStream=new BufferedInputStream(socket.getInputStream());
		}
		return requestStream;
	}
	
	private OutputStream getResponseStream() throws IOException{
		if(responseStream==null){
			responseStream=socket.getOutputStream();
		}
		return responseStream;
	}
	
	public Socket getSocket(){
		return socket;
	}
	
	public String getClientIp(){
		return remote.getHostAddress();
	}
	
	public boolean canKeepAlive(){
		return keepAlive;
	}
	
	public HttpParser getRequestParser(){
		return requestParser;
	}
	
	//reverseProxyのために作った
	public HttpParser getResponseParser(){
		return responseParser;
	}
	
	public String getRequestHeader(String name){
		return requestParser.getHeader(name);
	}
	
	public void setRequestHeader(String name,String value){
		requestParser.setHeader(name, value);
	}
	
	public void removeRequestHeader(String name){
		requestParser.removeHeader(name);
	}
	
	public Iterator getRequestHeaderNames(){
		return requestParser.getHeaderNames();
	}
	
	public InputStream getRequestBodyStream(){
		return requestParser.getBodyStream();
	}
	
	/* +ブラウザからのリクエストデータを読み込む */
	public boolean parseRequestHeader() throws IOException{
		requestParser.parse(getRequestStream());//+
		accessLog.setRequestLine(requestParser.getRequestLine());
		accessLog.setRequestHeaderLength(requestParser.getHeaderLength());
		
		requestMethod=requestParser.getMethod();
		requestUri=requestParser.getUri();
		if( requestUri.startsWith("http://")){
			//proxyへのリクエスト
			proxyRequest=true;
			try {
				URL url = new URL(requestUri);
				requestServer=url.getHost();
				requestPort=url.getPort();
				requestPath=url.getPath();
				requestFile=url.getFile();
				requestQuery=url.getQuery();
				if(requestPort<0){
					requestPort=80;
				}
			} catch (MalformedURLException e) {
				logger.error("URL error:" + getRequestUri(),e);
				throw new RuntimeException("URL error:" + getRequestUri(),e);
			}
		}else{
			//Webサーバへのリクエスト
			proxyRequest=false;
			requestServer=requestParser.getHeader("Host");
			int pos=requestUri.indexOf("?");
			if(pos>0){
				requestQuery=requestUri.substring(pos+1);
				requestPath=requestUri.substring(0,pos);
			}else{
				requestQuery="";
				requestPath=requestUri;
			}
			pos=requestPath.lastIndexOf("/");
			if(pos>0){
				requestFile=requestPath.substring(pos+1);
			}else{
				requestFile=requestPath;
			}
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
		}
		return true;
	}
	
	//proxyモードの場合、レスポンス全体を貰う
	//レスポンスを組み立てるが、ブラウザへのレスポンスはしない。
	public void setResponseStream(InputStream is) throws IOException{
//		if(!isProxyRequest()){//proxyへのリクエストである事を確認
//			throw new IllegalStateException("not proxy request");
//		}
		responseParser.parse(is);
	}
	
	//serverモードの場合、ボディストリームを貰う
	public void setResponseBody(InputStream is){
		responseParser.setBodyStream(is);
	}
	
	/* このメソッドを呼び出すとかならずisはクローズする */
	private int writeResponse(InputStream is,boolean throwIOException) throws IOException{
		int writeLength=0;
		if(is==null){
			return 0;
		}
		OutputStream os=getResponseStream();
		try {
			while(true){
				int len=is.read(buffer);
				if(len<=0){
					break;
				}
				os.write(buffer,0,len);
				writeLength+=len;
			}
		} catch (IOException e) {
			logger.warn("response IO Error", e);
			if(throwIOException){
				throw e;
			}
		}finally{
//			os.flush();
			try {
				is.close();//ここでクローズしてよいのか？よい！！openしたスレッドと読み込むスレッドが違う
			} catch (IOException ignore) {
			}
		}
		return writeLength;
	}
	
	//ResponseQueueから呼び出す
	public void responseDirect() throws IOException{
		try{
			/* ヘッダをレスポンス、例外の可能性あり */
			writeResponse(responsHeaderInputStream,true);
			String statusCode=responseParser.getStatusCode();
			accessLog.setStatusCode(statusCode);
			/* ボディをレスポンス */
			responseLength=writeResponse(responseParser.getBodyStream(),false);
			responseParser.setBodyStream(null);
			accessLog.setResponseLength(responseLength);
		}finally{
			accessLog.endProcess(timeCheckPoints);
			OutputStream os=getResponseStream();
			try {
				os.flush();
			} catch (IOException e) {
				logger.warn("os flush error",e);
			}
			//出力ラインをクローズし、クライアントにレスポンスの終了を通知する
			try {
				socket.shutdownOutput();
			} catch (IOException e) {
				logger.warn("shutdownOutput error",e);
			}
			InputStream bodyStream=responseParser.getBodyStream();
			if(bodyStream!=null){
				try {
					bodyStream.close();
				} catch (IOException e) {
					logger.warn("bodyStram close error",e);
				}
				responseParser.setBodyStream(null);
			}
			responsHeaderInputStream=null;
		}
	}
	
	//ResponseFactoryから呼び出し
	//このメソッドから有効なStreamが返却された場合は、必ずcloseする事
	//しないと、ResponseQueueがブロックします。
	public OutputStream getResponseStream(String statusCode,String contentType) {
		PipedOutputStream os=null;
		try {
			PipedInputStream is=new PipedInputStream();
			os = new PipedOutputStream(is);
			registerResponse(statusCode,contentType,is);
		} catch (IOException e) {
			registerResponse("500","getResponseStream IO error");
			logger.error("getResponseStream IO error",e);
		} catch (Throwable e) {
			registerResponse("500","getResponseStream someting error");
			logger.error("getResponseStream something error",e);
		} finally{
			startResponse();//レスポンス開始
		}
		return os;
	}
	
	//このメソッドから有効なWriterが返却された場合は、必ずcloseする事
	//しないと、ResponseQueueがブロックします。
	public Writer getResponseOut(String statusCode,String contentType) {
		OutputStream os=getResponseStream(statusCode,contentType);
		if(os==null){
			return null;
		}
		Writer out=null;
		try {
			out = new OutputStreamWriter(os,"utf-8");
		} catch (UnsupportedEncodingException e) {
			logger.error("getResponseOut error",e);//utf-8が理解できない場合
			try {
				os.close();
			} catch (IOException ignore) {
			}
		}
		return out;
	}
	
	//ResponseFactoryから呼び出し
	// コンテンツなし、
	private static byte[] emptyContent=new byte[0];
	public boolean registerResponse(String statusCode) {
		return registerResponse(statusCode,null,new ByteArrayInputStream(emptyContent));
	}
	
	//ResponseFactoryから呼び出し
	public boolean registerResponse(String statusCode,String text) {
		return registerResponse(statusCode,"text/plain; charset=utf-8",text);
	}
	
	//ResponseFactoryから呼び出し
	public boolean registerResponse(String statusCode,String contentType,String text) {
		byte[] contents=null;
		try {
			contents = text.getBytes("utf-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("InternalError",e);
		}
		setResponseHeader(CONTENT_LENGTH_HEADER,String.valueOf(contents.length));
		return registerResponse(statusCode,contentType,new ByteArrayInputStream(contents));
	}
	
	//ResponseFactoryから呼び出し
	public boolean registerResponse(String statusCode,String contentType,InputStream contentStream) {
		setStatusCode(statusCode);
		if( contentType!=null){
			setResponseHeader(CONTENT_TYPE_HEADER,contentType);
		}
		responseParser.setBodyStream(contentStream);
		return registerResponse();
	}
	
	//ResponseFactoryから呼び出し,自分でヘッダを作らないResponseFactoryから呼び出される、Proxy,Replay,Reverse
	public boolean registerResponse() {
		String httpVersion=responseParser.getResHttpVersion();
		if(httpVersion==null){//レスポンスバージョンが確定していない場合、myProxyがWebサーバ
			//myProxyがWebサーバなのでServerヘッダを追加
			responseParser.setResHttpVersion("HTTP/1.0");
			responseParser.setHeader("Server","QueueletHttpServer/0.4");
			responseParser.setHeader("Date", fomatDateHeader(new Date()));
		}
		return true;
	}
	
	//ResponseFactoryから呼び出し
	//responseHeaderOutputStreamを閉じないとResponseができなくなるため必ず実施する事
	//呼び出す前にBodyStreamを確定させておくこと
	public void startResponse(){
		if(responsHeaderOutputStream==null){
			return;
		}
		try {
			responseParser.writeSeriarizeHeader(responsHeaderOutputStream);
		} catch (IOException e) {
			logger.error("writeSeriarizeHeader error",e);
		}finally{
			try {
				responsHeaderOutputStream.close();
			} catch (IOException ignore) {}
			responsHeaderOutputStream=null;
		}
	}
	
	/* レスポンスに関する情報設定 */
	//動的コンテンツの場合でキャッシュさせない
	public void setNoCacheResponseHeaders(){
		setResponseHeader("Pragma","no-cache");
		setResponseHeader("Cache-Control","no-cache");
		setResponseHeader("Expires","Thu, 01 Dec 1994 16:00:00 GMT");
	}
	
	/**
	 * @param statusCode The statusCode to set.
	 */
	public void setStatusCode(String statusCode) {
		responseParser.setStatusCode(statusCode);
	}
	public void setResHttpVersion(String httpVersion) {
		responseParser.setResHttpVersion(httpVersion);
	}
	
	public void addResponseHeader(String name,String value){
		responseParser.addHeader(name, value);
	}
	
	public void setResponseHeader(String name,String value){
		responseParser.removeHeader(name);
		responseParser.addHeader(name, value);
	}
	
	public void removeResponseHeadr(String name){
		responseParser.removeHeader(name);
	}
	
	//使いまわすため
	public byte[] getBuffer(){
		return buffer;
	}
	
	/**
	 * @return Returns the method.
	 */
	public String getMethod() {
		return requestMethod;
	}
	/**
	 * @return Returns the requestUri.
	 */
	public String getRequestUri() {
		return requestUri;
	}
	
	public String getRequestQuery() {
		return requestQuery;
	}
	
	public String getRequestServer(){
		return requestServer;
	}
	
	public int getRequestServerPort(){
		return requestPort;
	}
	
	public String getRequestPath(){
		return requestPath;
	}
	
	public String getRequestFile(){
		return requestFile;
	}
	
	public boolean isProxyRequest() {
		return proxyRequest;
	}
	
	public static int  IndexOfByte( byte bytes[], int off, int end, char qq ){
        // Works only for UTF 
        while( off < end ) {
            byte b=bytes[off];
            if( b==qq )
                return off;
            off++;
        }
        return -1;
    }

	private void processParameters(byte bytes[], int start, int len, String enc) {
		int end = start + len;
		int pos = start;
		do {
			boolean noEq = false;
			int valStart = -1;
			int valEnd = -1;

			int nameStart = pos;
			int nameEnd = IndexOfByte(bytes, nameStart, end, '=');
			// Workaround for a&b&c encoding
			int nameEnd2 = IndexOfByte(bytes, nameStart, end, '&');
			if ((nameEnd2 != -1) && (nameEnd == -1 || nameEnd > nameEnd2)) {
				nameEnd = nameEnd2;
				noEq = true;
				valStart = nameEnd;
				valEnd = nameEnd;
			}
			if (nameEnd == -1)
				nameEnd = end;

			if (!noEq) {
				valStart = (nameEnd < end) ? nameEnd + 1 : end;
				valEnd = IndexOfByte(bytes, valStart, end, '&');
				if (valEnd == -1)
					valEnd = (valStart < end) ? end : valStart;
			}

			pos = valEnd + 1;

			if (nameEnd <= nameStart) {
				continue;
				// invalid chunk - it's better to ignore
			}
			String encName=new String(bytes, nameStart, nameEnd - nameStart);
			String encValue=new String(bytes, valStart, valEnd - valStart);
			try {
				String name=URLDecoder.decode(encName,enc);
				String value=URLDecoder.decode(encValue,enc);
				addParameter(name,value);
			} catch (IOException e) {
				// Exception during character decoding: skip parameter
				logger.warn("Parameters: Character decoding failed. "
						+ "Parameter skipped.", e);
			}
		} while (pos < end);
	}

	/**
	 * Read post body in an array.
	 */
	private int readPostBody(byte body[], int len) throws IOException {
		int offset = 0;
		do {
			int inputLen = requestParser.getBodyStream().read(body, offset, len - offset);
			if (inputLen <= 0) {
				return offset;
			}
			offset += inputLen;
		} while ((len - offset) > 0);
		return len;
	}

	private void setupParameter() throws IOException {
		parameterSetup=true;
		//GETの場合は、queryを見る
		if(!"POST".equalsIgnoreCase(requestMethod)){
			if(requestQuery==null){
				return;
			}
			byte[] queryBytes=requestQuery.getBytes();
			processParameters(queryBytes,0,queryBytes.length,parameterEncoding);
			return;
		}
		String contentType = requestParser.getContentType();
		if (contentType == null) {
			return;
		}
		int semicolon = contentType.indexOf(';');
		if (semicolon >= 0) {
			contentType = contentType.substring(0, semicolon).trim();
		} else {
			contentType = contentType.trim();
		}
		if (!("application/x-www-form-urlencoded".equals(contentType))) {
			return;
		}
		long contentLength=requestParser.getContentLength();
		if (buffer.length < (int)contentLength) {
			throw new RuntimeException("too long post body." + contentLength);
		}
		int actualLen = readPostBody(buffer, (int)contentLength);
		if (actualLen != contentLength) {
			throw new RuntimeException("short post body." + actualLen + "/"
					+ contentLength);
		}
		processParameters(buffer,0,(int)contentLength,parameterEncoding);
	}
	
	public Map getParameterMap() throws IOException {
		if (parameterSetup==false) {
			setupParameter();
		}
		return parameter;
	}

	public List getParameters(String name) throws IOException {
		if (parameterSetup==false) {
			setupParameter();
		}
		return (List) parameter.get(name);
	}
	
	private void addParameter(String name, String value) throws IOException {
		List values = getParameters(name);
		if (values == null) {
			values = new ArrayList();
			parameter.put(name, values);
		}
		values.add(value);
	}

	public String getParameter(String name) throws IOException {
		List parameters = getParameters(name);
		if(parameters==null){
			return null;
		}
		return (String) parameters.get(0);
	}

	public Iterator getParameterNames() throws IOException {
		if (parameterSetup==false) {
			setupParameter();
		}
		return parameter.keySet().iterator();
	}
	
	public Object getAttribute(String name){
		return attribute.get(name);
	}
	
	public void setAttribute(String name, Object value) {
		attribute.put(name, value);
	}

	public Iterator getAttributeNames(){
		return attribute.keySet().iterator();
	}
	
	public AccessLog getAccessLog(){
		return accessLog;
	}
	
	//通過したQUEUEを記録する
	public void passQueue(long processQueue){
		accessLog.passProcessQueue(processQueue);
	}
	
	private SimpleDateFormat headerDateFormat;
	public synchronized Date parseDateHeader(String header){
		if(header==null){
			return null;
		}
		String fields[]=header.split(";");
		try {
			return headerDateFormat.parse(fields[0]);
		} catch (ParseException e) {
			logger.warn("fail to parse date header." + header);
			return null;
		}
	}
	
	public String fomatDateHeader(long time){
		return fomatDateHeader(new Date(time));
	}
	
	public synchronized String fomatDateHeader(Date date){
		return headerDateFormat.format(date);
	}
	
}
