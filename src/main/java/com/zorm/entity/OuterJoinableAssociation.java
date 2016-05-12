package com.zorm.entity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.zorm.exception.MappingException;
import com.zorm.persister.QueryableCollection;
import com.zorm.persister.entity.Joinable;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.sql.JoinFragment;
import com.zorm.sql.JoinType;
import com.zorm.type.AssociationType;
import com.zorm.type.EntityType;
import com.zorm.util.JoinHelper;

public final class OuterJoinableAssociation {
	private final PropertyPath propertyPath;
	private final AssociationType joinableType;
	private final Joinable joinable;
	private final String lhsAlias; // belong to other persister
	private final String[] lhsColumns; // belong to other persister
	private final String rhsAlias;
	private final String[] rhsColumns;
	private final JoinType joinType;
	private final String on;
	private final Map enabledFilters;
	private final boolean hasRestriction;

	public static OuterJoinableAssociation createRoot(
			AssociationType joinableType,
			String alias,
			SessionFactoryImplementor factory) {
		return new OuterJoinableAssociation(
				new PropertyPath(),
				joinableType,
				null,
				null,
				alias,
				JoinType.LEFT_OUTER_JOIN,
				null,
				false,
				factory,
				Collections.EMPTY_MAP
		);
	}

	public OuterJoinableAssociation(
			PropertyPath propertyPath,
			AssociationType joinableType,
			String lhsAlias,
			String[] lhsColumns,
			String rhsAlias,
			JoinType joinType,
			String withClause,
			boolean hasRestriction,
			SessionFactoryImplementor factory,
			Map enabledFilters) throws MappingException {
		this.propertyPath = propertyPath;
		this.joinableType = joinableType;
		this.lhsAlias = lhsAlias;
		this.lhsColumns = lhsColumns;
		this.rhsAlias = rhsAlias;
		this.joinType = joinType;
		this.joinable = joinableType.getAssociatedJoinable(factory);
		this.rhsColumns = JoinHelper.getRHSColumnNames(joinableType, factory);
		this.on = joinableType.getOnCondition(rhsAlias, factory, enabledFilters)
			+ ( withClause == null || withClause.trim().length() == 0 ? "" : " and ( " + withClause + " )" );
		this.hasRestriction = hasRestriction;
		this.enabledFilters = enabledFilters; // needed later for many-to-many/filter application
	}

	public Joinable getJoinable() {
		return joinable;
	}

	public boolean isCollection() {
		return joinableType.isCollectionType();
	}

	public String getRHSAlias() {
		return rhsAlias;
	}
	
	private boolean isOneToOne() {
//		if ( joinableType.isEntityType() )  {
//			EntityType etype = (EntityType) joinableType;
//			return etype.isOneToOne() /*&& etype.isReferenceToPrimaryKey()*/;
//		}
//		else {
//			return false;
//		}
		return false;
	}
	
	private static int getPosition(String lhsAlias, List associations) {
		int result = 0;
		for ( int i=0; i<associations.size(); i++ ) {
			OuterJoinableAssociation oj = (OuterJoinableAssociation) associations.get(i);
			if ( oj.getJoinable().consumesEntityAlias() /*|| oj.getJoinable().consumesCollectionAlias() */ ) {
				if ( oj.rhsAlias.equals(lhsAlias) ) return result;
				result++;
			}
		}
		return -1;
	}

	public int getOwner(List associations) {
		if ( isOneToOne() || isCollection() ) {
			return getPosition(lhsAlias, associations);
		}
		else {
			return -1;
		}
	}

	public AssociationType getJoinableType() {
		return joinableType;
	}

	public String getRhsAlias() {
		return rhsAlias;
	}

	public JoinType getJoinType() {
		return joinType;
	}

	public void validateJoin(String path) throws MappingException {
		if ( rhsColumns==null || lhsColumns==null
				|| lhsColumns.length!=rhsColumns.length || lhsColumns.length==0 ) {
			throw new MappingException("invalid join columns for association: " + path);
		}
	}

	public boolean hasRestriction() {
		return hasRestriction;
	}

	public boolean isManyToManyWith(OuterJoinableAssociation other) {
		if ( joinable.isCollection() ) {
			QueryableCollection persister = ( QueryableCollection ) joinable;
			if ( persister.isManyToMany() ) {
				return persister.getElementType() == other.getJoinableType();
			}
		}
		return false;
	}

	public void addManyToManyJoin(JoinFragment outerjoin,QueryableCollection joinable2) {
	}

	public void addJoins(JoinFragment outerjoin) throws MappingException {
		outerjoin.addJoin(
			joinable.getTableName(),
			rhsAlias,
			lhsColumns,
			rhsColumns,
			joinType,
			on
		);
		outerjoin.addJoins(
			joinable.fromJoinFragment(rhsAlias, false, true),
			joinable.whereJoinFragment(rhsAlias, false, true)
		);
	}
}
