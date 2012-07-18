/*
 * 作成日: 2004/08/05
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
package naru.test;

/**
 * @author naru
 *
 * この生成されたコメントの挿入されるテンプレートを変更するため
 * ウィンドウ > 設定 > Java > コード生成 > コードとコメント
 */
public class Test {
	
	private static int boo(int i){
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(Thread.currentThread().getName() + ":$$ I am boo,param="+i);
		return 3;
	}
	private static void boo3(int i) throws ClassNotFoundException{
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(Thread.currentThread().getName() + ":$$ I am boo3,param="+i);
		Class.forName("xxxx");
		throw new RuntimeException("boo3 throw");
	}

	private int boo2(int i){
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(Thread.currentThread().getName() + ":$$ I am boo2,param="+i);
		return 4;
	}
	
	private int foo(String s){
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(Thread.currentThread().getName() + ":$$ I am foo,param="+s);
		return 5;
	}
	
	private String woo(String s){
		System.out.println(Thread.currentThread().getName() + ":$$ I am woo IN ,param="+s);
		System.out.flush();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(Thread.currentThread().getName() + ":$$ I am woo OUT,param="+s);
		System.out.flush();
		return "$77$";
	}
	

	public static void main(String[] args) {
		System.out.println("$$$$$$$$START$$$$$$$$$$");
		System.out.println(Thread.currentThread().getName() + ":##boo()=" + boo(11));
		try {
			System.out.println(Thread.currentThread().getName() + ":##boo3()=");boo3(11);
		} catch (Throwable e) {
		}
		Test t=new Test();
		for(int i=0;i<10;i++){
			System.out.println(Thread.currentThread().getName() + ":##t.toString()=" +t.toString());
			System.out.println(Thread.currentThread().getName() + ":##t.boo2()=" +t.boo2(i));
			System.out.println(Thread.currentThread().getName() + ":##t.foo()=" +t.foo("%%:"+ i));
			System.out.println(Thread.currentThread().getName() + ":##t.woo()=" +t.woo("&&:"+ i));
		}
		System.out.println("$$$$$$$$END$$$$$$$$$$");
	}
	/* (非 Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "I'm " + getClass().getName();
	}

}
