package models;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Friendship;
import twitter4j.PagableResponseList;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

import com.avaje.ebean.Page;

public class PagedUserHolderList extends ArrayList<TwitterUser.IUserHolder> {
	
	private static final long serialVersionUID = 1L;
	
	private Long next;
	private Long prev;
	private int totalSize;
	
	public static PagedUserHolderList create(Twitter twitter, twitter4j.User user, PagableResponseList<twitter4j.User> pagableTwitterUsers, int totalSize) throws TwitterException {
		PagedUserHolderList result = new PagedUserHolderList(
				pagableTwitterUsers.hasPrevious() ? pagableTwitterUsers.getPreviousCursor() : null,
				pagableTwitterUsers.hasNext() ? pagableTwitterUsers.getNextCursor() : null, 
				totalSize);
		long[] ids = new long[pagableTwitterUsers.size()];
		int i = 0;
		for (twitter4j.User twitterUser: pagableTwitterUsers) {
			ids[i++] = twitterUser.getId();
		}
		
		ResponseList<Friendship> friendships = twitter.lookupFriendships(ids);
		i = 0;
		
		for (twitter4j.User twitterUser: pagableTwitterUsers) {
			Friendship twitterFriendship = friendships.get(i++);
			
			result.add(TwitterUser.createHolder(null, twitterUser, twitterFriendship));
		}
		
		return result;
	}
	
	public static PagedUserHolderList create(Twitter twitter, twitter4j.User user, Page<TwitterUser> myTwitterUsers) throws TwitterException {
		PagedUserHolderList result = new PagedUserHolderList(
				myTwitterUsers.hasPrev() ? (long)myTwitterUsers.getPageIndex() -1 : null,
				myTwitterUsers.hasNext() ? (long)myTwitterUsers.getPageIndex() +1 : null, 
				myTwitterUsers.getTotalPageCount()*20);
		
		List<TwitterUser> myTwitterUsersList = myTwitterUsers.getList();
		long ids[] = new long[myTwitterUsersList.size()];
		
		int i = 0;
		for(TwitterUser myTwitterUser: myTwitterUsersList) {
			ids[i++] = myTwitterUser.id;
		}
		
		ResponseList<User> twitterUsers = twitter.lookupUsers(ids);
		i = 0;
		for (TwitterUser myTwitterUser: myTwitterUsersList) {
			User twitterUser = twitterUsers.get(i++);
			result.add(TwitterUser.createHolder(myTwitterUser, twitterUser, null));
		}
		return result;
	}
	
	private PagedUserHolderList(Long prev, Long next, int totalSize) {
		this.next = next;
		this.prev = prev;
		this.totalSize = totalSize;
	}
	
	public int totalSize() {
		return totalSize;
	}
	
	public boolean hasNext() {
		return next != null;
	}
	
	public boolean hasPrev() {
		return prev != null;
	}
	
	public Long getNextCursor() {
		return next;
	}
	
	public Long getPrevCursor() {
		return prev;
	}
}
