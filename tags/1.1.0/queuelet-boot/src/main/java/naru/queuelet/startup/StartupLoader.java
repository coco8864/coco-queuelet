/*
 * Created on 2004/11/25
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.queuelet.startup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;


/**
 * @author NARU
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class StartupLoader extends URLClassLoader {
	private String name;
	private boolean delegate=false;
	
	/* bootÇ∆commonsÇ…Ç†ÇÈÉNÉâÉXÇÕÇ»ÇÈÇ◊Ç≠â∫Ç≈loadÇ≥ÇπÇÈ */
	/* ÇªÇÍà»äOÇÕÇ»ÇÈÇ◊Ç≠è„Ç≈loadÇ≥ÇπÇÈ */
	static List bootAndCommonClasses=new ArrayList();
	static{
		Properties prop=new Properties();;
		InputStream is=null;
		try {
			is=StartupLoader.class.getResourceAsStream("commonAndSystemClasses.properties");
			prop.load(is);
		} catch (IOException e) {
		}finally{
			if(is!=null){
				try {
					is.close();
				} catch (IOException ignore) {
				}
			}
		}
		bootAndCommonClasses.addAll(prop.keySet());
	}
	
	private static void addJarPath(File libDir,List urls) throws MalformedURLException {
		File directory = libDir;
		if (!directory.isDirectory()
			|| !directory.exists()
			|| !directory.canRead())
			return;
		String filenames[] = directory.list();
		for (int j = 0; j < filenames.length; j++) {
			String filename = filenames[j].toLowerCase();
			if (!filename.endsWith(".jar"))
				continue;
			File file = new File(directory, filenames[j]);
			urls.add(file.toURL());
		}
		return;
	}
	private static URL[] filesToUrls(File[] classpaths,File[] libs){
		List urls=new ArrayList();
		try {
			if( classpaths!=null ){
				for(int i=0;i<classpaths.length;i++){
					urls.add(classpaths[i].toURL());
				}
			}
			if( libs!=null ){
				for(int i=0;i<libs.length;i++){
					addJarPath(libs[i],urls);
				}
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return (URL[])urls.toArray(new URL[0]);
	}
	private ClassLoader parent;
	
	/**
	 * @param arg0
	 * @param arg1
	 */
	public StartupLoader(String name,File[] classpaths,File[] libs,ClassLoader parent) {
		super(filesToUrls(classpaths,libs),parent);
		this.name=name;
		this.parent=parent;
	}
	
	public  Class loadClass(String className)throws ClassNotFoundException {
		Class cl=null;
		cl=findLoadedClass(className);
		if( cl!=null ){
//			System.out.println(name +":loadClass1:"+className);
			return cl;
		}
		if(delegate){
//			System.out.println(name +":loadClass2.1:"+className);
			return super.loadClass(className);
		}
		if( bootAndCommonClasses.contains(className)){
//			System.out.println(name +":loadClass2.0:"+className);
			return super.loadClass(className);
		}
		try {
			cl=findClass(className);
//			System.out.println(name +":loadClass3:"+className);
		} catch (ClassNotFoundException e) {
			cl=super.loadClass(className);
//			System.out.println(name +":loadClass4:"+className);
		}
		return cl;
	}
	
	public String toString(){
		return name + " : " + super.toString();
	}
}
