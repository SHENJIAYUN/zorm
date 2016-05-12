package com.zorm.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.zorm.engine.Mapping;
import com.zorm.exception.MappingException;
import com.zorm.exception.ZormException;
import com.zorm.meta.Size;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;

public interface Type extends Serializable{

	public boolean isAssociationType();
	
	public boolean isCollectionType();
	
	public boolean isEntityType();
	
	public boolean isAnyType();
	
	public boolean isComponentType();
	
	/**
	 * How many columns are used to persist this type.  Always the same as {@code sqlTypes(mapping).length}
	 *
	 * @param mapping The mapping object :/
	 *
	 * @return The number of columns
	 *
	 * @throws MappingException Generally indicates an issue accessing the passed mapping object.
	 */
	public int getColumnSpan(Mapping mapping) throws MappingException;
	
	public int[] sqlTypes(Mapping mapping) throws MappingException;
	
	public Size[] dictatedSizes(Mapping mapping) throws MappingException;
	
	public Size[] defaultSizes(Mapping mapping) throws MappingException;
	
	public Class getReturnedClass();
	
	public boolean isSame(Object x, Object y) throws ZormException;
	
	public boolean isEqual(Object x, Object y) throws ZormException;
	
	public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) throws ZormException;
	
	public int getHashCode(Object x) throws ZormException;
	
	public int getHashCode(Object x, SessionFactoryImplementor factory) throws ZormException;
	
	public int compare(Object x, Object y);
	
	public boolean isDirty(Object old, Object current, SessionImplementor session) throws ZormException;
	
	public boolean isDirty(Object oldState, Object currentState, boolean[] checkable, SessionImplementor session)
			throws ZormException;
	
	public boolean isModified(Object dbState, Object currentState, boolean[] checkable, SessionImplementor session)
			throws ZormException;
	
	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
			throws ZormException, SQLException;
	
	public Object nullSafeGet(ResultSet rs, String name, SessionImplementor session, Object owner)
			throws ZormException, SQLException;
	
	public void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SessionImplementor session)
			throws ZormException, SQLException;
	
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
			throws ZormException, SQLException;
	
	public String toLoggableString(Object value, SessionFactoryImplementor factory)
			throws ZormException;
	
	public String getName();
	
	public Object deepCopy(Object value, SessionFactoryImplementor factory)
			throws ZormException;
	
	public boolean isMutable();
	
	public Serializable disassemble(Object value, SessionImplementor session, Object owner) throws ZormException;
	
	public Object assemble(Serializable cached, SessionImplementor session, Object owner)
			throws ZormException;
	
	public void beforeAssemble(Serializable cached, SessionImplementor session);
	
	public Object hydrate(ResultSet rs, String[] names, SessionImplementor session, Object owner)
			throws ZormException, SQLException;
	
	public Object resolve(Object value, SessionImplementor session, Object owner)
			throws ZormException;
	
	public Object semiResolve(Object value, SessionImplementor session, Object owner)
			throws ZormException;
	
	public Type getSemiResolvedType(SessionFactoryImplementor factory);
	
	public Object replace(
			Object original, 
			Object target, 
			SessionImplementor session, 
			Object owner, 
			Map copyCache) throws ZormException;
	
	public Object replace(
			Object original, 
			Object target, 
			SessionImplementor session, 
			Object owner, 
			Map copyCache, 
			ForeignKeyDirection foreignKeyDirection) throws ZormException;
	
	public boolean[] toColumnNullness(Object value, Mapping mapping);
}
