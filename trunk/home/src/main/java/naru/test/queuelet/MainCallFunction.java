package naru.test.queuelet;

public class MainCallFunction implements java.io.Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public String stringStringFunc(String arg){
		System.out.println("stringStringFunc arg:"+ arg);
		return "stringStringFunc arg:"+ arg;
	}
	
	private int intIntFunc(int arg){
		System.out.println(Thread.currentThread().getName() + ":intIntFunc arg:"+ arg);
		return 100000+ arg;
	}
	
	private int[] intarryIntarrayFunc(int[] arg){
		System.out.println(Thread.currentThread().getName() + ":intarryIntarrayFunc arg:"+ arg);
		return new int[]{3,2,4};
	}
	
	private Object[] objarryObjarrayFunc(Object[] arg){
		System.out.println(Thread.currentThread().getName() + ":objarryObjarrayFunc arg:"+ arg);
		return new Object[]{"ret1","ret2","ret3"};
	}
	
	private void mixFunc(boolean b,byte by,char c,double d,float f,int i,long l,short s){
		System.out.println(Thread.currentThread().getName() + ":main call mixFunc return:void");
		return;
	}

	private void mixFunc2(boolean[] b,byte[] by,char[] c,double[] d,float[] f,int[] i,long[] l,short[] s){
		System.out.println(Thread.currentThread().getName() + ":main call mixFunc2 return:void");
		return;
	}
	
	private void mixFunc3(boolean[][] b){
		System.out.println(Thread.currentThread().getName() + ":main call mixFunc3 return:void");
		return;
	}

	private String throwExceptionFunc(String s){
		System.out.println(Thread.currentThread().getName() + ":main call throwExceptionFunc return:void");
		if( true ){
			throw new java.lang.IllegalStateException("expect exception");
		}
		return "thrwExceptionFunc";
	}
	
	
	public static void main(String[] args){
		MainCallFunction mcf=new MainCallFunction();
		
		String ret=mcf.stringStringFunc("test args");
		System.out.println(Thread.currentThread().getName() + ":main call stringStringFunc return:"+ ret);
		
//		mcf=new MainCallFunction();
		int intRc=mcf.intIntFunc(123);
		System.out.println("main call intIntFunc return:"+ intRc);
		
		int[] array=mcf.intarryIntarrayFunc(new int[]{8,4,6,9,7});
		System.out.println("main call intarryIntarrayFunc return:"+ array);
		
		Object[] objectArray=mcf.objarryObjarrayFunc(new Object[]{"arg1","arg2","arg3","arg4"});
		System.out.println("main call intarryIntarrayFunc return:"+ objectArray);
		
		
		mcf.mixFunc(false,(byte)1,(char)20,1.0,10.3f,7,5L,(short)54);
		System.out.println("main call mixFunc return: void");
		
		mcf.mixFunc2(null,null,null,null,null,null,null,null);
		System.out.println("main call mixFunc2 return: void");

		mcf.mixFunc3(null);
		System.out.println("main call mixFunc3 return: void");
		
		try {
			mcf.throwExceptionFunc("let't throw exception");
			System.out.println("main call throwExceptionFunc !!!never come here.");
		} catch (Exception e) {
			System.out.println("main call throwExceptionFunc throw exception."+ e.toString());
		}
/*
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
*/
	}
}
