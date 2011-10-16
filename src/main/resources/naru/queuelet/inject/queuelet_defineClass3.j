private Class queuelet_defineClass3(String name,
									  byte[] b,
									  int off,
									  int len,
									  java.security.CodeSource codeSource){
	/* protectionDomain ignore fix me */
	try{
		return queuelet_defineClass(name,b,off,len);
	}catch(Exception e){
	    e.printStackTrace();
	    throw new RuntimeException("queuelet_defineClass3 error");
	}
	return null;
}
