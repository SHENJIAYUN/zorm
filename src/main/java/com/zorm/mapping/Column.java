package com.zorm.mapping;

import java.io.Serializable;

import com.zorm.dialect.Dialect;
import com.zorm.dialect.function.SQLFunctionRegistry;
import com.zorm.engine.Mapping;
import com.zorm.exception.MappingException;
import com.zorm.persister.entity.Template;
import com.zorm.util.StringHelper;

public class Column implements Selectable, Serializable, Cloneable{
	public static final int DEFAULT_LENGTH = 255;
	public static final int DEFAULT_PRECISION = 19;
	public static final int DEFAULT_SCALE = 2;

	private int length=DEFAULT_LENGTH;
	private int precision=DEFAULT_PRECISION;
	private int scale=DEFAULT_SCALE;
	private Value value;
	private int typeIndex = 0;
	private String name;
	private boolean nullable=true;
	private boolean unique=false;
	private String sqlType;
	private Integer sqlTypeCode;
	private boolean quoted=false;
	int uniqueInteger;
	private String checkConstraint;
	private String comment;
	private String defaultValue;
	private String customWrite;
	private String customRead;

	public Column() {
	}

	public Column(String columnName) {
		setName(columnName);
	}
	
	public void setName(String name) {
		if (
			name.charAt(0)=='`' ||
			Dialect.QUOTE.indexOf( name.charAt(0) ) > -1 //TODO: deprecated, remove eventually
		) {
			quoted=true;
			this.name=name.substring( 1, name.length()-1 );
		}
		else {
			this.name = name;
		}
	}

	@Override
	public String getAlias(Dialect dialect) {
		String alias = name;
		String unique = Integer.toString(uniqueInteger) + '_';
		int lastLetter = StringHelper.lastIndexOfLetter(name);
		if ( lastLetter == -1 ) {
			alias = "column";
		}
		else if ( lastLetter < name.length()-1 ) {
			alias = name.substring(0, lastLetter+1);
		}
		if ( alias.length() > dialect.getMaxAliasLength() ) {
			alias = alias.substring( 0, dialect.getMaxAliasLength() - unique.length() );
		}
		boolean useRawName = name.equals(alias) && 
			!quoted && 
			!name.toLowerCase().equals("rowid");
		if ( useRawName ) {
			return alias;
		}
		else {
			return alias + unique;
		}
	}

	@Override
	public String getAlias(Dialect dialect, Table table) {
		return getAlias(dialect) + table.getUniqueInteger() + '_';
	}

	@Override
	public boolean isFormula() {
		return false;
	}

	@Override
	public String getTemplate(Dialect dialect,
			SQLFunctionRegistry functionRegistry) {
		return hasCustomRead()
				? Template.renderWhereStringTemplate( customRead, dialect, functionRegistry )
				: Template.TEMPLATE + '.' + getQuotedName( dialect );
	}

	@Override
	public String getText(Dialect dialect) {
		return getQuotedName(dialect);
	}

	@Override
	public String getText() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setNullable(boolean nullable) {
       this.nullable = nullable;		
	}

	public void setLength(int length) {
      this.length = length;		
	}

	public void setPrecision(int precision) {
       this.precision = precision;		
	}

	public void setScale(int scale) {
		this.scale = scale;
	}

	public void setSqlType(String sqlType) {
      this.sqlType=sqlType;		
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public void setCustomRead(String customRead) {
      this.customRead=customRead;		
	}
	
	public void setCustomWrite(String customWrite) {
		this.customWrite = customWrite;
	}

	public String getName() {
		return name;
	}

	public void setValue(Value value) {
		this.value = value;
	}

	public void setTypeIndex(int i) {
		this.typeIndex = i;
	}

	public String getCanonicalName() {
		return quoted?name:name.toLowerCase();
	}

	public boolean isQuoted() {
		return quoted;
	}

	public String getQuotedName() {
		return quoted ?
				"`" + name + "`" :
				name;
	}

	public String getQuotedName(Dialect dialect) {
		return quoted?dialect.openQuote()+name+dialect.closeQuote():name;
	}
	
	public boolean hasCustomRead() {
		return ( customRead != null && customRead.length() > 0 );
	}

	public String getReadExpr(Dialect dialect) {
		return hasCustomRead() ? customRead : getQuotedName( dialect );
	}

	public String getWriteExpr() {
		return ( customWrite != null && customWrite.length() > 0 ) ? customWrite : "?";
	}

	public boolean isNullable() {
		return nullable;
	}

	public int getLength() {
		return length;
	}

	public int getPrecision() {
		return precision;
	}

	public int getScale() {
		return scale;
	}
	
	public String getSqlType(Dialect dialect, Mapping mapping) {
		if ( sqlType == null ) {
            sqlType = dialect.getTypeName( getSqlTypeCode( mapping ), getLength(), getPrecision(), getScale() );
        }
        return sqlType;
	}
	
	public Value getValue() {
		return value;
	}
	
	public int getTypeIndex() {
		return typeIndex;
	}
	
	 public Integer getSqlTypeCode() {
	        return sqlTypeCode;
	    }
	
	public int getSqlTypeCode(Mapping mapping) throws MappingException {
        com.zorm.type.Type type = getValue().getType();
        try {
            int sqlTypeCode = type.sqlTypes( mapping )[getTypeIndex()];
            if ( getSqlTypeCode() != null && getSqlTypeCode() != sqlTypeCode ) {
                throw new MappingException( "SQLType code's does not match. mapped as " + sqlTypeCode + " but is " + getSqlTypeCode() );
            }
            return sqlTypeCode;
        }
        catch ( Exception e ) {
            throw new MappingException(
                    "Could not determine type for column " +
                            name +
                            " of type " +
                            type.getClass().getName() +
                            ": " +
                            e.getClass().getName(),
                    e
            );
        }
    }

	public String getDefaultValue() {
		return defaultValue;
	}

	public boolean isUnique() {
		return unique;
	}

	public boolean hasCheckConstraint() {
		return checkConstraint!=null;
	}

	public String getCheckConstraint() {
		return checkConstraint;
	}

	public String getComment() {
		return comment;
	}

	public String getSqlType() {
		return sqlType;
	}

}
