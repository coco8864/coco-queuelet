/*
 * Created on 2004/10/20
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.web;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;
import naru.util.HibernateUtil;

/**
 * @author NARU
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Controller implements Queuelet {
	static private Logger logger=Logger.getLogger(Controller.class);
	private static String AUTHRIZATION_HEADER_NAME="Authorization";
	
	private QueueletContext context;
	private Config config;
	private Configuration configuration;
	private String controllerUrl;
	private File traceBaseDir;
	private File importBaseDir;
	private DiskFileItemFactory diskFactory;
	private SimpleDateFormat fileNameFormat=new SimpleDateFormat("yyyyMMdd_HHmmssSSS");

	private String siriarizeFileName(HttpContext httpContext,String prefix,String ext){
		String ip=httpContext.getClientIp();
		StringBuffer sb=new StringBuffer(prefix);
		sb.append(ip);
		sb.append("-");
		Date now=new Date();
		String dateString;
		synchronized(fileNameFormat){
			dateString=fileNameFormat.format(now);
		}
		sb.append(dateString);
		sb.append(ext);
		return sb.toString();
	}
	
	private void deleteAccessLogs(String clientIp){
		Session session=null;
		try {
			session=HibernateUtil.currentSession();
			String queryString="select id from AccessLog";
			if(clientIp!=null){
				queryString=queryString+" where ip='"+clientIp+"'";
			}
			Query query=session.createQuery(queryString);
			Iterator itr=query.iterate();
			List deleteList=new ArrayList();
			while(itr.hasNext()){
				deleteList.add(itr.next());
			}
			//非同期処理に削除を依頼する。
			AsyncOperation op=new AsyncOperation(AsyncOperation.OPERATION_DELETE_ACCESSLOGS);
			op.putParameter("deleteLogIds", deleteList);
			op.doAsync(context);
		} catch (HibernateException e) {
			logger.error("fail to deleteAll",e);
			return;
		}finally{
			if(session!=null){
				HibernateUtil.clearSession();
			}
		}
	}
	
	private boolean setupTraceLogFile(HttpContext httpContext){
		long logId=-1;
		boolean isResponseStream=false;
		int sectionNum=1;//0:all,1:header,2:body
		try {
			String paramId=httpContext.getParameter("logId");
			logId=Long.parseLong(paramId);
			//request(*),response
			String stream=httpContext.getParameter("stream");
			isResponseStream="response".equals(stream);
			
			//all,header(*),body
			String section=httpContext.getParameter("section");
			if("all".equals(section)){
				sectionNum=0;
			}else if("body".equals(section)){
				sectionNum=2;
			}else{
				sectionNum=1;
			}
		} catch (IOException e1) {
			logger.error("download id error.id:"+logId);
		} catch (Throwable t){
			logger.error("download id error.id:"+logId);
		}
		if(logId==-1){
			httpContext.registerResponse("500","fail to get logId");
			return false;
		}
		AccessLog accessLog=AccessLog.getById(logId);
		if(accessLog==null){
			httpContext.registerResponse("404","trace log not found recored.id="+logId);
			return false;
		}
		
		String traceFileName=null;
		if(isResponseStream){
			traceFileName=accessLog.getResponseFile();
		}else{
			traceFileName=accessLog.getRequestFile();
		}
		if(traceFileName==null){
			httpContext.registerResponse("404","trace log not found file.id="+logId);
			return false;
		}
		File traceFile=new File(traceBaseDir,traceFileName);
		
		long offset=0;
		long length=-1;
		switch(sectionNum){
		case 0://all
			offset=0;
			length=-1;
			break;
		case 1://header
			offset=0;
			if(isResponseStream){
				long fileLength=traceFile.length();
				length=fileLength-accessLog.getResponseLength();
			}else{
				length=accessLog.getRequestHeaderLength();
			}
			break;
		case 2://body
			if(isResponseStream){
				long fileLength=traceFile.length();
				offset=fileLength-accessLog.getResponseLength();
			}else{
				offset=accessLog.getRequestHeaderLength();
			}
			length=-1;
			break;
		}
		if(length>0){
			httpContext.setAttribute(HttpContext.ATTRIBUTE_RESPONSE_FILE_LENGTH,new Long(length));
		}
		if(offset>0){
			httpContext.setAttribute(HttpContext.ATTRIBUTE_RESPONSE_FILE_OFFSET,new Long(offset));
		}
		httpContext.setAttribute(HttpContext.ATTRIBUTE_RESPONSE_FILE, traceFile);
		httpContext.setAttribute(HttpContext.ATTRIBUTE_RESPONSE_CONTENT_TYPE, "application/octet-stream");
		httpContext.setAttribute(HttpContext.ATTRIBUTE_RESPONSE_CONTENT_DISPOSITION, "filename=\"" + traceFile.getName() + "\"");
		context.enque(httpContext,Config.QUEUE_FILESYSTEM);
		return true;
	}
	
	private boolean doTraceDownload(HttpContext httpContext){
		if( setupTraceLogFile(httpContext)==false){
			httpContext.startResponse();//レスポンス開始
			return false;
		}
		return true;
	}
	
	private boolean doJson(HttpContext httpContext,String fileName,Map parameterMap){
		String callback=getParameter(parameterMap,"callback");
		httpContext.setNoCacheResponseHeaders();//動的コンテンツなのでキャッシュさせない
		Writer out=httpContext.getResponseOut("200","text/javascript; charset=utf-8");
		if(out==null){
			return false;
		}
		try {
			out.write(callback);
			out.write("(");
			if(fileName.startsWith("config")){
				String json;
				json=config.toJson();
				out.write(json);
			}else if(fileName.startsWith("accesslog")){
				String hql=getParameter(parameterMap,"hql");
				String maxResults=getParameter(parameterMap,"maxResults");
				String firstResult=getParameter(parameterMap,"firstResult");
				int iMaxResults=32;
				int iFirstResult=0;
				try {
					iMaxResults=Integer.parseInt(maxResults);
				} catch (RuntimeException e) {}
				try {
					iFirstResult=Integer.parseInt(firstResult);
				} catch (RuntimeException e) {}
			
				//jsonを大きくしすぎるとブラウザがパンクするので上限を設けた
				if(iMaxResults>=1024){
					iMaxResults=1024;
				}
				doAccessLogJson(out,hql,iFirstResult,iMaxResults);
			}
			out.write(");");
		} catch (IOException e) {
			logger.error("doJson IO error.",e);
		} catch (Throwable e) {
			logger.error("doJson IO error.!!",e);
		} finally {
			try {
				out.close();
			} catch (IOException ignore) {
			}
		}
		return true;
	}
	
	
	private void doAccessLogJson(Writer out,String hql,int firstResult,int maxResults) throws IOException{
		Iterator itr;
		try {
			itr = AccessLog.query(hql,firstResult,maxResults);
		} catch (HibernateException e) {
			logger.warn("failt to qury.hql:"+hql,e);
			HashSet set=new HashSet();
			String errString=e.toString();
			errString=errString.replaceAll(":", "_");
			errString=errString.replaceAll("'", "\"");
			errString=errString.replaceAll("\r", "");
			errString=errString.replaceAll("\n", "");
			errString=errString.replaceAll("\\[", "<");
			errString=errString.replaceAll("\\]", ">");
			String[] msg={"fail to qury",hql,errString};
			set.add(msg);
			itr=set.iterator();
		}
		try {
			out.write("[");
			String beginLine="[";
			while(itr.hasNext()){
				out.write(beginLine);
				beginLine=",[";
				Object o=itr.next();
				//複数列のテーブルの場合
				if(o instanceof Object[]){
					Object[] line=(Object [])o;
					for(int i=0;i<line.length;i++){
						Object cell=line[i];
						if(cell==null){
							out.write("'',");
						}else{
//							out.write("'"+cell+ ":"+cell.getClass().getName()+"',");
							if(cell instanceof Date){
								Date date=(Date)cell;
								out.write("'"+date.getTime()+"',");
							}else{
								out.write("'"+Config.jsonEscape(cell)+"',");
							}
						}
					}
				//１列のテーブルの場合
				}else{
					out.write("'"+o+"'");
				}
				out.write("]");
			}
			out.write("]");
		}finally{
			HibernateUtil.clearSession();
		}
		return;
	}
	
	private boolean checkTargetUrl(String targetUrl,String authValue){
		if(authValue==null){
			return false;
		}
		try{
			URL url=new URL(targetUrl);
			HttpURLConnection huc=(HttpURLConnection)url.openConnection();
//			huc.setRequestMethod("HEAD");
			huc.addRequestProperty(AUTHRIZATION_HEADER_NAME, authValue);
			int code=huc.getResponseCode();
			if( code==401){
				return false;
			}else if(code>=500){
				logger.warn("targetUrl check status code:"+ code + " " +targetUrl);
				return false;
			}
		}catch(Throwable e){
			logger.warn("targetUrl check error:"+ targetUrl,e);
			return false;
		}
		return true;
	}
	
	private void doSetConfig(String clientIp,Map parameterMap){
		boolean updateExceptProxyDomains=false;
		boolean updateMapping=false;
		boolean updateReplayPaths=false;
		
		Iterator itr=parameterMap.keySet().iterator();
		while(itr.hasNext()){
			String configName=(String)itr.next();
			Object orgConfigValue=configuration.getProperty(configName);
			if(configName.startsWith("Mapping.")){
				updateMapping=true;
			}else if(orgConfigValue==null || !(orgConfigValue instanceof String)){
				continue;//nameがMappingで始まらない、かつ、既存のString設定値がない
			}
			if("exceptProxyDomains".equals(configName)){
				updateExceptProxyDomains=true;
			}
			if("replayPaths".equals(configName)){
				updateReplayPaths=true;
			}
			List values=(List)parameterMap.get(configName);
			//IPアドレスを先頭に付加するキーを処理
			if(
				"replayMode".equals(configName) || 
				"deleteIfModifiedSince".equals(configName) ||
				"deleteReferer".equals(configName) ||
				"replayPaths".equals(configName) ||
				configName.startsWith("accessDb.") ||
				configName.startsWith("accessTrace.")){
				configName=clientIp+ "." +configName;
			}
			for(int i=0;i<values.size();i++){
				Object configValue=values.get(i);
				if(configValue==null || "".equals(configValue)){
					//TODO 消しちゃうと後で追加できなくなる
					configuration.clearProperty(configName);
				}else{
					configuration.setProperty(configName, values.get(i));
				}
			}
		}
		if(updateExceptProxyDomains){
			config.updateExceptProxyDomains();
		}
		if(updateMapping){
			config.updateMapping();
		}
		if(updateReplayPaths){
			config.setupReplayPaths(clientIp);
		}
	}
	
	private String getParameter(Map parameterMap,String name){
		List parameters=(List)parameterMap.get(name);
		if(parameters==null || parameters.size()==0){
			return null;
		}
		return (String)parameters.get(0);
	}
	
	//myProxyの設定はここに集中する
	//trueの場合、以降レスポンス処理を継続
	//falseの場合、このメソッドでレスポンス処理を実行、以降レスポンス処理は止める
	private boolean doCommand(HttpContext httpContext,Map parameterMap){
		String cmd=getParameter(parameterMap,"cmd");
		if("setConfig".equals(cmd)){
			parameterMap.remove("cmd");
			doSetConfig(httpContext.getClientIp(),parameterMap);
		}else if("addAuth".equals(cmd)){
			String targetUrl=getParameter(parameterMap,"targetUrl");
			String basicAuthHeader=httpContext.getRequestHeader(AUTHRIZATION_HEADER_NAME);
			if(checkTargetUrl(targetUrl,basicAuthHeader)==false){
				httpContext.setResponseHeader("WWW-Authenticate", "Basic realm=\"" + targetUrl +"\"");
				httpContext.registerResponse("401", "not authenticate:"+targetUrl);
				httpContext.startResponse();//レスポンス開始
				return false;
			}
			config.addAuthHeader(targetUrl, AUTHRIZATION_HEADER_NAME + ": "+basicAuthHeader);
		}else if("deleteAuth".equals(cmd)){
			String targetUrl=getParameter(parameterMap,"targetUrl");
			config.delAuthHeader(targetUrl);
		}else if("deleteAccessLog".equals(cmd)){
			String clientIp=httpContext.getClientIp();
			deleteAccessLogs(clientIp);
		}else if("clearReplayHistory".equals(cmd)){
			config.clearReplayHistory(httpContext.getClientIp());
		}
		return true;
	}
	
	private void velocityResponse(HttpContext httpContext,String nextPage){
		httpContext.setAttribute(HttpContext.ATTRIBUTE_VELOCITY_PAGE,nextPage);
		context.enque(httpContext,Config.QUEUE_VELOPAGE);//VelocityPageに画面生成処理を委譲
	}
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		HttpContext httpContext=(HttpContext)req;
		httpContext.passQueue(AccessLog.QUEUE_CONTROLLER);
		Map parameterMap=null;
		try {
			parameterMap=httpContext.getParameterMap();
		} catch (IOException e) {
			httpContext.registerResponse("500", "getParameter error");
			httpContext.startResponse();//レスポンス開始
			return false;
		}
		
		if(doCommand(httpContext,parameterMap)==false){
			return false;
		}
		//自リクエストFile名をテンプレート名とする
		String fileName=httpContext.getRequestFile();
		if( fileName.matches(".*\\.vm$") ){//.vmで終わるファイル名
			String path=httpContext.getRequestPath();
			//これは、controllerUrlで始まっているはず。
			String nextPage=path.substring(controllerUrl.length());
			velocityResponse(httpContext,nextPage);
			return true;
		}else if( fileName.matches(".*\\.json$") ){//.jsonで終わるファイル名
			doJson(httpContext,fileName,parameterMap);
			return true;
		}else if("traceDownload".equals(fileName)){
			//logid=??&stream=[request|response]&section=[all|header|body]
			doTraceDownload(httpContext);
			return true;
		}else if("exportAccesslog".equals(fileName)){
			doExportAccesslog(httpContext,parameterMap);
			return true;
		}else if("importAccesslog".equals(fileName)){//ファイルアップロード
			String nextPage=doImportAccesslog(httpContext,parameterMap);
			velocityResponse(httpContext,nextPage);
			return true;
		}
		
		//uriから、controllerUrlをとって、controllerRootの後ろにくっつける
		//でfileSystemに送る
		//既にmappingは設定されているのでFileSystemに送ればコンテンツがでる
		context.enque(httpContext,Config.QUEUE_FILESYSTEM);
		return false;
	}

	private void doExportAccesslog(HttpContext httpContext, Map parameterMap) {
		AsyncOperation op=new AsyncOperation(AsyncOperation.OPERATION_EXPORT_ACCESSLOG);
		String exportFileName=siriarizeFileName(httpContext,"myp",".zip");
		op.putParameter("exportFile", exportFileName);
		String query=getParameter(parameterMap,"hql");
		op.putParameter("query",query);
		
		logger.info(httpContext.getClientIp()+ "からの依頼により、移出処理を開始しました。query:"+query + " exportファイル名:"+exportFileName);
		op.doAsync(context);

		httpContext.setNoCacheResponseHeaders();//動的コンテンツなのでキャッシュさせない
		httpContext.registerResponse("200", "移出処理を開始しました。exportファイル名:"+exportFileName);
		httpContext.startResponse();//レスポンス開始
	}
	
	private String doImportAccesslog(HttpContext httpContext, Map parameterMap) {
		//TODO fileupload
		File importFile=null;
		try {
			FileUpload fileUpload=new FileUpload(diskFactory);
			FileuploadRequestContext ctx=new FileuploadRequestContext(httpContext.getRequestParser());
			List itemList=fileUpload.parseRequest(ctx);
			//必ず１つのアップロードファイルとする
		    FileItem item = (FileItem) itemList.get(0);
		    //バッティングするのもいやなので、アップロードファイルのファイル名は使わない
			String importFileName=siriarizeFileName(httpContext,"import",".zip");
		    importFile=new File(importBaseDir,importFileName);
		    item.write(importFile);
		    
			AsyncOperation op=new AsyncOperation(AsyncOperation.OPERATION_IMPORT_ACCESSLOG);
			op.putParameter("importFile", importFile);
			logger.info(httpContext.getClientIp()+ "からの依頼により、移入処理を開始しました。uploadファイル名："+ item.getName() +" importファイル名:"+importFile.getName() + " size:"+importFile.length());
			op.doAsync(context);
			
			String uploadName=new File(item.getName()).getName();
			httpContext.setAttribute("message", "移入処理処理を開始しました。uploadファイル名："+ uploadName +" size:"+importFile.length());
			return "setting.vm";
		} catch (FileUploadException e) {
			logger.error("fail to import fileupload.",e);
		} catch (IOException e) {
			logger.error("fail to import IO Error.",e);
		} catch (Exception e) {
			logger.error("fail to import.",e);
		}
		httpContext.setAttribute("message", "移入処理処理に失敗しました。");
		return "setting.vm";
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletContext, java.util.Map)
	 */
	public void init(QueueletContext context, Map param) {
		this.context=context;
		this.config=Config.getInstance();
		this.configuration=config.getConfiguration();
		this.controllerUrl=configuration.getString("controllerUrl");
		this.traceBaseDir=new File(config.getConfiguration().getString("traceBaseDir"));
		this.importBaseDir=new File(config.getConfiguration().getString("importBaseDir"));
		this.diskFactory=new DiskFileItemFactory();
		diskFactory.setRepository(importBaseDir);		
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}
}
