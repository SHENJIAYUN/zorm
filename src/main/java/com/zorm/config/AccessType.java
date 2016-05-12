package com.zorm.config;

public enum AccessType {
	/**
	 * Default access strategy is property
	 */
	DEFAULT( "property" ),

	/**
	 * Access to value via property
	 */
	PROPERTY( "property" ),

	/**
	 * Access to value via field
	 */
	FIELD( "field" );

	private final String accessType;

	AccessType(String type) {
		this.accessType = type;
	}

	public String getType() {
		return accessType;
	}

	public static AccessType getAccessStrategy(javax.persistence.AccessType type) {
		if ( type == null ) {
			return DEFAULT;
		}
		else if ( FIELD.getType().equals( type ) ) {
			return FIELD;
		}
		else if ( PROPERTY.getType().equals( type ) ) {
			return PROPERTY;
		}
		else {
			// TODO historically if the type string could not be matched default access was used. Maybe this should be an exception though!?
			return DEFAULT;
		}
	}

	public static AccessType getAccessStrategy(String type) {
        if(type==null){
        	return DEFAULT;
        }
        else if(FIELD.getType().equals(type)){
        	return FIELD;
        }
        else if(PROPERTY.getType().equals(type)){
        	return PROPERTY;
        }else{
        	return DEFAULT;
        }
	}
}
