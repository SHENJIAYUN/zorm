package com.zorm.sql;

import com.zorm.util.StringHelper;

public abstract class JoinFragment {
	public abstract void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, JoinType joinType);

	public abstract void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, JoinType joinType, String on);

	public abstract void addCrossJoin(String tableName, String alias);

	public abstract void addJoins(String fromFragment, String whereFragment);

	public abstract String toFromFragmentString();

	public abstract String toWhereFragmentString();

	// --Commented out by Inspection (12/4/04 9:10 AM): public abstract void addCondition(String alias, String[] columns, String condition);
	public abstract void addCondition(String alias, String[] fkColumns, String[] pkColumns);

	public abstract boolean addCondition(String condition);
	// --Commented out by Inspection (12/4/04 9:10 AM): public abstract void addFromFragmentString(String fromFragmentString);

	public abstract JoinFragment copy();
	
	protected boolean addCondition(StringBuilder buffer, String on) {
		if ( StringHelper.isNotEmpty( on ) ) {
			if ( !on.startsWith( " and" ) ) buffer.append( " and " );
			buffer.append( on );
			return true;
		}
		else {
			return false;
		}
	}
}
