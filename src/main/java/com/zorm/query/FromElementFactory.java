package com.zorm.query;

import antlr.SemanticException;
import antlr.collections.AST;

import com.zorm.persister.entity.EntityPersister;
import com.zorm.persister.entity.Joinable;
import com.zorm.persister.entity.Queryable;
import com.zorm.type.EntityType;
import com.zorm.util.ASTUtil;
import com.zorm.util.PathHelper;

public class FromElementFactory implements SqlTokenTypes{
	private FromClause fromClause;
	private FromElement origin;
	private String path;

	private String classAlias;
	private String[] columns;
	private boolean implied;
	private boolean inElementsFunction;
	private boolean collection;
	
	public FromElementFactory(FromClause fromClause, FromElement origin, String path) {
		this.fromClause = fromClause;
		this.origin = origin;
		this.path = path;
		collection = false;
	}
	
	public FromElementFactory(
	        FromClause fromClause,
	        FromElement origin,
	        String path,
	        String classAlias,
	        String[] columns,
	        boolean implied) {
		this( fromClause, origin, path );
		this.classAlias = classAlias;
		this.columns = columns;
		this.implied = implied;
		collection = true;
	}
	
	FromElement addFromElement() throws SemanticException {
		FromClause parentFromClause = fromClause.getParentFromClause();
		if ( parentFromClause != null ) {
			// Look up class name using the first identifier in the path.
			String pathAlias = PathHelper.getAlias( path );
			FromElement parentFromElement = parentFromClause.getFromElement( pathAlias );
//			if ( parentFromElement != null ) {
//				return createFromElementInSubselect( path, pathAlias, parentFromElement, classAlias );
//			}
		}

		EntityPersister entityPersister = fromClause.getSessionFactoryHelper().requireClassPersister( path );

		FromElement elem = createAndAddFromElement( path,
				classAlias,
				entityPersister,
				( EntityType ) ( ( Queryable ) entityPersister ).getType(),
				null );

		// Add to the query spaces.
		fromClause.getWalker().addQuerySpaces( entityPersister.getQuerySpaces() );

		return elem;
	}
	
	private FromElement createAndAddFromElement(
	        String className,
	        String classAlias,
	        EntityPersister entityPersister,
	        EntityType type,
	        String tableAlias) {
		if ( !( entityPersister instanceof Joinable ) ) {
			throw new IllegalArgumentException( "EntityPersister " + entityPersister + " does not implement Joinable!" );
		}
		FromElement element = createFromElement( entityPersister );
		initializeAndAddFromElement( element, className, classAlias, entityPersister, type, tableAlias );
		return element;
	}

	private void initializeAndAddFromElement(
	        FromElement element,
	        String className,
	        String classAlias,
	        EntityPersister entityPersister,
	        EntityType type,
	        String tableAlias) {
		if ( tableAlias == null ) {
			AliasGenerator aliasGenerator = fromClause.getAliasGenerator();
			tableAlias = aliasGenerator.createName( entityPersister.getEntityName() );
		}
		element.initializeEntity( fromClause, className, entityPersister, type, classAlias, tableAlias );
	}
	
	private FromElement createFromElement(EntityPersister entityPersister) {
		Joinable joinable = ( Joinable ) entityPersister;
		String text = joinable.getTableName();
		AST ast = createFromElement( text );
		FromElement element = ( FromElement ) ast;
		return element;
	}
	
	private AST createFromElement(String text) {
		AST ast = ASTUtil.create( fromClause.getASTFactory(),
				implied ? IMPLIED_FROM : FROM_FRAGMENT, // This causes the factory to instantiate the desired class.
				text );
		ast.setType( FROM_FRAGMENT );
		return ast;
	}
}
