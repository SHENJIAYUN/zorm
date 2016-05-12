package com.zorm;

import java.io.Serializable;
import java.util.Iterator;

import com.zorm.entity.EntityMode;
import com.zorm.exception.CallbackException;
import com.zorm.transaction.Transaction;
import com.zorm.type.Type;

public class EmptyInterceptor implements Interceptor,Serializable{
	public static final Interceptor INSTANCE = new EmptyInterceptor();
	
	protected EmptyInterceptor() {}

	@Override
	public boolean onLoad(Object entity, Serializable id, Object[] state,
			String[] propertyNames, Type[] types) throws CallbackException {
		return false;
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id,
			Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) throws CallbackException {
		return false;
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state,
			String[] propertyNames, Type[] types) throws CallbackException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onDelete(Object entity, Serializable id, Object[] state,
			String[] propertyNames, Type[] types) throws CallbackException {
		
	}

	@Override
	public void onCollectionRecreate(Object collection, Serializable key)
			throws CallbackException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCollectionRemove(Object collection, Serializable key)
			throws CallbackException {
	}

	@Override
	public void onCollectionUpdate(Object collection, Serializable key)
			throws CallbackException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void preFlush(Iterator entities) throws CallbackException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postFlush(Iterator entities) throws CallbackException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Boolean isTransient(Object entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] findDirty(Object entity, Serializable id,
			Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {
		return null;
	}

	@Override
	public Object instantiate(String entityName, EntityMode entityMode,
			Serializable id) throws CallbackException {
		return null;
	}

	@Override
	public String getEntityName(Object object) throws CallbackException {
		return null;
	}

	@Override
	public Object getEntity(String entityName, Serializable id)
			throws CallbackException {
		return null;
	}

	@Override
	public void afterTransactionBegin(Transaction tx) {
	}

	@Override
	public void beforeTransactionCompletion(Transaction tx) {
		
	}

	@Override
	public void afterTransactionCompletion(Transaction tx) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String onPrepareStatement(String sql) {
		return sql;
	}

}
