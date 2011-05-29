package naru.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;

public class JsonLib {
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		/*
		File imputFile=new File(new File("ctrl/exports"),"export.zip");
		InputStream is=new FileInputStream(imputFile);
		ZipInputStream zis=new ZipInputStream(is);
		while(true){
			ZipEntry ze=zis.getNextEntry();
			if(ze==null){
				break;
			}
			System.out.println("ze.getName()="+ ze.getName());
			System.out.println("ze.getCompressedSize()="+ ze.getCompressedSize());
			System.out.println("ze.getSize()="+ ze.getSize());
		}
		zis.close();
		*/
		/*
		
		JsonConfig config=new JsonConfig();
//		String[] excludes={"peekRequest","peekResponse"};
//		config.setExcludes(excludes);
//		config.setJavaPropertyFilter(new AccessLogPropertyFilter());
//		config.setJsonPropertyFilter(new AccessLogPropertyFilter());
		config.setRootClass(AccessLog.class);
//		JSONObject json=JSONObject.fromObject(new AccessLog(),config);
		JSON json=JSONSerializer.toJSON(new AccessLog(),config);
		System.out.println("json:"+json);
		String jsonString=json.toString();
		
		Object o=JSONSerializer.toJava(json,config);
		System.out.println("o"+o);
		
		JSON j2=JSONObject.fromObject(jsonString, config);
		Object o2=JSONSerializer.toJava(j2,config);
		System.out.println("o2"+o2);
		
		AccessLog al=new AccessLog();
		al.setIp("123.456.789.abc");
		String s3=al.toJson();
		System.out.println("accesslog.toJson()="+s3);
		
		AccessLog o3=AccessLog.fromJson(s3);
		System.out.println("o3"+o3);
		*/
		String jsonString="{\"id\":10026,\"ip\":\"10.33.103.86\",\"mappingDestination\":\"\",\"mappingSource\":\"\",\"processQueue\":131078,\"processTime\":92,\"requestBody\":\"\",\"requestFile\":\"\",\"requestHeaderLength\":651,\"requestLine\":\"GET http://mwsc1.soft.fujitsu.com/ HTTP/1.1\",\"responseFile\":\"\",\"responseLength\":552,\"startTime\":{\"date\":4,\"day\":1,\"hours\":19,\"minutes\":52,\"month\":7,\"nanos\":506000000,\"seconds\":25,\"time\":1217847145506,\"timezoneOffset\":-540,\"year\":108},\"statusCode\":\"200\",\"timeRecode\":\"2,2,4,4,\"}";
		AccessLog accessLog=AccessLog.fromJson(jsonString);
		System.out.println(accessLog);
		System.out.println(accessLog.getStartTime());
		
		String js=accessLog.toJson();
		System.out.println(js);
		
		JSON json=JSONSerializer.toJSON(accessLog,jsonConfig);
		System.out.println(json.toString());
		accessLog=AccessLog.fromJson(json.toString());
		System.out.println(accessLog);
		System.out.println(accessLog.getStartTime());
		
		JSON json2=JSONObject.fromObject(json.toString());
		accessLog=(AccessLog)JSONSerializer.toJava(json2,jsonConfig);
		System.out.println(accessLog);
		System.out.println(accessLog.getStartTime());
	}
	private static JsonConfig jsonConfig;
	static{
		jsonConfig=new JsonConfig();
		jsonConfig.setRootClass(AccessLog.class);
		DatePropertyFilter dpf=new DatePropertyFilter();
		jsonConfig.setJavaPropertyFilter(dpf);
		jsonConfig.setJsonPropertyFilter(dpf);
	}

}
