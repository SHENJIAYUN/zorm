package com.zorm.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.zorm.engine.NonNullableTransientDependencies;
import com.zorm.engine.Status;
import com.zorm.entity.EntityEntry;
import com.zorm.exception.PropertyValueException;
import com.zorm.exception.TransientPropertyValueException;
import com.zorm.session.SessionImplementor;
import com.zorm.util.IdentitySet;
import com.zorm.util.MessageHelper;

public class UnresolvedEntityInsertActions {
	private static final int INIT_SIZE = 5;

	private final Map<AbstractEntityInsertAction,NonNullableTransientDependencies> dependenciesByAction =
			new IdentityHashMap<AbstractEntityInsertAction,NonNullableTransientDependencies>( INIT_SIZE );
	private final Map<Object,Set<AbstractEntityInsertAction>> dependentActionsByTransientEntity =
			new IdentityHashMap<Object,Set<AbstractEntityInsertAction>>( INIT_SIZE );

	/**
	 * Add an unresolved insert action.
	 *
	 * @param insert - unresolved insert action.
	 * @param dependencies - non-nullable transient dependencies
	 *                       (must be non-null and non-empty).
	 *
	 * @throws IllegalArgumentException if {@code dependencies is null or empty}.
	 */
	public void addUnresolvedEntityInsertAction(AbstractEntityInsertAction insert, NonNullableTransientDependencies dependencies) {
		if ( dependencies == null || dependencies.isEmpty() ) {
			throw new IllegalArgumentException(
					"Attempt to add an unresolved insert action that has no non-nullable transient entities."
			);
		}
		dependenciesByAction.put( insert, dependencies );
		addDependenciesByTransientEntity( insert, dependencies );
	}

	/**
	 * Returns the unresolved insert actions.
	 * @return the unresolved insert actions.
	 */
	public Iterable<AbstractEntityInsertAction> getDependentEntityInsertActions() {
		return dependenciesByAction.keySet();
	}

	/**
	 * Throws {@link org.hibernate.PropertyValueException} if there are any unresolved
	 * entity insert actions that depend on non-nullable associations with
	 * a transient entity. This method should be called on completion of
	 * an operation (after all cascades are completed) that saves an entity.
	 *
	 * @throws org.hibernate.PropertyValueException if there are any unresolved entity
	 * insert actions; {@link org.hibernate.PropertyValueException#getEntityName()}
	 * and {@link org.hibernate.PropertyValueException#getPropertyName()} will
	 * return the entity name and property value for the first unresolved
	 * entity insert action.
	 */
	public void checkNoUnresolvedActionsAfterOperation() throws PropertyValueException {
		if ( isEmpty() ) {
		}
		else {
			AbstractEntityInsertAction firstDependentAction =
					dependenciesByAction.keySet().iterator().next();

			logCannotResolveNonNullableTransientDependencies( firstDependentAction.getSession() );

			NonNullableTransientDependencies nonNullableTransientDependencies =
					dependenciesByAction.get( firstDependentAction );
			Object firstTransientDependency =
					nonNullableTransientDependencies.getNonNullableTransientEntities().iterator().next();
			String firstPropertyPath =
					nonNullableTransientDependencies.getNonNullableTransientPropertyPaths( firstTransientDependency ).iterator().next();
			throw new TransientPropertyValueException(
					"Not-null property references a transient value - transient instance must be saved before current operation",
					firstDependentAction.getSession().guessEntityName( firstTransientDependency ),
					firstDependentAction.getEntityName(),
					firstPropertyPath
			);
		}
	}

	private void logCannotResolveNonNullableTransientDependencies(SessionImplementor session) {
		for ( Map.Entry<Object,Set<AbstractEntityInsertAction>> entry : dependentActionsByTransientEntity.entrySet() ) {
			Object transientEntity = entry.getKey();
			String transientEntityName = session.guessEntityName( transientEntity );
			Serializable transientEntityId = session.getFactory().getEntityPersister( transientEntityName ).getIdentifier( transientEntity, session );
			String transientEntityString = MessageHelper.infoString( transientEntityName, transientEntityId );
			Set<String> dependentEntityStrings = new TreeSet<String>();
			Set<String> nonNullableTransientPropertyPaths = new TreeSet<String>();
			for ( AbstractEntityInsertAction dependentAction : entry.getValue() ) {
				dependentEntityStrings.add( MessageHelper.infoString( dependentAction.getEntityName(), dependentAction.getId() ) );
				for ( String path : dependenciesByAction.get( dependentAction ).getNonNullableTransientPropertyPaths( transientEntity ) ) {
					String fullPath = new StringBuilder( dependentAction.getEntityName().length() + path.length() + 1 )
							.append( dependentAction.getEntityName() )
							.append( '.' )
							.append( path )
							.toString();
					nonNullableTransientPropertyPaths.add( fullPath );
				}
			}
		}
	}

