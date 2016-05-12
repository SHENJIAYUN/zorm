package com.zorm.util;

import java.io.Serializable;

public final  class IdentifierGeneratorHelper {
	public static final Serializable SHORT_CIRCUIT_INDICATOR = new Serializable() {
		@Override
		public String toString() {
			return "SHORT_CIRCUIT_INDICATOR";
		}
	};
	
	public static final Serializable POST_INSERT_INDICATOR = new Serializable() {
		@Override
		public String toString() {
			return "POST_INSERT_INDICATOR";
		}
	};
}
