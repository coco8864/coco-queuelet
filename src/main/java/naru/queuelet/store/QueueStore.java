/*
 * Created on 2004/11/09
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package naru.queuelet.store;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import naru.queuelet.core.QueueletProperties;
import naru.queuelet.util.ObjectIoUtil;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;

/**
 * @author naru
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class QueueStore {
	static private Logger logger=logger=Logger.getLogger(QueueStore.class.getName());
	private DataSource ds;
	private QueueletProperties queueletProperties;
	private ClassLoader loader;
	private Map sqlMap=new HashMap();
	
	public QueueStore(QueueletProperties queueletProperties){
		this.queueletProperties=queueletProperties;
	}
	
	public boolean init(String driver,String url,String user,String psssword,ClassLoader loader){
    	BasicDataSource bds = new BasicDataSource();
    	bds.setDriverClassName(driver);
    	bds.setUsername(user);
    	bds.setPassword(psssword);
    	bds.setUrl(url);
    	this.ds=(DataSource)bds;
    	/*
		try {
			Connection conn=ds.getConnection();
		} catch (SQLException e) {
			logger.error("QueueStore init error.",e);
	    	return false;
		}
		*/
    	this.loader=loader;
    	return true;
	}

	public void term(){
        BasicDataSource bds = (BasicDataSource) ds;
        try {
			bds.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		ds=null;
	}

	/* terminalに依存しないSQLがほしい場合 */
	private QueueStoreSql getSql(){
		QueueStoreSql queulStoreSql=null;
		Iterator itr=sqlMap.values().iterator();
		if(itr.hasNext()){
			queulStoreSql=(QueueStoreSql)itr.next();
		}else{
			queulStoreSql=new QueueStoreSql("queuelet.store.dummyTerminal",queueletProperties);
			sqlMap.put("queuelet.store.dummyTerminal",queulStoreSql);
		}
		return queulStoreSql;
	}
	
	private QueueStoreSql getSql(String terminal){
		QueueStoreSql queulStoreSql=null;
		queulStoreSql=(QueueStoreSql)sqlMap.get(terminal);
		if( queulStoreSql==null){
			queulStoreSql=new QueueStoreSql(terminal,queueletProperties);
			sqlMap.put(terminal,queulStoreSql);
			/* terminalが存在しない場合は、作っておく */
			if( !check(terminal) ){
				create(terminal);
			}
		}
		return queulStoreSql;
	}
	
	private void finishSql(Connection conn,Statement st,ResultSet rs){
		if(rs!=null){
			try {
				rs.close();
			} catch (SQLException ignore) {
				ignore.printStackTrace();
			}
		}
		if(st!=null){
			try {
				st.close();
			} catch (SQLException ignore) {
				ignore.printStackTrace();
			}
		}
		if(conn!=null){
			try {
				conn.close();
			} catch (SQLException ignore) {
				ignore.printStackTrace();
			}
		}
	}
	
	public int executeQueryInt(String sql) throws SQLException{
		Connection conn=null;
		Statement st=null;
		ResultSet rs=null;
		int result=-1;
		try {
			conn=ds.getConnection();
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			if( rs.next()){
				result=rs.getInt(1);
			}
			return result;
		}finally{
			finishSql(conn,st,rs);
		}
	}

	public int executeUpdate(String sql) throws SQLException{
		Connection conn=null;
		Statement st=null;
		int result;
		try {
			conn=ds.getConnection();
			st = conn.createStatement();
			result = st.executeUpdate(sql);
		}finally{
			finishSql(conn,st,null);
		}
		return result;
	}

	public boolean execute(String sql) throws SQLException{
		Connection conn=null;
		Statement st=null;
		boolean result;
		try {
			conn=ds.getConnection();
			st = conn.createStatement();
			result = st.execute(sql);
		}finally{
			finishSql(conn,st,null);
		}
		return result;
	}
	
	
	public PreparedStatement PrepareExecute(String sql) throws SQLException{
		Connection conn=null;
		PreparedStatement st=null;
		int result;
		try {
			conn=ds.getConnection();
			st = conn.prepareStatement(sql);
		}finally{
			finishSql(conn,null,null);
		}
		return st;
	}

	public boolean shutdown(){
		QueueStoreSql sql=getSql();
		try {
//			execute(sql.getShutdownSQL());
			executeUpdate(sql.getShutdownSQL());
			return true;
		} catch (SQLException e) {
			logger.warn("dropTerminal error.",e);
		}
		return false;
	}
	
	public boolean drop(String terminal){
		QueueStoreSql sql=getSql(terminal);
		try {
			executeUpdate(sql.getDropSQL());
			return true;
		} catch (SQLException e) {
			logger.warn("dropTerminal error.",e);
		}
		return false;
	}
	
	public boolean create(String terminal){
		QueueStoreSql sql=getSql(terminal);
		try {
			executeUpdate(sql.getCrateSQL());
			return true;
		} catch (SQLException e) {
			logger.warn("dropTerminal error.",e);
		}
		return false;
	}
	
	public boolean check(String terminal){	
		QueueStoreSql sql=getSql(terminal);
		try {
			int length=executeQueryInt(sql.getCheckSQL());
			return (length==1);
		} catch (SQLException e) {
			logger.warn("checkTerminal error.",e);
		} catch (Throwable e) {
			logger.error("checkTerminal error.",e);
		}finally{
		}
		return false;
	}

	public int getLength(String terminal){
		QueueStoreSql sql=getSql(terminal);
		try {
			int length=executeQueryInt(sql.getCountSQL());
			return length;
		} catch (SQLException e) {
			logger.warn("checkTerminal error.",e);
		}finally{
		}
		return -1;
	}
	
	public void enque(Object req,String terminal){
		if( !(req instanceof Serializable) ){
			throw new IllegalArgumentException("not Serializable object:"+ req);
		}
		QueueStoreSql sql=getSql(terminal);
		Connection conn=null;
		PreparedStatement prep=null;
		try {
			conn=ds.getConnection();
			prep =
			    conn.prepareCall(sql.getInsertSQL());
			// Fill the second parameter: Name
			byte[] b=ObjectIoUtil.objectTobytes(req);
			prep.setObject(1, b);
			// Its a file: add it to the table
			prep.execute();
		} catch (SQLException e){
			logger.warn("failt to enque DB.",e);
		} catch (IOException e) {
			logger.warn("failt to enque IO(serialize).",e);
		}finally{
			finishSql(conn,prep,null);
		}
	}
	
	public List deque(int count,String terminal){
		List queueObjects=new ArrayList();
		if( count==0 ){
			return queueObjects;
		}
		QueueStoreSql sql=getSql(terminal);
		Connection conn=null;
		Statement st=null;
		ResultSet rs=null;
		PreparedStatement prep=null;
		try {
			conn=ds.getConnection();
			st = conn.createStatement();
			st.setMaxRows(count);
			rs = st.executeQuery(sql.getListSQL());
			prep = conn.prepareCall(sql.getDeleteSQL());
			// Moves to the next record until no more records
			while (rs.next()) {
				int id=rs.getInt(1);
				byte[]b=(byte[])rs.getObject(2);
				Object queueObject=ObjectIoUtil.bytesToObject(b,loader);

				prep.setInt(1,id);
				int rowCount=prep.executeUpdate();
				if( rowCount==1){
					queueObjects.add(queueObject);
				}else if(rowCount==0){
					/* SELECT時には、存在したが、削除時には無かった.他terminalから削除された、正常 */
					logger.info("DELETE FROM other terminal." + terminal);
				}else{
					logger.error("DELETE FROM multi row." + rowCount + "terminal:" + terminal);
				}
				prep.clearParameters();
			}
			rs.close();
			rs=null;
		} catch (SQLException e) {
			logger.warn("fail to deque DB.",e);
		} catch (SecurityException e) {
			logger.warn("fail to deque Security.",e);
		} catch (IOException e) {
			logger.warn("fail to deque IO.",e);
		} catch (ClassNotFoundException e) {
			logger.warn("fail to deque ClassLoader.",e);
		}finally{
			finishSql(conn,st,rs);
		}
		return queueObjects;
	}
}
