package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.TwitterUser.Category;

import play.db.ebean.Model.Finder;
import twitter4j.Friendship;
import twitter4j.PagableResponseList;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Page;

public class PagedUserHolderList extends ArrayList<PagedUserHolderList.UserHolder> {
	
	private static final long serialVersionUID = 1L;
	
	private Long next;
	private Long prev;
	private int totalSize;
	
	public interface IUserHolder {
		String getScreenName();
		String getName();
		String getDescription();
		String getImageURL();
		String getProfileURL();
		int getFollowersCount();
		int getFriendsCount();
		int getTweetCount();
		TwitterUser getTwitterUser();
	}
	
	public static class UserHolder implements IUserHolder {
		private final twitter4j.User twitterUser;
		private final TwitterUser myTwitterUser;
		
		private UserHolder(User twitterUser, TwitterUser myTwitterUser) {
			super();
			this.twitterUser = twitterUser;
			this.myTwitterUser = myTwitterUser;
		}

		@Override
		public String getScreenName() {
			return twitterUser.getScreenName();
		}

		@Override
		public String getName() {
			return twitterUser.getName();
		}

		@Override
		public String getDescription() {
			return twitterUser.getDescription();
		}

		@Override
		public String getImageURL() {
			return twitterUser.getProfileImageURL();
		}

		@Override
		public String getProfileURL() {
			return twitterUser.getURL();
		}

		@Override
		public int getFollowersCount() {
			return twitterUser.getFollowersCount();
		}

		@Override
		public int getFriendsCount() {
			return twitterUser.getFriendsCount();
		}

		@Override
		public int getTweetCount() {
			return twitterUser.getStatusesCount();
		}

		@Override
		public TwitterUser getTwitterUser() {
			return myTwitterUser;
		}
	}
	
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
			
			result.add(new UserHolder(twitterUser, update(null, twitterUser, twitterFriendship)));
		}
		
		return result;
	}
	
	private static TwitterUser update(TwitterUser myTwitterUser, User twitterUser, Friendship twitterFriendship) {
		boolean isNew = false;
		if (myTwitterUser == null) {
			isNew = true;
			myTwitterUser = TwitterUser.find.byId(twitterUser.getId());
			if (myTwitterUser == null) {
				myTwitterUser = new TwitterUser();
				myTwitterUser.id = twitterUser.getId();
				myTwitterUser.screenName = twitterUser.getScreenName();
				myTwitterUser.added = new Date();
			}
		}
		myTwitterUser.lastUpdated = new Date();
		if (twitterFriendship != null) {
			boolean isFollower = twitterFriendship.isFollowedBy();
			if (isFollower != myTwitterUser.isFollower || isNew) {
				myTwitterUser.isFollower = isFollower;
				if (!isFollower) {
					myTwitterUser.timesHasBeenFollower++;
				} else {
					myTwitterUser.isFollowerSince = new Date();
				}
			}
			boolean isFriend = twitterFriendship.isFollowing();
			if (isFriend != myTwitterUser.isFriend || isNew) {
				myTwitterUser.isFriend = isFriend;
				if (!isFriend) {
					myTwitterUser.timesHasBeenFriend++;
				} else {
					myTwitterUser.isFriendSince = new Date();
				}
			}
		}
		myTwitterUser.friendsCount = twitterUser.getFriendsCount();
		myTwitterUser.followersCount = twitterUser.getFollowersCount();
		myTwitterUser.description = twitterUser.getDescription();
		myTwitterUser.save();
		
		TwitterUser.find.byId(myTwitterUser.id).actions.size();
		return myTwitterUser;
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
			result.add(new UserHolder(twitterUser, update(myTwitterUser, twitterUser, null)));
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
