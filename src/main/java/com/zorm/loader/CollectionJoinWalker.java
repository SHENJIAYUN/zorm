package com.zorm.loader;

import com.zorm.engine.LoadQueryInfluencers;
import com.zorm.entity.JoinWalker;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.util.StringHelper;

public abstract class CollectionJoinWalker extends JoinWalker{
	
	public CollectionJoinWalker(SessionFactoryImplementor factory, LoadQueryInfluencers loadQueryInfluencers) {
		super( factory, loadQueryInfluencers );
	}
	
	protected StringBuilder whereString(String alias, String[] columnNames, String subselect, int batchSize) {
		if (subselect==null) {
			return super.whereString(alias, columnNames, batchSize);
		}
		else {
			StringBuilder buf = new StringBuilder();
			if (columnNames.length>1) buf.append('(');
			buf.append( StringHelper.join(", ", StringHelper.qualify(alias, columnNames) ) );
			if (columnNames.length>1) buf.append(')');
			buf.append(" in ")
				.append('(')
				.append(subselect) 
				.append(')');
			return buf;
		}
	}
}
