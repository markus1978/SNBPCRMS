package models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import utils.DataStoreConnection;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class TextSearchPage<T> implements Page<T> {
	
	private List<T> data;
	
	public TextSearchPage(BasicDBList dbList, Class<T> theClass) {
		int length = dbList.size();
		data = new ArrayList<T>(length);
		for (int i = 0; i < length; i++) {
			data.add(DataStoreConnection.map((BasicDBObject)((BasicDBObject)dbList.get(i)).get("obj"), theClass));
		}
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

	public void add(int index, T element) {
		data.add(index, element);
	}

	public boolean add(T e) {
		return data.add(e);
	}

	public boolean addAll(Collection<? extends T> c) {
		return data.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends T> c) {
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

	public T get(int index) {
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

	public Iterator<T> iterator() {
		return data.iterator();
	}

	public int lastIndexOf(Object o) {
		return data.lastIndexOf(o);
	}

	public ListIterator<T> listIterator() {
		return data.listIterator();
	}

	public ListIterator<T> listIterator(int index) {
		return data.listIterator(index);
	}

	public T remove(int index) {
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

	public T set(int index, T element) {
		return data.set(index, element);
	}

	public int size() {
		return data.size();
	}

	public List<T> subList(int fromIndex, int toIndex) {
		return data.subList(fromIndex, toIndex);
	}

	public Object[] toArray() {
		return data.toArray();
	}

	public <E> E[] toArray(E[] a) {
		return data.toArray(a);
	}

	
}
