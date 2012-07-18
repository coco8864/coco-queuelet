/*
 * Created on 2004/11/19
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.queuelet.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * @author naru
 *
 * Container環境情報の制御
 * コンテナ機能の追い出し
 */
public class QueueletProperties {
	static private Logger logger=Logger.getLogger(QueueletProperties.class);
	private Properties userProperties=new Properties();
	private Properties sysProperties=new Properties();
	
	public QueueletProperties(File userProperties,File sysProperties){
		readProperties(this.userProperties,userProperties);
		readProperties(this.sysProperties,sysProperties);
	}
	public void addUserProperties(File fileName){
		readProperties(this.userProperties,fileName);
	}
	
	private void readProperties(Properties prop,File fileName){
		File propertyFile=fileName;
		if( !propertyFile.canRead() ){
			logger.warn("can't read property file." + propertyFile.getAbsolutePath());
			return;
		}
		FileInputStream fis=null;
		try {
			fis=new FileInputStream(propertyFile);
			prop.load(fis);
		} catch (IOException e) {
			logger.error("read property error." + propertyFile.getAbsolutePath(),e);
		}finally{
			if( fis!=null ){
				try {
					fis.close();
				} catch (IOException ignore) {
					ignore.printStackTrace();
				}
			}
		}
	}
	
	private String getValue(String key,Properties runtimeProperties){
		String value=null;
		if( runtimeProperties!=null){
			value=runtimeProperties.getProperty(key);
			if( value!=null ){
				return value;
			}
		}
		value=System.getProperty(key);//-Dがあれば最優先
		if(value!=null){
			return value;
		}
		value=userProperties.getProperty(key);//QUEUELET_HOME\queuelet.propertiesが次
		if( value!=null){
			return value;
		}
		value=sysProperties.getProperty(key);//QUEUELET_HOME\sys\queuelet.propertiesが次
		if( value!=null){
			return value;
		}
		value=System.getenv(key);//環境変数にあれば採用
		if( value!=null){
			return value;
		}
		logger.warn("not replace key:" + key);
		return null;
	}
	
	private String replaceOne(String in,int pos,Properties runtimeProperties){
		int startReplace=in.indexOf("$",pos);
		if( startReplace<0){
			return in;
		}
		StringBuffer start=null;
		String end="";
		int startPos=in.indexOf("{");
		int endPos=in.indexOf("}");
		String key=null;
		if( startPos<0 || endPos<0 ){
			/* 何れかの括弧がなく対応が取れていない場合 */
			key=in.substring(startReplace+1);
			start=new StringBuffer(in.substring(0,startReplace));
		}else{
			key=in.substring(startPos+1,endPos);
			start=new StringBuffer(in.substring(0,startReplace));
			end=in.substring(endPos+1);
		}
		key=key.trim();
		String value=getValue(key,runtimeProperties);
		if(value==null){//該当する値がなかった場合、空白に変換
			value="";
		}
		return start.append(value).append(end).toString();
	}
	
	public void setSysProperty(Object key,Object value){
		sysProperties.put(key,value);
	}

	public String resolveProperty(String in){
		return resolveProperty(in,null);
	}
	
	/* aaa${xxx}bbb -> aaayyybbb */
	public String resolveProperty(String in,Properties runtimeProperties){
		if( in==null ){
			return null;
		}
		int pos=0;
		String out=in;
		while(true){
			pos=in.indexOf("$",pos);
			if( pos<0){
				break;
			}
			out=replaceOne(in,pos,runtimeProperties);
			if( in.equals(out) ){
				pos++;//変換されなかった場合の読み飛ばし
			}
			in=out;
		}
		return out;
	}

	public String getProperty(String in){
		return 	getProperty(in,null);
	}

	public String getProperty(String in,Properties runtimeProperties){
		String key=getValue(in,runtimeProperties);
		if( key==null){
			return null;
		}
		key=key.trim();
		return resolveProperty(key,runtimeProperties);
	}
}
