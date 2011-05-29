/*
 * Created on 2004/10/20
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Map;

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
public class FileSystem implements Queuelet {
	static private Logger logger=Logger.getLogger(FileSystem.class);
	private static Configuration contentTypeConfig=Config.getInstance().getConfiguration("ContentType");
	
	private QueueletContext context;
	private Config config;
	private Configuration configuration;
	private boolean listing;
	private String[] welcomFiles;
	
	private String getContentType(HttpContext httpContext,File file){
		String contentType=(String)httpContext.getAttribute(HttpContext.ATTRIBUTE_RESPONSE_CONTENT_TYPE);
		if(contentType!=null){
			return contentType;
		}
		String name=file.getName();
		int pos=name.lastIndexOf(".");
		if( pos>0 ){
			String ext=name.substring(pos+1);
			contentType=contentTypeConfig.getString(ext);
			if( contentType!=null){
				return contentType;
			}
		}
		//�^�킵���́AOctedStream
		return "application/octet-stream";
	}
	
	private long getContentLength(HttpContext httpContext,File file){
		Long length=(Long)httpContext.getAttribute(HttpContext.ATTRIBUTE_RESPONSE_FILE_LENGTH);
		if(length!=null){
			return length.longValue();
		}
		return file.length();
	}
	
	private InputStream getResponseStream(HttpContext httpContext,File file) throws IOException{
		Long offset=(Long)httpContext.getAttribute(HttpContext.ATTRIBUTE_RESPONSE_FILE_OFFSET);
		InputStream fileIs=new FileInputStream(file);
		if(offset==null){
			return fileIs;
		}
		fileIs.skip(offset.longValue());
		return fileIs;
	}
	
	//���݊m�F�ς݂̃t�@�C�������X�|���X����B
	private boolean sendFile(HttpContext httpContext,File file) throws IOException{
		String ifModifiedSince=httpContext.getRequestHeader(HttpContext.IF_MODIFIED_SINCE_HEADER);
		Date ifModifiedSinceDate=httpContext.parseDateHeader(ifModifiedSince);
		long ifModifiedSinceTime=-1;
		if(ifModifiedSinceDate!=null){
			ifModifiedSinceTime=ifModifiedSinceDate.getTime();
		}
		long lastModifiedTime=file.lastModified();
		String lastModified=httpContext.fomatDateHeader(new Date(lastModifiedTime));
		//�t�@�C�����t�Ƃ��ĕ\���ł���l�ɂ́A�덷�����邽�߁A�\���ł��鎞�����擾
		lastModifiedTime=httpContext.parseDateHeader(lastModified).getTime();
		
		if( ifModifiedSinceTime>=lastModifiedTime ){
			httpContext.registerResponse("304");
			return true;
		}
		httpContext.setResponseHeader(HttpContext.LAST_MODIFIED_HEADER, lastModified);
		long contntLength=getContentLength(httpContext,file);
		httpContext.addResponseHeader(
				HttpContext.CONTENT_LENGTH_HEADER,Long.toString(contntLength));
		
		String contentDisposition=(String)httpContext.getAttribute(HttpContext.ATTRIBUTE_RESPONSE_CONTENT_DISPOSITION);
		if( contentDisposition!=null){
			httpContext.setResponseHeader(HttpContext.CONTENT_DISPOSITION_HEADER, contentDisposition);
		}
		
		InputStream fileIs=getResponseStream(httpContext,file);
		String contentType=getContentType(httpContext,file);
		httpContext.registerResponse("200",contentType,fileIs);
		return true;
	}
	
	//���݊m�F�ς݂̃f�B���N�g�����ꗗ���X�|���X����B
	private boolean snedFileList(HttpContext httpContext,String uriPath,File file) throws IOException{
		if(!uriPath.endsWith("/")){
			uriPath=uriPath+"/";
		}
		httpContext.setAttribute("source", file.getAbsoluteFile());
		httpContext.setAttribute("base", uriPath);
		httpContext.setAttribute("fileList", file.listFiles());
		httpContext.setAttribute(HttpContext.ATTRIBUTE_VELOCITY_PAGE,"/fileSystem/listing.vm");
		context.enque(httpContext,Config.QUEUE_VELOPAGE);//VelocityPage�ɉ�ʐ����������Ϗ�
		return false;//�Ϗ����邩��
	}
	
	private boolean sendResponse(HttpContext httpContext) throws IOException{
		File file=(File)httpContext.getAttribute(HttpContext.ATTRIBUTE_RESPONSE_FILE);
		if(file!=null){//���X�|���X����t�@�C�����A���ڎw�肳�ꂽ�ꍇ
			return sendFile(httpContext,file);
		}
		
		String uriPath=httpContext.getRequestUri();
		try {//���{��t�@�C�����̑Ή�
			uriPath = URLDecoder.decode(uriPath,"utf-8");
		} catch (UnsupportedEncodingException e) {
			logger.error("URLDecoder.decode error",e);
			throw new IllegalArgumentException("URLDecoder.decode error");
		}
		MappingEntry mappingEntry=(MappingEntry)httpContext.getAttribute(HttpContext.ATTRIBUTE_MAPPING_ENTRY);
		String fullPath=mappingEntry.getFullPath(uriPath);
		//�N�G���̍폜
		int pos=fullPath.indexOf('?');
		if(pos>=0){
			fullPath=fullPath.substring(0,pos);
		}
		file = new File(fullPath);
		
		//�g���o�[�T�����ꂽ��Alogging����404
		File destination=(File)mappingEntry.getDestinationObject();
		String fileCanonPath;
		try {
			fileCanonPath = file.getCanonicalPath();
		} catch (IOException e) {
			logger.warn("fail to getCanonicalPath.file:"+file.getAbsolutePath(),e);
			httpContext.registerResponse("404","Not Found");
			return true;
		}
		String distCanonPath=destination.getCanonicalPath();
		if( !fileCanonPath.startsWith(distCanonPath) ){
			logger.warn("traversal error. file:" + fileCanonPath + " dist:" + distCanonPath);
			httpContext.registerResponse("404","Not Found");
			return true;
		}
		
		if( !file.exists() || !file.canRead()){//���݂��Ȃ�������A�ǂݍ��߂Ȃ�������
			logger.debug("Not found");
			httpContext.registerResponse("404","Not Found");
			return true;
		}
		
		//wellcomfile����
		if( file.isDirectory() && welcomFiles!=null){
			for(int i=0;i<welcomFiles.length;i++){
				File wellcomFile=new File(file,welcomFiles[i]);
				if( wellcomFile.exists() && wellcomFile.canRead()){
					file=wellcomFile;
					//�����AURI��"/"�ŏI����Ă��Ȃ������瑊�΂������ł��Ȃ��̂ŁA���_�C���N�g
					if(!uriPath.endsWith("/")){
						String hostHeader=httpContext.getRequestHeader("Host");
						httpContext.setResponseHeader(HttpContext.LOCATION_HEADER, 
								"http://" + hostHeader + uriPath + "/" + welcomFiles[i]);
						httpContext.registerResponse("302");
						return true;
					}
					break;
				}
			}
		}
		
		if(file.isFile()){//�t�@�C����������
			return sendFile(httpContext,file);
		}
		if(listing && file.isDirectory()){//�f�B���N�g����������
			//velocityPage���烊�X�g�o��
			return snedFileList(httpContext,uriPath,file);
		}
		logger.debug("Not allow listing");
		httpContext.registerResponse("404","Not Found");
		return true;
	}
	
	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		boolean rc=true;//����n�́Atrue,�ُ�n�́Afalse
		HttpContext httpContext=(HttpContext)req;
		httpContext.passQueue(AccessLog.QUEUE_FILESYSTEM);
		httpContext.setResponseHeader("Connection","close");//Keep-Alive���Ȃ�
		try {
			rc=sendResponse(httpContext);
		} catch (IOException e) {
			logger.error("Response error.",e);
			httpContext.registerResponse("500","Internal Server error");
		}finally{
			if(rc){
				httpContext.startResponse();
			}
		}
		return rc;
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletContext, java.util.Map)
	 */
	public void init(QueueletContext context, Map param) {
		this.context=context;
		this.config=Config.getInstance();
		this.configuration=config.getConfiguration();
		this.listing=configuration.getBoolean("listing");
		String welcome=configuration.getString("welcomeFiles");
		if(welcome!=null){
			this.welcomFiles=welcome.split(";");
		}
	}

	/* (non-Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}
