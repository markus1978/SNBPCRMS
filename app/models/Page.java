package models;

import java.util.List;

public interface Page<E> extends List<E> { 
	
	public boolean hasNext();
	
	public boolean hasPrev();
	
	public long getNextCursor();
	
	public long getPrevCursor();
}
