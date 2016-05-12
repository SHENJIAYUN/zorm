package com.zorm.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.zorm.FetchMode;
import com.zorm.LockMode;
import com.zorm.LockOptions;
import com.zorm.engine.CascadeStyle;
import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.exception.MappingException;
import com.zorm.persister.QueryableCollection;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.persister.entity.OuterJoinLoadable;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.sql.JoinType;
import com.zorm.type.AssociationType;

public class EntityJoinWalker extends AbstractEntityJoinWalker{
	private final LockOptions lockOptions = new LockOptions();
	//private final int[][] compositeKeyManyToOneTargetIndices;

	public EntityJoinWalker(
			OuterJoinLoadable persister, 
			String[] uniqueKey, 
			int batchSize, 
			LockMode lockMode,
			final SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		super( persister, factory, loadQueryInfluencers );

		this.lockOptions.setLockMode(lockMode);
		
		StringBuilder whereCondition = whereString( getAlias(), uniqueKey, batchSize )
				.append( persister.filterFragment( getAlias(), Collections.EMPTY_MAP ) );

		AssociationInitCallbackImpl callback = new AssociationInitCallbackImpl( factory );
		initAll( whereCondition.toString(), "", lockOptions, callback );
	}

	public EntityJoinWalker(
			OuterJoinLoadable persister,
			String[] uniqueKey,
			int batchSize,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		super( persister, factory, loadQueryInfluencers );
		LockOptions.copy(lockOptions, this.lockOptions);
	}
	
	public String getComment() {
		return "load " + getPersister().getEntityName();
	}
	
	protected JoinType getJoinType(
			OuterJoinLoadable persister,
			PropertyPath path,
			int propertyNumber,
			AssociationType associationType,
			FetchMode metadataFetchMode,
			CascadeStyle metadataCascadeStyle,
			String lhsTable,
			String[] lhsColumns,
			boolean nullable,
			int currentDepth) throws MappingException {
		if ( lockOptions.getLockMode().greaterThan( LockMode.READ ) ) {
			return JoinType.NONE;
		}
		if ( isTooDeep( currentDepth )
				|| ( associationType.isCollectionType() && isTooManyCollections() ) ) {
			return JoinType.NONE;
		}
		if ( !isJoinedFetchEnabledInMapping( metadataFetchMode, associationType ) ) {
			return JoinType.NONE;
		}
		if ( isDuplicateAssociation( lhsTable, lhsColumns, associationType ) ) {
			return JoinType.NONE;
		}
		return getJoinType( nullable, currentDepth );
	}
	
	private static class AssociationInitCallbackImpl implements AssociationInitCallback {
		private final SessionFactoryImplementor factory;
		private final HashMap<String,OuterJoinableAssociation> associationsByAlias
				= new HashMap<String, OuterJoinableAssociation>();
		private final HashMap<String,Integer> positionsByAlias = new HashMap<String, Integer>();
		private final ArrayList<String> aliasesForAssociationsWithCompositesIds
				= new ArrayList<String>();

		public AssociationInitCallbackImpl(SessionFactoryImplementor factory) {
			this.factory = factory;
		}

		@Override
		public void associationProcessed(OuterJoinableAssociation oja,
				int position) {
			associationsByAlias.put( oja.getRhsAlias(), oja );
			positionsByAlias.put( oja.getRhsAlias(), position );
			EntityPersister entityPersister = null;
			if ( oja.getJoinableType().isCollectionType() ) {
				entityPersister = ( ( QueryableCollection) oja.getJoinable() ).getElementPersister();
			}
			else if ( oja.getJoinableType().isEntityType() ) {
				entityPersister = ( EntityPersister ) oja.getJoinable();
			}
//			if ( entityPersister != null
//					&& entityPersister.getIdentifierType().isComponentType()
//					&& ! entityPersister.getEntityMetamodel().getIdentifierProperty().isEmbedded()
//					&& hasAssociation( (CompositeType) entityPersister.getIdentifierType() ) ) {
//				aliasesForAssociationsWithCompositesIds.add( oja.getRhsAlias() );
//			}
			
		}
	}
}
