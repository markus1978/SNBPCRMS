package models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import twitter4j.PagableResponseList;
import twitter4j.ResponseList;
import twitter4j.User;

public class TwitterPage implements Page<TwitterUser> {

	private final PagableResponseList<User> twitterResponse;
	private final List<TwitterUser> twitterUserList = new ArrayList<TwitterUser>();
	
	public static Page<TwitterUser> create(PagableResponseList<User> twitterResponse, TwitterMe me) {
		TwitterPage page = new TwitterPage(twitterResponse);
		for(User t4jUser: twitterResponse) {			
			page.twitterUserList.add(TwitterUser.update(null, me, t4jUser));
		}
		return page;
	}
	
	public static Page<TwitterUser> create(ResponseList<User> twitterResponse, TwitterMe me) {
		TwitterPage page = new TwitterPage(null);
		for(User t4jUser: twitterResponse) {			
			page.twitterUserList.add(TwitterUser.update(null, me, t4jUser));
		}
		return page;
	}

	private TwitterPage(PagableResponseList<User> twitterResponse) {
		super();
		this.twitterResponse = twitterResponse;
	}

	@Override
	public boolean hasNext() {
		return twitterResponse == null ? false : twitterResponse.hasNext();
	}

	@Override
	public boolean hasPrev() {
		return twitterResponse == null ? false: twitterResponse.hasPrevious();
	}

	@Override
	public long getNextCursor() {
		return twitterResponse == null ? 0 : twitterResponse.getNextCursor();
	}

	@Override
	public long getPrevCursor() {
		return twitterResponse == null ? 0 : twitterResponse.getPreviousCursor();
	}

	// delegates

	public void add(int index, TwitterUser element) {
		twitterUserList.add(index, element);
	}

	public boolean add(TwitterUser e) {
		return twitterUserList.add(e);
	}

	public boolean addAll(Collection<? extends TwitterUser> c) {
		return twitterUserList.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends TwitterUser> c) {
		return twitterUserList.addAll(index, c);
	}

	public void clear() {
		twitterUserList.clear();
	}

	public boolean contains(Object o) {
		return twitterUserList.contains(o);
	}

	public boolean containsAll(Collection<?> c) {
		return twitterUserList.containsAll(c);
	}

	public boolean equals(Object o) {
		return twitterUserList.equals(o);
	}

	public TwitterUser get(int index) {
		return twitterUserList.get(index);
	}

	public int hashCode() {
		return twitterUserList.hashCode();
	}

	public int indexOf(Object o) {
		return twitterUserList.indexOf(o);
	}

	public boolean isEmpty() {
		return twitterUserList.isEmpty();
	}

	public Iterator<TwitterUser> iterator() {
		return twitterUserList.iterator();
	}

	public int lastIndexOf(Object o) {
		return twitterUserList.lastIndexOf(o);
	}

	public ListIterator<TwitterUser> listIterator() {
		return twitterUserList.listIterator();
	}

	public ListIterator<TwitterUser> listIterator(int index) {
		return twitterUserList.listIterator(index);
	}

	public TwitterUser remove(int index) {
		return twitterUserList.remove(index);
	}

	public boolean remove(Object o) {
		return twitterUserList.remove(o);
	}

	public boolean removeAll(Collection<?> c) {
		return twitterUserList.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return twitterUserList.retainAll(c);
	}

	public TwitterUser set(int index, TwitterUser element) {
		return twitterUserList.set(index, element);
	}

	public int size() {
		return twitterUserList.size();
	}

	public List<TwitterUser> subList(int fromIndex, int toIndex) {
		return twitterUserList.subList(fromIndex, toIndex);
	}

	public Object[] toArray() {
		return twitterUserList.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return twitterUserList.toArray(a);
	}
}
