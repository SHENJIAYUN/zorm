package com.zorm.query;

import java.util.*;

import com.zorm.exception.QueryException;
import com.zorm.persister.entity.Queryable;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.util.ASTUtil;

import antlr.collections.AST;

public class AssignmentSpecification {
	private final Set tableNames;
	private final ParameterSpecification[] hqlParameters;
	private final AST eq;
	private final SessionFactoryImplementor factory;

	private String sqlAssignmentString;
	
	public AssignmentSpecification(AST eq, Queryable persister) {
		if ( eq.getType() != SqlTokenTypes.EQ ) {
			throw new QueryException( "assignment in set-clause not associated with equals" );
		}

		this.eq = eq;
		this.factory = persister.getFactory();

		// Needed to bump this up to DotNode, because that is the only thing which currently
		// knows about the property-ref path in the correct format; it is either this, or
		// recurse over the DotNodes constructing the property path just like DotNode does
		// internally
		DotNode lhs = ( DotNode ) eq.getFirstChild();
		SqlNode rhs = ( SqlNode ) lhs.getNextSibling();

		validateLhs( lhs );

		final String propertyPath = lhs.getPropertyPath();
		Set temp = new HashSet();
		// yuck!
//		if ( persister instanceof UnionSubclassEntityPersister ) {
//			UnionSubclassEntityPersister usep = ( UnionSubclassEntityPersister ) persister;
//			String[] tables = persister.getConstraintOrderedTableNameClosure();
//			int size = tables.length;
//			for ( int i = 0; i < size; i ++ ) {
//				temp.add( tables[i] );
//			}
//		}
//		else {
//			temp.add(
//					persister.getSubclassTableName( persister.getSubclassPropertyTableNumber( propertyPath ) )
//			);
//		}
		this.tableNames = Collections.unmodifiableSet( temp );

		if (rhs==null) {
			hqlParameters = new ParameterSpecification[0];
		}
		else if ( isParam( rhs ) ) {
			hqlParameters = new ParameterSpecification[] { ( ( ParameterNode ) rhs ).getHqlParameterSpecification() };
		}
		else {
			List parameterList = ASTUtil.collectChildren(
			        rhs,
			        new ASTUtil.IncludePredicate() {
				        public boolean include(AST node) {
					        return isParam( node );
			            }
			        }
			);
			hqlParameters = new ParameterSpecification[ parameterList.size() ];
			Iterator itr = parameterList.iterator();
			int i = 0;
			while( itr.hasNext() ) {
				hqlParameters[i++] = ( ( ParameterNode ) itr.next() ).getHqlParameterSpecification();
			}
		}
	}
	
	private static boolean isParam(AST node) {
		return node.getType() == SqlTokenTypes.PARAM || node.getType() == SqlTokenTypes.NAMED_PARAM;
	}
	
	private void validateLhs(FromReferenceNode lhs) {
		// make sure the lhs is "assignable"...
		if ( !lhs.isResolved() ) {
			throw new UnsupportedOperationException( "cannot validate assignablity of unresolved node" );
		}

		if ( lhs.getDataType().isCollectionType() ) {
			throw new QueryException( "collections not assignable in update statements" );
		}
		else if ( lhs.getDataType().isComponentType() ) {
			throw new QueryException( "Components currently not assignable in update statements" );
		}
		else if ( lhs.getDataType().isEntityType() ) {
			// currently allowed...
		}

		// TODO : why aren't these the same?
		if ( lhs.getImpliedJoin() != null || lhs.getFromElement().isImplied() ) {
			throw new QueryException( "Implied join paths are not assignable in update statements" );
		}
	}

	public ParameterSpecification[] getParameters() {
		return hqlParameters;
	}
}
