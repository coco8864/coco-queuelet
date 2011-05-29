package naru.web;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

public class Config{
	static private Logger logger=Logger.getLogger(Config.class);
	
	public static String QUEUE_REQUEST="request";
	public static String QUEUE_RESPONSE="response";
	public static String QUEUE_CLOSE="close";
	public static String QUEUE_FILESYSTEM="fileSystem";
	public static String QUEUE_PROXY="proxy";
	public static String QUEUE_CONTROLLER="controller";
	public static String QUEUE_PEEK="peek";
	public static String QUEUE_ACCESSLOG="accessLog";
	public static String QUEUE_REPLAY="replay";
	public static String QUEUE_REVERSE="reverse";
	public static String QUEUE_VELOPAGE="velocityPage";
	public static String QUEUE_HTTPCONTEXT_POOL="httpContextPool";
	public static String QUEUE_ACCESSLOG_POOL="accessLogPool";
	public static String QUEUE_ASYNC_SERVICE="asyncServicel";
	
	public static String AUTH_HEADERS_KEY="authHeaders";
	public static String REPLAY_HISTORY_KEY="replayHistory";
	public static String PATH_MAPPING_KEY="pathMapping";
	
	private static Config config;
	
	private Configuration configuration=null;
	private Map authHeaders;
	private Map replayHistory;
	private Map replayPaths;
	private Set pathMapping;
	private Pattern exceptProxyDomainsPattern;
	
	public static Config getInstance(){
		if(config==null){
			config=new Config();
		}
		return config;
	}
	
	private Config(){
		try {
//			InputStream is=Config.class.getClassLoader().getResourceAsStream("myProxy.properties");
			InputStream is=new FileInputStream("myProxy.properties");
			Reader reader=new InputStreamReader(is,"MS932");
			PropertiesConfiguration propConfig=new PropertiesConfiguration();
			propConfig.load(reader);
			configuration=propConfig;
			authHeaders=Collections.synchronizedMap(new HashMap());
			replayHistory=Collections.synchronizedMap(new HashMap());
			replayPaths=Collections.synchronizedMap(new HashMap());
			pathMapping=Collections.synchronizedSortedSet(new TreeSet(MappingEntry.mappingComparator));
			
			//proxy除外リストの初期化
			updateExceptProxyDomains();
			
			//Mappingの初期化
			updateMapping();
			
		} catch (UnsupportedEncodingException e) {
			logger.error("fail to read myProxy.properties",e);
			throw new RuntimeException(e);
		} catch (ConfigurationException e) {
			logger.error("fail to read myProxy.properties",e);
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			logger.error("fail to read myProxy.properties",e);
			throw new RuntimeException(e);
		}
	}
	
	public Set getReplayHistory(String ip){
		return (Set)replayHistory.get(ip);
	}
	
	public void addReplayHistory(String ip,Set history){
		replayHistory.put(ip,history);
	}
	
	public void clearReplayHistory(String clientIp){
		if(clientIp==null){//全ヒストリを削除（オプション変更時）
			replayHistory.clear();
			return;
		}
		//単一IPのヒストリを削除
		Set history=getReplayHistory(clientIp);
		if( history==null){
			return;
		}
		history.clear();
	}
	
	public void setupReplayPaths(String clientIp) {
		String configReplayPaths=configuration.getString(clientIp + ".replayPaths");
		if(configReplayPaths==null || "".equals(configReplayPaths)){
			replayPaths.remove(clientIp);
			return;
		}
		configReplayPaths=configReplayPaths.replaceAll(";","|");
		replayPaths.put(clientIp,Pattern.compile(configReplayPaths));
		//パターンが変更された、この端末の履歴をクリアする。
		clearReplayHistory(clientIp);
	}
	
	public boolean isReplay(String clientIp,String path){
		if( configuration.getBoolean(clientIp+ ".replayMode",false)==false){
			return false;
		}
		Pattern pattern=(Pattern)replayPaths.get(clientIp);
		if(pattern==null){
			logger.warn("replayMode but no pattern");
			return true;
		}
		synchronized(pattern){
			Matcher mattcher=pattern.matcher(path);
			return mattcher.find();
		}
	}
	
	public void addAuthHeader(String url,String header){
		authHeaders.put(url, header);
	}
	
	public void delAuthHeader(String url){
		authHeaders.remove(url);
	}
	
	public String getAuthHeader(String url){
		synchronized(authHeaders){
			Iterator itr=authHeaders.keySet().iterator();
			while(itr.hasNext()){
				String authUrl=(String)itr.next();
				if( url.startsWith(authUrl) ){
					return (String)authHeaders.get(authUrl);
				}
			}
		}
		return null;
	}
	
	public Iterator getAuthUrls(){
		return authHeaders.keySet().iterator();
	}
	
	public void updateExceptProxyDomains(){
		String exceptProxyDomains=configuration.getString("exceptProxyDomains",null);
		if(exceptProxyDomains==null){
			exceptProxyDomainsPattern=null;
			return;
		}
		exceptProxyDomains=exceptProxyDomains.replaceAll(";","|");
		exceptProxyDomains=exceptProxyDomains.replaceAll("\\*", "\\\\S*");
		exceptProxyDomainsPattern=Pattern.compile(exceptProxyDomains);
	}
	
