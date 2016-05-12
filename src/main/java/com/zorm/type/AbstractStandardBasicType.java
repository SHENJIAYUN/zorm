package com.zorm.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.zorm.Zorm;
import com.zorm.config.Environment;
import com.zorm.engine.Mapping;
import com.zorm.exception.MappingException;
import com.zorm.exception.ZormException;
import com.zorm.meta.Size;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.type.descriptor.LobCreator;
import com.zorm.type.descriptor.WrapperOptions;
import com.zorm.type.descriptor.java.JavaTypeDescriptor;
import com.zorm.type.descriptor.java.MutabilityPlan;
import com.zorm.type.descriptor.sql.SqlTypeDescriptor;
import com.zorm.util.ArrayHelper;
import com.zorm.util.StringHelper;

public abstract class AbstractStandardBasicType<T> implements BasicType,StringRepresentableType<T>{

	private static final long serialVersionUID = -4271648222905335737L;
	private static final Size DEFAULT_SIZE = new Size( 19, 2, 255, Size.LobMultiplier.NONE ); // to match legacy behavior
	private final Size dictatedSize = new Size();
	
	private SqlTypeDescriptor sqlTypeDescriptor;
	private JavaTypeDescriptor<T> javaTypeDescriptor;
	
	public AbstractStandardBasicType(SqlTypeDescriptor sqlTypeDescriptor, JavaTypeDescriptor<T> javaTypeDescriptor) {
		this.sqlTypeDescriptor = sqlTypeDescriptor;
		this.javaTypeDescriptor = javaTypeDescriptor;
	}
	
	public T fromString(String string) {
		return javaTypeDescriptor.fromString( string );
	}

	public String toString(T value) {
		return javaTypeDescriptor.toString( value );
	}

	public T fromStringValue(String xml) throws ZormException {
		return fromString( xml );
	}

	public String toXMLString(T value, SessionFactoryImplementor factory) throws ZormException {
		return toString( value );
	}

	public T fromXMLString(String xml, Mapping factory) throws ZormException {
		return StringHelper.isEmpty( xml ) ? null : fromStringValue( xml );
	}

	protected MutabilityPlan<T> getMutabilityPlan() {
		return javaTypeDescriptor.getMutabilityPlan();
	}

