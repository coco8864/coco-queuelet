private Class queuelet_defineClass(String name,
									  byte[] b,
									  int off,
									  int len)
							   throws ClassFormatError{
/*	System.out.println("queuelet_defineClass:"+name);*/
	if(__queueletHooker==null){
	System.out.println("queuelet_defineClass return queueletHooker:null");
		return super.defineClass(name,b,off,len); 
	}
	byte[] byteCode=__queueletHooker.getByteCode(name,b,off,len);
	if(byteCode!=(byte[])null){
		try{
			Class c=defineClass(name,byteCode,0,byteCode.length);
			__queueletHooker.registerClass(name,c);
			return c; 
		}catch(Throwable e){
		}
	}
	return super.defineClass(name,b,off,len); 
}
