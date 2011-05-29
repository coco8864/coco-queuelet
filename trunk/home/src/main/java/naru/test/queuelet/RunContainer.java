package naru.test.queuelet;

import java.io.FileNotFoundException;

import naru.queuelet.Container;

public class RunContainer {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws FileNotFoundException, InterruptedException {
		Container container=new Container(args[0]);
		synchronized(container){
			container.wait();
		}
		container.stop();
	}

}
