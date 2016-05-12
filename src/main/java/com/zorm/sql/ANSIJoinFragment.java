package com.zorm.sql;

import com.zorm.exception.AssertionFailure;

public class ANSIJoinFragment extends JoinFragment {

	private StringBuilder buffer = new StringBuilder();
	private StringBuilder conditions = new StringBuilder();
	
	public void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, JoinType joinType) {
		addJoin(tableName, alias, fkColumns, pkColumns, joinType, null);
	}

	public void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, JoinType joinType, String on) {
		String joinString;
		switch (joinType) {
			case INNER_JOIN:
				joinString = " inner join ";
				break;
			case LEFT_OUTER_JOIN:
				joinString = " left outer join ";
				break;
			case RIGHT_OUTER_JOIN:
				joinString = " right outer join ";
				break;
			case FULL_JOIN:
				joinString = " full outer join ";
				break;
			default:
				throw new AssertionFailure("undefined join type");
		}

		buffer.append(joinString)
			.append(tableName)
			.append(' ')
			.append(alias)
			.append(" on ");


		for ( int j=0; j<fkColumns.length; j++) {
			buffer.append( fkColumns[j] )
				.append('=')
				.append(alias)
				.append('.')
				.append( pkColumns[j] );
			if ( j<fkColumns.length-1 ) buffer.append(" and ");
		}

		addCondition(buffer, on);

	}

	@Override
	public void addCrossJoin(String tableName, String alias) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addJoins(String fromFragment, String whereFragment) {
       buffer.append(fromFragment);
	}

	@Override
	public String toFromFragmentString() {
		return buffer.toString();
	}

	@Override
	public String toWhereFragmentString() {
		return conditions.toString();
	}

	@Override
	public void addCondition(String alias, String[] fkColumns,
			String[] pkColumns) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean addCondition(String condition) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public JoinFragment copy() {
		// TODO Auto-generated method stub
		return null;
	}

}
