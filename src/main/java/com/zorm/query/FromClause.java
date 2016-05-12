package com.zorm.query;

import java.util.*;

import com.zorm.util.ASTIterator;
import com.zorm.util.ASTUtil;

import antlr.SemanticException;
import antlr.collections.AST;

public class FromClause extends SqlWalkerNode implements SqlTokenTypes{
	public static final int ROOT_LEVEL = 1;

	private int level = ROOT_LEVEL;
	private Set fromElements = new HashSet();
	private Map fromElementByClassAlias = new HashMap();
	private Map fromElementByTableAlias = new HashMap();
	private Map fromElementsByPath = new HashMap();
	
	private FromClause parentFromClause;
	
	
	public FromElement addFromElement(String path, AST alias) throws SemanticException {
		String classAlias = ( alias == null ) ? null : alias.getText();
		checkForDuplicateClassAlias( classAlias );
		FromElementFactory factory = new FromElementFactory( this, null, path, classAlias, null, false );
		return factory.addFromElement();
	}
	
	private void checkForDuplicateClassAlias(String classAlias) throws SemanticException {
		if ( classAlias != null && fromElementByClassAlias.containsKey( classAlias ) ) {
			throw new SemanticException( "Duplicate definition of alias '" + classAlias + "'" );
		}
	}
	
	public FromClause getParentFromClause() {
		return parentFromClause;
	}

	public FromElement getFromElement(String aliasOrClassName) {
		FromElement fromElement = ( FromElement ) fromElementByClassAlias.get( aliasOrClassName );
		if ( fromElement == null && getSessionFactoryHelper().isStrictJPAQLComplianceEnabled() ) {
			fromElement = findIntendedAliasedFromElementBasedOnCrazyJPARequirements( aliasOrClassName );
		}
		if ( fromElement == null && parentFromClause != null ) {
			fromElement = parentFromClause.getFromElement( aliasOrClassName );
		}
		return fromElement;
	}
	
	private FromElement findIntendedAliasedFromElementBasedOnCrazyJPARequirements(String specifiedAlias) {
		Iterator itr = fromElementByClassAlias.entrySet().iterator();
		while ( itr.hasNext() ) {
			Map.Entry entry = ( Map.Entry ) itr.next();
			String alias = ( String ) entry.getKey();
			if ( alias.equalsIgnoreCase( specifiedAlias ) ) {
				return ( FromElement ) entry.getValue();
			}
		}
		return null;
	}

	void registerFromElement(FromElement element) {
		fromElements.add( element );
		String classAlias = element.getClassAlias();
		if ( classAlias != null ) {
			// The HQL class alias refers to the class name.
			fromElementByClassAlias.put( classAlias, element );
		}
		// Associate the table alias with the element.
		String tableAlias = element.getTableAlias();
		if ( tableAlias != null ) {
			fromElementByTableAlias.put( tableAlias, element );
		}
	}

	private int fromElementCounter = 0;
	
	public int nextFromElementCounter() {
		return fromElementCounter++;
	}

	public void setParentFromClause(FromClause parentFromClause) {
		this.parentFromClause = parentFromClause;
		if ( parentFromClause != null ) {
			level = parentFromClause.getLevel() + 1;
			parentFromClause.addChild( this );
		}
	}

	private int getLevel() {
		return level;
	}

	public boolean isFromElementAlias(String possibleAlias) {
		boolean isAlias = containsClassAlias( possibleAlias );
		if ( !isAlias && parentFromClause != null ) {
			// try the parent FromClause...
			isAlias = parentFromClause.isFromElementAlias( possibleAlias );
		}
		return isAlias;
	}
	
	public boolean containsClassAlias(String alias) {
		boolean isAlias = fromElementByClassAlias.containsKey( alias );
		if ( !isAlias && getSessionFactoryHelper().isStrictJPAQLComplianceEnabled() ) {
			isAlias = findIntendedAliasedFromElementBasedOnCrazyJPARequirements( alias ) != null;
		}
		return isAlias;
	}
	
	private static ASTUtil.FilterPredicate projectionListPredicate = new ASTUtil.IncludePredicate() {
		@Override
        public boolean include(AST node) {
			FromElement fromElement = ( FromElement ) node;
			return fromElement.inProjectionList();
		}
	};

	public List getProjectionList() {
		return ASTUtil.collectChildren( this, projectionListPredicate );
	}

	public boolean isSubQuery() {
		return parentFromClause!=null;
	}
	
	private static ASTUtil.FilterPredicate explicitFromPredicate = new ASTUtil.IncludePredicate() {
		@Override
        public boolean include(AST node) {
			final FromElement fromElement = ( FromElement ) node;
			return !fromElement.isImplied();
		}
	};

	public List getExplicitFromElements() {
		return ASTUtil.collectChildren( this, explicitFromPredicate );
	}

	public FromElement getFromElement() {
		return (FromElement) getFromElements().get(0);
	}
	
	private static ASTUtil.FilterPredicate fromElementPredicate = new ASTUtil.IncludePredicate() {
		@Override
        public boolean include(AST node) {
			FromElement fromElement = ( FromElement ) node;
			return fromElement.isFromOrJoinFragment();
		}
	};

	public List getFromElements() {
		return ASTUtil.collectChildren( this, fromElementPredicate );
	}

	public void resolve() {
		ASTIterator iter = new ASTIterator( this.getFirstChild() );
		Set childrenInTree = new HashSet();
		while ( iter.hasNext() ) {
			childrenInTree.add( iter.next() );
		}
		for ( Iterator iterator = fromElements.iterator(); iterator.hasNext(); ) {
			FromElement fromElement = ( FromElement ) iterator.next();
			if ( !childrenInTree.contains( fromElement ) ) {
				throw new IllegalStateException( "Element not in AST: " + fromElement );
			}
		}
	}
}
