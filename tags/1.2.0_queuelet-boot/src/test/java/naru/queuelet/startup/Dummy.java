package naru.queuelet.startup;

import java.io.Serializable;

public class Dummy implements Serializable{
	public String func1(String param) {
		System.out.println(Thread.currentThread().getName() +":"+ this.getClass().getName()+":func1");
		return "func1ReturnRaw";
	}
	public String func2(String param) {
		System.out.println(Thread.currentThread().getName() +":"+ this.getClass().getName()+":func2");
		return "func2ReturnRaw";
	}
	public String func3(String param) {
		System.out.println(Thread.currentThread().getName() +":"+ this.getClass().getName()+":func3");
		return "func3ReturnRaw";
	}
	public String func4(String param) {
		System.out.println(Thread.currentThread().getName() +":"+ this.getClass().getName()+":func4");
		return "func4ReturnRaw";
	}
}
