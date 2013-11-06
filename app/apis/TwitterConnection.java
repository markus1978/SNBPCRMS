package apis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Page;
import models.TwitterMe;
import models.TwitterPage;
import models.TwitterUser;
import play.libs.Akka;
import scala.concurrent.duration.Duration;
import twitter4j.IDs;
import twitter4j.PagableResponseList;
import twitter4j.Paging;
import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import controllers.Application;

public class TwitterConnection {
	private static final String consumerKey = "uObLVPxuBJqfrEOEB3ms1g";
	private static final String consumerSecret = "IAYcfFI5Xhq6g0McCdPVM5EEFOqq8PUkPH7KQu58w";
	private static final String accessToken = "127843079-7SDkbTQjdCK761FrZxd68ED6ktEMbSGn8pQTbe1h";
	private static final String accessSecret = "H32HL32hwiA1TOFfXQ7BA5GKFHJBoGl6qJYCYuBT2Wd4z";
	
	private static Twitter twitter = null;
	private static Map<String, RateLimitStatus> ratelimits = new HashMap<String, RateLimitStatus>();
	
	public static Map<String, RateLimitStatus> ratelimits() {
		return ratelimits;
	}
	
	public enum RateLimitPolicy { wait, fail }
	
	public static Twitter twitter() {
		if (twitter == null) {
			TwitterFactory factory = new TwitterFactory();
			twitter = factory.getInstance();
			twitter.setOAuthConsumer(consumerKey, consumerSecret);
			twitter.setOAuthAccessToken(new AccessToken(accessToken, accessSecret));	
		}		
		return twitter;
	}
	
