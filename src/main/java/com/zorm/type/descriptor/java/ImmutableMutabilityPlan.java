package com.zorm.type.descriptor.java;

import java.io.Serializable;

/**
 * Mutability plan for immutable objects
 *
 * @author Steve Ebersole
 */
public class ImmutableMutabilityPlan<T> implements MutabilityPlan<T> {
	public static final ImmutableMutabilityPlan INSTANCE = new ImmutableMutabilityPlan();

	/**
	 * {@inheritDoc}
	 */
	public boolean isMutable() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public T deepCopy(T value) {
		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	public Serializable disassemble(T value) {
		return (Serializable) value;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public T assemble(Serializable cached) {
		return (T) cached;
	}
}
