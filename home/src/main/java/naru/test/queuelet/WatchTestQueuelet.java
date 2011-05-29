package naru.test.queuelet;

import java.util.Map;

import naru.queuelet.Queuelet;
import naru.queuelet.QueueletContext;
import naru.queuelet.watch.StartupInfo;

public class WatchTestQueuelet implements Queuelet,Runnable{
	private QueueletContext context;

	public void init(QueueletContext context, Map param) {
		this.context=context;
		StartupInfo startupInfo=(StartupInfo)param.get(PARAM_KEY_STARTUPINFO);
		if(startupInfo!=null){
			System.out.println(startupInfo.getName()+":"+startupInfo.getRestartCount());
		}
		Thread t=new Thread(this);
		t.start();
	}

	public boolean service(Object arg0) {
		return false;
	}

	public void term() {
	}

	public void run() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		StartupInfo startupInfo=new StartupInfo();
		startupInfo.setType(StartupInfo.TYPE_QUEUELET);
		startupInfo.setJavaHeapSize(123);
		String[] args={"xxxx"};
		startupInfo.setArgs(args);
		context.finish(false, true,startupInfo);
	}

}
