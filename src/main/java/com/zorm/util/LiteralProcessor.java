package com.zorm.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import antlr.collections.AST;

import com.zorm.exception.ZormException;
import com.zorm.query.SqlWalker;
import com.zorm.query.TokenTypes;

public class LiteralProcessor implements TokenTypes{
	
	private static final Log log = LogFactory.getLog(LiteralProcessor.class);
	
	private SqlWalker walker;
	public static DecimalLiteralFormat DECIMAL_LITERAL_FORMAT = DecimalLiteralFormat.EXACT;

	public LiteralProcessor(SqlWalker hqlSqlWalker) {
		this.walker = hqlSqlWalker;
	}

	public void processNumeric(AST literal) {
		if ( literal.getType() == NUM_INT
				|| literal.getType() == NUM_LONG
				|| literal.getType() == NUM_BIG_INTEGER ) {
			literal.setText( determineIntegerRepresentation( literal.getText(), literal.getType() ) );
        } else if (literal.getType() == NUM_FLOAT
				|| literal.getType() == NUM_DOUBLE
				|| literal.getType() == NUM_BIG_DECIMAL ) {
			literal.setText( determineDecimalRepresentation( literal.getText(), literal.getType() ) );
        } else {
        	log.debug("Unexpected literal token type ["+literal.getType()+"] passed for numeric processing");
        }
	}
	
	public String determineDecimalRepresentation(String text, int type) {
		String literalValue = text;
		if ( type == NUM_FLOAT ) {
			if ( literalValue.endsWith( "f" ) || literalValue.endsWith( "F" ) ) {
				literalValue = literalValue.substring( 0, literalValue.length() - 1 );
			}
		}
		else if ( type == NUM_DOUBLE ) {
			if ( literalValue.endsWith( "d" ) || literalValue.endsWith( "D" ) ) {
				literalValue = literalValue.substring( 0, literalValue.length() - 1 );
			}
		}
		else if ( type == NUM_BIG_DECIMAL ) {
			if ( literalValue.endsWith( "bd" ) || literalValue.endsWith( "BD" ) ) {
				literalValue = literalValue.substring( 0, literalValue.length() - 2 );
			}
		}

		final BigDecimal number;
		try {
			number = new BigDecimal( literalValue );
		}
		catch( Throwable t ) {
			throw new ZormException( "Could not parse literal [" + text + "] as big-decimal", t );
		}

		return DECIMAL_LITERAL_FORMAT.getFormatter().format( number );
	}
	
	private String determineIntegerRepresentation(String text, int type) {
		try {
			if ( type == NUM_BIG_INTEGER ) {
				String literalValue = text;
				if ( literalValue.endsWith( "bi" ) || literalValue.endsWith( "BI" ) ) {
					literalValue = literalValue.substring( 0, literalValue.length() - 2 );
				}
				return new BigInteger( literalValue ).toString();
			}
			if ( type == NUM_INT ) {
				try {
					return Integer.valueOf( text ).toString();
				}
				catch( NumberFormatException e ) {
					log.debug("Could not format incoming text ["+text+"] as a NUM_INT; assuming numeric overflow and attempting as NUM_LONG");
				}
			}
			String literalValue = text;
			if ( literalValue.endsWith( "l" ) || literalValue.endsWith( "L" ) ) {
				literalValue = literalValue.substring( 0, literalValue.length() - 1 );
			}
			return Long.valueOf( literalValue ).toString();
		}
		catch( Throwable t ) {
			throw new ZormException( "Could not parse literal [" + text + "] as integer", t );
		}
	}
	
	public static enum DecimalLiteralFormat {
		/**
		 * Indicates that Float and Double literal values should
		 * be treated using the SQL "exact" format (i.e., '.001')
		 */
		EXACT {
			@Override
			public DecimalFormatter getFormatter() {
				return ExactDecimalFormatter.INSTANCE;
			}
		},
		/**
		 * Indicates that Float and Double literal values should
		 * be treated using the SQL "approximate" format (i.e., '1E-3')
		 */
		@SuppressWarnings( {"UnusedDeclaration"})
		APPROXIMATE {
			@Override
			public DecimalFormatter getFormatter() {
				return ApproximateDecimalFormatter.INSTANCE;
			}
		};

		public abstract DecimalFormatter getFormatter();
	}
	
	private static interface DecimalFormatter {
		String format(BigDecimal number);
	}
	
	private static class ExactDecimalFormatter implements DecimalFormatter {
		public static final ExactDecimalFormatter INSTANCE = new ExactDecimalFormatter();

		public String format(BigDecimal number) {
			return number.toString();
		}
	}
	
	private static class ApproximateDecimalFormatter implements DecimalFormatter {
		public static final ApproximateDecimalFormatter INSTANCE = new ApproximateDecimalFormatter();

		private static final String FORMAT_STRING = "#0.0E0";

		public String format(BigDecimal number) {
			try {
				// TODO : what amount of significant digits need to be supported here?
				//      - from the DecimalFormat docs:
				//          [significant digits] = [minimum integer digits] + [maximum fraction digits]
				DecimalFormat jdkFormatter = new DecimalFormat( FORMAT_STRING );
				jdkFormatter.setMinimumIntegerDigits( 1 );
				jdkFormatter.setMaximumFractionDigits( Integer.MAX_VALUE );
				return jdkFormatter.format( number );
			}
			catch( Throwable t ) {
				throw new ZormException( "Unable to format decimal literal in approximate format [" + number.toString() + "]", t );
			}
		}
	}
}
