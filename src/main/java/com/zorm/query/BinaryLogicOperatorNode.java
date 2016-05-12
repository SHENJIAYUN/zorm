package com.zorm.query;

import java.util.Arrays;

import com.zorm.exception.TypeMismatchException;
import com.zorm.exception.ZormException;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.type.StandardBasicTypes;
import com.zorm.type.Type;
import com.zorm.util.StringHelper;

import antlr.SemanticException;
import antlr.collections.AST;

public class BinaryLogicOperatorNode extends SqlWalkerNode implements BinaryOperatorNode{

	private static final long serialVersionUID = 3520844506218165107L;

	@Override
	public void initialize() throws SemanticException {
		Node lhs = getLeftHandOperand();
		if ( lhs == null ) {
			throw new SemanticException( "left-hand operand of a binary operator was null" );
		}
		Node rhs = getRightHandOperand();
		if ( rhs == null ) {
			throw new SemanticException( "right-hand operand of a binary operator was null" );
		}

		Type lhsType = extractDataType( lhs );
		Type rhsType = extractDataType( rhs );

		if ( lhsType == null ) {
			lhsType = rhsType;
		}
		if ( rhsType == null ) {
			rhsType = lhsType;
		}

		if ( ExpectedTypeAwareNode.class.isAssignableFrom( lhs.getClass() ) ) {
			( ( ExpectedTypeAwareNode ) lhs ).setExpectedType( rhsType );
		}
		if ( ExpectedTypeAwareNode.class.isAssignableFrom( rhs.getClass() ) ) {
			( ( ExpectedTypeAwareNode ) rhs ).setExpectedType( lhsType );
		}

		mutateRowValueConstructorSyntaxesIfNecessary( lhsType, rhsType );
	}

	private int getColumnSpan(Type type, SessionFactoryImplementor sfi) {
		int columnSpan = type.getColumnSpan( sfi );
//		if ( columnSpan == 0 && type instanceof OneToOneType ) {
//			columnSpan = ( ( OneToOneType ) type ).getIdentifierOrUniqueKeyType( sfi ).getColumnSpan( sfi );
//		}
		return columnSpan;
	}
	
	protected final void mutateRowValueConstructorSyntaxesIfNecessary(Type lhsType, Type rhsType) {
		// TODO : this really needs to be delayed until after we definitively know all node types
		// where this is currently a problem is parameters for which where we cannot unequivocally
		// resolve an expected type
		SessionFactoryImplementor sessionFactory = getSessionFactoryHelper().getFactory();
		if ( lhsType != null && rhsType != null ) {
			int lhsColumnSpan = getColumnSpan( lhsType, sessionFactory );
			if ( lhsColumnSpan != getColumnSpan( rhsType, sessionFactory ) ) {
				throw new TypeMismatchException(
						"left and right hand sides of a binary logic operator were incompatibile [" +
						lhsType.getName() + " : "+ rhsType.getName() + "]"
				);
			}
			if ( lhsColumnSpan > 1 ) {
				// for dialects which are known to not support ANSI-SQL row-value-constructor syntax,
				// we should mutate the tree.
				if ( !sessionFactory.getDialect().supportsRowValueConstructorSyntax() ) {
					mutateRowValueConstructorSyntax( lhsColumnSpan );
				}
			}
		}
	}
	
	protected static String[] extractMutationTexts(Node operand, int count) {
		if ( operand instanceof ParameterNode ) {
			String[] rtn = new String[count];
			Arrays.fill( rtn, "?" );
			return rtn;
		}
		else if ( operand.getType() == SqlTokenTypes.VECTOR_EXPR ) {
			String[] rtn = new String[ operand.getNumberOfChildren() ];
			int x = 0;
			AST node = operand.getFirstChild();
			while ( node != null ) {
				rtn[ x++ ] = node.getText();
				node = node.getNextSibling();
			}
			return rtn;
		}
		else if ( operand instanceof SqlNode ) {
			String nodeText = operand.getText();
			if ( nodeText.startsWith( "(" ) ) {
				nodeText = nodeText.substring( 1 );
			}
			if ( nodeText.endsWith( ")" ) ) {
				nodeText = nodeText.substring( 0, nodeText.length() - 1 );
			}
			String[] splits = StringHelper.split( ", ", nodeText );
			if ( count != splits.length ) {
				throw new ZormException( "SqlNode's text did not reference expected number of columns" );
			}
			return splits;
		}
		else {
			throw new ZormException( "dont know how to extract row value elements from node : " + operand );
		}
	}
	
