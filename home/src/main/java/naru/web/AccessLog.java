package naru.web;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import naru.util.HibernateUtil;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * @hibernate.class
 *  table="ACCESS_LOG"
 *  dynamic-update="true"
 *  dynamic-insert="true"
 *  lazy="true"
 * 
 * @author naru hayashi
 */
public class AccessLog {
	private static Logger logger=Logger.getLogger(AccessLog.class);	
	private static JsonConfig jsonConfig;
	static{
		jsonConfig=new JsonConfig();
		jsonConfig.setRootClass(AccessLog.class);
		DatePropertyFilter dpf=new DatePropertyFilter();
		jsonConfig.setJavaPropertyFilter(dpf);
		jsonConfig.setJsonPropertyFilter(dpf);
	}
	
	public static AccessLog fromJson(String jsonString){
		JSON json=JSONObject.fromObject(jsonString);
		AccessLog accessLog=(AccessLog)JSONSerializer.toJava(json,jsonConfig);
		return accessLog;
	}
	
	public static AccessLog getById(long id){
		return getById(new Long(id));
	}
	
	public static AccessLog getById(Long id){
		Session session=null;
		try {
			session=HibernateUtil.currentSession();
			AccessLog accessLog=(AccessLog)session.get(AccessLog.class, id);
			return accessLog;
		} catch (HibernateException e) {
			logger.error("fail to get AccessLog.id:"+id,e);
			return null;
		}finally{
			if(session!=null){
				session.close();
				HibernateUtil.closeSession();
			}
		}
	}
	
	public static void delete(String clientIp){
		Session session=null;
		try {
			session=HibernateUtil.currentSession();
			String queryString="delete AccessLog";
			if(clientIp!=null){
				queryString=queryString+" where ip='"+clientIp+"'";
			}
			Query query=session.createQuery(queryString);
			query.executeUpdate();
			HibernateUtil.commitAndClearSession();
			session=null;
		} catch (HibernateException e) {
			logger.error("fail to deleteAll",e);
			return;
		}finally{
			if(session!=null){
				HibernateUtil.clearSession();
			}
		}
	}
	
	public static int countOfAccessLog(String whereSection) throws HibernateException{
		String queryString="select count(id) from AccessLog";
		if(whereSection!=null){
			queryString+= " where "+whereSection;
		}
		Iterator itr=query(queryString,0,0);
		Integer count=(Integer)itr.next();
		return count.intValue();
	}
	
	/**
	 * 結果を使い終わったあとは、HibernateUtil.clearSession()する事
	 * @param hql
	 * @param firstResult
	 * @param maxResults
	 * @return
	 */
	public static Iterator query(String hql,int firstResult,int maxResults) throws HibernateException{
		try {
			Session session=HibernateUtil.currentSession();
			Query query=session.createQuery(hql);
			if(maxResults>0){//0以下の場合は該当するレコードすべてを返却する。
				query.setMaxResults(maxResults);
			}
			query.setFirstResult(firstResult);
			return query.iterate();
		} catch (RuntimeException e) {
			//hqlに文法違反があった場合、IllegalStateExceptionで復帰してくる。
			logger.warn("fail to query.",e);
			if(e instanceof HibernateException){
				throw e;
			}
			throw new HibernateException(e);
		}
	}
	
	public void insert(){
		Session session=null;
		try {
			session=HibernateUtil.currentSession();
			session.saveOrUpdate(this);
			HibernateUtil.commitAndClearSession();
			session=null;
		} catch (HibernateException e) {
			logger.error("fail to insert",e);
		}finally{
			if(session!=null){
				HibernateUtil.clearSession();
			}
		}
	}
	
	public void update(){
		if(id==null){
			return;
		}
		insert();
	}
	
	public void delete(){
		Session session=null;
		try {
			session=HibernateUtil.currentSession();
			session.delete(this);
			HibernateUtil.commitAndClearSession();
			session=null;
		} catch (HibernateException e) {
			logger.error("fail to insert",e);
		}finally{
			if(session!=null){
				HibernateUtil.clearSession();
			}
		}
	}
	
	public String toJson(){
		JSON json=JSONSerializer.toJSON(this,jsonConfig);
		return json.toString();
	}
	