	/**
	 * Returns true if there are no unresolved entity insert actions.
	 * @return true, if there are no unresolved entity insert actions; false, otherwise.
	 */
	public boolean isEmpty() {
		return dependenciesByAction.isEmpty();
	}

	@SuppressWarnings({ "unchecked" })
	private void addDependenciesByTransientEntity(AbstractEntityInsertAction insert, NonNullableTransientDependencies dependencies) {
		for ( Object transientEntity : dependencies.getNonNullableTransientEntities() ) {
			Set<AbstractEntityInsertAction> dependentActions = dependentActionsByTransientEntity.get( transientEntity );
			if ( dependentActions == null ) {
				dependentActions = new IdentitySet();
				dependentActionsByTransientEntity.put( transientEntity, dependentActions );
			}
			dependentActions.add( insert );
		}
	}

	/**
	 * Resolve any dependencies on {@code managedEntity}.
	 *
	 * @param managedEntity - the managed entity name
	 * @param session - the session
	 *
	 * @return the insert actions that depended only on the specified entity.
	 *
	 * @throws IllegalArgumentException if {@code managedEntity} did not have managed or read-only status.
	 */
	@SuppressWarnings({ "unchecked" })
	public Set<AbstractEntityInsertAction> resolveDependentActions(Object managedEntity, SessionImplementor session) {
		EntityEntry entityEntry = session.getPersistenceContext().getEntry( managedEntity );
		if ( entityEntry.getStatus() != Status.MANAGED && entityEntry.getStatus() != Status.READ_ONLY ) {
			throw new IllegalArgumentException( "EntityEntry did not have status MANAGED or READ_ONLY: " + entityEntry );
		}
		// Find out if there are any unresolved insertions that are waiting for the
		// specified entity to be resolved.
		Set<AbstractEntityInsertAction> dependentActions = dependentActionsByTransientEntity.remove( managedEntity );
		if ( dependentActions == null ) {
			return Collections.emptySet();  //NOTE EARLY EXIT!
		}
		Set<AbstractEntityInsertAction> resolvedActions = new IdentitySet(  );
		for ( AbstractEntityInsertAction dependentAction : dependentActions ) {
			NonNullableTransientDependencies dependencies = dependenciesByAction.get( dependentAction );
			dependencies.resolveNonNullableTransientEntity( managedEntity );
			if ( dependencies.isEmpty() ) {
				// dependentAction only depended on managedEntity..
				dependenciesByAction.remove( dependentAction );
				resolvedActions.add( dependentAction );
			}
		}
		return resolvedActions;
	}

	/**
	 * Clear this {@link UnresolvedEntityInsertActions}.
	 */
	public void clear() {
		dependenciesByAction.clear();
		dependentActionsByTransientEntity.clear();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( '[' );
		for ( Map.Entry<AbstractEntityInsertAction,NonNullableTransientDependencies> entry : dependenciesByAction.entrySet() ) {
			AbstractEntityInsertAction insert = entry.getKey();
			NonNullableTransientDependencies dependencies = entry.getValue();
			sb.append( "[insert=" )
					.append( insert )
					.append( " dependencies=[" )
					.append( dependencies.toLoggableString( insert.getSession() ) )
					.append( "]" );
		}
		sb.append( ']');
		return sb.toString();
	}

	/**
	 * Serialize this {@link UnresolvedEntityInsertActions} object.
	 * @param oos - the output stream
	 * @throws IOException if there is an error writing this object to the output stream.
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		int queueSize = dependenciesByAction.size();
		oos.writeInt( queueSize );
		for ( AbstractEntityInsertAction unresolvedAction : dependenciesByAction.keySet() ) {
			oos.writeObject( unresolvedAction );
		}
	}

	/**
	 * Deerialize a {@link UnresolvedEntityInsertActions} object.
	 *
	 * @param ois - the input stream.
	 * @param session - the session.
	 *
	 * @return the deserialized  {@link UnresolvedEntityInsertActions} object
	 * @throws IOException if there is an error writing this object to the output stream.
	 * @throws ClassNotFoundException if there is a class that cannot be loaded.
	 */
	public static UnresolvedEntityInsertActions deserialize(
			ObjectInputStream ois,
			SessionImplementor session) throws IOException, ClassNotFoundException {

		UnresolvedEntityInsertActions rtn = new UnresolvedEntityInsertActions();

		int queueSize = ois.readInt();
		for ( int i = 0; i < queueSize; i++ ) {
			AbstractEntityInsertAction unresolvedAction = ( AbstractEntityInsertAction ) ois.readObject();
			unresolvedAction.afterDeserialize( session );
//			rtn.addUnresolvedEntityInsertAction(
//					unresolvedAction,
//					unresolvedAction.findNonNullableTransientEntities()
//			);
		}
		return rtn;
	}
}
