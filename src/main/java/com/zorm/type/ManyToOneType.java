package com.zorm.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

import com.zorm.engine.Mapping;
import com.zorm.exception.MappingException;
import com.zorm.exception.ZormException;
import com.zorm.meta.Size;
import com.zorm.persister.entity.Joinable;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;

public class ManyToOneType extends EntityType {

	private final boolean ignoreNotFound;
	private boolean isLogicalOneToOne;
	
	public ManyToOneType(TypeFactory.TypeScope scope, String referencedEntityName) {
		this( scope, referencedEntityName, false );
	}
	
	public ManyToOneType(TypeFactory.TypeScope scope, String referencedEntityName, boolean lazy) {
		this( scope, referencedEntityName, null, lazy, true, false, false );
	}
	
	public ManyToOneType(
			TypeFactory.TypeScope scope,
			String referencedEntityName,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean ignoreNotFound,
			boolean isLogicalOneToOne) {
		super( scope, referencedEntityName, uniqueKeyPropertyName, !lazy, unwrapProxy );
		this.ignoreNotFound = ignoreNotFound;
		this.isLogicalOneToOne = isLogicalOneToOne;
	}
	
	protected boolean isNullable() {
		return ignoreNotFound;
	}

	public boolean isAlwaysDirtyChecked() {
		return true;
	}

	public boolean isOneToOne() {
		return false;
	}

	public boolean isLogicalOneToOne() {
		return isLogicalOneToOne;
	}
	
	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		return ForeignKeyDirection.FOREIGN_KEY_FROM_PARENT;
	}

	@Override
	public boolean useLHSPrimaryKey() {
		return false;
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return getIdentifierOrUniqueKeyType( mapping ).getColumnSpan( mapping );
	}

	@Override
	public int[] sqlTypes(Mapping mapping) throws MappingException {
		return getIdentifierOrUniqueKeyType( mapping ).sqlTypes( mapping );
	}

	@Override
	public Size[] dictatedSizes(Mapping mapping) throws MappingException {
		return getIdentifierOrUniqueKeyType( mapping ).dictatedSizes( mapping );
	}

	@Override
	public Size[] defaultSizes(Mapping mapping) throws MappingException {
		return getIdentifierOrUniqueKeyType( mapping ).defaultSizes( mapping );
	}
	
	public boolean isDirty(
			Object old,
			Object current,
			SessionImplementor session) throws ZormException {
		if ( isSame( old, current ) ) {
			return false;
		}
		Object oldid = getIdentifier( old, session );
		Object newid = getIdentifier( current, session );
		return getIdentifierType( session ).isDirty( oldid, newid, session );
	}

	public boolean isDirty(
			Object old,
			Object current,
			boolean[] checkable,
			SessionImplementor session) throws ZormException {
		if ( isAlwaysDirtyChecked() ) {
			return isDirty( old, current, session );
		}
		else {
			if ( isSame( old, current ) ) {
				return false;
			}
			Object oldid = getIdentifier( old, session );
			Object newid = getIdentifier( current, session );
			return getIdentifierType( session ).isDirty( oldid, newid, checkable, session );
		}
		
	}

	@Override
	public boolean isModified(
			Object old,
			Object current,
			boolean[] checkable,
			SessionImplementor session)
			throws ZormException {
		if ( current == null ) {
			return old!=null;
		}
		if ( old == null ) {
			return true;
		}
		return getIdentifierOrUniqueKeyType( session.getFactory() )
				.isDirty( old, getIdentifier( current, session ), session );
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String name,
			SessionImplementor session, Object owner) throws ZormException,
			SQLException {
		return null;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index,
			boolean[] settable, SessionImplementor session)
			throws ZormException, SQLException {
		getIdentifierOrUniqueKeyType( session.getFactory() )
		   .nullSafeSet( st, getIdentifier( value, session ), index, settable, session );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index,
			SessionImplementor session) throws ZormException, SQLException {

	}

	@Override
	public String toLoggableString(Object value,
			SessionFactoryImplementor factory) throws ZormException {
		return null;
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory)
			throws ZormException {
		return value;
	}


	@Override
	public Serializable disassemble(Object value, SessionImplementor session,
			Object owner) throws ZormException {
		return null;
	}

	@Override
	public Object assemble(Serializable cached, SessionImplementor session,
			Object owner) throws ZormException {
		return null;
	}

	@Override
	public void beforeAssemble(Serializable cached, SessionImplementor session) {

	}

	@Override
	public Object hydrate(
			ResultSet rs, 
			String[] names,
			SessionImplementor session, 
			Object owner) throws ZormException,SQLException {
		Serializable id = (Serializable) getIdentifierOrUniqueKeyType( session.getFactory() )
				.nullSafeGet( rs, names, session, null );
		return id;
	}

	@Override
	public Object semiResolve(Object value, SessionImplementor session,
			Object owner) throws ZormException {
		return null;
	}

	@Override
	public Type getSemiResolvedType(SessionFactoryImplementor factory) {
		return null;
	}

	@Override
	public Object replace(Object original, Object target,
			SessionImplementor session, Object owner, Map copyCache)
			throws ZormException {
		return null;
	}

	@Override
	public Object replace(Object original, Object target,
			SessionImplementor session, Object owner, Map copyCache,
			ForeignKeyDirection foreignKeyDirection) throws ZormException {
		return null;
	}

	@Override
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		boolean[] result = new boolean[ getColumnSpan( mapping ) ];
		if ( value != null ) {
			Arrays.fill( result, true );
		}
		return result;
	}

}
