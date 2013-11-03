package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import models.Presence.Category;
import models.Presence.Tier;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

@Entity
public class TwitterUser extends Model {

	private static final long serialVersionUID = 1L;
	
	@Id
	public Long id;

	@Constraints.Required
	public String screenName;
	
	@Constraints.Required
	public Date added;
	
	@Constraints.Required
	public Date lastUpdated;
	
	@Constraints.Required
	public boolean isFollower;
	
	public Date isFollowerSince;
	
	public int timesHasBeenFollower = 0;
	
	@Constraints.Required
	public boolean isFriend;
	
	public Date isFriendSince;
	
	public int timesHasBeenFriend = 0;

	@Constraints.Required
	public int followersCount;
	
	@Constraints.Required
	public int friendsCount;
	
	@Constraints.Required
	public String description;
	
	public boolean isStarred = false;
	
	@OneToOne
	public Presence presence;
	
	public static Finder<Long,TwitterUser> find = new Finder<Long,TwitterUser>(Long.class, TwitterUser.class); 
	
	public Presence getPresence() {
		if (presence == null) {
			presence = new Presence();
			presence.category = Category.notAssigned;
			presence.tier = Tier.notAssigned;
			presence.name = this.screenName;
			presence.twitterUser = this;
			
			List<String> url = new ArrayList<String>();
			url.add("http://twitter.com/" + screenName);
			presence.setChannelURLs(url);
		} 
		return presence;
	}
	
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
			return "http://twitter.com/" + twitterUser.getScreenName();
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
	
	public static IUserHolder createHolder(Twitter twitter, TwitterUser existingTwitterUser, User t4jUser) throws TwitterException {
		return new UserHolder(t4jUser, update(twitter, existingTwitterUser, t4jUser));
	}
	
	public static TwitterUser update(Twitter twitter, TwitterUser existingTwitterUser, User t4jUser) throws TwitterException {
		boolean isNew = false;
		if (existingTwitterUser == null) {
			isNew = true;
			existingTwitterUser = TwitterUser.find.byId(t4jUser.getId());
			if (existingTwitterUser == null) {
				existingTwitterUser = new TwitterUser();
				existingTwitterUser.id = t4jUser.getId();
				existingTwitterUser.screenName = t4jUser.getScreenName();
				existingTwitterUser.added = new Date();
			}
		}
		existingTwitterUser.lastUpdated = new Date();

		TwitterMe me = TwitterMe.instance(twitter);
		boolean isFollower = me.isFollower(existingTwitterUser);
		if (isFollower != existingTwitterUser.isFollower || isNew) {
			existingTwitterUser.isFollower = isFollower;
			if (!isFollower) {
				if (!isNew) {
					existingTwitterUser.timesHasBeenFollower++;
				}
			} else {
				existingTwitterUser.isFollowerSince = new Date();
			}
		}
		boolean isFriend = me.isFriend(existingTwitterUser);
		if (isFriend != existingTwitterUser.isFriend || isNew) {
			existingTwitterUser.isFriend = isFriend;
			if (!isFriend) {
				if (!isNew) {
					existingTwitterUser.timesHasBeenFriend++;
				}
			} else {
				existingTwitterUser.isFriendSince = new Date();
			}
		}

		existingTwitterUser.friendsCount = t4jUser.getFriendsCount();
		existingTwitterUser.followersCount = t4jUser.getFollowersCount();
		existingTwitterUser.description = t4jUser.getDescription();
		existingTwitterUser.save();
		
		return existingTwitterUser;
	}

}
