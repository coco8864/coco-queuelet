package naru.queuelet.watch;

import java.io.File;
import java.io.Serializable;


public class StartupInfo implements Serializable{
	public static int TYPE_QUEUELET=1;
	public static int TYPE_JAVA=2;
	public static int TYPE_GENERAL=3;
	
	private int type;//queuelet=0,java=1,general=2
	private String name;
	private int restartCount;/* 使用しない */
	
	private int javaHeapSize=-1;//mbyte単位
	private String[] javaVmOptions=null;
	private String[] args=null;//queueletの場合は、conf以降,javaの場合は、javaコマンド以降
	private String[] envs=null;/* 使用しない */
	private File currentDir=null;/* 使用しない */
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getRestartCount() {
		return restartCount;
	}
	public void setRestartCount(int restartCount) {
		this.restartCount = restartCount;
	}
	public int getJavaHeapSize() {
		return javaHeapSize;
	}
	public void setJavaHeapSize(int javaHeapSize) {
		this.javaHeapSize = javaHeapSize;
	}
	public String[] getJavaVmOptions() {
		return javaVmOptions;
	}
	public void setJavaVmOptions(String[] javaVmOptions) {
		this.javaVmOptions = javaVmOptions;
	}
	public String[] getArgs() {
		return args;
	}
	public void setArgs(String[] args) {
		this.args = args;
	}
	public String[] getEnvs() {
		return envs;
	}
	public void setEnvs(String[] envs) {
		this.envs = envs;
	}
	public File getCurrentDir() {
		return currentDir;
	}
	public void setCurrentDir(File currentDir) {
		this.currentDir = currentDir;
	}
}

