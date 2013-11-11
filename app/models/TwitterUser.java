package models;

import java.util.Date;

import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.utils.IndexDirection;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;

import apis.TwitterConnection.RateLimitPolicy;
import twitter4j.User;
import utils.DataStoreConnection;

@Entity
public class TwitterUser {
	
	@Id public Long id;

	public String screenName;
	public String description;
	public String name;
	public String imageURL;
	public int followersCount;
	public int friendsCount;
	public int tweetCount;
	
	@Indexed(value=IndexDirection.DESC) public Date added;
	public Date lastUpdated;
	
	public boolean isProtected;
	public boolean isFollower;	
	public Date isFollowerSince;	
	public int timesHasBeenFollower = 0;
	public boolean isFriend;
	public Date isFriendSince;
	public int timesHasBeenFriend = 0;
			
	public boolean isStarred = false;
		
	@Reference public Presence presence; 
	
	
	public void save() {
		DataStoreConnection.datastore().save(this);
	}
	
	public static Page<TwitterUser> find(String whereConditions, int count, long offset) {
		Query<TwitterUser> query = DataStoreConnection.datastore()
			.find(TwitterUser.class);
		if (whereConditions != null && !whereConditions.trim().equals("")) {
			query = query.where("function() { return (" + whereConditions + "); }");
		}
		query
			.order("-added")
			.offset((int)offset)
			.limit(count)
			.disableValidation()
			.disableCursorTimeout();
		
		return new MorphiaPage<TwitterUser>(query);
	}
	
	public static Page<TwitterUser> textSearch(String search, long cursor, RateLimitPolicy rateLimitPolicy) {
		BasicDBObject cmd = new BasicDBObject(); //"text", new BasicDBObject("search", search));
		cmd.put("text", "TwitterUser");
		cmd.put("search", search);
		CommandResult result = DataStoreConnection.datastore().getDB().command(cmd);
		return new TextSearchPage<TwitterUser>(((BasicDBList)result.get("results")), TwitterUser.class);
	}
	
	public static TwitterUser find(Long id) {
		return DataStoreConnection.datastore().get(TwitterUser.class, id);
	}
	
	public Presence getPresence() {
		if (presence == null) {
			presence = Presence.create(this);
			presence.save();
		} 
		return presence;
	}
	
	public static String getProfileURL(TwitterUser twitterUser) {
		return "http://twitter.com/" + twitterUser.screenName;
	}
	
	public static TwitterUser get(TwitterMe twitterMe, User t4jUser) {
		return update(null, twitterMe, t4jUser);
	}
	
	public static TwitterUser update(TwitterUser existingTwitterUser, TwitterMe me, User t4jUser) {
		boolean isNew = false;
		if (existingTwitterUser == null) {
			isNew = true;
			existingTwitterUser = TwitterUser.find(t4jUser.getId());
			if (existingTwitterUser == null) {
				existingTwitterUser = new TwitterUser();
				existingTwitterUser.id = t4jUser.getId();
				existingTwitterUser.screenName = t4jUser.getScreenName();
				existingTwitterUser.name = t4jUser.getName();
				existingTwitterUser.imageURL = t4jUser.getProfileImageURL();
				existingTwitterUser.tweetCount = t4jUser.getStatusesCount();
				existingTwitterUser.added = new Date();
				existingTwitterUser.isProtected = t4jUser.isProtected();
			}
		}
		existingTwitterUser.lastUpdated = new Date();

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
