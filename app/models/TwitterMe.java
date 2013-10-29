package models;

import java.util.Collection;
import java.util.HashSet;

import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class TwitterMe {

	private final Collection<Long> friends = new HashSet<>();
	private final Collection<Long> follower = new HashSet<>();
	private final String screenName;
	private final long id;
	
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
			instance = new TwitterMe(t4jUserMe.getId(), t4jUserMe.getScreenName());
			
			IDs friendsIDs = twitter.getFriendsIDs(t4jUserMe.getId(), -1);
			for (long id: friendsIDs.getIDs()) instance.friends.add(id);
			while (friendsIDs.hasNext()) {
				friendsIDs = twitter.getFriendsIDs(t4jUserMe.getId(), friendsIDs.getNextCursor());
				for (long id: friendsIDs.getIDs()) instance.friends.add(id);
			}
			
			IDs followerIDs = twitter.getFollowersIDs(t4jUserMe.getId(), -1);
			for (long id: followerIDs.getIDs()) instance.follower.add(id);
			while (followerIDs.hasNext()) {
				followerIDs = twitter.getFollowersIDs(t4jUserMe.getId(), followerIDs.getNextCursor());
				for (long id: followerIDs.getIDs()) instance.follower.add(id);
			}
		}
		return instance;
	}
}
