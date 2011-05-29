package naru.web;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.RequestContext;

public class FileuploadRequestContext implements RequestContext {
	private String characterEncoding="utf-8";
	private int contentLength;
	private String contentType;
	private InputStream bodyStream;
	
	public FileuploadRequestContext(HttpParser requestParser){
		this.contentLength=(int)requestParser.getContentLength();
		this.contentType=requestParser.getContentType();
		this.bodyStream=requestParser.getBodyStream();
	}

	public String getCharacterEncoding() {
		return characterEncoding;
	}

	public int getContentLength() {
		return contentLength;
	}

	public String getContentType() {
		return contentType;
	}

	public InputStream getInputStream() throws IOException {
		return bodyStream;
	}
}
