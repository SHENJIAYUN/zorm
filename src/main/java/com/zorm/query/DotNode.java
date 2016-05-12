package com.zorm.query;

import com.zorm.exception.QueryException;
import com.zorm.type.Type;
import com.zorm.util.StringHelper;

import antlr.SemanticException;
import antlr.collections.AST;

public class DotNode extends FromReferenceNode implements DisplayableNode, SelectExpression {

	private static final int DEREF_UNKNOWN = 0;
	private static final int DEREF_ENTITY = 1;
	private static final int DEREF_COMPONENT = 2;
	private static final int DEREF_COLLECTION = 3;
	private static final int DEREF_PRIMITIVE = 4;
	private static final int DEREF_IDENTIFIER = 5;
	private static final int DEREF_JAVA_CONSTANT = 6;
	
	private String propertyName;
	private String propertyPath;
	private String[] columns;
	private int dereferenceType = DEREF_UNKNOWN;
	
	@Override
	public String getPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setScalarColumnText(int i) throws SemanticException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getDisplayText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void resolveInFunctionCall(boolean generateJoin, boolean implicitJoin)
			throws SemanticException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resolve(boolean generateJoin, boolean implicitJoin,
			String classAlias, AST parent) throws SemanticException {
		// TODO Auto-generated method stub
		if ( isResolved() ) {
			return;
		}
		Type propertyType = prepareLhs();
//		if ( propertyType == null ) {
//			if ( parent == null ) {
//				getWalker().getLiteralProcessor().lookupConstant( this );
//			}
//			// If the propertyType is null and there isn't a parent, just
//			// stop now... there was a problem resolving the node anyway.
//			return;
//		}
		if ( propertyType.isComponentType() ) {
			// The property is a component...
//			checkLhsIsNotCollection();
//			dereferenceComponent( parent );
//			initText();
		}
		else if ( propertyType.isEntityType() ) {
			// The property is another class..
//			checkLhsIsNotCollection();
//			dereferenceEntity( ( EntityType ) propertyType, implicitJoin, classAlias, generateJoin, parent );
//			initText();
		}
		else if ( propertyType.isCollectionType() ) {
			// The property is a collection...
//			checkLhsIsNotCollection();
//			dereferenceCollection( ( CollectionType ) propertyType, implicitJoin, false, classAlias, parent );
		}
		else {
			// Otherwise, this is a primitive type.
//			if ( ! CollectionProperties.isAnyCollectionProperty( propertyName ) ) {
//				checkLhsIsNotCollection();
//			}
			dereferenceType = DEREF_PRIMITIVE;
			initText();
		}
		setResolved();
	}
	
	private void initText() {
		String[] cols = getColumns();
		String text = StringHelper.join( ", ", cols );
		if ( cols.length > 1 && getWalker().isComparativeExpressionClause() ) {
			text = "(" + text + ")";
		}
		setText( text );
	}
	
	private String[] getColumns() throws QueryException {
		if ( columns == null ) {
			// Use the table fromElement and the property name to get the array of column names.
			String tableAlias = getLhs().getFromElement().getTableAlias();
			columns = getFromElement().toColumns( tableAlias, propertyPath, false );
		}
		return columns;
	}
	
	private Type prepareLhs() throws SemanticException {
		FromReferenceNode lhs = getLhs();
		lhs.prepareForDot( propertyName );
		return getDataType();
	}
	
	@Override
    public Type getDataType() {
		if ( super.getDataType() == null ) {
			FromElement fromElement = getLhs().getFromElement();
			if ( fromElement == null ) return null;
			// If the lhs is a collection, use CollectionPropertyMapping
			Type propertyType = fromElement.getPropertyType( propertyName, propertyPath );
			super.setDataType( propertyType );
		}
		return super.getDataType();
	}

	public FromReferenceNode getLhs() {
		FromReferenceNode lhs = ( ( FromReferenceNode ) getFirstChild() );
		if ( lhs == null ) {
			throw new IllegalStateException( "DOT node with no left-hand-side!" );
		}
		return lhs;
	}

	public void resolveFirstChild() throws SemanticException{
		FromReferenceNode lhs = ( FromReferenceNode ) getFirstChild();
		SqlNode property = ( SqlNode ) lhs.getNextSibling();
		String propName = property.getText();
		propertyName = propName;
		if ( propertyPath == null ) {
			propertyPath = propName;
		}
		lhs.resolve( true, true, null, this );
		setFromElement( lhs.getFromElement() );	
		checkSubclassOrSuperclassPropertyReference( lhs, propName );
	}
	
	private boolean checkSubclassOrSuperclassPropertyReference(FromReferenceNode lhs, String propertyName) {
		if ( lhs != null && !( lhs instanceof IndexNode ) ) {
			final FromElement source = lhs.getFromElement();
			if ( source != null ) {
				source.handlePropertyBeingDereferenced( lhs.getDataType(), propertyName );
			}
		}
		return false;
	}

	public void setPropertyPath(String path) {
		this.propertyPath = path;
	}

	public String getPropertyPath() {
		return propertyPath;
	}

}
