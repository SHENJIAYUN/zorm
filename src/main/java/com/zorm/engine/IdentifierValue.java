package com.zorm.engine;

import java.io.Serializable;

public class IdentifierValue implements UnsavedValueStrategy{
	private final Serializable value;
	
	protected IdentifierValue() {
		this.value = null;
	}
	
	public IdentifierValue(Serializable value) {
		this.value = value;
	}

	public Serializable getDefaultValue(Object currentId) {
		return value;
	}
	
	/**
	 * Assume the transient instance is newly instantiated if
	 * its identifier is null or equal to <tt>value</tt>
	 */

	/**
	 * Does the given identifier belong to a new instance?
	 */
	public Boolean isUnsaved(Object id) {
		return id==null || id.equals(value);
	}

	@Override
	public String toString() {
		return "identifier unsaved-value: " + value;
	}
	
	public static final IdentifierValue ANY = new IdentifierValue() {
		@Override
		public final Boolean isUnsaved(Object id) {
			return Boolean.TRUE;
		}
		@Override
		public Serializable getDefaultValue(Object currentValue) {
			return (Serializable) currentValue;
		}
		@Override
		public String toString() {
			return "SAVE_ANY";
		}
	};

	/**
	 * Never assume the transient instance is newly instantiated
	 */
	public static final IdentifierValue NONE = new IdentifierValue() {
		@Override
		public final Boolean isUnsaved(Object id) {
			return Boolean.FALSE;
		}
		@Override
		public Serializable getDefaultValue(Object currentValue) {
			return (Serializable) currentValue;
		}
		@Override
		public String toString() {
			return "SAVE_NONE";
		}
	};

	/**
	 * Assume the transient instance is newly instantiated if the identifier
	 * is null.
	 */
	public static final IdentifierValue NULL = new IdentifierValue() {
		@Override
		public final Boolean isUnsaved(Object id) {
			return id==null;
		}
		@Override
		public Serializable getDefaultValue(Object currentValue) {
			return null;
		}
		@Override
		public String toString() {
			return "SAVE_NULL";
		}
	};

	/**
	 * Assume nothing.
	 */
	public static final IdentifierValue UNDEFINED = new IdentifierValue() {
		@Override
		public final Boolean isUnsaved(Object id) {
			return null;
		}
		@Override
		public Serializable getDefaultValue(Object currentValue) {
			return null;
		}
		@Override
		public String toString() {
			return "UNDEFINED";
		}
	};
}
