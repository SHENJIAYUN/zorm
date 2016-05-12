package com.zorm.exception;

import java.sql.SQLException;

public class SQLStateConverter extends StandardSQLExceptionConverter implements SQLExceptionConverter {
	public SQLStateConverter(final ViolatedConstraintNameExtracter extracter) {
		super();
		final ConversionContext conversionContext = new ConversionContext() {
			@Override
			public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
				return extracter;
			}
		};
		addDelegate( new SQLStateConversionDelegate( conversionContext ) );
	}
}