	protected T getReplacement(T original, T target, SessionImplementor session) {
		if ( !isMutable() ) {
			return original;
		}
		else if ( isEqual( original, target ) ) {
			return original;
		}
		else {
			return deepCopy( original );
		}
	}

	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return value == null ? ArrayHelper.FALSE : ArrayHelper.TRUE;
	}

	public String[] getRegistrationKeys() {
		return registerUnderJavaType()
				? new String[] { getName(), javaTypeDescriptor.getJavaTypeClass().getName() }
				: new String[] { getName() };
	}

	protected boolean registerUnderJavaType() {
		return false;
	}

	protected static Size getDefaultSize() {
		return DEFAULT_SIZE;
	}

	protected Size getDictatedSize() {
		return dictatedSize;
	}
	
	// final implementations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public final JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}
	
	public final void setJavaTypeDescriptor( JavaTypeDescriptor<T> javaTypeDescriptor ) {
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	public final SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptor;
	}
	
	public final void setSqlTypeDescriptor( SqlTypeDescriptor sqlTypeDescriptor ) {
		this.sqlTypeDescriptor = sqlTypeDescriptor;
	}

	public final Class getReturnedClass() {
		return javaTypeDescriptor.getJavaTypeClass();
	}

	public final int getColumnSpan(Mapping mapping) throws MappingException {
		return sqlTypes( mapping ).length;
	}

	public final int[] sqlTypes(Mapping mapping) throws MappingException {
		return new int[] { sqlTypeDescriptor.getSqlType() };
	}

	@Override
	public Size[] dictatedSizes(Mapping mapping) throws MappingException {
		return new Size[] { getDictatedSize() };
	}

	@Override
	public Size[] defaultSizes(Mapping mapping) throws MappingException {
		return new Size[] { getDefaultSize() };
	}

	public final boolean isAssociationType() {
		return false;
	}

	public final boolean isCollectionType() {
		return false;
	}

	public final boolean isComponentType() {
		return false;
	}

	public final boolean isEntityType() {
		return false;
	}

	public final boolean isAnyType() {
		return false;
	}

	public final boolean isXMLElement() {
		return false;
	}

	@SuppressWarnings({ "unchecked" })
	public final boolean isSame(Object x, Object y) {
		return isEqual( x, y );
	}

	@SuppressWarnings({ "unchecked" })
	public final boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) {
		return isEqual( x, y );
	}

	@SuppressWarnings({ "unchecked" })
	public final boolean isEqual(Object one, Object another) {
		return javaTypeDescriptor.areEqual( (T) one, (T) another );
	}

	@SuppressWarnings({ "unchecked" })
	public final int getHashCode(Object x) {
		return javaTypeDescriptor.extractHashCode( (T) x );
	}

	public final int getHashCode(Object x, SessionFactoryImplementor factory) {
		return getHashCode( x );
	}

	@SuppressWarnings({ "unchecked" })
	public final int compare(Object x, Object y) {
		return javaTypeDescriptor.getComparator().compare( (T) x, (T) y );
	}

	public final boolean isDirty(Object old, Object current, SessionImplementor session) {
		return isDirty( old, current );
	}

	public final boolean isDirty(Object old, Object current, boolean[] checkable, SessionImplementor session) {
		return checkable[0] && isDirty( old, current );
	}

	protected final boolean isDirty(Object old, Object current) {
		return !isSame( old, current );
	}

	public final boolean isModified(
			Object oldHydratedState,
			Object currentState,
			boolean[] checkable,
			SessionImplementor session) {
		return isDirty( oldHydratedState, currentState );
	}

	public final Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SessionImplementor session,
			Object owner) throws SQLException {
		return nullSafeGet( rs, names[0], session );
	}

	public final Object nullSafeGet(ResultSet rs, String name, SessionImplementor session, Object owner)
			throws SQLException {
		return nullSafeGet( rs, name, session );
	}

	public final T nullSafeGet(ResultSet rs, String name, final SessionImplementor session) throws SQLException {
		final WrapperOptions options = getOptions(session);
		return nullSafeGet( rs, name, options );
	}

	protected final T nullSafeGet(ResultSet rs, String name, WrapperOptions options) throws SQLException {
		return remapSqlTypeDescriptor( options ).getExtractor( javaTypeDescriptor ).extract( rs, name, options );
	}

	public Object get(ResultSet rs, String name, SessionImplementor session) throws ZormException, SQLException {
		return nullSafeGet( rs, name, session );
	}

	@SuppressWarnings({ "unchecked" })
	public final void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			final SessionImplementor session) throws SQLException {
		final WrapperOptions options = getOptions(session);
		nullSafeSet( st, value, index, options );
	}

	@SuppressWarnings({ "unchecked" })
	protected final void nullSafeSet(PreparedStatement st, Object value, int index, WrapperOptions options) throws SQLException {
		remapSqlTypeDescriptor( options ).getBinder( javaTypeDescriptor ).bind( st, ( T ) value, index, options );
	}

	protected SqlTypeDescriptor remapSqlTypeDescriptor(WrapperOptions options) {
		return options.remapSqlTypeDescriptor( sqlTypeDescriptor );
	}

	public void set(PreparedStatement st, T value, int index, SessionImplementor session) throws ZormException, SQLException {
		nullSafeSet( st, value, index, session );
	}

	@SuppressWarnings({ "unchecked" })
	public final String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return javaTypeDescriptor.extractLoggableRepresentation( (T) value );
	}

	public final boolean isMutable() {
		return getMutabilityPlan().isMutable();
	}

	@SuppressWarnings({ "unchecked" })
	public final Object deepCopy(Object value, SessionFactoryImplementor factory) {
		return deepCopy( (T) value );
	}

	protected final T deepCopy(T value) {
		return getMutabilityPlan().deepCopy( value );
	}

	@SuppressWarnings({ "unchecked" })
	public final Serializable disassemble(Object value, SessionImplementor session, Object owner) throws ZormException {
		return getMutabilityPlan().disassemble( (T) value );
	}

	public final Object assemble(Serializable cached, SessionImplementor session, Object owner) throws ZormException {
		return getMutabilityPlan().assemble( cached );
	}

	public final void beforeAssemble(Serializable cached, SessionImplementor session) {
	}

	public final Object hydrate(ResultSet rs, String[] names, SessionImplementor session, Object owner)
			throws ZormException, SQLException {
		return nullSafeGet(rs, names, session, owner);
	}

	public final Object resolve(Object value, SessionImplementor session, Object owner) throws ZormException {
		return value;
	}

	public final Object semiResolve(Object value, SessionImplementor session, Object owner) throws ZormException {
		return value;
	}

	public final Type getSemiResolvedType(SessionFactoryImplementor factory) {
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	public final Object replace(Object original, Object target, SessionImplementor session, Object owner, Map copyCache) {
		return getReplacement( (T) original, (T) target, session );
	}

	@SuppressWarnings({ "unchecked" })
	public Object replace(
			Object original,
			Object target,
			SessionImplementor session,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection) {
		return ForeignKeyDirection.FOREIGN_KEY_FROM_PARENT == foreignKeyDirection
				? getReplacement( (T) original, (T) target, session )
				: target;
	}

	private WrapperOptions getOptions(final SessionImplementor session) {
		return new WrapperOptions() {
			public boolean useStreamForLobBinding() {
				return  session.getFactory().getDialect().useInputStreamToInsertBlob();
			}

			public LobCreator getLobCreator() {
				return Zorm.getLobCreator( session );
			}

			public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
				final SqlTypeDescriptor remapped = sqlTypeDescriptor.canBeRemapped()
						? session.getFactory().getDialect().remapSqlTypeDescriptor( sqlTypeDescriptor )
						: sqlTypeDescriptor;
				return remapped == null ? sqlTypeDescriptor : remapped;
			}
		};
	}

}
