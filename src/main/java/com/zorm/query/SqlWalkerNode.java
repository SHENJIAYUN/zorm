package com.zorm.query;

import antlr.ASTFactory;

import com.zorm.util.SessionFactoryHelper;

public class SqlWalkerNode extends SqlNode implements InitializeableNode {
	/**
	 * A pointer back to the phase 2 processor.
	 */
	private SqlWalker walker;

	public void initialize(Object param) {
		walker = ( SqlWalker ) param;
	}

	public SqlWalker getWalker() {
		return walker;
	}

	public SessionFactoryHelper getSessionFactoryHelper() {
		return walker.getSessionFactoryHelper();
	}

	public ASTFactory getASTFactory() {
		return walker.getASTFactory();
	}

	public AliasGenerator getAliasGenerator() {
		return walker.getAliasGenerator();
	}
}
