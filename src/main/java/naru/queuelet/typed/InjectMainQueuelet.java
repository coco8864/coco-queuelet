/*
 * 作成日: 2004/10/08
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.queuelet.typed;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;
import naru.queuelet.startup.Startup;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class InjectMainQueuelet implements Queuelet {

	/* (非 Javadoc)
	 * @see naru.queuelet.Queuelet#service(java.lang.Object)
	 */
	public boolean service(Object req) {
		return false;
	}

	/* (非 Javadoc)
	 * @see naru.queuelet.Queuelet#init(naru.queuelet.QueueletCommand, java.util.Map)
	 */
	public void init(QueueletContext command, Map param) {
//		String classPath=System.getProperty("TARGET_CLASSPATH");
//		String className=System.getProperty("TARGET_CLASSNAME");
		String[] args=(String[])param.get("QueueletArgs");
		if( args==null || args.length<2 ){
			System.out.println("run injectMain.xml classpath classname");
			return;
		}
		
		String classPath=args[0];
		String className=args[1];
		
		File outDir=Startup.startupProperteis.queueletPath("bin");
		try {
			ClassPool cp=ClassPool.getDefault();
			ClassLoader system=ClassLoader.getSystemClassLoader();
			if(classPath!=null){
				cp.insertClassPath(classPath);
			}
			CtClass cc=cp.get(className);
			CtMethod cmMain=cc.getDeclaredMethod("main");
			cmMain.insertBefore("{if(" +				"naru.queuelet.startup.Startup.mainHooker((Class)" +				"Class.forName(\"" + cmMain.getDeclaringClass().getName() + "\")," +					"(String[])$1)){return;}}");
			cc.writeFile(outDir.getAbsolutePath());
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (CannotCompileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		InputStream is=null;
		OutputStream os=null;
		try {
			String fileName=null;
			int pos=className.lastIndexOf(".");
			if( pos>0){
				fileName=className.substring(pos+1);
			}
			File confXml=Startup.startupProperteis.queueletPath("conf/"+fileName + ".xml");
			if( confXml.exists()){
				System.out.println("conf file already exists." + confXml.getAbsolutePath());
				return;
			}
			is=new FileInputStream(Startup.startupProperteis.queueletPath("conf/template.xml"));
			os=new FileOutputStream(confXml);
			byte buff[]=new byte[1024];
			while(true){
				int len=is.read(buff);
				if(len<=0){
					break;
				}
				os.write(buff,0,len);
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if(is!=null){
				try {
					is.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
			if(os!=null){
				try {
					os.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
		}
	}

	/* (非 Javadoc)
	 * @see naru.queuelet.Queuelet#term()
	 */
	public void term() {
	}

}
