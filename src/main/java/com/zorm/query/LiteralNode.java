package com.zorm.query;

import com.zorm.type.StandardBasicTypes;
import com.zorm.type.Type;
import com.zorm.util.ColumnHelper;

import antlr.SemanticException;

public class LiteralNode extends AbstractSelectExpression implements SqlTokenTypes {

	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateSingleScalarColumn( this, i );
	}

	public Type getDataType() {
		switch ( getType() ) {
			case NUM_INT:
				return StandardBasicTypes.INTEGER;
//			case NUM_FLOAT:
//				return StandardBasicTypes.FLOAT;
			case NUM_LONG:
				return StandardBasicTypes.LONG;
//			case NUM_DOUBLE:
//				return StandardBasicTypes.DOUBLE;
//			case NUM_BIG_INTEGER:
//				return StandardBasicTypes.BIG_INTEGER;
//			case NUM_BIG_DECIMAL:
//				return StandardBasicTypes.BIG_DECIMAL;
			case QUOTED_STRING:
				return StandardBasicTypes.STRING;
			case TRUE:
			case FALSE:
				return StandardBasicTypes.BOOLEAN;
			default:
				return null;
		}
	}
}