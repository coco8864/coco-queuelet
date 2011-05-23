package naru.queuelet.startup;

public class Deamon implements Runnable{
	private WatchInfo watchInfo;
	
	private Deamon(){
	}
	
	public void run() {
		while(true){
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}
			if(!watchInfo.isRun()){//èIóπÇµÇΩ
				if(watchInfo.isRestart()){
					watchInfo.executeChild();
				}else{
					System.out.println("Deamon stoped");
					watchInfo.term();
					return;
				}
			}
			if(!watchInfo.isForceEnd()){//ã≠êßí‚é~óvãÅ
				watchInfo.terminateChild();
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	}

}
