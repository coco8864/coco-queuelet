package naru.queuelet.watch;

import java.io.IOException;

public class WatchTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			WatchInfo watchInfo=WatchInfo.create("test");
			watchInfo.setIsForceEnd(true);
			System.out.println(watchInfo.getCommandLine());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
