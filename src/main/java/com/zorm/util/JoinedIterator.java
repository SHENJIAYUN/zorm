package com.zorm.util;

import java.util.Iterator;
import java.util.List;

public class JoinedIterator implements Iterator {
	private static final Iterator[] ITERATORS = {};

	// wrapped iterators
	private Iterator[] iterators;

	// index of current iterator in the wrapped iterators array
	private int currentIteratorIndex;

	// the current iterator
	private Iterator currentIterator;

	// the last used iterator
	private Iterator lastUsedIterator;

	public JoinedIterator(List iterators) {
		this( (Iterator[]) iterators.toArray(ITERATORS) );
	}

	public JoinedIterator(Iterator[] iterators) {
		if( iterators==null )
			throw new NullPointerException("Unexpected NULL iterators argument");
		this.iterators = iterators;
	}

	public JoinedIterator(Iterator first, Iterator second) {
		this( new Iterator[] { first, second } );
	}
	
	@Override
	public boolean hasNext() {
		updateCurrentIterator();
		return currentIterator.hasNext();
	}

	protected void updateCurrentIterator() {
		if(currentIterator == null){
			if(iterators.length==0){
				currentIterator = EmptyIterator.INSTANCE;
			}
			else{
				currentIterator = iterators[0];
			}
			lastUsedIterator = currentIterator;
		}
		
		while(!currentIterator.hasNext() && currentIteratorIndex<iterators.length-1){
			currentIteratorIndex++;
			currentIterator = iterators[currentIteratorIndex];
		}
	}

	@Override
	public Object next() {
		updateCurrentIterator();
		return currentIterator.next();
	}

}
