package com.zorm.util;

import java.util.*;

import antlr.collections.AST;

public class ASTIterator implements Iterator {
	private AST next, current;
	private LinkedList parents = new LinkedList();

	public void remove() {
		throw new UnsupportedOperationException( "remove() is not supported" );
	}

	public boolean hasNext() {
		return next != null;
	}

	public Object next() {
		return nextNode();
	}

	public ASTIterator(AST tree) {
		next = tree;
		down();
	}

	public AST nextNode() {
		current = next;
		if ( next != null ) {
			AST nextSibling = next.getNextSibling();
			if ( nextSibling == null ) {
				next = pop();
			}
			else {
				next = nextSibling;
				down();
			}
		}
		return current;
	}

	private void down() {
		while ( next != null && next.getFirstChild() != null ) {
			push( next );
			next = next.getFirstChild();
		}
	}

	private void push(AST parent) {
		parents.addFirst( parent );
	}

	private AST pop() {
		if ( parents.size() == 0 ) {
			return null;
		}
		else {
			return ( AST ) parents.removeFirst();
		}
	}

}
