package models;

import java.util.Collection;
import java.util.HashSet;

import javax.persistence.Entity;
import javax.persistence.Id;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;
import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import controllers.Application;

@Entity
public class TwitterMe extends Model {
	
	private static final long serialVersionUID = 1L;

	public static Finder<Long,TwitterMe> find = new Finder<Long,TwitterMe>(Long.class, TwitterMe.class); 
	
	@Id
	public long id;
	
	public String screenName;
	
	public Long latestFollower = null;
	public Long latestRetweet = null;
	public Long latestMention = null;
	public Long lastestDirectMessage = null;

	private final Collection<Long> friends = new HashSet<>();
	private final Collection<Long> follower = new HashSet<>();
	
	private TwitterMe(long id, String screenName) {
		this.screenName = screenName;
		this.id = id;
	}
	
	public String screenName() {
		return screenName;
	}
	
	public long id() {
		return id;
	}
	
	public boolean isFriend(TwitterUser twitterUser) {
		return friends.contains(twitterUser.id);
	}
	
	public boolean isFollower(TwitterUser twitterUser) {
		return follower.contains(twitterUser.id);
	}
	
	private static TwitterMe instance = null;
	
	public static TwitterMe update(Twitter twitter) throws TwitterException {
		if (instance != null) {
			User t4jUserMe = twitter.showUser("mscheidgen");
			if (t4jUserMe.getFollowersCount() != instance.follower.size() ||
					t4jUserMe.getFriendsCount() != instance.friends.size()) {
				instance = null;
			}
		}
		return instance(twitter);
	}
	
	public static TwitterMe instance(Twitter twitter)  throws TwitterException {
		if (instance == null) {
			User t4jUserMe = twitter.showUser("mscheidgen");
			Application.ratelimits.put("users/show", t4jUserMe.getRateLimitStatus());
			
			instance = find.byId(t4jUserMe.getId());
			if (instance == null) {
				instance = new TwitterMe(t4jUserMe.getId(), t4jUserMe.getScreenName());
			}
			
			IDs friendsIDs = twitter.getFriendsIDs(t4jUserMe.getId(), -1);
			Application.ratelimits.put("friends/ids", friendsIDs.getRateLimitStatus());
			for (long id: friendsIDs.getIDs()) instance.friends.add(id);
			while (friendsIDs.hasNext()) {
				friendsIDs = twitter.getFriendsIDs(t4jUserMe.getId(), friendsIDs.getNextCursor());
				Application.ratelimits.put("friends/ids", friendsIDs.getRateLimitStatus());
				for (long id: friendsIDs.getIDs()) instance.friends.add(id);
			}
			
			boolean isOldFollower = instance.latestFollower == null;
			Long newLatestFollower = null;
			IDs followerIDs = twitter.getFollowersIDs(t4jUserMe.getId(), -1);
			boolean first = true;
			while (first || followerIDs.hasNext()) {
				if (!first) {
					followerIDs = twitter.getFollowersIDs(t4jUserMe.getId(), followerIDs.getNextCursor());
				} else {
					first = false;
				}
				Application.ratelimits.put("followers/ids", friendsIDs.getRateLimitStatus());
				for (long id: followerIDs.getIDs()) {
					if (newLatestFollower == null) {
						newLatestFollower = id;
					}
					instance.follower.add(id);
					if (instance.latestFollower == id) {
						isOldFollower = true;
					} else if (!isOldFollower) {
						// TODO create Action for new follower
					}
				}
			}
			instance.latestFollower = newLatestFollower;
			
			// TODO look for new mentions
			// TODO look for new retweets
			// TODO look for new directmessages
			// TODO look for new favorits
			instance.save();
		}
		return instance;
	}
}
