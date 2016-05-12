package com.zorm.query;

import java.lang.reflect.Constructor;

import antlr.ASTFactory;
import antlr.Token;
import antlr.collections.AST;

public class SqlASTFactory extends ASTFactory implements SqlTokenTypes {
	private SqlWalker walker;
	/**
	 * Create factory with a specific mapping from token type
	 * to Java AST node type.  Your subclasses of ASTFactory
	 * can override and reuse the map stuff.
	 */
	public SqlASTFactory(SqlWalker walker) {
		super();
		this.walker = walker;
	}

	/**
	 * Returns the class for a given token type (a.k.a. AST node type).
	 *
	 * @param tokenType The token type.
	 * @return Class - The AST node class to instantiate.
	 */
	public Class getASTNodeType(int tokenType) {
		switch ( tokenType ) {
			case SELECT:
			case QUERY:
				return QueryNode.class;
			case UPDATE:
				return UpdateStatement.class;
			case DELETE:
				return DeleteStatement.class;
//			case INSERT:
//				return InsertStatement.class;
//			case INTO:
//				return IntoClause.class;
			case FROM:
				return FromClause.class;
			case FROM_FRAGMENT:
				return FromElement.class;
//			case IMPLIED_FROM:
//				return ImpliedFromElement.class;
			case DOT:
				return DotNode.class;
//			case INDEX_OP:
//				return IndexNode.class;
//				// Alias references and identifiers use the same node class.
			case ALIAS_REF:
			case IDENT:
				return IdentNode.class;
//			case RESULT_VARIABLE_REF:
//				return ResultVariableRefNode.class;
//			case SQL_TOKEN:
//				return SqlFragment.class;
//			case METHOD_CALL:
//				return MethodNode.class;
//			case ELEMENTS:
//			case INDICES:
//				return CollectionFunction.class;
			case SELECT_CLAUSE:
				return SelectClause.class;
			case SELECT_EXPR:
				return SelectExpressionImpl.class;
//			case AGGREGATE:
//				return AggregateNode.class;
//			case COUNT:
//				return CountNode.class;
//			case CONSTRUCTOR:
//				return ConstructorNode.class;
			case NUM_INT:
			case NUM_FLOAT:
			case NUM_LONG:
			case NUM_DOUBLE:
			case NUM_BIG_INTEGER:
			case NUM_BIG_DECIMAL:
			case QUOTED_STRING:
				return LiteralNode.class;
//			case TRUE:
//			case FALSE:
//				return BooleanLiteralNode.class;
//			case JAVA_CONSTANT:
//				return JavaConstantNode.class;
//			case ORDER:
//				return OrderByClause.class;
//			case PLUS:
//			case MINUS:
//			case STAR:
//			case DIV:
//			case MOD:
//				return BinaryArithmeticOperatorNode.class;
//			case UNARY_MINUS:
//			case UNARY_PLUS:
//				return UnaryArithmeticNode.class;
//			case CASE2:
//				return Case2Node.class;
//			case CASE:
//				return CaseNode.class;
			case PARAM:
			case NAMED_PARAM:
				return ParameterNode.class;
			case EQ:
			case NE:
			case LT:
			case GT:
			case LE:
			case GE:
			case LIKE:
			case NOT_LIKE:
				return BinaryLogicOperatorNode.class;
//			case IN:
//			case NOT_IN:
//				return InLogicOperatorNode.class;
//			case BETWEEN:
//			case NOT_BETWEEN:
//				return BetweenOperatorNode.class;
//			case IS_NULL:
//				return IsNullLogicOperatorNode.class;
//			case IS_NOT_NULL:
//				return IsNotNullLogicOperatorNode.class;
//			case EXISTS:
//				return UnaryLogicOperatorNode.class;
//			case KEY: {
//				return MapKeyNode.class;
//			}
//			case VALUE: {
//				return MapValueNode.class;
//			}
//			case ENTRY: {
//				return MapEntryNode.class;
//			}
			default:
				return SqlNode.class;
		} // switch
	}

	protected AST createUsingCtor(Token token, String className) {
		Class c;
		AST t;
		try {
			c = Class.forName( className );
			Class[] tokenArgType = new Class[]{antlr.Token.class};
			Constructor ctor = c.getConstructor( tokenArgType );
			if ( ctor != null ) {
				t = ( AST ) ctor.newInstance( new Object[]{token} ); // make a new one
				initializeSqlNode( t );
			}
			else {
				// just do the regular thing if you can't find the ctor
				// Your AST must have default ctor to use this.
				t = create( c );
			}
		}
		catch ( Exception e ) {
			throw new IllegalArgumentException( "Invalid class or can't make instance, " + className );
		}
		return t;
	}

	private void initializeSqlNode(AST t) {
		// Initialize SQL nodes here.
		if ( t instanceof InitializeableNode ) {
			InitializeableNode initializeableNode = ( InitializeableNode ) t;
			initializeableNode.initialize( walker );
		}
//		if ( t instanceof SessionFactoryAwareNode ) {
//			( ( SessionFactoryAwareNode ) t ).setSessionFactory( walker.getSessionFactoryHelper().getFactory() );
//		}
	}

	/**
	 * Actually instantiate the AST node.
	 *
	 * @param c The class to instantiate.
	 * @return The instantiated and initialized node.
	 */
	protected AST create(Class c) {
		AST t;
		try {
			t = ( AST ) c.newInstance(); // make a new one
			initializeSqlNode( t );
		}
		catch ( Exception e ) {
			error( "Can't create AST Node " + c.getName() );
			return null;
		}
		return t;
	}
}
