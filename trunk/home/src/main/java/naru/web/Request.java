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
	//proxy�Ƃ��ē��삷�鎞��Authentication
	private String proxyAuthenticate=null;
	//web�T�[�o�Ƃ��ē��삷�鎞(reverseproxy,file)��Authentication
	private String webAuthenticate=null;
	private String webAuthenticateCookieKey=null;
	private Pattern webAuthenticatePattern=null;
	
	//WWW-Authenticate: Basic realm="FIND2",Basic��reverse�̎����l����Ǝg���Ȃ�
	//Authorization: Basic XXXbase64XXX
	//"Proxy-Authenticate", "Basic Realm=\"myProxy\"",proxy�̏ꍇ��������g��
	//Proxy-Authorization: Basic XXXbase64XXX
	//Response�@Queue�Ɉ˗�����O�Ƀ��X�|���X��ԋp�������ꍇ
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
		if(proxyAuthenticate==null){//�F�؂Ȃ����[�h
			return entry;
		}
		if( paHeader!=null){
			String[] paParts=paHeader.split(" ");
			if( paParts.length>=2 &&
				"Basic".equalsIgnoreCase(paParts[0]) &&
				proxyAuthenticate.equalsIgnoreCase(paParts[1]) ){
				return entry;//�F�ؐ���
			}
		}
		//�F�؎��s
		httpContext.addResponseHeader("Proxy-Authenticate", "Basic Realm=\"myProxy\"");
		httpContext.registerResponse("407","myProxy Proxy-Authenticate");
		httpContext.startResponse();
		return null;//�����ŃR���e���c��������̂�entry�Ȃ�
	}
	
	private MappingEntry responseWebAuthenticationForm(HttpContext httpContext,String orgPath) {
		//�F�؉�ʂ�VelocityPage����o�͂���
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
		
		//webAuthenticateCookieKey���������Ȃ�A���������̂�ݒ肷��B
		httpContext.removeRequestHeader(HttpContext.COOKIE_HEADER);
		String updateCookie=matcher.replaceAll("");
		if(!"".equals(updateCookie)){//webAuthenticateCookieKey�𔲂������ʉ����c���
			httpContext.setRequestHeader(HttpContext.COOKIE_HEADER,updateCookie);
		}
		//"��'�Ŋ����Ă���ꍇ�́A�폜����
		return stripQuote(cookieAuth);
	}
	
	private MappingEntry webAuthentication(HttpContext httpContext,MappingEntry entry) {
		if(webAuthenticate==null){
			return entry;//�F�؂Ȃ����[�h
		}
		String cookieAuth=getCookieAuthAndFilter(httpContext);
		
		String uri=httpContext.getRequestUri();
		String orgPath=null;
		if(uri.indexOf(webAuthenticateForm)>=0){//�F�؉�ʂ���̃��N�G�X�g
			String user;
			String pass;
			try {
				user = httpContext.getParameter("user");
				pass = httpContext.getParameter("pass");
				orgPath = httpContext.getParameter("orgPath");
			} catch (IOException e) {//�F�؎��s�Ɣ��f
				logger.warn("webAuthenticate fail to getParameter.",e);
				return responseWebAuthenticationForm(httpContext,orgPath);
			}
			String inputAuth=encodeBase64(user+":"+pass);
			if(webAuthenticate.equals(inputAuth)){//�F�ؐ���
				httpContext.addResponseHeader("Set-Cookie", webAuthenticateCookieKey +"=" +webAuthenticate + "; path=/");
				String location="http://"+httpContext.getRequestServer()+orgPath;
				httpContext.addResponseHeader(HttpContext.LOCATION_HEADER,location);
				httpContext.registerResponse("302","success webAuthenticate");
				httpContext.startResponse();
				return null;//�����ŃR���e���c��������̂�entry�Ȃ�
			}
		}
		if(cookieAuth!=null&&webAuthenticate.equals(cookieAuth)){
			return entry;//�F����
		}
		
		//�F�A�F�؎��s�A�F��Form�����X�|���X����BwebAuthenticateForm����̃��N�G�X�g�łȂ�����orgPath��,null
		return responseWebAuthenticationForm(httpContext,orgPath);
	}

	//���ӁF�F�؂��K�v�Ȃ��ꍇ��PROXY_AUTHORIZATION_HEADER��Cookie�w�b�_���폜����K�v������B
	private MappingEntry authentication(HttpContext httpContext,MappingEntry entry) {
		String queue=entry.getQueue();
		//Replay�Ώۂ͔F�؂��o�C�p�X����
		if(queue.equals(Config.QUEUE_REPLAY)){
			return entry;
		}
		//proxy�F�؂��K�v�ȏꍇ
		if(queue.equals(Config.QUEUE_PROXY)){
			return proxyAuthentication(httpContext,entry);
		}
		//web�F�؂��K�v�ȏꍇ
		return webAuthentication(httpContext,entry);
	}
	
	//URI�Ɛݒ肩��Ăяo����QUEUE�����߂�B
	private MappingEntry mappingQueue(HttpContext httpContext){
		String uri=httpContext.getRequestUri();
		if( MappingEntry.controllerEntry.matches(uri) ){//controller�ւ̃��N�G�X�g���H
			httpContext.setAttribute(HttpContext.ATTRIBUTE_MAPPING_ENTRY, MappingEntry.controllerEntry);
			return MappingEntry.controllerEntry;
		}
		String clientIp=httpContext.getClientIp();
		
		//���N�G�X�g��Replay�Ώۂ��H
		if( config.isReplay(clientIp, uri)){
			return MappingEntry.replayEntry;
		}
		if(httpContext.isProxyRequest()){
			//TODO �����I��Web�T�[�o�Ƃ��ē��삷�郂�[�h�v,
			//requestLine��http://����n�܂��Ă��N���C�A���g���Ԉ���đ����Ă����Ɣ��f����
			return MappingEntry.proxyEntry;
		}
		MappingEntry mappingEntry=config.mapping(uri);
		if( mappingEntry==null){
			return null;
		}
		
		//�}�b�s���O�悪Replay�Ώۂ��H
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
		//DB�ւ̃A�N�Z�X���O�̎�L��
		if( !isLogging(clientIp,"accessDb." + queue) ){
			return;
		}
		//DB�ɋL�^����
		accessLog.insert();
		//���N�G�X�gStream��peek����
		if(isLogging(clientIp,"accessTrace.request." + queue) ){
			PeekStream requestPeeker=accessLog.setupPeekRequest();
			httpContext.peekRequest(requestPeeker);
			context.enque(requestPeeker, Config.QUEUE_PEEK);
				
			//���Ɏ󂯎���Ă���w�b�_������PeekStream�ɗ�������
			HttpParser requestParser=httpContext.getRequestParser();
			OutputStream os=requestPeeker.getPeekOutputStream();
			requestParser.writeSeriarizeHeader(os);
		}
		//���X�|���XStream��peek����
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
			//�w��IP����̃��N�G�X�g���ǂ������`�F�b�N
			if( checkIp(socket)==false ){
				return false;
			}
			httpContext=(HttpContext)context.deque(Config.QUEUE_HTTPCONTEXT_POOL);
			httpContext.setSocket(socket);
		}else{//KeepAlive���������鎞�͂�����
			httpContext=(HttpContext)req;
		}
		
		//�����̋N�_��accessLog�̒��ɍ̂���
		AccessLog accessLog=(AccessLog)context.deque(Config.QUEUE_ACCESSLOG_POOL);
		httpContext.setupAccessLog(accessLog);
		
		boolean parseHeader=false;
		try {
			parseHeader=httpContext.parseRequestHeader();
		} catch (IOException e) {
			//�s�����N�G�X�g
			logger.error("fail to parse Request.",e);
		} catch( Throwable t){//java.nio.channels.ClosedSelectorException�����������肷��
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
		//��Astream�̏�����
		try {
			setupTraceLog(httpContext,queue,accessLog);
		} catch (IOException e) {
			//stream�̏������Ɏ��s�A���X�|���X���ł��Ȃ�
			logger.error("fail to setupAccessTrace",e);
			responseDirect(httpContext,"404","failt to accesslog." + httpContext.getRequestUri());
			return false;
		}
		
		//�ȍ~���ʂɃ��X�|���X�ł���
		//response�\��
		context.enque(httpContext,Config.QUEUE_RESPONSE);
		
		entry=authentication(httpContext,entry);
		if(entry==null){
			return false;
		}
		accessLog.setMappingSource(entry.getSourcePath());
		accessLog.setMappingDestination(entry.getDestination());
		
		//response�쐬�˗�(�F�؎���queue���ύX����Ă���\��������)
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
			//[�󔒂���]key[�󔒂���]=[�󔒂���]value[�󔒂���](;�������͕���)�@�`���̐��K�\��
			//'"'�Ŋ�����\��������炵�������K�\���ł͓��
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