	private Long id;
	private Date startTime;
	private String ip;
	private String requestLine;
	private long requestHeaderLength;//リクエストヘッダ長
	private String requestBody;//"application/x-www-form-urlencoded"の場合のみ記録
	private String statusCode;
	private long responseLength;//レスポンスボディ長//respons==>responseの間違い
	private long processTime;
	private long processQueue;
	private String timeRecode;
	private String requestFile;
	private String responseFile;
	
	private String mappingSource;
	private String mappingDestination;
	
	private PeekStream peekRequest;
	private PeekStream peekResponse;
	
	public static long QUEUE_REQUEST=   0x00000001;
	public static long QUEUE_RESPONSE=  0x00000002;
	public static long QUEUE_CLOSE=     0x00000004;
	public static long QUEUE_CONTROLLER=0x00010000;
	public static long QUEUE_PROXY=     0x00020000;
	public static long QUEUE_FILESYSTEM=0x00040000;
	public static long QUEUE_VELOPAGE=  0x00080000;
	public static long QUEUE_REVERSE=   0x00100000;
	public static long QUEUE_REPLAY=    0x00200000;
	
	public AccessLog(){
//		peekRequest=new PeekStream();
//		peekResponse=new PeekStream();
	}
	
	public PeekStream setupPeekRequest(){
		if(peekRequest==null){//AccessLogはプールしているので、初回に作成するだけ
			peekRequest=new PeekStream();
		}
		if(id!=null){
			this.requestFile="req" + id.toString()+".log";
			peekRequest.setTraceName(requestFile);
		}
		return peekRequest;
	}
	
	public PeekStream setupPeekResponse(){
		if(peekResponse==null){//AccessLogはプールしているので、初回に作成するだけ
			peekResponse=new PeekStream();
		}
		if(id!=null){
			this.responseFile="res" + id.toString()+".log";
			peekResponse.setTraceName(responseFile);
		}
		return peekResponse;
	}
	
	public void recycle(){
		if(peekRequest!=null){
			peekRequest.waitForPeek();
		}
		if(peekResponse!=null){
			peekResponse.waitForPeek();
		}
		id=null;
		startTime=null;
		ip=requestLine=statusCode=timeRecode=requestFile=responseFile=requestBody=null;
		responseLength=processTime=processQueue=0;
	}
	
	public void log(){
		StringBuffer sb=new StringBuffer(ip);
		sb.append(" \"");
		sb.append(requestLine);
		sb.append("\" ");
		sb.append(statusCode);
		sb.append(" ");
		sb.append(responseLength);
		sb.append(" ");
		sb.append(processTime);
		sb.append(" ");
		logger.info(sb.toString());
	}
	
	/**
	 * @hibernate.id
	 *  column="ID"
	 *  generator-class = "native"
	 * 
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	/**
	 * @hibernate.property
	 *  column="START_TIME"
	 *  
	 * @return the startTime
	 */
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	
	
	/**
	 * @hibernate.property
	 *  column="IPADDRESS"
	 *  
	 * @hibernate.column
     *  name ="IPADDRESS"
     *  length="16"
	 *  
	 * @return the ip
	 */
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	
	/**
	 * @hibernate.property
	 *  column="REQUEST_LINE"
	 *  
	 * @hibernate.column
     *  name ="REQUEST_LINE"
     *  length="2048"
	 *  
	 * @return the requestLine
	 */
	public String getRequestLine() {
		return requestLine;
	}
	public void setRequestLine(String requestLine) {
		this.requestLine = requestLine;
	}
	
	/**
	 * @hibernate.property
	 *  column="PROCESS_QUEUE"
	 *  
	 * @return the processQueue
	 */
	public long getProcessQueue() {
		return processQueue;
	}

	public void setProcessQueue(long processQueue) {
		this.processQueue = processQueue;
	}
	
	public void passProcessQueue(long processQueue){
		this.processQueue|=processQueue;
	}
	
	
	/**
	 * @hibernate.property
	 *  column="REQUEST_BODY"
	 *  
	 * @hibernate.column
     *  name ="REQUEST_BODY"
     *  length="1024"
	 *  
	 * @return the requestBody
	 */
	public String getRequestBody() {
		return requestBody;
	}

	public void setRequestBody(String requestBody) {
		this.requestBody = requestBody;
	}
	
