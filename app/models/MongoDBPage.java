package models;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.mongodb.morphia.query.Query;

public class MongoDBPage<E> implements Page<E> {
	private final Query<E> query;
	private final List<E> data;
	
	protected MongoDBPage(Query<E> query) {
		this.query = query;
		data = query.asList();
	}
	
	public boolean hasNext() {
		return data.size() == query.getLimit();
	}
	
	public boolean hasPrev() {
		return query.getOffset() > 0;
	}
	
	public long getNextCursor() {
		return query.getOffset() + data.size();
	}
	
	public long getPrevCursor() {
		return query.getOffset() - query.getLimit();
	}

	// delegates
	public boolean add(E e) {
		return data.add(e);
	}

	public void add(int index, E element) {
		data.add(index, element);
	}

	public boolean addAll(Collection<? extends E> c) {
		return data.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends E> c) {
		return data.addAll(index, c);
	}

	public void clear() {
		data.clear();
	}

	public boolean contains(Object o) {
		return data.contains(o);
	}

	public boolean containsAll(Collection<?> c) {
		return data.containsAll(c);
	}

	public boolean equals(Object o) {
		return data.equals(o);
	}

	public E get(int index) {
		return data.get(index);
	}

	public int hashCode() {
		return data.hashCode();
	}

	public int indexOf(Object o) {
		return data.indexOf(o);
	}

	public boolean isEmpty() {
		return data.isEmpty();
	}

	public Iterator<E> iterator() {
		return data.iterator();
	}

	public int lastIndexOf(Object o) {
		return data.lastIndexOf(o);
	}

	public ListIterator<E> listIterator() {
		return data.listIterator();
	}

	public ListIterator<E> listIterator(int index) {
		return data.listIterator(index);
	}

	public E remove(int index) {
		return data.remove(index);
	}

	public boolean remove(Object o) {
		return data.remove(o);
	}

	public boolean removeAll(Collection<?> c) {
		return data.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return data.retainAll(c);
	}

	public E set(int index, E element) {
		return data.set(index, element);
	}

	public int size() {
		return data.size();
	}

	public List<E> subList(int fromIndex, int toIndex) {
		return data.subList(fromIndex, toIndex);
	}

	public Object[] toArray() {
		return data.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return data.toArray(a);
	}
}
