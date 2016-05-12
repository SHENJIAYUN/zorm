package com.zorm.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import antlr.NoViableAltException;
import antlr.RecognitionException;
import antlr.collections.AST;

import com.zorm.session.SessionFactoryImplementor;

public class SqlGenerator extends SqlGeneratorBase implements ErrorReporter{
	
	private ParseErrorHandler parseErrorHandler;
	private SessionFactoryImplementor sessionFactory;
	
	private List collectedParameters = new ArrayList();
	private SqlWriter writer = new DefaultWriter();
	
	interface SqlWriter {
		void clause(String clause);

		void commaBetweenParameters(String comma);
	}
	
	class DefaultWriter implements SqlWriter {
		public void clause(String clause) {
			getStringBuilder().append( clause );
		}

		public void commaBetweenParameters(String comma) {
			getStringBuilder().append( comma );
		}
	}
	
	@Override
    protected void fromFragmentSeparator(AST a) {
		// check two "adjecent" nodes at the top of the from-clause tree
		AST next = a.getNextSibling();
		if ( next == null || !hasText( a ) ) {
			return;
		}

//		FromElement left = ( FromElement ) a;
//		FromElement right = ( FromElement ) next;
//
//		///////////////////////////////////////////////////////////////////////
//		// HACK ALERT !!!!!!!!!!!!!!!!!!!!!!!!!!!!
//		// Attempt to work around "ghost" ImpliedFromElements that occasionally
//		// show up between the actual things being joined.  This consistently
//		// occurs from index nodes (at least against many-to-many).  Not sure
//		// if there are other conditions
//		//
//		// Essentially, look-ahead to the next FromElement that actually
//		// writes something to the SQL
//		while ( right != null && !hasText( right ) ) {
//			right = ( FromElement ) right.getNextSibling();
//		}
//		if ( right == null ) {
//			return;
//		}
//		///////////////////////////////////////////////////////////////////////
//
//		if ( !hasText( right ) ) {
//			return;
//		}
//
//		if ( right.getRealOrigin() == left ||
//		     ( right.getRealOrigin() != null && right.getRealOrigin() == left.getRealOrigin() ) ) {
//			// right represents a joins originating from left; or
//			// both right and left reprersent joins originating from the same FromElement
//			if ( right.getJoinSequence() != null && right.getJoinSequence().isThetaStyle() ) {
//				writeCrossJoinSeparator();
//			}
//			else {
//				out( " " );
//			}
//		}
//		else {
//			// these are just two unrelated table references
//			writeCrossJoinSeparator();
//		}
	}
	
	public SqlGenerator(SessionFactoryImplementor sfi) {
		super();
		parseErrorHandler = new ErrorCounter();
		sessionFactory = sfi;
	}
	
	@Override
    protected void out(String s) {
		writer.clause( s );
	}
	
	public ParseErrorHandler getParseErrorHandler() {
		return parseErrorHandler;
	}
	
	public List getCollectedParameters() {
		return collectedParameters;
	}
  
    public String getSQL() {
		return getStringBuilder().toString();
	}
    
    @Override
    protected void out(AST n) {
		if ( n instanceof Node ) {
			out( ( ( Node ) n ).getRenderText( sessionFactory ) );
		}
		else {
			super.out( n );
		}

		if ( n instanceof ParameterNode ) {
			collectedParameters.add( ( ( ParameterNode ) n ).getHqlParameterSpecification() );
		}
//		else if ( n instanceof ParameterContainer ) {
//			if ( ( ( ParameterContainer ) n ).hasEmbeddedParameters() ) {
//				ParameterSpecification[] specifications = ( ( ParameterContainer ) n ).getEmbeddedParameters();
//				if ( specifications != null ) {
//					collectedParameters.addAll( Arrays.asList( specifications ) );
//				}
//			}
//		}
	}
    
}
