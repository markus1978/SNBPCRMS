package models;

import java.util.ArrayList;
import java.util.List;

import twitter4j.PagableResponseList;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

import com.avaje.ebean.Page;

import controllers.Application;

public class TwitterUserPage extends ArrayList<TwitterUser> {
	
	private static final long serialVersionUID = 1L;
	
	private Long next;
	private Long prev;
	private int totalSize;
	
	public static TwitterUserPage create(Twitter twitter, List<twitter4j.User> userList) throws TwitterException {		
		TwitterUserPage result = new TwitterUserPage(null, null, userList.size());
		
		for (twitter4j.User twitterUser: userList) {			
			result.add(TwitterUser.update(twitter, null, twitterUser));
		}
		
		return result;
	}
	
	public static TwitterUserPage create(Twitter twitter, PagableResponseList<twitter4j.User> pagableTwitterUsers, int totalSize) throws TwitterException {		
		TwitterUserPage result = new TwitterUserPage(
				pagableTwitterUsers.hasPrevious() ? pagableTwitterUsers.getPreviousCursor() : null,
				pagableTwitterUsers.hasNext() ? pagableTwitterUsers.getNextCursor() : null, 
				totalSize);
		
		for (twitter4j.User twitterUser: pagableTwitterUsers) {			
			result.add(TwitterUser.update(twitter, null, twitterUser));
		}
		
		return result;
	}
	
	public static TwitterUserPage create(Twitter twitter, Page<TwitterUser> myTwitterUsers) throws TwitterException {
		TwitterUserPage result = new TwitterUserPage(
				myTwitterUsers.hasPrev() ? (long)myTwitterUsers.getPageIndex() -1 : null,
				myTwitterUsers.hasNext() ? (long)myTwitterUsers.getPageIndex() +1 : null, 
				myTwitterUsers.getTotalPageCount()*20);
		
		List<TwitterUser> myTwitterUsersList = myTwitterUsers.getList();
		long ids[] = new long[myTwitterUsersList.size()];
		
		int i = 0;
		for(TwitterUser myTwitterUser: myTwitterUsersList) {
			ids[i++] = myTwitterUser.id;
		}
		
		if (ids.length > 0) {
			ResponseList<User> twitterUsers = twitter.lookupUsers(ids);
			Application.ratelimits.put("users/lookup", twitterUsers.getRateLimitStatus());
	
			i = 0;
			for (TwitterUser myTwitterUser: myTwitterUsersList) {
				User twitterUser = twitterUsers.get(i++);
				result.add(TwitterUser.update(twitter, myTwitterUser, twitterUser));
			}
		}
		return result;
	}
	
	private TwitterUserPage(Long prev, Long next, int totalSize) {
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