	//AccessLogにbody情報を記録するか否かを判定する。
	public static boolean isRecodeBody(String method,String contentType,long contentLength){
		if(!method.equalsIgnoreCase("POST")){
			return false;
		}
		if(contentLength>1024l){
			return false;
		}
		if (contentType == null) {
			return false;
		}
		int semicolon = contentType.indexOf(';');
		if (semicolon >= 0) {
			contentType = contentType.substring(0, semicolon).trim();
		} else {
			contentType = contentType.trim();
		}
		if (!("application/x-www-form-urlencoded".equals(contentType))) {
			return false;
		}
		return true;
	}
	
	/**
	 * @hibernate.property
	 *  column="STATUS_CODE"
	 *  
	 * @hibernate.column
     *  name ="STATUS_CODE"
     *  length="3"
	 *  
	 * @return the statusCode
	 */
	public String getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}
	
	/**
	 * @hibernate.property
	 *  column="RESPONSE_LENGTH"
	 *  
	 * @return the responsLength
	 */
	public long getResponseLength() {
		return responseLength;
	}
	public void setResponseLength(long responseLength) {
		this.responseLength = responseLength;
	}
	
	/**
	 * @hibernate.property
	 *  column="REQUEST_HADER_LENGTH"
	 *  
	 * @return the requestHeaderLength
	 */
	public long getRequestHeaderLength() {
		return requestHeaderLength;
	}

	public void setRequestHeaderLength(long requestHeaderLength) {
		this.requestHeaderLength = requestHeaderLength;
	}
	
	/**
	 * @hibernate.property
	 *  column="PROCESS_TIME"
	 *  
	 * @return the processTime
	 */
	public long getProcessTime() {
		return processTime;
	}
	public void setProcessTime(long processTime) {
		this.processTime = processTime;
	}
	
	//処理終了後の締め作業を行う
	public void endProcess(List timeCheckPoints){//処理終了時に呼び出す
		if(startTime==null){
			setProcessTime(-1);
			return;
		}
		long start=startTime.getTime();
		setProcessTime(System.currentTimeMillis()-start);
		if(timeCheckPoints==null){
			return;
		}
		StringBuffer sb=new StringBuffer();
		Iterator itr=timeCheckPoints.iterator();
		while(itr.hasNext()){
			Long pointTime=(Long)itr.next();
			sb.append(pointTime.longValue()-start);
			sb.append(",");
		}
		setTimeRecode(sb.toString());
	}
	
	/**
	 * @hibernate.property
	 *  column="TIME_RECODE"
	 *  
	 * @hibernate.column
     *  name ="TIME_RECODE"
     *  length="16"
	 *  
	 * @return the timeRecode
	 */
	public String getTimeRecode() {
		return timeRecode;
	}
	public void setTimeRecode(String timeRecode) {
		this.timeRecode = timeRecode;
	}

	/**
	 * @hibernate.property
	 *  column="REQUEST_FILE"
	 *  
	 * @hibernate.column
     *  name ="REQUEST_FILE"
     *  length="16"
	 *  
	 * @return the requestFile
	 */
	public String getRequestFile() {
		return requestFile;
	}

	public void setRequestFile(String requestFile) {
		this.requestFile = requestFile;
	}

	/**
	 * @hibernate.property
	 *  column="RESPONSE_FILE"
	 *  
	 * @hibernate.column
     *  name ="RESPONSE_FILE"
     *  length="16"
     *  
	 * @return the responseFile
	 */
	public String getResponseFile() {
		return responseFile;
	}

	public void setResponseFile(String responseFile) {
		this.responseFile = responseFile;
	}

	/**
	 * @hibernate.property
	 *  column="MAPPING_SOURCE"
	 *  
	 * @hibernate.column
     *  name ="MAPPING_SOURCE"
     *  length="128"
	 *  
	 * @return the requestFile
	 */
	public String getMappingSource() {
		return mappingSource;
	}

	public void setMappingSource(String mappingSource) {
		this.mappingSource = mappingSource;
	}

	/**
	 * @hibernate.property
	 *  column="MAPPING_DESTINATION"
	 *  
	 * @hibernate.column
     *  name ="MAPPING_DESTINATION"
     *  length="128"
	 *  
	 * @return the requestFile
	 */
	public String getMappingDestination() {
		return mappingDestination;
	}

	public void setMappingDestination(String mappingDestination) {
		this.mappingDestination = mappingDestination;
	}

}