	private static void waitForRatelimitForBackgroundTask(RateLimitPolicy rateLimitPolicy, String... ratelimits) {
		if (rateLimitPolicy == RateLimitPolicy.wait) {
			boolean approved = false;
			while (!approved) {
				approved = true;
				for (String ratelimit : ratelimits) {
					RateLimitStatus rateLimitStatus = TwitterConnection.ratelimits
							.get(ratelimit);
					if (rateLimitStatus != null) {
						if (rateLimitStatus.getRemaining() <= 3) {
							approved = false;
							try {
								Thread.sleep(rateLimitStatus
										.getSecondsUntilReset() * 1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
	}
   
	public static List<Status> timeline(long twitterUserID, long maxStatusID, RateLimitPolicy rateLimitPolicy) {
		waitForRatelimitForBackgroundTask(rateLimitPolicy, "statuses/user_timeline");
	   
		try {
			ResponseList<twitter4j.Status> userTimelinePage = null;
			if (maxStatusID == -1) {
				userTimelinePage = TwitterConnection.twitter().getUserTimeline(twitterUserID);    			
			} else {
				Paging paging = new Paging();
				paging.setMaxId(maxStatusID);
				userTimelinePage = TwitterConnection.twitter().getUserTimeline(paging);    			
			}
			ratelimits.put("statuses/user_timeline", userTimelinePage.getRateLimitStatus());
			return userTimelinePage;
		} catch (TwitterException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Status status(long statusID, RateLimitPolicy rateLimitPolicy) {
		waitForRatelimitForBackgroundTask(rateLimitPolicy, "statuses/show");
		try {
			Status status = twitter().showStatus(statusID);
			ratelimits.put("statuses/show", status.getRateLimitStatus());
			return status;
		} catch (TwitterException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Page<TwitterUser> friends(String screenName, long cursor, RateLimitPolicy rateLimitPolicy) {
    	waitForRatelimitForBackgroundTask(rateLimitPolicy, "friends/list");
    	
    	try {
			PagableResponseList<User> twitterResponse = twitter().getFriendsList(screenName, cursor);
			ratelimits.put("friends/list", twitterResponse.getRateLimitStatus());
			return TwitterPage.create(twitterResponse, TwitterMe.instance());
    	} catch (TwitterException e) {
    		throw new RuntimeException(e);
    	}
	}

	public static Page<TwitterUser> followers(String screenName, long cursor, RateLimitPolicy rateLimitPolicy) {
		waitForRatelimitForBackgroundTask(rateLimitPolicy, "followers/list");
		try {			
			PagableResponseList<User> twitterResponse = twitter().getFollowersList(screenName, cursor);
			ratelimits.put("followers/list", twitterResponse.getRateLimitStatus());
			return TwitterPage.create(twitterResponse, TwitterMe.instance());
		} catch (TwitterException e) {
			throw new RuntimeException(e);
		}
    }
	
    public static Page<TwitterUser> suggestions(String categorySlug, RateLimitPolicy rateLimitPolicy) {
    	waitForRatelimitForBackgroundTask(rateLimitPolicy, "users/suggestions/:slug");
    	try {	
			if (categorySlug == null) {
	    		ResponseList<twitter4j.Category> suggestedUserCategories = twitter().getSuggestedUserCategories();
	    		categorySlug = suggestedUserCategories.get(0).getSlug();
	    		StringBuffer categories = new StringBuffer();
	    		for (twitter4j.Category category: suggestedUserCategories) {
	    			categories.append("'" + category.getName() + "':" + category.getSlug() + " ");
	    		}
	    		Application.log.add("Suggestion categories are " + categories.toString());
	    	}
	    	ResponseList<twitter4j.User> response = twitter().getUserSuggestions(categorySlug);
	    	ratelimits.put("users/suggestions/:slug", response.getRateLimitStatus());
			return TwitterPage.create(response, TwitterMe.instance());
		} catch (TwitterException e) {
			throw new RuntimeException(e);
		}
    }
    
    public static void startBackgroundImport(final String query) {
    	Akka.system().scheduler().scheduleOnce(
    			Duration.Zero(),
    	        new Runnable() {
    	            public void run() {
    	            	try {
    	            		if (query.startsWith("friends:") || query.startsWith("followers:")) {
    	            			String screenName = query.split(":")[1].trim();
    	            			IDs idsResponse = null;
    	            			int totalUsersImported = 0;
    	            			while (idsResponse == null || idsResponse.hasNext()) {
    	            				if (query.startsWith("friends:")) {
    	            					waitForRatelimitForBackgroundTask(RateLimitPolicy.wait, "friends/ids");
		    	            			idsResponse = twitter().getFriendsIDs(screenName, idsResponse == null ? -1 : idsResponse.getNextCursor());
		    	            			ratelimits.put("friends/ids", idsResponse.getRateLimitStatus());
    	            				} else {
    	            					waitForRatelimitForBackgroundTask(RateLimitPolicy.wait, "followers/ids");
		    	            			idsResponse = twitter().getFollowersIDs(screenName, idsResponse == null ? -1 : idsResponse.getNextCursor());
		    	            			ratelimits.put("followers/ids", idsResponse.getRateLimitStatus());
    	            				}
	    	            			long[] allIDs = idsResponse.getIDs();
									for (int i = 0; i*100 < allIDs.length; i++) {
										int size = 100;
										if (i*100 + 100 > allIDs.length) {
											size = allIDs.length - i*100;
										}
	    	            				long[] ids = new long[size];
	    	            				for (int ii = 0; ii < size; ii++) {
	    	            					ids[ii] = allIDs[i*100+ii];
	    	            				}
	    	            				waitForRatelimitForBackgroundTask(RateLimitPolicy.wait, "users/lookup");
	    	            				ResponseList<User> users = twitter().lookupUsers(ids);
	    	            				ratelimits.put("users/lookup", users.getRateLimitStatus());
	    	            				for (User t4jUser: users) {
	    	            					TwitterUser.update(null, TwitterMe.instance(), t4jUser);
	    	            				}
	    	            				totalUsersImported += ids.length;
	    	            				Application.log.add("Imported " + totalUsersImported + " users.");
	    	            			}
    	            			}    	            			
    	            		} else {
	    	            		int userCount = 0;
		    	            	long cursor = -1;
		    	            	Page<TwitterUser> result = TwitterUser.find(query, 100, cursor);
		    	            	userCount += result.size();
		    	            	Application.log.add("Imported " + userCount + " users.");
		    	            	while (result.hasNext()) {
		    	            		result = TwitterUser.find(query, 100, result.getNextCursor());
		    	            		update(result, RateLimitPolicy.wait);
		    	            		userCount += result.size();
		    	            		Application.log.add("Imported " + userCount + " users.");
		    	            	}
    	            		}
    	            	} catch (TwitterException e) {
    	            		if (e.getRateLimitStatus() != null) {
    	            			ratelimits.put("*", e.getRateLimitStatus());	
    	            		}   
    	            	} catch (Exception e) {
    	            		e.printStackTrace();
    	            		Application.log.add("Exception: " + e.getLocalizedMessage());
    	            	}
    	            }
    	        },
    	        Akka.system().dispatcher()
    	);
    }
    
    public static void update(List<TwitterUser> twitterUsers, RateLimitPolicy rateLimitPolicy) {
    	if (twitterUsers.size() > 100) {
    		throw new IllegalArgumentException("Too many users for one update.");
    	}
    	long ids[] = new long[twitterUsers.size()];
    	int i = 0;
    	for (TwitterUser twitterUser: twitterUsers) {
    		ids[i++] = twitterUser.id;
    	}
    	
    	try {
	    	waitForRatelimitForBackgroundTask(RateLimitPolicy.wait, "users/lookup");
			ResponseList<User> users = twitter().lookupUsers(ids);
			ratelimits.put("users/lookup", users.getRateLimitStatus());
			i = 0;
			for (User t4jUser: users) {
				TwitterUser.update(twitterUsers.get(i++), TwitterMe.instance(), t4jUser);
			}
    	} catch (TwitterException e) {
    		throw new RuntimeException(e);
    	}
    }
    
	public static void update(TwitterMe instance, RateLimitPolicy rateLimitPolicy) {
		try {
			waitForRatelimitForBackgroundTask(rateLimitPolicy, "users/show");
			User t4jUserMe = twitter.showUser(instance.screenName);
			ratelimits.put("users/show", t4jUserMe.getRateLimitStatus());
						
			boolean first = true;
			IDs ids = null;
			while (first || ids.hasNext()) {			
				waitForRatelimitForBackgroundTask(rateLimitPolicy, "friends/ids");
				ids = twitter.getFriendsIDs(t4jUserMe.getId(), first ? -1 : ids.getNextCursor());
				ratelimits.put("friends/ids", ids.getRateLimitStatus());
				for (long id: ids.getIDs()) {
					instance.updateFriend(id); // TODO create action for new friend
				}
				first = false;
			}
			
			first = true;
			while (first || ids.hasNext()) {			
				waitForRatelimitForBackgroundTask(rateLimitPolicy, "followers/ids");
				ids = twitter.getFollowersIDs(t4jUserMe.getId(), first ? -1 : ids.getNextCursor());
				ratelimits.put("followers/ids", ids.getRateLimitStatus());
				for (long id: ids.getIDs()) {
					instance.updateFollower(id); // TODO create action for new follower
				}
				first = false;
			}
			
			// TODO keep track of deleted friends and followers													
			// TODO look for new mentions
			// TODO look for new retweets
			// TODO look for new directmessages
			// TODO look for new favorits
		} catch (TwitterException e) {
			throw new RuntimeException(e);
		}		
	}
}
