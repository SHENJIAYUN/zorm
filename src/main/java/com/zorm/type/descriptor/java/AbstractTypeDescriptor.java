package com.zorm.type.descriptor.java;

import java.io.Serializable;
import java.util.Comparator;

import com.zorm.exception.ZormException;
import com.zorm.util.ComparableComparator;
import com.zorm.util.EqualsHelper;

/**
 * Abstract adapter for Java type descriptors.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTypeDescriptor<T> implements JavaTypeDescriptor<T>, Serializable {
	private final Class<T> type;
	private final MutabilityPlan<T> mutabilityPlan;
	private final Comparator<T> comparator;

	/**
	 * Initialize a type descriptor for the given type.  Assumed immutable.
	 *
	 * @param type The Java type.
	 *
	 * @see #AbstractTypeDescriptor(Class, MutabilityPlan)
	 */
	@SuppressWarnings({ "unchecked" })
	protected AbstractTypeDescriptor(Class<T> type) {
		this( type, (MutabilityPlan<T>) ImmutableMutabilityPlan.INSTANCE );
	}

	/**
	 * Initialize a type descriptor for the given type.  Assumed immutable.
	 *
	 * @param type The Java type.
	 * @param mutabilityPlan The plan for handling mutability aspects of the java type.
	 */
	@SuppressWarnings({ "unchecked" })
	protected AbstractTypeDescriptor(Class<T> type, MutabilityPlan<T> mutabilityPlan) {
		this.type = type;
		this.mutabilityPlan = mutabilityPlan;
		this.comparator = Comparable.class.isAssignableFrom( type )
				? (Comparator<T>) ComparableComparator.INSTANCE
				: null;
	}

	/**
	 * {@inheritDoc}
	 */
	public MutabilityPlan<T> getMutabilityPlan() {
		return mutabilityPlan;
	}

	/**
	 * {@inheritDoc}
	 */
	public Class<T> getJavaTypeClass() {
		return type;
	}

	/**
	 * {@inheritDoc}
	 */
	public int extractHashCode(T value) {
		return value.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean areEqual(T one, T another) {
		return EqualsHelper.equals( one, another );
	}

	/**
	 * {@inheritDoc}
	 */
	public Comparator<T> getComparator() {
		return comparator;
	}

	/**
	 * {@inheritDoc}
	 */
	public String extractLoggableRepresentation(T value) {
		return (value == null) ? "null" : value.toString();
	}

	protected ZormException unknownUnwrap(Class conversionType) {
		throw new ZormException(
				"Unknown unwrap conversion requested: " + type.getName() + " to " + conversionType.getName()
		);
	}

	protected ZormException unknownWrap(Class conversionType) {
		throw new ZormException(
				"Unknown wrap conversion requested: " + conversionType.getName() + " to " + type.getName()
		);
	}
}