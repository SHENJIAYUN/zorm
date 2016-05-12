package com.zorm.query;

import antlr.RecognitionException;

public interface ErrorReporter {
	void reportError(RecognitionException e);

	void reportError(String s);

	void reportWarning(String s);
}