	public void updateMapping(){
		pathMapping.clear();
		Configuration mapping=configuration.subset("Mapping");
		Iterator itr=mapping.getKeys();
		while(itr.hasNext()){
			String sourcePath=(String)itr.next();
			String destination=mapping.getString(sourcePath);
			if( addMapping(sourcePath,destination)==false ){
				logger.warn("fail to mappint.sourcePath:"+ sourcePath + " destination:"+destination);
			}
		}
	}
	
	/// *.bbb.com は、"\\S*.bbb.com"でマッチ
	public boolean isUseProxy(String domain){
		if(domain==null){
			return false;//proxyサーバ設定がなければproxyなし
		}
		if( exceptProxyDomainsPattern==null ){
			return false;
		}
		Matcher matcher=null;
		synchronized(exceptProxyDomainsPattern){
			matcher = exceptProxyDomainsPattern.matcher(domain);
		}
		//マッチしたら、proxyを使わない、マッチしなかったらproxyを使う
		return !matcher.matches();
	}
	
	public boolean addMapping(String sourcePath,String destination){
		MappingEntry entry=MappingEntry.getEntry(sourcePath, destination);
		if( entry==null ){
			return false;
		}
		pathMapping.add(entry);
		return true;
	}
	
	public MappingEntry delMapping(String sourcePath){
		Iterator itr=pathMapping.iterator();
		while(itr.hasNext()){
			MappingEntry entry=(MappingEntry)itr.next();
			if(sourcePath.equals(entry.getSourcePath())){
				itr.remove();
				return entry;
			}
		}
		return null;
	}
	
	public MappingEntry mapping(String uri){
		Iterator itr=pathMapping.iterator();
		while(itr.hasNext()){
			MappingEntry entry=(MappingEntry)itr.next();
			if(entry.matches(uri)){
				return entry;
			}
		}
		return null;
	}
	
	public Iterator getMappingPaths(){
		return pathMapping.iterator();
	}
	
	public String toJson(){
		return toJson(configuration);
	}
	
	public String toJson(Configuration configuration){
		Iterator itr=configuration.getKeys();
		StringBuffer sb=new StringBuffer();
		sb.append("{");
		String sep="'";
		while(itr.hasNext()){
			String name=(String)itr.next();
			//ブラウザに見せても意味ないのは返さない
			if(name.startsWith("ReasonPhrase.") || name.startsWith("ContentType.")){
				continue;
			}
			Object value=configuration.getProperty(name);
			/*
			if(value==null){
				continue;
			}
			*/
			sb.append(sep);
			sb.append(name);
			sb.append("'");
			sb.append(":'");
			sb.append(jsonEscape(value));
			sb.append("'");
			sep=",'";
		}
		sb.append("}");
		return sb.toString();
	}
	
	public Configuration getConfiguration(){
		return configuration;
	}
	
	public Configuration getConfiguration(String subkey){
		if(subkey==null){
			return configuration;
		}
		return configuration.subset(subkey);
	}
	
	// エスケープ文字
	private static final char CH_ESCAPE = '\\';
	private static final char CH_SQUOT = '\'';
	private static final char CH_DQUOT = '\"';
	private static final char CH_BS = '\b';    // 0x08
	private static final char CH_HT = '\t';    // 0x09
	private static final char CH_LF = '\n';    // 0x0A
	private static final char CH_FF = '\f';    // 0x0C
	private static final char CH_CR = '\r';    // 0x0D
	private static final String STR_BS = "\\b";
	private static final String STR_HT = "\\t";
	private static final String STR_LF = "\\n";
	private static final String STR_FF = "\\f";
	private static final String STR_CR = "\\r";

    public static String jsonEscape(Object obj) {
    	if (obj == null) {
    		return null;
    	}
    	String string=obj.toString();
    	if (string == null || string.length() == 0) {
    		return string;
    	}
    	StringBuffer sb = new StringBuffer();
		for(int i = 0; i < string.length(); i++) {
			char ch = string.charAt(i);
			if(ch == CH_ESCAPE) {
				sb.append(CH_ESCAPE).append(CH_ESCAPE);
			} else if (ch == CH_SQUOT) {
				sb.append(CH_ESCAPE).append(CH_SQUOT);
			} else if (ch == CH_DQUOT) {
				sb.append(CH_ESCAPE).append(CH_DQUOT);
			} else if (ch == CH_BS) {
				sb.append(STR_BS);
			} else if (ch == CH_HT) {
				sb.append(STR_HT);
			} else if (ch == CH_LF) {
				sb.append(STR_LF);
			} else if (ch == CH_FF) {
				sb.append(STR_FF);
			} else if (ch == CH_CR) {
				sb.append(STR_CR);
			} else {
				sb.append(ch);
			}
		}
    	return sb.toString();
    }
}
