package com.zorm.entity;

import java.util.ArrayList;
import java.util.List;

import com.zorm.LockOptions;
import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.exception.MappingException;
import com.zorm.persister.entity.Loadable;
import com.zorm.persister.entity.OuterJoinLoadable;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.sql.JoinFragment;
import com.zorm.sql.Select;
import com.zorm.util.StringHelper;

public abstract class AbstractEntityJoinWalker extends JoinWalker{
	private final OuterJoinLoadable persister;
	private final String alias;

	public AbstractEntityJoinWalker(
			OuterJoinLoadable persister,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) {
		this( persister, factory, loadQueryInfluencers, null );
	}

	public AbstractEntityJoinWalker(
			OuterJoinLoadable persister,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers,
			String alias) {
		super( factory, loadQueryInfluencers );
		this.persister = persister;
		this.alias = ( alias == null ) ? generateRootAlias( persister.getEntityName() ) : alias;
	}
	
	protected final String getAlias() {
		return alias;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected final void initAll(
			final String whereString,
			final String orderByString,
			final LockOptions lockOptions,
			final AssociationInitCallback callback) throws MappingException {
		walkEntityTree( persister, getAlias() );
		List allAssociations = new ArrayList();
		allAssociations.addAll( associations );
		allAssociations.add( OuterJoinableAssociation.createRoot( persister.getEntityType(), alias, getFactory() ) );
		initPersisters( allAssociations, lockOptions, callback );
		initStatementString( whereString, orderByString, lockOptions );
	}
	
	private void initStatementString(
			final String condition,
			final String orderBy,
			final LockOptions lockOptions) throws MappingException {
		initStatementString(null, condition, orderBy, "", lockOptions);
	}
	
	private void initStatementString(
			final String projection,
			final String condition,
			final String orderBy,
			final String groupBy,
			final LockOptions lockOptions) throws MappingException {

		final int joins = countEntityPersisters( associations );
		suffixes = BasicLoader.generateSuffixes( joins + 1 );

		JoinFragment ojf = mergeOuterJoins( associations );

		Select select = new Select( getDialect() )
				.setLockOptions( lockOptions )
				.setSelectClause(
						projection == null ?
								persister.selectFragment( alias, suffixes[joins] ) + selectString( associations ) :
								projection
				)
				.setFromClause(
						getDialect().appendLockHint( lockOptions, persister.fromTableFragment( alias ) ) +
								persister.fromJoinFragment( alias, true, true )
				)
				.setWhereClause( condition )
				.setOuterJoins(
						ojf.toFromFragmentString(),
						ojf.toWhereFragmentString() + getWhereFragment()
				)
				.setOrderByClause( orderBy( associations, orderBy ) )
				.setGroupByClause( groupBy );

		if ( getFactory().getSettings().isCommentsEnabled() ) {
			select.setComment( getComment() );
		}
		sql = select.toStatementString();
	}
	
	public abstract String getComment();
	
	protected String getWhereFragment() throws MappingException {
		return persister.whereJoinFragment(alias, true, true);
	}
	
	protected final Loadable getPersister() {
		return persister;
	}
}
