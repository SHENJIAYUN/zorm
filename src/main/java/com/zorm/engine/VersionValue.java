package com.zorm.engine;

import com.zorm.exception.MappingException;
import com.zorm.util.IdentifierGeneratorHelper;

public class VersionValue implements UnsavedValueStrategy {


	private final Object value;
	/**
	 * Assume the transient instance is newly instantiated if the version
	 * is null, otherwise assume it is a detached instance.
	 */
	public static final VersionValue NULL = new VersionValue() {
		@Override
		public final Boolean isUnsaved(Object version) {
			return version==null;
		}
		@Override
		public Object getDefaultValue(Object currentValue) {
			return null;
		}
		@Override
		public String toString() {
			return "VERSION_SAVE_NULL";
		}
	};
	/**
	 * Assume the transient instance is newly instantiated if the version
	 * is null, otherwise defer to the identifier unsaved-value.
	 */
	public static final VersionValue UNDEFINED = new VersionValue() {
		@Override
		public final Boolean isUnsaved(Object version) {
			return version==null ? Boolean.TRUE : null;
		}
		@Override
		public Object getDefaultValue(Object currentValue) {
			return currentValue;
		}
		@Override
		public String toString() {
			return "VERSION_UNDEFINED";
		}
	};
	/**
	 * Assume the transient instance is newly instantiated if the version
	 * is negative, otherwise assume it is a detached instance.
	 */
	public static final VersionValue NEGATIVE = new VersionValue() {

		@Override
		public final Boolean isUnsaved(Object version) throws MappingException {
			if (version==null) return Boolean.TRUE;
			if ( version instanceof Number ) {
				return ( (Number) version ).longValue() < 0l;
			}
			throw new MappingException( "unsaved-value NEGATIVE may only be used with short, int and long types" );
		}
		
		@Override
		public Object getDefaultValue(Object currentValue) {
//			return IdentifierGeneratorHelper.getIntegralDataTypeHolder( currentValue.getClass() )
//					.initialize( -1L )
//					.makeValue();
			return null;
		}
		@Override
		public String toString() {
			return "VERSION_NEGATIVE";
		}
	};

	protected VersionValue() {
		this.value = null;
	}

	/**
	 * Assume the transient instance is newly instantiated if
	 * its version is null or equal to <tt>value</tt>
	 * @param value value to compare to
	 */
	public VersionValue(Object value) {
		this.value = value;
	}

	@Override
	public Boolean isUnsaved(Object version) throws MappingException  {
		return version==null || version.equals(value);
	}

	@Override
	public Object getDefaultValue(Object currentValue) {
		return value;
	}

	@Override
    public String toString() {
		return "version unsaved-value: " + value;
	}

}
