package naru.web;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;

public class MappingEntry {
	public static Comparator mappingComparator=new MappingComparator();
	public static MappingEntry replayEntry=new MappingEntry(null,null,null,Config.QUEUE_REPLAY);
	public static MappingEntry proxyEntry=new MappingEntry(null,null,null,Config.QUEUE_PROXY);
	public static MappingEntry velopageEntry=new MappingEntry(null,null,null,Config.QUEUE_VELOPAGE);
	public static MappingEntry controllerEntry;
	static{
		Config config=Config.getInstance();
		Configuration configuration=config.getConfiguration();
		String controllerUrl=configuration.getString("controllerUrl");
		String controllerDocRoot=configuration.getString("controllerDocRoot");
		controllerEntry=MappingEntry.getControllerEntry(controllerUrl,controllerDocRoot);
	}
	
	private static class MappingComparator implements Comparator{
		public int compare(Object o1, Object o2) {
			MappingEntry e1=(MappingEntry)o1;
			MappingEntry e2=(MappingEntry)o2;
			//CONTROLLERは、ここには格納しない
			//sourceが長いものを先頭に持ってくる
			int length1=e1.sourcePath.length();
			int length2=e2.sourcePath.length();
			if( length1>length2){
				return -1;
			}else if( length1<length2){
				return 1;
			}
			//後は、一定の値を返却すればよい
			return e1.sourcePath.compareTo(e2.sourcePath);
		}
	}
	private String sourcePath;
	private Pattern replacePattern;
	private String destination;
	private String queue;
	//reverseの場合URIオブジェクト
	//fileSystemの場合Fileオブジェクト
	//controllerの場合、Fileオブジェクト(controllerDocRoot)
	private Object destinationObject;
	
	private static URI reverseUri(String destination){
		try {
			URI uri=new URI(destination);
			if( "http".equals(uri.getScheme())||"https".equals(uri.getScheme())){
				return uri;
			}
		} catch (URISyntaxException e) {
		}
		return null;
	}
	
	private static File fileSystemDirectory(String destination){
		File file=new File(destination);
		if(!file.exists()||!file.isDirectory()){
			return null;
		}
		return file;
	}
	
	public static MappingEntry getControllerEntry(String controllerUrl,String controllerDocRoot){
		File file=fileSystemDirectory(controllerDocRoot);
		if( file==null){
			throw new RuntimeException("fail to controller mapping.controllerDocRoot:"+ controllerDocRoot);
		}
		return new MappingEntry(controllerUrl,controllerDocRoot,file,Config.QUEUE_CONTROLLER);
	}
	
	public static MappingEntry getEntry(String sourcePath,String destination){
		URI uri=reverseUri(destination);
		if( uri!=null ){
			return new MappingEntry(sourcePath,destination,uri,Config.QUEUE_REVERSE);
		}
		File file=fileSystemDirectory(destination);
		if(file!=null){
			return new MappingEntry(sourcePath,destination,file,Config.QUEUE_FILESYSTEM);
		}
		return null;
	}
	
	private MappingEntry(String sourcePath,String destination,Object destinationObject,String queue){
		this.sourcePath=sourcePath;
		if(destination!=null){//windowsのパス区切りをunix風な"/"に変換
			destination=destination.replaceAll("\\\\", "/");
		}
		this.destination=destination;
		this.replacePattern=Pattern.compile("^"+sourcePath);
		this.destinationObject=destinationObject;
		this.queue=queue;
	}

	public boolean matches(String uri){
		return uri.startsWith(sourcePath);
	}
	
	public String getFullPath(String uri){
//		return uri.replaceFirst("^"+sourcePath, destination);
		synchronized(replacePattern){
			return replacePattern.matcher(uri).replaceFirst(destination);
		}
	}
	
	public String getSourcePath() {
		return sourcePath;
	}

	public String getQueue() {
		return queue;
	}

	public String getDestination() {
		return destination;
	}

	public Object getDestinationObject() {
		return destinationObject;
	}

}
