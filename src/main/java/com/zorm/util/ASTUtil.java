package com.zorm.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import com.zorm.query.SqlWalkerNode;
import com.zorm.util.ASTUtil.FilterPredicate;

import antlr.ASTFactory;
import antlr.collections.AST;
import antlr.collections.impl.ASTArray;

public class ASTUtil {
	
	public static interface FilterPredicate {
		/**
		 * Returns true if the node should be filtered out.
		 *
		 * @param n The node.
		 *
		 * @return true if the node should be filtered out, false to keep the node.
		 */
		boolean exclude(AST n);
	}

	public abstract static class IncludePredicate implements FilterPredicate {
		public final boolean exclude(AST node) {
			return !include( node );
		}

		public abstract boolean include(AST node);
	}
	
	public static Map generateTokenNameCache(Class tokenTypeInterface) {
		final Field[] fields = tokenTypeInterface.getFields();
		Map cache = new HashMap( (int)( fields.length * .75 ) + 1 );
		for ( int i = 0; i < fields.length; i++ ) {
			final Field field = fields[i];
			if ( Modifier.isStatic( field.getModifiers() ) ) {
				try {
					cache.put( field.get( null ), field.getName() );
				}
				catch ( Throwable ignore ) {
				}
			}
		}
		return cache;
	}
	
	public static AST createParent(ASTFactory factory, int parentType, String parentText, AST child) {
		ASTArray array = createAstArray( factory, 2, parentType, parentText, child );
		return factory.make( array );
	}
	
	private static ASTArray createAstArray(ASTFactory factory, int size, int parentType, String parentText, AST child1) {
		ASTArray array = new ASTArray( size );
		array.add( factory.create( parentType, parentText ) );
		array.add( child1 );
		return array;
	}
	
	public static boolean isSubtreeChild(AST fixture, AST test) {
		AST n = fixture.getFirstChild();
		while ( n != null ) {
			if ( n == test ) {
				return true;
			}
			if ( n.getFirstChild() != null && isSubtreeChild( n, test ) ) {
				return true;
			}
			n = n.getNextSibling();
		}
		return false;
	}
	
	public static String getPathText(AST n) {
		StringBuilder buf = new StringBuilder();
		getPathText( buf, n );
		return buf.toString();
	}
	
	private static void getPathText(StringBuilder buf, AST n) {
		AST firstChild = n.getFirstChild();
		// If the node has a first child, recurse into the first child.
		if ( firstChild != null ) {
			getPathText( buf, firstChild );
		}
		// Append the text of the current node.
		buf.append( n.getText() );
		// If there is a second child (RHS), recurse into that child.
		if ( firstChild != null && firstChild.getNextSibling() != null ) {
			getPathText( buf, firstChild.getNextSibling() );
		}
	}
	
	public static AST findTypeInChildren(AST parent, int type) {
		AST n = parent.getFirstChild();
		while ( n != null && n.getType() != type ) {
			n = n.getNextSibling();
		}
		return n;
	}
	
	public static AST createBinarySubtree(ASTFactory factory, int parentType, String parentText, AST child1, AST child2) {
		ASTArray array = createAstArray( factory, 3, parentType, parentText, child1 );
		array.add( child2 );
		return factory.make( array );
	}
	
	public static AST create(ASTFactory astFactory, int type, String text) {
		return astFactory.create( type, text );
	}

	public static List collectChildren(AST root, FilterPredicate predicate) {
		return new CollectingNodeVisitor( predicate ).collect( root );
	}
	
	private static class CollectingNodeVisitor implements NodeTraverser.VisitationStrategy {
		private final FilterPredicate predicate;
		private final List collectedNodes = new ArrayList();

		public CollectingNodeVisitor(FilterPredicate predicate) {
			this.predicate = predicate;
		}

		public void visit(AST node) {
			if ( predicate == null || !predicate.exclude( node ) ) {
				collectedNodes.add( node );
			}
		}

		public List getCollectedNodes() {
			return collectedNodes;
		}

		public List collect(AST root) {
			NodeTraverser traverser = new NodeTraverser( this );
			traverser.traverseDepthFirst( root );
			return collectedNodes;
		}
	}

	public static AST getLastChild(AST parent) {
		return getLastSibling( parent.getFirstChild() );
	}
	
	private static AST getLastSibling(AST a) {
		AST last = null;
		while ( a != null ) {
			last = a;
			a = a.getNextSibling();
		}
		return last;
	}

	public static AST createSibling(ASTFactory astFactory, int type, String text, AST prevSibling) {
		AST node = astFactory.create( type, text );
		return insertSibling( node, prevSibling );
	}

	public static AST insertSibling(AST node, AST prevSibling) {
		node.setNextSibling( prevSibling.getNextSibling() );
		prevSibling.setNextSibling( node );
		return node;
	}
}
