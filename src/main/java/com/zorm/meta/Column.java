package com.zorm.meta;

import com.zorm.dialect.Dialect;
import com.zorm.exception.MappingException;
import com.zorm.util.StringHelper;

public class Column extends AbstractSimpleValue{
	private final Identifier columnName;
	private boolean nullable;
	private boolean unique;

	private String defaultValue;
	private String checkCondition;
	private String sqlType;

	private String readFragment;
	private String writeFragment;

	private String comment;

	private Size size = new Size();

	protected Column(TableSpecification table, int position, String name) {
		this( table, position, Identifier.toIdentifier( name ) );
	}

	protected Column(TableSpecification table, int position, Identifier name) {
		super( table, position );
		this.columnName = name;
	}

	@Override
	public String getAlias(Dialect dialect) {
		String alias = columnName.getName();
		int lastLetter = StringHelper.lastIndexOfLetter( columnName.getName() );
		if ( lastLetter == -1 ) {
			alias = "column";
		}
		boolean useRawName =
				columnName.getName().equals( alias ) &&
						alias.length() <= dialect.getMaxAliasLength() &&
						! columnName.isQuoted() &&
						! columnName.getName().toLowerCase().equals( "rowid" );
		if ( ! useRawName ) {
			String unique =
					new StringBuilder()
					.append( getPosition() )
					.append( '_' )
					.append( getTable().getTableNumber() )
					.append( '_' )
					.toString();
			if ( unique.length() >= dialect.getMaxAliasLength() ) {
				throw new MappingException(
						"Unique suffix [" + unique + "] length must be less than maximum [" + dialect.getMaxAliasLength() + "]"
				);
			}
			if ( alias.length() + unique.length() > dialect.getMaxAliasLength()) {
				alias = alias.substring( 0, dialect.getMaxAliasLength() - unique.length() );
			}
			alias = alias + unique;
		}
		return alias;
	}

}
