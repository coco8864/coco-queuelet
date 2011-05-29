/*
 * Created on 2004/10/21
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;
import naru.util.HibernateUtil;

/**
 * @author naru hayashi
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AsyncService implements Queuelet,Runnable {
	private static Logger logger=Logger.getLogger(AsyncService.class);	
	
	
	private QueueletContext context;
	private Config config;
	private Thread timerThread;
	private Configuration configuration;
	private File traceBaseDir;
	private File exportBaseDir;
	private boolean stop;
	private long intervalTime=600000;
	
	//必ずディレクトリ配下のファイルをポイントする。
	private File getEntryFile(File dir,String name) throws IOException{
		if(name==null){
			return null;
		}
		File file=new File(dir,name);
		String fileCanonPath=file.getCanonicalPath();
		String dirCanonPath=dir.getCanonicalPath();
		if( !fileCanonPath.startsWith(dirCanonPath) ){
			logger.warn("traversal error. file:" + fileCanonPath + " dir:" + dirCanonPath);
			return null;
		}
		return file;
	}
	
	private void zipInputFile(ZipInputStream zis,File traceFile) throws IOException{
		byte[] buff=new byte[1024];
		FileOutputStream fos=new FileOutputStream(traceFile);
		try {
			while(true){
				int readLength=zis.read(buff);
				if(readLength<=0){
					break;
				}
				fos.write(buff,0,readLength);
			}
		} finally {
			try {
				fos.close();
			} catch (IOException ignore) {
				logger.warn(ignore);
			}
		}
	}
	
	private AccessLog zipInputAccessLog(ZipInputStream zos) throws IOException{
		char[] buff=new char[1024];
		Reader reader=new InputStreamReader(zos);
		StringBuffer sb=new StringBuffer();
		while(true){
			int readLength=reader.read(buff);
			if(readLength<=0){
				break;
			}
			sb.append(buff,0,readLength);
		}
		String jsonString=sb.toString();
		return AccessLog.fromJson(jsonString);
	}
	
	private void accessLogImport(File importFile){
		ZipInputStream zis=null;
		try {
			InputStream is=new FileInputStream(importFile);
			zis=new ZipInputStream(is);
			AccessLog prevAccessLog=null;//前回処理時のAccessLog
			String prevOrgId=null;
			int count=0;
			while(true){
				ZipEntry ze=zis.getNextEntry();
				if(ze==null){
					break;
				}
				String name=ze.getName();
				String orgId=name.split("/")[0];
				String traceName=null;
				if(name.endsWith("accesslog.json")){
					AccessLog accessLog=zipInputAccessLog(zis);
					accessLog.setId(null);
					accessLog.setRequestFile(null);
					accessLog.setResponseFile(null);
					accessLog.insert();
					prevAccessLog=accessLog;
					prevOrgId=orgId;
					count++;
					continue;
				}else if(name.endsWith("req_trace.log") && orgId.equals(prevOrgId)){
					traceName="req" + prevAccessLog.getId().toString()+".log";
					zipInputFile(zis,new File(traceBaseDir,traceName));
					prevAccessLog.setRequestFile(traceName);
				}else if(name.endsWith("res_trace.log") && orgId.equals(prevOrgId)){
					traceName="res" + prevAccessLog.getId().toString()+".log";
					zipInputFile(zis,new File(traceBaseDir,traceName));
					prevAccessLog.setResponseFile(traceName);
				}else{
					logger.warn("import unkown data:"+name);
					continue;
				}
				prevAccessLog.update();//resとreqで２かいupdateされ冗長だが仕方ない
			}
			zis.close();
			zis=null;
			//正しく処理できれば、ファイルを消す
			importFile.delete();
			logger.info("移入処理が正常終了しました。importファイル名:"+importFile.getName() + " レコード数:" + count );
			return;
		} catch (IOException e) {
			logger.error("failt to import",e);
		}finally{
			if(zis!=null){
				try {
					zis.close();
				} catch (IOException ignore) {
					logger.warn(ignore);
				}
			}
			HibernateUtil.clearSession();
		}
		logger.info("移入処理が失敗しました。importファイル名:"+importFile.getName());
	}
	
	private void zipOutputFile(ZipOutputStream zos,String entryName,File traceFile,byte buff[]) throws IOException{
		if(!traceFile.exists()){
			return;
		}
		ZipEntry ze=new ZipEntry(entryName);
		ze.setSize(traceFile.length());
		zos.putNextEntry(ze);
		FileInputStream fis=new FileInputStream(traceFile);
		try {
			while(true){
				int readLength=fis.read(buff);
				if(readLength<=0){
					break;
				}
				zos.write(buff,0,readLength);
				
			}
		} finally {
			try {
				fis.close();
				zos.closeEntry();
			} catch (IOException ignore) {
				logger.warn(ignore);
			}
		}
	}
	
	private void zipOutputAccessLog(ZipOutputStream zos,AccessLog accessLog) throws IOException{
		String json=accessLog.toJson();
		byte[] jsonbytes=json.getBytes("utf-8");
		
		String id=accessLog.getId().toString();
		ZipEntry ze=new ZipEntry(id+"/accesslog.json");
		ze.setSize(jsonbytes.length);
		zos.putNextEntry(ze);
		zos.write(jsonbytes);
		zos.closeEntry();
		byte[] buff=new byte[1024];
		String requestFile=accessLog.getRequestFile();
		if(requestFile!=null){
			zipOutputFile(zos,id+"/req_trace.log",new File(traceBaseDir,requestFile),buff);
		}
		String responseFile=accessLog.getResponseFile();
		if(responseFile!=null){
			zipOutputFile(zos,id+"/res_trace.log",new File(traceBaseDir,responseFile),buff);
		}
	}
	
	private void accessLogExport(String exportFileName,String query){
		ZipOutputStream zos=null;
		try {
			File outputFile=new File(exportBaseDir,exportFileName);
			OutputStream os=new FileOutputStream(outputFile);
			zos=new ZipOutputStream(os);
			Iterator itr=AccessLog.query(query, 0, 0);
			int count=0;
			while(itr.hasNext()){
				AccessLog accessLog=(AccessLog)itr.next();
				zipOutputAccessLog(zos,accessLog);				
				count++;
			}
			zos.close();
			zos=null;
			logger.info("移出処理が正常終了しました。exportファイル名:"+exportFileName + " レコード数:" + count );
			return;
		} catch (HibernateException e) {
			logger.error("failt to expot",e);
		} catch (IOException e) {
			logger.error("failt to expot",e);
		}finally{
			if(zos!=null){
				try {
					zos.close();
				} catch (IOException ignore) {
					logger.warn(ignore);
				}
			}
			HibernateUtil.clearSession();
		}
		logger.info("移出処理が失敗しました。exportファイル名:"+exportFileName);
	}
	
	private void deleteTraceFile(String fileName){
		File trace;
		try {
			trace = getEntryFile(traceBaseDir,fileName);
		} catch (IOException e) {
			logger.warn("fail to delete trace."+fileName,e);
			return;
		}
		if(trace==null){
			return;
		}
		if(trace.exists()){
			trace.delete();
		}
		return;
	}
	
	private void deleteAccessTraceFiles(List traceList){
		Iterator itr=traceList.iterator();
		while(itr.hasNext()){
			String fileName=(String)itr.next();
			deleteTraceFile(fileName);
		}
	}

	private void deleteAccessLogs(List deleteLogIds){
		Iterator itr=deleteLogIds.iterator();
		while(itr.hasNext()){
			Long id=(Long)itr.next();
			AccessLog accessLog=AccessLog.getById(id);
			if(accessLog==null){
				continue;
			}
			deleteTraceFile(accessLog.getRequestFile());
			deleteTraceFile(accessLog.getResponseFile());
			accessLog.delete();
		}
	}
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		AsyncOperation ope=(AsyncOperation)req;
		String operation=ope.getOperation();
		if(AsyncOperation.OPERATION_DELETE_TRACE_FILES.equals(operation)){
			List deleteList=(List)ope.getParameterObject("deleteList");
			deleteAccessTraceFiles(deleteList);
		}else if(AsyncOperation.OPERATION_DELETE_ACCESSLOGS.equals(operation)){
			deleteAccessLogs((List)ope.getParameterObject("deleteLogIds"));
		}else if(AsyncOperation.OPERATION_EXPORT_ACCESSLOG.equals(operation)){
			accessLogExport(ope.getParameter("exportFile"),ope.getParameter("query"));
		}else if(AsyncOperation.OPERATION_IMPORT_ACCESSLOG.equals(operation)){
			accessLogImport((File)ope.getParameterObject("importFile"));
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletContext, java.util.Map)
	 */
	public void init(QueueletContext context, Map param) {
		timerThread=new Thread(this);
		timerThread.setName("AsyncService TimerThread");
		
		this.context=context;
		this.config=Config.getInstance();
		this.configuration=config.getConfiguration();
		this.traceBaseDir=new File(config.getConfiguration().getString("traceBaseDir"));
		this.exportBaseDir=new File(config.getConfiguration().getString("exportBaseDir"));
		this.intervalTime=configuration.getLong("intervalTime", 600000);
		timerThread.start();
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
		stop=true;
		try {
			timerThread.join();
		} catch (InterruptedException e) {
			logger.error("fail to wait AsyncService timerThread stop",e);
		}
	}
	
	private void checkAndDeleteAccessLog(){
		int count=AccessLog.countOfAccessLog(null);
		logger.info("accessLog count:" +count);
		int maxAccessLogCount=configuration.getInt("maxAccessLogCount", 0);
		if(maxAccessLogCount<=0){
			return;//制限なし
		}
		if(count<=maxAccessLogCount){
			return;
		}
		logger.info("timer check delete accessLog start count:" +(count-maxAccessLogCount));
		Iterator itr=AccessLog.query("select id from AccessLog", 0, (count-maxAccessLogCount));
		List deleteLogIds=new ArrayList();
		while(itr.hasNext()){
			deleteLogIds.add(itr.next());
		}
		deleteAccessLogs(deleteLogIds);
		logger.info("timer check delete accessLog end");
	}

	public void run() {
		logger.info(Thread.currentThread().getName() + " start");
		while(true){
			if(stop){
				break;
			}
			try {
				Thread.sleep(intervalTime);
			} catch (InterruptedException e) {
			}
			checkAndDeleteAccessLog();
		}
		logger.info(Thread.currentThread().getName() + " end");
	}
}
