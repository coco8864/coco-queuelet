package naru.web;

import java.util.HashMap;
import java.util.Map;

import naru.queuelet.QueueletContext;

public class AsyncOperation {
	public static String OPERATION_DELETE_TRACE_FILES="deleteTraceFiles";
	public static String OPERATION_DELETE_ACCESSLOGS="deleteAccessLogs";
	public static String OPERATION_IMPORT_ACCESSLOG="importAccesslog";
	public static String OPERATION_EXPORT_ACCESSLOG="exportAccesslog";
	
	private String operation;
	private Map parameters;
	
	public AsyncOperation(String operation){
		this(operation,new HashMap());
	}
	public AsyncOperation(String operation,Map parameters){
		this.operation=operation;
		this.parameters=parameters;
	}
	
	public void doAsync(QueueletContext context){
		context.enque(this, Config.QUEUE_ASYNC_SERVICE);
	}
	
	public String getOperation() {
		return operation;
	}
	public Map getParametersMap() {
		return parameters;
	}
	public Object getParameterObject(String name){
		return parameters.get(name);
	}
	public void putParameter(String name,Object value){
		parameters.put(name, value);
	}
	public String getParameter(String name){
		return (String)getParameterObject(name);
	}
}
