private Class queuelet_findClass(String name)
							   throws ClassNotFoundException{
	if(__queueletHooker==null){
		return super.findClass(name); 
	}
	Class c;
/*	c=__queueletHooker.getClass(name);
	if(c!=null){
	    return c;
	}*/
	byte[] byteCode=__queueletHooker.getByteCode(name);
	if(byteCode!=(byte[])null){
		try{
			c=defineClass(name,byteCode,0,byteCode.length);
			__queueletHooker.registerClass(name,c);
			return c; 
		}catch(Throwable e){
		}
	}
	return super.findClass(name);
}
