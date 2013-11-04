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
	
	public String description;
	public String name;
	public String imageURL;
	public int followersCount;
	public int friendsCount;
	public int tweetCount;
	
	@Constraints.Required
	public Date added;
	
	@Constraints.Required
	public Date lastUpdated;
	
	public boolean isFollower;	
	public Date isFollowerSince;	
	public int timesHasBeenFollower = 0;
	public boolean isFriend;
	public Date isFriendSince;
	public int timesHasBeenFriend = 0;
			
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
	
	public static String getProfileURL(TwitterUser twitterUser) {
		return "http://twitter.com/" + twitterUser.screenName;
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
				existingTwitterUser.name = t4jUser.getName();
				existingTwitterUser.imageURL = t4jUser.getProfileImageURL();
				existingTwitterUser.tweetCount = t4jUser.getStatusesCount();
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
