package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import play.data.validation.Constraints;
import play.db.ebean.Model;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

@Entity
public class TwitterUser extends Model {

	private static final long serialVersionUID = 1L;
	
	public enum Tier { one, two, three, notAssigned };
	public enum Category { publication, developer, user, notAssigned };
	public enum Status { neutral, wishlist, ignored, notAssigned };
	
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
	public Tier tier = Tier.notAssigned;
	
	@Constraints.Required
	public Category category = Category.notAssigned;
	
	@Constraints.Required
	public Status status = Status.notAssigned;

	@Constraints.Required
	public String description;
	
	public static Finder<Long,TwitterUser> find = new Finder<Long,TwitterUser>(Long.class, TwitterUser.class); 
	
	@OneToMany(mappedBy="target", cascade=CascadeType.ALL)
	public List<Action> actions = new ArrayList<Action>();
	
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
	
	private static TwitterUser update(Twitter twitter, TwitterUser existingTwitterUser, User t4jUser) throws TwitterException {
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
		
		TwitterUser.find.byId(existingTwitterUser.id).actions.size();
		return existingTwitterUser;
	}

}
