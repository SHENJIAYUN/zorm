package com.zorm.type;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.zorm.exception.ZormException;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.util.EqualsHelper;

public abstract class AbstractType implements Type{

	public boolean isEntityType() {
		return false;
	}
	
	public boolean isAssociationType() {
		return false;
	}

	public boolean isCollectionType() {
		return false;
	}

	public boolean isComponentType() {
		return false;
	}
	
	public boolean isAnyType() {
		return false;
	}
	
	public boolean isDirty(Object old, Object current, SessionImplementor session) throws ZormException {
		return !isSame( old, current );
	}
	
	public boolean isModified(Object old, Object current, boolean[] checkable, SessionImplementor session)
			throws ZormException {
				return isDirty(old, current, session);
			}
	
	public boolean isSame(Object x, Object y) throws ZormException {
		return isEqual(x, y );
	}

	public boolean isEqual(Object x, Object y) {
		return EqualsHelper.equals(x, y);
	}
	
	public int getHashCode(Object x) {
		return x.hashCode();
	}

	public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) {
		return isEqual(x, y );
	}
	
	public int getHashCode(Object x, SessionFactoryImplementor factory) {
		return getHashCode(x );
	}
	
	@Override
	public Object hydrate(
			ResultSet rs,
			String[] names,
			SessionImplementor session,
			Object owner)
		throws ZormException, SQLException {
			return nullSafeGet(rs, names, session, owner);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public int compare(Object x, Object y) {
		return ( (Comparable) x ).compareTo(y);
	}
}
