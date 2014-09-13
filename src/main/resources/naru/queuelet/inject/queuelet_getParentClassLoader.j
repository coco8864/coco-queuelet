private static ClassLoader queuelet_getParentClassLoader(ClassLoader cl){
	ClassLoader system=ClassLoader.getSystemClassLoader();
	if(cl==null || cl.equals(system)){
		return __queueletRootClassLoader;
	}
	return cl;
}
