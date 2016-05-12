package com.zorm.event;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.FlushMode;
import com.zorm.exception.ZormException;

public class DefaultAutoFlushEventListener extends AbstractFlushingEventListener implements AutoFlushEventListener {

	private static final Log log = LogFactory.getLog(DefaultAutoFlushEventListener.class);
	
	@Override
	public void onAutoFlush(AutoFlushEvent event) throws ZormException {
		final EventSource source = event.getSession();
		if ( flushMightBeNeeded(source) ) {
			// Need to get the number of collection removals before flushing to executions
			// (because flushing to executions can add collection removal actions to the action queue).
			final int oldSize = source.getActionQueue().numberOfCollectionRemovals();
			flushEverythingToExecutions(event);
			if ( flushIsReallyNeeded(event, source) ) {
				log.trace( "Need to execute flush" );

				performExecutions(source);
				//postFlush(source);
				// note: performExecutions() clears all collectionXxxxtion
				// collections (the collection actions) in the session

				if ( source.getFactory().getStatistics().isStatisticsEnabled() ) {
					source.getFactory().getStatisticsImplementor().flush();
				}
			}
			else {
				log.trace( "Don't need to execute flush" );
				source.getActionQueue().clearFromFlushNeededCheck( oldSize );
			}

			event.setFlushRequired( flushIsReallyNeeded( event, source ) );
		}
	}
	
	private boolean flushIsReallyNeeded(AutoFlushEvent event, final EventSource source) {
//		return source.getActionQueue()
//				.areTablesToBeUpdated( event.getQuerySpaces() ) ||
//						source.getFlushMode()==FlushMode.ALWAYS;
		return false;
	}

	private boolean flushMightBeNeeded(final EventSource source) {
//		return !source.getFlushMode().lessThan(FlushMode.AUTO) &&
//				source.getDontFlushFromFind() == 0 &&
//				( source.getPersistenceContext().getEntityEntries().size() > 0 ||
//						source.getPersistenceContext().getCollectionEntries().size() > 0 );
		return false;
	}

}
