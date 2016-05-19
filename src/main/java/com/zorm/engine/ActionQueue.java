package com.zorm.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.zorm.action.AbstractEntityInsertAction;
import com.zorm.action.AfterTransactionCompletionProcess;
import com.zorm.action.BeforeTransactionCompletionProcess;
import com.zorm.action.BulkOperationCleanupAction;
import com.zorm.action.CollectionRecreateAction;
import com.zorm.action.CollectionRemoveAction;
import com.zorm.action.CollectionUpdateAction;
import com.zorm.action.EntityDeleteAction;
import com.zorm.action.EntityInsertAction;
import com.zorm.action.EntityUpdateAction;
import com.zorm.action.Executable;
import com.zorm.action.UnresolvedEntityInsertActions;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.ZormException;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;

public class ActionQueue {
	private static final int INIT_QUEUE_LIST_SIZE = 5;

	private SessionImplementor session;

	// Object insertions, updates, and deletions have list semantics because
	// they must happen in the right order so as to respect referential
	// integrity
	private UnresolvedEntityInsertActions unresolvedInsertions;
	private ArrayList insertions;
	private ArrayList<EntityDeleteAction> deletions;
	private ArrayList updates;
	private ArrayList collectionCreations;
	private ArrayList collectionUpdates;
	private ArrayList collectionRemovals;

	private AfterTransactionCompletionProcessQueue afterTransactionProcesses;
	private BeforeTransactionCompletionProcessQueue beforeTransactionProcesses;

	/**
	 * Constructs an action queue bound to the given session.
	 *
	 * @param session SessionImpl实例
	 */
	public ActionQueue(SessionImplementor session) {
		this.session = session;
		init();
	}

	private void init() {
		unresolvedInsertions = new UnresolvedEntityInsertActions();
		insertions = new ArrayList<AbstractEntityInsertAction>( INIT_QUEUE_LIST_SIZE );
		deletions = new ArrayList<EntityDeleteAction>( INIT_QUEUE_LIST_SIZE );
		updates = new ArrayList( INIT_QUEUE_LIST_SIZE );

		collectionCreations = new ArrayList( INIT_QUEUE_LIST_SIZE );
		collectionRemovals = new ArrayList( INIT_QUEUE_LIST_SIZE );
		collectionUpdates = new ArrayList( INIT_QUEUE_LIST_SIZE );

		afterTransactionProcesses = new AfterTransactionCompletionProcessQueue( session );
		beforeTransactionProcesses = new BeforeTransactionCompletionProcessQueue( session );
	}

	public void clear() {
		updates.clear();
		insertions.clear();
		deletions.clear();

		collectionCreations.clear();
		collectionRemovals.clear();
		collectionUpdates.clear();

		unresolvedInsertions.clear();
	}

	@SuppressWarnings({ "unchecked" })
	public void addAction(EntityDeleteAction action) {
		deletions.add( action );
	}

//	@SuppressWarnings({ "unchecked" })
//	public void addAction(EntityUpdateAction action) {
//		updates.add( action );
//	}

	public boolean hasUnresolvedEntityInsertActions() {
		return ! unresolvedInsertions.isEmpty();
	}


	/**
	 * Execute any registered {@link org.hibernate.action.spi.BeforeTransactionCompletionProcess}
	 */
	public void beforeTransactionCompletion() {
		beforeTransactionProcesses.beforeTransactionCompletion();
	}

	/**
	 * Check whether any insertion or deletion actions are currently queued.
	 *
	 * @return True if insertions or deletions are currently queued; false otherwise.
	 */
	public boolean areInsertionsOrDeletionsQueued() {
		return ( insertions.size() > 0 || ! unresolvedInsertions.isEmpty() || deletions.size() > 0 );
	}

	/**
	 * Returns a string representation of the object.
	 *
	 * @return a string representation of the object.
	 */
	@Override
    public String toString() {
		return new StringBuilder()
				.append( "ActionQueue[insertions=" ).append( insertions )
				.append( " updates=" ).append( updates )
				.append( " deletions=" ).append( deletions )
				.append( " collectionCreations=" ).append( collectionCreations )
				.append( " collectionRemovals=" ).append( collectionRemovals )
				.append( " collectionUpdates=" ).append( collectionUpdates )
				.append( " unresolvedInsertDependencies=" ).append( unresolvedInsertions )
				.append( "]" )
				.toString();
	}

	public int numberOfCollectionRemovals() {
		return collectionRemovals.size();
	}

	public int numberOfCollectionUpdates() {
		return collectionUpdates.size();
	}

	public int numberOfCollectionCreations() {
		return collectionCreations.size();
	}

	public int numberOfDeletions() {
		return deletions.size();
	}

	public int numberOfUpdates() {
		return updates.size();
	}

	public int numberOfInsertions() {
		return insertions.size();
	}