	private void mutateRowValueConstructorSyntax(int valueElements) {
		// mutation depends on the types of nodes involved...
		int comparisonType = getType();
		String comparisonText = getText();
		setType( SqlTokenTypes.AND );
		setText( "AND" );
		String[] lhsElementTexts = extractMutationTexts( getLeftHandOperand(), valueElements );
		String[] rhsElementTexts = extractMutationTexts( getRightHandOperand(), valueElements );

		ParameterSpecification lhsEmbeddedCompositeParameterSpecification =
				getLeftHandOperand() == null || ( !ParameterNode.class.isInstance( getLeftHandOperand() ) )
						? null
						: ( ( ParameterNode ) getLeftHandOperand() ).getHqlParameterSpecification();

		ParameterSpecification rhsEmbeddedCompositeParameterSpecification =
				getRightHandOperand() == null || ( !ParameterNode.class.isInstance( getRightHandOperand() ) )
						? null
						: ( ( ParameterNode ) getRightHandOperand() ).getHqlParameterSpecification();

		translate( valueElements, comparisonType, comparisonText,
                lhsElementTexts, rhsElementTexts,
                lhsEmbeddedCompositeParameterSpecification,
                rhsEmbeddedCompositeParameterSpecification, this );
	}
	
	protected void translate( int valueElements, int comparisonType,
            String comparisonText, String[] lhsElementTexts,
            String[] rhsElementTexts,
            ParameterSpecification lhsEmbeddedCompositeParameterSpecification,
            ParameterSpecification rhsEmbeddedCompositeParameterSpecification,
            AST container ) {
        for ( int i = valueElements - 1; i > 0; i-- ) {
			if ( i == 1 ) {
				AST op1 = getASTFactory().create( comparisonType, comparisonText );
				AST lhs1 = getASTFactory().create( SqlTokenTypes.SQL_TOKEN, lhsElementTexts[0] );
				AST rhs1 = getASTFactory().create( SqlTokenTypes.SQL_TOKEN, rhsElementTexts[0] );
				op1.setFirstChild( lhs1 );
				lhs1.setNextSibling( rhs1 );
				container.setFirstChild( op1 );
				AST op2 = getASTFactory().create( comparisonType, comparisonText );
				AST lhs2 = getASTFactory().create( SqlTokenTypes.SQL_TOKEN, lhsElementTexts[1] );
				AST rhs2 = getASTFactory().create( SqlTokenTypes.SQL_TOKEN, rhsElementTexts[1] );
				op2.setFirstChild( lhs2 );
				lhs2.setNextSibling( rhs2 );
				op1.setNextSibling( op2 );

				// "pass along" our initial embedded parameter node(s) to the first generated
				// sql fragment so that it can be handled later for parameter binding...
				SqlFragment fragment = ( SqlFragment ) lhs1;
				if ( lhsEmbeddedCompositeParameterSpecification != null ) {
					fragment.addEmbeddedParameter( lhsEmbeddedCompositeParameterSpecification );
				}
				if ( rhsEmbeddedCompositeParameterSpecification != null ) {
					fragment.addEmbeddedParameter( rhsEmbeddedCompositeParameterSpecification );
				}
			}
			else {
				AST op = getASTFactory().create( comparisonType, comparisonText );
				AST lhs = getASTFactory().create( SqlTokenTypes.SQL_TOKEN, lhsElementTexts[i] );
				AST rhs = getASTFactory().create( SqlTokenTypes.SQL_TOKEN, rhsElementTexts[i] );
				op.setFirstChild( lhs );
				lhs.setNextSibling( rhs );
				AST newContainer = getASTFactory().create( SqlTokenTypes.AND, "AND" );
				container.setFirstChild( newContainer );
				newContainer.setNextSibling( op );
				container = newContainer;
			}
		}
    }


	
	protected Type extractDataType(Node operand) {
		Type type = null;
		if ( operand instanceof SqlNode ) {
			type = ( ( SqlNode ) operand ).getDataType();
		}
		if ( type == null && operand instanceof ExpectedTypeAwareNode ) {
			type = ( ( ExpectedTypeAwareNode ) operand ).getExpectedType();
		}
		return type;
	}

	@Override
	public Node getLeftHandOperand() {
		return ( Node ) getFirstChild();
	}

	@Override
	public Node getRightHandOperand() {
		return ( Node ) getFirstChild().getNextSibling();
	}
	
	@Override
	public Type getDataType() {
		return StandardBasicTypes.BOOLEAN;
	}

}
