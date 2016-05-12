package com.zorm.query;

import com.zorm.dialect.function.SQLFunction;
import com.zorm.type.Type;

import antlr.SemanticException;
import antlr.collections.AST;

public class IdentNode extends FromReferenceNode implements SelectExpression {

	@Override
	public String getDisplayText() {
		return null;
	}

	@Override
	public String getPath() {
		return null;
	}

	@Override
	public void setScalarColumnText(int i) throws SemanticException {
		
	}
	
	private boolean resolveAsAlias() {
		// This is not actually a constant, but a reference to FROM element.
		FromElement element = getWalker().getCurrentFromClause().getFromElement( getText() );
		if ( element != null ) {
			setType( SqlTokenTypes.ALIAS_REF );
			setFromElement( element );
			String[] columnExpressions = element.getIdentityColumns();
			final boolean isInNonDistinctCount = getWalker().isInCount() && ! getWalker().isInCountDistinct();
			final boolean isCompositeValue = columnExpressions.length > 1;
			if ( isCompositeValue ) {
//				if ( isInNonDistinctCount && ! getWalker().getSessionFactoryHelper().getFactory().getDialect().supportsTupleCounts() ) {
//					setText( columnExpressions[0] );
//				}
//				else {
//					String joinedFragment = StringHelper.join( ", ", columnExpressions );
//					// avoid wrapping in parenthesis (explicit tuple treatment) if possible due to varied support for
//					// tuple syntax across databases..
//					final boolean shouldSkipWrappingInParenthesis =
//							getWalker().isInCount()
//							|| getWalker().getCurrentTopLevelClauseType() == HqlSqlTokenTypes.ORDER
//							|| getWalker().getCurrentTopLevelClauseType() == HqlSqlTokenTypes.GROUP;
//					if ( ! shouldSkipWrappingInParenthesis ) {
//						joinedFragment = "(" + joinedFragment + ")";
//					}
//					setText( joinedFragment );
//				}
				return true;
			}
			else if ( columnExpressions.length > 0 ) {
				setText( columnExpressions[0] );
				return true;
			}
		}
		return false;
	}
	
	public void resolve(boolean generateJoin, boolean implicitJoin, String classAlias, AST parent) {
		if (!isResolved()) {
			if (getWalker().getCurrentFromClause().isFromElementAlias(getText())) {
				if (resolveAsAlias()) {
					setResolved();
					// We represent a from-clause alias
				}
			}
			else if (parent != null && parent.getType() == SqlTokenTypes.DOT) {
//				DotNode dot = (DotNode) parent;
//				if (parent.getFirstChild() == this) {
//					if (resolveAsNakedComponentPropertyRefLHS(dot)) {
//						// we are the LHS of the DOT representing a naked comp-prop-ref
//						setResolved();
//					}
//				}
//				else {
//					if (resolveAsNakedComponentPropertyRefRHS(dot)) {
//						// we are the RHS of the DOT representing a naked comp-prop-ref
//						setResolved();
//					}
//				}
			}
			else {
//				int result = resolveAsNakedPropertyRef();
//				if (result == PROPERTY_REF) {
//					// we represent a naked (simple) prop-ref
//					setResolved();
//				}
//				else if (result == COMPONENT_REF) {
//					// EARLY EXIT!!!  return so the resolve call explicitly coming from DotNode can
//					// resolve this...
//					return;
//				}
			}

			// if we are still not resolved, we might represent a constant.
			//      needed to add this here because the allowance of
			//      naked-prop-refs in the grammar collides with the
			//      definition of literals/constants ("nondeterminism").
			//      TODO: cleanup the grammar so that "processConstants" is always just handled from here
			if (!isResolved()) {
				try {
					//getWalker().getLiteralProcessor().processConstant(this, false);
				}
				catch (Throwable ignore) {
					// just ignore it for now, it'll get resolved later...
				}
			}
		}
	}
	@Override
	public void resolveInFunctionCall(boolean generateJoin, boolean implicitJoin)
			throws SemanticException {
		
	  }

	@Override
    public Type getDataType() {
		Type type = super.getDataType();
		if ( type != null ) {
			return type;
		}
		FromElement fe = getFromElement();
		if ( fe != null ) {
			return fe.getDataType();
		}
		SQLFunction sf = getWalker().getSessionFactoryHelper().findSQLFunction( getText() );
		if ( sf != null ) {
			return sf.getReturnType( null, getWalker().getSessionFactoryHelper().getFactory() );
		}
		return null;
	}
}
	