	/**
	 * Order the {@link #insertions} queue such that we group inserts
	 * against the same entity together (without violating constraints).  The
	 * original order is generated by cascade order, which in turn is based on
	 * the directionality of foreign-keys.  So even though we will be changing
	 * the ordering here, we need to make absolutely certain that we do not
	 * circumvent this FK ordering to the extent of causing constraint
	 * violations
	 */
	private void sortInsertActions() {
		new InsertActionSorter().sort();
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	public ArrayList cloneDeletions() {
		return ( ArrayList ) deletions.clone();
	}

	public void clearFromFlushNeededCheck(int previousCollectionRemovalSize) {
		collectionCreations.clear();
		collectionUpdates.clear();
		updates.clear();
		// collection deletions are a special case since update() can add
		// deletions of collections not loaded by the session.
		for ( int i = collectionRemovals.size() - 1; i >= previousCollectionRemovalSize; i-- ) {
			collectionRemovals.remove( i );
		}
	}

	public boolean hasAnyQueuedActions() {
		return updates.size() > 0 ||
				insertions.size() > 0 ||
				! unresolvedInsertions.isEmpty() ||
				deletions.size() > 0 ||
				collectionUpdates.size() > 0 ||
				collectionRemovals.size() > 0 ||
				collectionCreations.size() > 0;
	}

	private static class BeforeTransactionCompletionProcessQueue {
		private SessionImplementor session;
		private List<BeforeTransactionCompletionProcess> processes = new ArrayList<BeforeTransactionCompletionProcess>();

		private BeforeTransactionCompletionProcessQueue(SessionImplementor session) {
			this.session = session;
		}

		public void register(BeforeTransactionCompletionProcess process) {
			if ( process == null ) {
				return;
			}
			processes.add( process );
		}

		public void beforeTransactionCompletion() {
			for ( BeforeTransactionCompletionProcess process : processes ) {
				try {
					process.doBeforeTransactionCompletion( session );
				}
				catch (ZormException he) {
					throw he;
				}
				catch (Exception e) {
					throw new AssertionFailure( "Unable to perform beforeTransactionCompletion callback", e );
				}
			}
			processes.clear();
		}
	}

	private static class AfterTransactionCompletionProcessQueue {
		private SessionImplementor session;
		private Set<String> querySpacesToInvalidate = new HashSet<String>();
		private List<AfterTransactionCompletionProcess> processes
				= new ArrayList<AfterTransactionCompletionProcess>( INIT_QUEUE_LIST_SIZE * 3 );

		private AfterTransactionCompletionProcessQueue(SessionImplementor session) {
			this.session = session;
		}

		public void addSpacesToInvalidate(String[] spaces) {
			for ( String space : spaces ) {
				addSpaceToInvalidate( space );
			}
		}

		public void addSpaceToInvalidate(String space) {
			querySpacesToInvalidate.add( space );
		}

		public void register(AfterTransactionCompletionProcess process) {
			if ( process == null ) {
				return;
			}
			processes.add( process );
		}

		public void afterTransactionCompletion(boolean successful) {
			processes.clear();
			querySpacesToInvalidate.clear();
		}

	}

	/**
	 * Sorts the insert actions using more hashes.
	 *
	 * @author Jay Erb
	 */
	private class InsertActionSorter {
		// the mapping of entity names to their latest batch numbers.
		private HashMap<String,Integer> latestBatches = new HashMap<String,Integer>();
		private HashMap<Object,Integer> entityBatchNumber;

		// the map of batch numbers to EntityInsertAction lists
		private HashMap<Integer,List<EntityInsertAction>> actionBatches = new HashMap<Integer,List<EntityInsertAction>>();

		public InsertActionSorter() {
			//optimize the hash size to eliminate a rehash.
			entityBatchNumber = new HashMap<Object,Integer>( insertions.size() + 1, 1.0f );
		}

		/**
		 * Sort the insert actions.
		 */
		@SuppressWarnings({ "unchecked", "UnnecessaryBoxing" })
		public void sort() {
			// the list of entity names that indicate the batch number
			for ( EntityInsertAction action : (List<EntityInsertAction>) insertions ) {
				// remove the current element from insertions. It will be added back later.
				String entityName = action.getEntityName();

				// the entity associated with the current action.
				Object currentEntity = action.getInstance();

				Integer batchNumber;
				if ( latestBatches.containsKey( entityName ) ) {
					// There is already an existing batch for this type of entity.
					// Check to see if the latest batch is acceptable.
					batchNumber = findBatchNumber( action, entityName );
				}
				else {
					// add an entry for this type of entity.
					// we can be assured that all referenced entities have already
					// been processed,
					// so specify that this entity is with the latest batch.
					// doing the batch number before adding the name to the list is
					// a faster way to get an accurate number.

					batchNumber = actionBatches.size();
					latestBatches.put( entityName, batchNumber );
				}
				entityBatchNumber.put( currentEntity, batchNumber );
				addToBatch( batchNumber, action );
			}
			insertions.clear();

			// now rebuild the insertions list. There is a batch for each entry in the name list.
			for ( int i = 0; i < actionBatches.size(); i++ ) {
				List<EntityInsertAction> batch = actionBatches.get( i );
				for ( EntityInsertAction action : batch ) {
					insertions.add( action );
				}
			}
		}

		/**
		 * Finds an acceptable batch for this entity to be a member as part of the {@link InsertActionSorter}
		 *
		 * @param action The action being sorted
		 * @param entityName The name of the entity affected by the action
		 *
		 * @return An appropriate batch number; todo document this process better
		 */
		private Integer findBatchNumber(
				EntityInsertAction action,
				String entityName) {
			// loop through all the associated entities and make sure they have been
			// processed before the latest
			// batch associated with this entity type.

			// the current batch number is the latest batch for this entity type.
			Integer latestBatchNumberForType = latestBatches.get( entityName );

			// loop through all the associations of the current entity and make sure that they are processed
			// before the current batch number
			Object[] propertyValues = action.getState();
			Type[] propertyTypes = action.getPersister().getClassMetadata()
					.getPropertyTypes();

			for ( int i = 0; i < propertyValues.length; i++ ) {
				Object value = propertyValues[i];
				Type type = propertyTypes[i];
				if ( type.isEntityType() && value != null ) {
					// find the batch number associated with the current association, if any.
					Integer associationBatchNumber = entityBatchNumber.get( value );
					if ( associationBatchNumber != null && associationBatchNumber.compareTo( latestBatchNumberForType ) > 0 ) {
						// create a new batch for this type. The batch number is the number of current batches.
						latestBatchNumberForType = actionBatches.size();
						latestBatches.put( entityName, latestBatchNumberForType );
						// since this entity will now be processed in the latest possible batch,
						// we can be assured that it will come after all other associations,
						// there's not need to continue checking.
						break;
					}
				}
			}
			return latestBatchNumberForType;
		}

		private void addToBatch(Integer batchNumber, EntityInsertAction action) {
			List<EntityInsertAction> actions = actionBatches.get( batchNumber );

			if ( actions == null ) {
				actions = new LinkedList<EntityInsertAction>();
				actionBatches.put( batchNumber, actions );
			}
			actions.add( action );
		}

	}

	public void addAction(EntityInsertAction action) {
		addInsertAction(action);
	}

	private void addInsertAction(AbstractEntityInsertAction insert) {
		addResolvedEntityInsertAction( insert );
	}

	@SuppressWarnings("unchecked")
	private void addResolvedEntityInsertAction(AbstractEntityInsertAction insert) {
		if(insert.isEarlyInsert()){
		}
		else{
			insertions.add(insert);
		}
		insert.makeEntityManaged();
	}

	public void sortActions() {
		
	}

	public void prepareActions() throws ZormException{
		prepareActions( collectionRemovals );
		prepareActions( collectionUpdates );
		prepareActions( collectionCreations );
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void prepareActions(List queue) {
		for ( Executable executable : (List<Executable>) queue ) {
			executable.beforeExecutions();
		}
	}

	public void executeActions() throws ZormException{
		if(! unresolvedInsertions.isEmpty()){
			throw new IllegalStateException(
					"About to execute actions, but there are unresolved entity insert actions."
			);
		}
		executeActions( insertions );   //执行插入操作
		executeActions( updates );
		executeActions( collectionRemovals );
		executeActions( collectionUpdates );
		executeActions( collectionCreations );
		executeActions( deletions );
	}

	@SuppressWarnings("rawtypes")
	private void executeActions(List list) throws ZormException {
		for(Object aList : list){
			execute((Executable)aList);
		}
		list.clear();
		session.getTransactionCoordinator().getJdbcCoordinator().executeBatch();
	}

	private void execute(Executable executable) {
		try{
			executable.execute();
		}
		finally{
			registerCleanupActions( executable );
		}
	}

	private void registerCleanupActions(Executable executable) {
		beforeTransactionProcesses.register( executable.getBeforeTransactionCompletionProcess() );
		
		afterTransactionProcesses.register( executable.getAfterTransactionCompletionProcess() );
	}

	public void afterTransactionCompletion(boolean successful) {
		afterTransactionProcesses.afterTransactionCompletion( successful );
	}

	public void addAction(EntityUpdateAction entityUpdateAction) {
		updates.add(entityUpdateAction);
	}

	public void addAction(BulkOperationCleanupAction cleanupAction) {
		registerCleanupActions( cleanupAction );
	}

	public void addAction(CollectionRecreateAction collectionRecreateAction) {
		collectionCreations.add(collectionRecreateAction);
	}

	public void addAction(CollectionUpdateAction action) {
		collectionUpdates.add( action );
	}

	public void addAction(CollectionRemoveAction collectionRemoveAction) {
		collectionRemovals.add(collectionRemoveAction);
	}

	public void sortCollectionActions() {
		if ( session.getFactory().getSettings().isOrderUpdatesEnabled() ) {
			java.util.Collections.sort( collectionCreations );
			java.util.Collections.sort( collectionUpdates );
			java.util.Collections.sort( collectionRemovals );
		}
	}

	public boolean hasAfterTransactionActions() {
		return afterTransactionProcesses.processes.size() > 0;
	}
}
