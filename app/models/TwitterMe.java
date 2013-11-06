package models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;

import utils.DataStoreConnection;
import apis.TwitterConnection;
import apis.TwitterConnection.RateLimitPolicy;

@Entity
public class TwitterMe {
	 		
	@Id public String screenName;
	
	public Long id;
	public Long latestRetweet = null;
	public Long latestMention = null;
	public Long lastestDirectMessage = null;

	private List<Long> friends = new ArrayList<Long>();
	private List<Long> followers = new ArrayList<Long>();
	
	@Transient private Collection<Long> friendsSet = new HashSet<Long>();
	@Transient private Collection<Long> followerSet = new HashSet<Long>();
	
	private TwitterMe(String screenName) {
		this.screenName = screenName;
	}
	
	public String screenName() {
		return screenName;
	}
	
	public long id() {
		return id;
	}
	
	public boolean isFriend(TwitterUser twitterUser) {
		return friendsSet.contains(twitterUser.id);
	}
	
	public boolean isFollower(TwitterUser twitterUser) {
		return followerSet.contains(twitterUser.id);
	}
	
	/** 
	 * @return false if friendID is not part of TwitterMe friends.
	 */
	public boolean updateFriend(Long friendID) {
		if (friendsSet.contains(friendID)) {
			return true;
		} else {
			friendsSet.add(friendID);
			return false;
		}
	}

	/** 
	 * @return false if followerID is not part of TwitterMe followers.
	 */
	public boolean updateFollower(Long followerID) {
		if (followerSet.contains(followerID)) {
			return true;
		} else {
			followerSet.add(followerID);
			return false;
		}
	}
	
	public void save() {
		friends.clear(); friends.addAll(friendsSet);
		followers.clear(); followers.addAll(followerSet);
	}
	
	private static TwitterMe instance = null;
	
	public static TwitterMe instance() {
		String screenName = "mscheidgen";
		if (instance == null) {
			instance = DataStoreConnection.datastore().get(TwitterMe.class, screenName);
			if (instance == null) {
				instance = new TwitterMe(screenName);
			}
			
			instance.friendsSet.addAll(instance.friends);
			instance.followerSet.addAll(instance.followers);
			
			TwitterConnection.update(instance, RateLimitPolicy.fail);
			instance.save();
		}
		return instance;
	}
}
