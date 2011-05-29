/*
 * Created on 2004/10/20
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.web;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;

/**
 * @author NARU
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Request implements Queuelet {
	static private Logger logger=Logger.getLogger(Request.class);
	private static String webAuthenticateForm="auth/webAuthenticate.vm";
	private QueueletContext context;
	private Config config;
	private Configuration configuration;
	private Set clientIps;
	//proxyとして動作する時のAuthentication
	private String proxyAuthenticate=null;
	//webサーバとして動作する時(reverseproxy,file)のAuthentication
	private String webAuthenticate=null;
	private String webAuthenticateCookieKey=null;
	private Pattern webAuthenticatePattern=null;
	
	//WWW-Authenticate: Basic realm="FIND2",Basicはreverseの事を考えると使えない
	//Authorization: Basic XXXbase64XXX
	//"Proxy-Authenticate", "Basic Realm=\"myProxy\"",proxyの場合こちらを使う
	//Proxy-Authorization: Basic XXXbase64XXX
	//Response　Queueに依頼する前にレスポンスを返却したい場合
	private void responseDirect(HttpContext httpContext,String statusCode,String text){
		httpContext.registerResponse(statusCode,text);
		httpContext.startResponse();
		try {
			httpContext.responseDirect();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		context.enque(httpContext,Config.QUEUE_CLOSE);
	}
	
	private MappingEntry proxyAuthentication(HttpContext httpContext,MappingEntry entry) {
		String paHeader=httpContext.getRequestHeader(HttpContext.PROXY_AUTHORIZATION_HEADER);
		httpContext.removeRequestHeader(HttpContext.PROXY_AUTHORIZATION_HEADER);
		if(proxyAuthenticate==null){//認証なしモード
			return entry;
		}
		if( paHeader!=null){
			String[] paParts=paHeader.split(" ");
			if( paParts.length>=2 &&
				"Basic".equalsIgnoreCase(paParts[0]) &&
				proxyAuthenticate.equalsIgnoreCase(paParts[1]) ){
				return entry;//認証成功
			}
		}
		//認証失敗
		httpContext.addResponseHeader("Proxy-Authenticate", "Basic Realm=\"myProxy\"");
		httpContext.registerResponse("407","myProxy Proxy-Authenticate");
		httpContext.startResponse();
		return null;//自分でコンテンツを作ったのでentryなし
	}
	
	private MappingEntry responseWebAuthenticationForm(HttpContext httpContext,String orgPath) {
		//認証画面をVelocityPageから出力する
		if(orgPath==null){
			orgPath=httpContext.getRequestUri();
		}
		httpContext.setAttribute("orgPath",orgPath);
		httpContext.setAttribute(HttpContext.ATTRIBUTE_VELOCITY_PAGE,webAuthenticateForm);
		return MappingEntry.velopageEntry;
	}
	
    private String stripQuote( String value ){
    	if (((value.startsWith("\"")) && (value.endsWith("\""))) ||
    			((value.startsWith("'") && (value.endsWith("'"))))) {
    		try {
    			return value.substring(1,value.length()-1);
    		} catch (Exception ex) { 
    		}
    	}
    	return value;
    }  
	
	//Cookie: FujitsuWebsite_common_01=k7.fujitsu.co.jp.63941208497096656; NIN=1; RIYOU=4; OUSU=UgxIaMwUtKwIN3hwh8OiYYcrBNVyVpK3HlrKr15VacKsBFa_bDTDO3x40ronDNDJrMuu1v/uYVZ4n5eAg1in7tJJQhicQHsdDZzU3FB0gWDMyVd0cyW7pQ==; POTO=0
	//Set-Cookie: JSESSIONID=54KAU74VEVVUM8AVE7NJKNIIJ08114SFEH6VTFT3IH12CJU17R7RMRDEIKDG20001G000000.Naru001_001
	private String getCookieAuthAndFilter(HttpContext httpContext) {
		String cookieHeader=httpContext.getRequestHeader(HttpContext.COOKIE_HEADER);
		if(cookieHeader==null){
			return null;
		}
		Matcher matcher;
		synchronized(webAuthenticatePattern){
			matcher = webAuthenticatePattern.matcher(cookieHeader);
		}
		if(matcher.find()==false){
			return null;
		}
		String cookieAuth=matcher.group(1);
		
		//webAuthenticateCookieKeyがあったなら、抜いたものを設定する。
		httpContext.removeRequestHeader(HttpContext.COOKIE_HEADER);
		String updateCookie=matcher.replaceAll("");
		if(!"".equals(updateCookie)){//webAuthenticateCookieKeyを抜いた結果何か残れば
			httpContext.setRequestHeader(HttpContext.COOKIE_HEADER,updateCookie);
		}
		//"や'で括られている場合は、削除する
		return stripQuote(cookieAuth);
	}
	
	private MappingEntry webAuthentication(HttpContext httpContext,MappingEntry entry) {
		if(webAuthenticate==null){
			return entry;//認証なしモード
		}
		String cookieAuth=getCookieAuthAndFilter(httpContext);
		
		String uri=httpContext.getRequestUri();
		String orgPath=null;
		if(uri.indexOf(webAuthenticateForm)>=0){//認証画面からのリクエスト
			String user;
			String pass;
			try {
				user = httpContext.getParameter("user");
				pass = httpContext.getParameter("pass");
				orgPath = httpContext.getParameter("orgPath");
			} catch (IOException e) {//認証失敗と判断
				logger.warn("webAuthenticate fail to getParameter.",e);
				return responseWebAuthenticationForm(httpContext,orgPath);
			}
			String inputAuth=encodeBase64(user+":"+pass);
			if(webAuthenticate.equals(inputAuth)){//認証成功
				httpContext.addResponseHeader("Set-Cookie", webAuthenticateCookieKey +"=" +webAuthenticate + "; path=/");
				String location="http://"+httpContext.getRequestServer()+orgPath;
				httpContext.addResponseHeader(HttpContext.LOCATION_HEADER,location);
				httpContext.registerResponse("302","success webAuthenticate");
				httpContext.startResponse();
				return null;//自分でコンテンツを作ったのでentryなし
			}
		}
		if(cookieAuth!=null&&webAuthenticate.equals(cookieAuth)){
			return entry;//認可成功
		}
		
		//認可、認証失敗、認証Formをレスポンスする。webAuthenticateFormからのリクエストでない限りorgPathは,null
		return responseWebAuthenticationForm(httpContext,orgPath);
	}

	//注意：認証が必要ない場合もPROXY_AUTHORIZATION_HEADERやCookieヘッダを削除する必要がある。
	private MappingEntry authentication(HttpContext httpContext,MappingEntry entry) {
		String queue=entry.getQueue();
		//Replay対象は認証をバイパスする
		if(queue.equals(Config.QUEUE_REPLAY)){
			return entry;
		}
		//proxy認証が必要な場合
		if(queue.equals(Config.QUEUE_PROXY)){
			return proxyAuthentication(httpContext,entry);
		}
		//web認証が必要な場合
		return webAuthentication(httpContext,entry);
	}
	
	//URIと設定から呼び出し先QUEUEを決める。
	private MappingEntry mappingQueue(HttpContext httpContext){
		String uri=httpContext.getRequestUri();
		if( MappingEntry.controllerEntry.matches(uri) ){//controllerへのリクエストか？
			httpContext.setAttribute(HttpContext.ATTRIBUTE_MAPPING_ENTRY, MappingEntry.controllerEntry);
			return MappingEntry.controllerEntry;
		}
		String clientIp=httpContext.getClientIp();
		
		//リクエストがReplay対象か？
		if( config.isReplay(clientIp, uri)){
			return MappingEntry.replayEntry;
		}
		if(httpContext.isProxyRequest()){
			//TODO 強制的にWebサーバとして動作するモード要,
			//requestLineがhttp://から始まってもクライアントが間違って送ってきたと判断する
			return MappingEntry.proxyEntry;
		}
		MappingEntry mappingEntry=config.mapping(uri);
		if( mappingEntry==null){
			return null;
		}
		
		//マッピング先がReplay対象か？
		if( config.isReplay(clientIp, mappingEntry.getDestination())){
			return MappingEntry.replayEntry;
		}
		httpContext.setAttribute(HttpContext.ATTRIBUTE_MAPPING_ENTRY, mappingEntry);
		return mappingEntry;
	}
	
	private boolean isLogging(String clientIp,String key){
		boolean defaultValue=configuration.getBoolean(key,false);
		return configuration.getBoolean(clientIp+"."+key,defaultValue);
	}
	
	private void setupTraceLog(HttpContext httpContext,String queue,AccessLog accessLog) throws IOException{
		String clientIp=httpContext.getClientIp();
		//DBへのアクセスログ採取有無
		if( !isLogging(clientIp,"accessDb." + queue) ){
			return;
		}
		//DBに記録する
		accessLog.insert();
		//リクエストStreamのpeek処理
		if(isLogging(clientIp,"accessTrace.request." + queue) ){
			PeekStream requestPeeker=accessLog.setupPeekRequest();
			httpContext.peekRequest(requestPeeker);
			context.enque(requestPeeker, Config.QUEUE_PEEK);
				
			//既に受け取っているヘッダ部分をPeekStreamに流し込む
			HttpParser requestParser=httpContext.getRequestParser();
			OutputStream os=requestPeeker.getPeekOutputStream();
			requestParser.writeSeriarizeHeader(os);
		}
		//レスポンスStreamのpeek処理
		if(isLogging(clientIp,"accessTrace.response." + queue) ){
			PeekStream responsePeeker=accessLog.setupPeekResponse();
			httpContext.peekResponse(responsePeeker);
			context.enque(responsePeeker, Config.QUEUE_PEEK);
		}
	}
	
	private boolean checkIp(Socket socket){
		if( clientIps==null){
			return true;
		}
		String clietnIp=socket.getInetAddress().getHostAddress();
		if(!clientIps.contains(clietnIp)){
			logger.warn("Illigal clientIp."+clietnIp);
			try {
				socket.close();
			} catch (IOException e) {
				logger.warn("Illigal clientIp."+clietnIp+ " close error",e);
			}
			return false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		HttpContext httpContext=null;
		if(req instanceof Socket){
			Socket socket=(Socket)req;
			//指定IPからのリクエストかどうかをチェック
			if( checkIp(socket)==false ){
				return false;
			}
			httpContext=(HttpContext)context.deque(Config.QUEUE_HTTPCONTEXT_POOL);
			httpContext.setSocket(socket);
		}else{//KeepAliveを実装する時はこちら
			httpContext=(HttpContext)req;
		}
		
		//処理の起点がaccessLogの中に採られる
		AccessLog accessLog=(AccessLog)context.deque(Config.QUEUE_ACCESSLOG_POOL);
		httpContext.setupAccessLog(accessLog);
		
		boolean parseHeader=false;
		try {
			parseHeader=httpContext.parseRequestHeader();
		} catch (IOException e) {
			//不当リクエスト
			logger.error("fail to parse Request.",e);
		} catch( Throwable t){//java.nio.channels.ClosedSelectorExceptionが発生したりする
			logger.error("fail to parse Request.!!",t);
		}
		if( !parseHeader ){
			context.enque(httpContext,Config.QUEUE_CLOSE);
			return false;
		}
		
		MappingEntry entry=mappingQueue(httpContext);
		if(entry==null){
			logger.warn("fail to mapping.URL:"+httpContext.getRequestUri());
			responseDirect(httpContext,"404","failt to mapping." + httpContext.getRequestUri());
			return false;
		}
		String queue=entry.getQueue();
		//一連streamの初期化
		try {
			setupTraceLog(httpContext,queue,accessLog);
		} catch (IOException e) {
			//streamの初期化に失敗、レスポンスもできない
			logger.error("fail to setupAccessTrace",e);
			responseDirect(httpContext,"404","failt to accesslog." + httpContext.getRequestUri());
			return false;
		}
		
		//以降普通にレスポンスできる
		//response予約
		context.enque(httpContext,Config.QUEUE_RESPONSE);
		
		entry=authentication(httpContext,entry);
		if(entry==null){
			return false;
		}
		accessLog.setMappingSource(entry.getSourcePath());
		accessLog.setMappingDestination(entry.getDestination());
		
		//response作成依頼(認証時にqueueが変更されている可能性がある)
		context.enque(httpContext,entry.getQueue());
		return true;
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletContext, java.util.Map)
	 */
	public void init(QueueletContext context, Map param) {
		this.context=context;
		this.config=Config.getInstance();
		this.configuration=config.getConfiguration();
		
		String configClientIps=(String)param.get("clientIps");
		if(configClientIps!=null && !"".equals(configClientIps)){
			clientIps=new HashSet();
			String[] clientIpsArray=configClientIps.split(";");
			for(int i=0;i<clientIpsArray.length;i++){
				clientIps.add(clientIpsArray[i]);
			}
		}
		
		String confProxyAuthenticate=(String)param.get("proxyAuthenticate");
		if(confProxyAuthenticate!=null&&!"".equals(confProxyAuthenticate)){
			this.proxyAuthenticate=encodeBase64(confProxyAuthenticate);
		}
		String confWebserverAuthenticate=(String)param.get("webserverAuthenticate");
		if(confWebserverAuthenticate!=null&&!"".equals(confWebserverAuthenticate)){
			this.webAuthenticate=encodeBase64(confWebserverAuthenticate);
			this.webAuthenticateCookieKey=(String)param.get("webAuthenticateCookieKey");
			//[空白かも]key[空白かも]=[空白かも]value[空白かも](;もしくは文末)　形式の正規表現
			//'"'で括られる可能性もあるらしいが正規表現では難しい
			this.webAuthenticatePattern=Pattern.compile(" *" + webAuthenticateCookieKey +" *= *(\\S*)(;|\\z)");
		}
	}

	private String encodeBase64(String text){
		try {
			byte[] bytes=Base64.encodeBase64(text.getBytes("iso8859_1"));
			return new String(bytes,"iso8859_1");
		} catch (UnsupportedEncodingException e) {
			logger.error("iso8859_1 unkown?",e);
			throw new RuntimeException("iso8859_1 unkown?",e);
		}
	}
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}
