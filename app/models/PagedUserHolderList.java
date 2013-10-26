package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.avaje.ebean.Page;

import play.db.ebean.Model.Finder;

import twitter4j.Friendship;
import twitter4j.PagableResponseList;
import twitter4j.Relationship;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

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
		boolean isFriend();
		boolean isFollower();
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
		public boolean isFriend() {
			return myTwitterUser.isFriend;
		}

		@Override
		public boolean isFollower() {
			return myTwitterUser.isFollower;
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
		
		Finder<Long, TwitterUser> finder = new Finder<Long, TwitterUser>(Long.class, TwitterUser.class);
		for (twitter4j.User twitterUser: pagableTwitterUsers) {
			Friendship twitterFriendship = friendships.get(i++);
			
			TwitterUser myTwitterUser = finder.byId(twitterUser.getId());
			if (myTwitterUser == null) {
				myTwitterUser = new TwitterUser();
				myTwitterUser.id = twitterUser.getId();
				myTwitterUser.added = new Date();
			}
			myTwitterUser.lastUpdated = new Date();
			myTwitterUser.isFollower = twitterFriendship.isFollowedBy();
			myTwitterUser.isFriend = twitterFriendship.isFollowing();
			myTwitterUser.friendsCount = twitterUser.getFriendsCount();
			myTwitterUser.followersCount = twitterUser.getFollowersCount();
			myTwitterUser.description = twitterUser.getDescription();
			myTwitterUser.save();
			
			result.add(new UserHolder(twitterUser, myTwitterUser));
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
			result.add(new UserHolder(twitterUser, myTwitterUser));
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
