package com.zorm.dialect;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.LockMode;
import com.zorm.LockOptions;
import com.zorm.config.Environment;
import com.zorm.dialect.function.SQLFunction;
import com.zorm.dialect.function.StandardAnsiSqlAggregationFunctions;
import com.zorm.engine.RowSelection;
import com.zorm.exception.ConversionContext;
import com.zorm.exception.MappingException;
import com.zorm.exception.SQLExceptionConversionDelegate;
import com.zorm.exception.SQLExceptionConverter;
import com.zorm.exception.ViolatedConstraintNameExtracter;
import com.zorm.exception.ZormException;
import com.zorm.mapping.Column;
import com.zorm.sql.ANSICaseFragment;
import com.zorm.sql.ANSIJoinFragment;
import com.zorm.sql.CaseFragment;
import com.zorm.sql.JoinFragment;
import com.zorm.type.descriptor.sql.SqlTypeDescriptor;
import com.zorm.util.ReflectHelper;
import com.zorm.util.StringHelper;

public abstract class Dialect implements ConversionContext{
  private static final Log log = LogFactory.getLog(Dialect.class);
  public static final String DEFAULT_BATCH_SIZE = "15";
  public static final String NO_BATCH = "0";
  
    /**
	 * Characters used for quoting SQL identifiers
	 */
	public static final String QUOTE = "`\"[";
	public static final String CLOSED_QUOTE = "`\"]";
	
	private final TypeNames typeNames = new TypeNames();
	private final TypeNames zormTypeNames = new TypeNames();
	private final Properties properties = new Properties();
	private final Map<String, SQLFunction> sqlFunctions = new HashMap<String, SQLFunction>();
	private final Set<String> sqlKeywords = new HashSet<String>();
	
	protected Dialect() {
        log.info("Using dialect:"+this);
        StandardAnsiSqlAggregationFunctions.primeFunctionMap(sqlFunctions);
        
     // standard sql92 functions (can be overridden by subclasses)
        
	}
	
	/**
	 * Get an instance of the dialect specified by the current <tt>System</tt> properties.
	 *
	 * @return The specified Dialect
	 * @throws HibernateException If no dialect was specified, or if it could not be instantiated.
	 */
	public static Dialect getDialect() throws ZormException {
		String dialectName = Environment.getProperties().getProperty( Environment.DIALECT );
		return instantiateDialect( dialectName );
	}

	private static Dialect instantiateDialect(String dialectName) throws ZormException{
		if ( dialectName == null ) {
			throw new ZormException( "The dialect was not set. Set the property hibernate.dialect." );
		}
		try {
			return ( Dialect ) ReflectHelper.classForName( dialectName ).newInstance();
		}
		catch ( ClassNotFoundException cnfe ) {
			throw new ZormException( "Dialect class not found: " + dialectName );
		}
		catch ( Exception e ) {
			throw new ZormException( "Could not instantiate given dialect class: " + dialectName, e );
		}
	}
	
