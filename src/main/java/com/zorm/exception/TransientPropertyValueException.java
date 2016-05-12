package com.zorm.exception;

import com.zorm.util.StringHelper;

public class TransientPropertyValueException extends TransientObjectException {
	private final String transientEntityName;
	private final String propertyOwnerEntityName;
	private final String propertyName;

	/**
	 * Constructs an {@link TransientPropertyValueException} instance.
	 *
	 * @param message - the exception message;
	 * @param transientEntityName - the entity name for the transient entity
	 * @param propertyOwnerEntityName - the entity name for entity that owns
	 * the association property.
	 * @param propertyName - the property name
	 */
	public TransientPropertyValueException(
			String message, 
			String transientEntityName, 
			String propertyOwnerEntityName, 
			String propertyName) {
		super(message);
		this.transientEntityName = transientEntityName;
		this.propertyOwnerEntityName = propertyOwnerEntityName;
		this.propertyName = propertyName;
	}

	/**
	 * Returns the entity name for the transient entity.
	 * @return the entity name for the transient entity.
	 */
	public String getTransientEntityName() {
		return transientEntityName;
	}

	/**
	 * Returns the entity name for entity that owns the association
	 * property.
	 * @return the entity name for entity that owns the association
	 * property
	 */
	public String getPropertyOwnerEntityName() {
		return propertyOwnerEntityName;
	}

	/**
	 * Returns the property name.
	 * @return the property name.
	 */
	public String getPropertyName() {
		return propertyName;
	}


	/**
	 * Return the exception message.
	 * @return the exception message.
	 */
	@Override
	public String getMessage() {
		return new StringBuilder( super.getMessage() )
				.append( ": " )
				.append( StringHelper.qualify( propertyOwnerEntityName, propertyName ) )
				.append( " -> " )
				.append( transientEntityName )
				.toString();
	}
}
