package com.zorm.query;

import java.io.StringReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.util.ASTUtil;

import antlr.RecognitionException;
import antlr.TokenStream;
import antlr.collections.AST;

@SuppressWarnings("unused")
public final class QueryParser extends QueryBaseParser{
  private static final Log log = LogFactory.getLog(QueryParser.class);

  private ParseErrorHandler parseErrorHandler;
  
  private ASTPrinter printer = getASTPrinter();
  
  public ParseErrorHandler getParseErrorHandler() {
		return parseErrorHandler;
	}
  
  private static ASTPrinter getASTPrinter() {
		return new ASTPrinter( com.zorm.query.TokenTypes.class );
	}
  
  private QueryParser(TokenStream lexer) {
		super( lexer );
		initialize();
	}
  
  private void initialize() {
		parseErrorHandler = new ErrorCounter();
		setASTFactory(new QueryASTFactory());	// Create nodes that track line and column number.
	}
  
  public static QueryParser getInstance(String hql) {
	//查询语句作为参数创建词法分析器，继承自ANTLR创建的QueryBaseLexer
    QueryLexer lexer = new QueryLexer(new StringReader( hql ));
	return new QueryParser( lexer );
  }
  
  @Override
	public AST processEqualityExpression(AST x) throws RecognitionException {
	  if ( x == null ) {
			return null;
		}

		int type = x.getType();
		if ( type == EQ || type == NE ) {
			boolean negated = type == NE;
			if ( x.getNumberOfChildren() == 2 ) {
				AST a = x.getFirstChild();
				AST b = a.getNextSibling();
				// (EQ NULL b) => (IS_NULL b)
				if ( a.getType() == NULL && b.getType() != NULL ) {
					return createIsNullParent( b, negated );
				}
				// (EQ a NULL) => (IS_NULL a)
				else if ( b.getType() == NULL && a.getType() != NULL ) {
					return createIsNullParent( a, negated );
				}
				else if ( b.getType() == EMPTY ) {
					return processIsEmpty( a, negated );
				}
				else {
					return x;
				}
			}
			else {
				return x;
			}
		}
		else {
			return x;
		}
	}
  
  private AST createIsNullParent(AST node, boolean negated) {
		node.setNextSibling( null );
		int type = negated ? IS_NOT_NULL : IS_NULL;
		String text = negated ? "is not null" : "is null";
		return ASTUtil.createParent( astFactory, type, text, node );
	}
  
  private AST processIsEmpty(AST node, boolean negated) {
		node.setNextSibling( null );
		AST ast = createSubquery( node );
		ast = ASTUtil.createParent( astFactory, EXISTS, "exists", ast );
		if ( !negated ) {
			ast = ASTUtil.createParent( astFactory, NOT, "not", ast );
		}
		return ast;
	}
  
  private AST createSubquery(AST node) {
		AST ast = ASTUtil.createParent( astFactory, RANGE, "RANGE", node );
		ast = ASTUtil.createParent( astFactory, FROM, "from", ast );
		ast = ASTUtil.createParent( astFactory, SELECT_FROM, "SELECT_FROM", ast );
		ast = ASTUtil.createParent( astFactory, QUERY, "QUERY", ast );
		return ast;
	}
  
}