	private static final ViolatedConstraintNameExtracter EXTRACTER = new ViolatedConstraintNameExtracter() {
		public String extractConstraintName(SQLException sqle) {
			return null;
		}
	};
	
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}
	
	/**
	 * Get an instance of the dialect specified by the given properties or by
	 * the current <tt>System</tt> properties.
	 *
	 * @param props The properties to use for finding the dialect class to use.
	 * @return The specified Dialect
	 * @throws HibernateException If no dialect was specified, or if it could not be instantiated.
	 */
	public static Dialect getDialect(Properties props) throws ZormException {
		String dialectName = props.getProperty( Environment.DIALECT );
		if ( dialectName == null ) {
			return getDialect();
		}
		return instantiateDialect( dialectName );
	}
	
	/**
	 * Retrieve a set of default Zorm properties for this database.
	 *
	 * @return a set of Zorm properties
	 */
	public final Properties getDefaultProperties() {
		return properties;
	}
	
	@Override
    public String toString() {
		return getClass().getName();
	}
	
	// database type mapping support
	/**
	 * Get the name of the database type associated with the given
	 * {@link java.sql.Types} typecode.
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @return the database type name
	 * @throws HibernateException If no mapping was specified for that type.
	 */
	public String getTypeName(int code) throws ZormException {
		String result = typeNames.get( code );
		if ( result == null ) {
			throw new ZormException( "No default type mapping for (java.sql.Types) " + code );
		}
		return result;
	}
	
	/**
	 * Get the name of the database type associated with the given
	 * {@link java.sql.Types} typecode with the given storage specification
	 * parameters.
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @param length The datatype length
	 * @param precision The datatype precision
	 * @param scale The datatype scale
	 * @return the database type name
	 * @throws HibernateException If no mapping was specified for that type.
	 */
	public String getTypeName(int code, long length, int precision, int scale) throws ZormException {
		String result = typeNames.get( code, length, precision, scale );
		if ( result == null ) {
			throw new ZormException(String.format( "No type mapping for java.sql.Types code: %s, length: %s", code, length ));
		}
		return result;
	}
	
	/**
	 * Get the name of the database type appropriate for casting operations
	 * (via the CAST() SQL function) for the given {@link java.sql.Types} typecode.
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @return The database type name
	 */
	public String getCastTypeName(int code) {
		return getTypeName( code, Column.DEFAULT_LENGTH, Column.DEFAULT_PRECISION, Column.DEFAULT_SCALE );
	}
	
	public String cast(String value, int jdbcTypeCode, int length, int precision, int scale) {
		if ( jdbcTypeCode == Types.CHAR ) {
			return "cast(" + value + " as char(" + length + "))";
		}
		else {
			return "cast(" + value + "as " + getTypeName( jdbcTypeCode, length, precision, scale ) + ")";
		}
	}
	
	public String cast(String value, int jdbcTypeCode, int length) {
		return cast( value, jdbcTypeCode, length, Column.DEFAULT_PRECISION, Column.DEFAULT_SCALE );
	}
	
	public String cast(String value, int jdbcTypeCode, int precision, int scale) {
		return cast( value, jdbcTypeCode, Column.DEFAULT_LENGTH, precision, scale );
	}
	
	/**
	 * Subclasses register a type name for the given type code and maximum
	 * column length. <tt>$l</tt> in the type name with be replaced by the
	 * column length (if appropriate).
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @param capacity The maximum length of database type
	 * @param name The database type name
	 */
	protected void registerColumnType(int code, long capacity, String name) {
		typeNames.put( code, capacity, name );
	}
	
	/**
	 * Subclasses register a type name for the given type code. <tt>$l</tt> in
	 * the type name with be replaced by the column length (if appropriate).
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @param name The database type name
	 */
	protected void registerColumnType(int code, String name) {
		typeNames.put( code, name );
	}
	
	public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		if ( sqlTypeDescriptor == null ) {
			throw new IllegalArgumentException( "sqlTypeDescriptor is null" );
		}
		if ( ! sqlTypeDescriptor.canBeRemapped() ) {
			return sqlTypeDescriptor;
		}

		final SqlTypeDescriptor overridden = getSqlTypeDescriptorOverride( sqlTypeDescriptor.getSqlType() );
		return overridden == null ? sqlTypeDescriptor : overridden;
	}
	
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		SqlTypeDescriptor descriptor;
		switch ( sqlCode ) {
			case Types.CLOB: {
				//descriptor = useInputStreamToInsertBlob() ? ClobTypeDescriptor.STREAM_BINDING : null;
				//break;
			}
			default: {
				descriptor = null;
				break;
			}
		}
		return descriptor;
	}

	public boolean useInputStreamToInsertBlob() {
		return true;
	}

	public final Map<String, SQLFunction> getFunctions() {
		return sqlFunctions;
	}

	public char openQuote() {
		return '"';
	}
	
	public char closeQuote() {
		return '"';
	}

	public Class getNativeIdentifierGeneratorClass() {
		return null;
	}

	public boolean forceLobAsLastValue() {
		return false;
	}

	public final String quote(String name) {
		if ( name == null ) {
			return null;
		}

		if ( name.charAt( 0 ) == '`' ) {
			return openQuote() + name.substring( 1, name.length() - 1 ) + closeQuote();
		}
		else {
			return name;
		}
	}

	public String getAddPrimaryKeyConstraintString(String constraintName) {
		return " add constraint " + constraintName + " primary key ";
	}

	public boolean supportsNotNullUnique() {
		return true;
	}

	public String getAddUniqueConstraintString(String constraintName) {
		return " add constraint " + constraintName + " unique ";
	}

	public boolean supportsUniqueConstraintInCreateAlterTable() {
		return true;
	}

	public boolean qualifyIndexName() {
		return true;
	}

	public String getAddForeignKeyConstraintString(String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		StringBuilder res = new StringBuilder( 30 );

		res.append( " add constraint " )
				.append( constraintName )
				.append( " foreign key (" )
				.append( StringHelper.join( ", ", foreignKey ) )
				.append( ") references " )
				.append( referencedTable );

		if ( !referencesPrimaryKey ) {
			res.append( " (" )
					.append( StringHelper.join( ", ", primaryKey ) )
					.append( ')' );
		}

		return res.toString();
	}

	public boolean supportsCascadeDelete() {
		return true;
	}

	public String getDropForeignKeyString() {
		return " drop constraint ";
	}

	public String getAddColumnString() {
		throw new UnsupportedOperationException( "No add column syntax supported by " + getClass().getName() );
	}

	public String getNullColumnString() {
		return "";
	}

	public boolean supportsUnique() {
		return true;
	}

	public boolean supportsColumnCheck() {
		return true;
	}

	public String getColumnComment(String columnComment) {
		return "";
	}

	public String getCreateTemporaryTableString() {
		return  "create table";
	}

	public String getCreateTemporaryTablePostfix() {
		return "";
	}

	public String getCreateTableString() {
		return "create table";
	}

	public String getCreateMultisetTableString() {
		return getCreateTableString();
	}

	public boolean hasDataTypeInIdentityColumn() {
		return true;
	}

	public String getIdentityColumnString(int type) {
		return getIdentityColumnString();
	}

	private String getIdentityColumnString() {
		throw new MappingException( getClass().getName() + " does not support identity key generation" );
	}

	public boolean supportsTableCheck() {
		return true;
	}

	public String getTableComment(String comment) {
		return "";
	}

	public boolean supportsIfExistsBeforeTableName() {
		return false;
	}

	public String getCascadeConstraintsString() {
		return "";
	}

	public boolean supportsIfExistsAfterTableName() {
		return false;
	}

	public String getTableTypeString() {
		return "";
	}

	public boolean supportsCommentOn() {
		return false;
	}

	public String getIdentityInsertString() {
		return null;
	}

	public String getNoColumnsInsertString() {
		return "values ( )";
	}

	public boolean supportsInsertSelectIdentity() {
		return false;
	}

	public String appendIdentitySelectToInsert(String result) {
		return result;
	}

	public LimitHandler buildLimitHandler(String sql, RowSelection rowSelection) {
		return new LegacyLimitHandler( this, sql, rowSelection );
	}

	public boolean supportsLimit() {
		return false;
	}

	public boolean supportsLimitOffset() {
		return supportsLimit();
	}

	public int registerResultSetOutParameter(CallableStatement st, int col) {
		throw new UnsupportedOperationException(
				getClass().getName() +
				" does not support resultsets via stored procedures"
			);
	}

	public boolean supportsLockTimeouts() {
		return true;
	}

	public boolean isLockTimeoutParameterized() {
		return false;
	}

	public JoinFragment createOuterJoinFragment() {
		return new ANSIJoinFragment();
	}

	public String getForUpdateString(LockOptions lockOptions) {
        LockMode lockMode = lockOptions.getLockMode();
        return getForUpdateString( lockMode, lockOptions.getTimeOut() );
	}
	
	public String getForUpdateString() {
		return " for update";
	}
	
	public String getReadLockString(int timeout) {
		return getForUpdateString();
	}

	public String getWriteLockString(int timeout) {
		return getForUpdateString();
	}
	
	public String getForUpdateNowaitString() {
		// by default we report no support for NOWAIT lock semantics
		return getForUpdateString();
	}

	
	private String getForUpdateString(LockMode lockMode, int timeout){
	       switch ( lockMode ) {
	            case UPGRADE:
	                return getForUpdateString();
	            case PESSIMISTIC_READ:
	                return getReadLockString( timeout );
	            case PESSIMISTIC_WRITE:
	                return getWriteLockString( timeout );
	            case UPGRADE_NOWAIT:
	            case FORCE:
	            case PESSIMISTIC_FORCE_INCREMENT:
	                return getForUpdateNowaitString();
	            default:
	                return "";
	        }
	    }

	public String transformSelectString(String select) {
		return select;
	}

	public String appendLockHint(LockOptions lockOptions, String tableName){
		return tableName;
	}

	public int getMaxAliasLength() {
		return 10;
	}

	public SQLExceptionConverter buildSQLExceptionConverter() {
		return null;
	}

	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return null;
	}

	public int getInExpressionCountLimit() {
		return 0;
	}

	public String toBooleanValueString(boolean booleanValue) {
		return booleanValue ? "1" : "0";
	}

	public boolean supportsRowValueConstructorSyntax() {
		return false;
	}

	public boolean forUpdateOfColumns() {
		return false;
	}

	public String applyLocksToSql(String sql, LockOptions locks,
			Map keyColumnNames) {
		return null;
	}

	public CaseFragment createCaseFragment() {
		return new ANSICaseFragment();
	}

	public String getSelectClauseNullString(int sqlType) {
		return "null";
	}

	public boolean supportsUnionAll() {
		return false;
	}

	public boolean hasSelfReferentialForeignKeyBug() {
		return false;
	}
}
