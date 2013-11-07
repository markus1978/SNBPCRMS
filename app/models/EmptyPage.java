package models;

import java.util.ArrayList;

public class EmptyPage <E> extends ArrayList<E> implements Page<E> {
	private static final long serialVersionUID = 1L;
	
	private EmptyPage() {
		super(0);
	}

	@Override
	public boolean hasNext() {		
		return false;
	}

	@Override
	public boolean hasPrev() {
		return false;
	}

	@Override
	public long getNextCursor() {
		return 0;
	}

	@Override
	public long getPrevCursor() {
		return 0;
	}		
	
	public static final <T> Page<T> empty(Class<T> nop) {
		return new EmptyPage<T>();
	};
}
