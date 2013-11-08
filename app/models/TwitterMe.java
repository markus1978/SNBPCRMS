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
	
	private String consumerKey;
	private String consumerSecret;
	private String accessToken;
	private String accessSecret;
	
	public Long id;
	public long latestRetweet = 0;
	public long latestMention = 0;
	public long lastestDirectMessage = 0;

	public List<Long> friends = new ArrayList<Long>();
	public List<Long> followers = new ArrayList<Long>();
	
	@Transient private Collection<Long> friendsSet = new HashSet<Long>();
	@Transient private Collection<Long> followerSet = new HashSet<Long>();
	
	public TwitterMe() {
		
	}
	
	private TwitterMe(String screenName, String consumerKey, String consumerSecret,
			String accessToken, String accessSecret) {
		super();
		this.screenName = screenName;
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.accessToken = accessToken;
		this.accessSecret = accessSecret;
		save();
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
	public boolean addFriend(Long friendID) {
		if (friendsSet.contains(friendID)) {
			return true;
		} else {
			friendsSet.add(friendID);
			return false;
		}
	}
	
	public boolean removeFriend(long friendID) {
		return friendsSet.remove(friendID);
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
	
	private static TwitterMe find(String screenName) {
		return DataStoreConnection.datastore().get(TwitterMe.class, screenName);
	}
	
	public void save() {
		friends.clear(); friends.addAll(friendsSet);
		followers.clear(); followers.addAll(followerSet);
		DataStoreConnection.datastore().save(this);
	}
	
	private static TwitterMe instance = null;
	
	public static TwitterMe get(String screenName) {
		if (instance == null || !instance.screenName.equals(screenName)) {			
			instance = find(screenName);
			boolean isNew = instance == null;
			if (isNew) {
				instance = new TwitterMe(	// TODO login functionality
					"mscheidgen",
					"uObLVPxuBJqfrEOEB3ms1g",
					"IAYcfFI5Xhq6g0McCdPVM5EEFOqq8PUkPH7KQu58w",
					"127843079-lSfAi1HpoYXCFqsp4rXYq16DwlzHOeCjP7dd5nrv",
					"U3wjEMIVCPiKvalWL7oEMFNEfwocNGPrmeUc6HtG80zTk");				
			}
			
			instance.friendsSet.addAll(instance.friends);
			instance.followerSet.addAll(instance.followers);
			
			if (isNew) {
				TwitterConnection.get(instance).update(instance, RateLimitPolicy.fail, isNew);
			}
			instance.save();
		}
		return instance;
	}
	
	public String getConsumerKey() {
		return consumerKey;
	}

	public String getConsumerSecret() {
		return consumerSecret;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getAccessSecret() {
		return accessSecret;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((screenName == null) ? 0 : screenName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TwitterMe other = (TwitterMe) obj;
		if (screenName == null) {
			if (other.screenName != null)
				return false;
		} else if (!screenName.equals(other.screenName))
			return false;
		return true;
	}
}
