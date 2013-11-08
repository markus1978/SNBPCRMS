package apis;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import models.Action;
import models.Action.ActionType;
import models.Action.Service;
import models.Page;
import models.TwitterMe;
import models.TwitterPage;
import models.TwitterUser;
import play.libs.Akka;
import scala.concurrent.duration.Duration;
import twitter4j.DirectMessage;
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
	
	private final TwitterMe twitterMe;
	private final Map<String, RateLimitStatus> ratelimits = new HashMap<String, RateLimitStatus>();
	private Twitter twitter = null;
	
	public Map<String, RateLimitStatus> ratelimits() {
		return ratelimits;
	}
	
	public enum RateLimitPolicy { wait, fail, skip }
		
	private static Map<TwitterMe, TwitterConnection> connections = new HashMap<TwitterMe, TwitterConnection>();
	
	public static TwitterConnection get(TwitterMe twitterMe) {		
		TwitterConnection twitterConnection = connections.get(twitterMe);
		if (twitterConnection == null) {
			twitterConnection = new TwitterConnection(twitterMe);
			connections.put(twitterMe, twitterConnection);
		}
		return twitterConnection;
	}
	
	private TwitterConnection(TwitterMe twitterMe) {
		this.twitterMe = twitterMe;
	}
	
	public TwitterMe getTwitterMe() {
		return twitterMe;
	}
	
	protected Twitter twitter() {
		if (twitter == null) {
			TwitterFactory factory = new TwitterFactory();
			twitter = factory.getInstance();
			twitter.setOAuthConsumer(twitterMe.getConsumerKey(), twitterMe.getConsumerSecret());
			twitter.setOAuthAccessToken(new AccessToken(twitterMe.getAccessToken(), twitterMe.getAccessSecret()));	
		}		
		return twitter;
	}
	
	public void addRatelimit(String key, RateLimitStatus rateLimitStatus) {
		ratelimits.put(key, rateLimitStatus);
	}
	
	public boolean waitForRatelimitForBackgroundTask(RateLimitPolicy rateLimitPolicy, String... ratelimits) {
		if (rateLimitPolicy == RateLimitPolicy.wait || rateLimitPolicy == RateLimitPolicy.skip) {
			boolean approved = false;
			while (!approved) {
				approved = true;
				for (String ratelimit : ratelimits) {
					RateLimitStatus rateLimitStatus = this.ratelimits.get(ratelimit);
					if (rateLimitStatus != null) {
						if (rateLimitStatus.getRemaining() <= 3) {
							approved = false;
							try {
								if (rateLimitPolicy == RateLimitPolicy.skip) {
									return false;
								}
								Thread.sleep(rateLimitStatus.getSecondsUntilReset() * 1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		return true;
	}
   
	public List<Status> timeline(long twitterUserID, long maxStatusID, RateLimitPolicy rateLimitPolicy) {
		waitForRatelimitForBackgroundTask(rateLimitPolicy, "statuses/user_timeline");
	   
		try {
			ResponseList<twitter4j.Status> userTimelinePage = null;
			if (maxStatusID == -1) {
				userTimelinePage = twitter().getUserTimeline(twitterUserID);    			
			} else {
				Paging paging = new Paging();
				paging.setMaxId(maxStatusID);
				userTimelinePage = twitter().getUserTimeline(paging);    			
			}
			ratelimits.put("statuses/user_timeline", userTimelinePage.getRateLimitStatus());
			return userTimelinePage;
		} catch (TwitterException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Status status(long statusID, RateLimitPolicy rateLimitPolicy) {
		waitForRatelimitForBackgroundTask(rateLimitPolicy, "statuses/show");
		try {
			Status status = twitter().showStatus(statusID);
			ratelimits.put("statuses/show", status.getRateLimitStatus());
			return status;
		} catch (TwitterException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Page<TwitterUser> friends(String screenName, long cursor, RateLimitPolicy rateLimitPolicy) {
    	waitForRatelimitForBackgroundTask(rateLimitPolicy, "friends/list");
    	
    	try {
			PagableResponseList<User> twitterResponse = twitter().getFriendsList(screenName, cursor);
			ratelimits.put("friends/list", twitterResponse.getRateLimitStatus());
			return TwitterPage.create(twitterResponse, twitterMe);
    	} catch (TwitterException e) {
    		throw new RuntimeException(e);
    	}
	}

	public Page<TwitterUser> followers(String screenName, long cursor, RateLimitPolicy rateLimitPolicy) {
		waitForRatelimitForBackgroundTask(rateLimitPolicy, "followers/list");
		try {			
			PagableResponseList<User> twitterResponse = twitter().getFollowersList(screenName, cursor);
			ratelimits.put("followers/list", twitterResponse.getRateLimitStatus());
			return TwitterPage.create(twitterResponse, twitterMe);
		} catch (TwitterException e) {
			throw new RuntimeException(e);
		}
    }
	
    public Page<TwitterUser> suggestions(String categorySlug, RateLimitPolicy rateLimitPolicy) {
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
			return TwitterPage.create(response, twitterMe);
		} catch (TwitterException e) {
			throw new RuntimeException(e);
		}
    }
    
    public void startBackgroundImport(final String query) {
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
	    	            					TwitterUser.update(null, twitterMe, t4jUser);
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
    
    public void update(List<TwitterUser> twitterUsers, RateLimitPolicy rateLimitPolicy) {
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
				TwitterUser.update(twitterUsers.get(i++), twitterMe, t4jUser);
			}
    	} catch (TwitterException e) {
    		throw new RuntimeException(e);
    	}
    }
    
	public void update(TwitterMe instance, RateLimitPolicy rateLimitPolicy, boolean isNew) {
		final TwitterActionFactory twitterActionFactory = new TwitterActionFactory(this);
		try {
			waitForRatelimitForBackgroundTask(rateLimitPolicy, "users/show");
			User t4jUserMe = twitter().showUser(instance.screenName);
			ratelimits.put("users/show", t4jUserMe.getRateLimitStatus());
			
			HashSet<Long> oldFriends = new HashSet<>(instance.friends);
			HashSet<Long> oldFollower = new HashSet<>(instance.followers);
			
			boolean first = true;
			IDs ids = null;
			while (first || ids.hasNext()) {			
				waitForRatelimitForBackgroundTask(rateLimitPolicy, "friends/ids");
				ids = twitter().getFriendsIDs(t4jUserMe.getId(), first ? -1 : ids.getNextCursor());
				ratelimits.put("friends/ids", ids.getRateLimitStatus());
				for (long id: ids.getIDs()) {
					instance.addFriend(id);
					if (!oldFriends.remove(id) && !isNew) {
						twitterActionFactory.sendFollow(id);
					}
				}
				first = false;
			}
			
			first = true;
			while (first || ids.hasNext()) {			
				waitForRatelimitForBackgroundTask(rateLimitPolicy, "followers/ids");
				ids = twitter().getFollowersIDs(t4jUserMe.getId(), first ? -1 : ids.getNextCursor());
				ratelimits.put("followers/ids", ids.getRateLimitStatus());
				for (long id: ids.getIDs()) {
					instance.updateFollower(id);
					if (!oldFollower.remove(id) && !isNew) {
						twitterActionFactory.receiveFollow(id);
					}
				}
				first = false;
			}
			
			if (!isNew) {
				// actions for removed friends and followers
				for(Long friend: oldFriends) {
					twitterActionFactory.sendUnFollow(friend);
				}
				for(Long follower: oldFollower) {
					twitterActionFactory.receiveUnFollow(follower);
				}
			
				// actions for mentions
				instance.latestMention = pullItems(new PullItemHandler<Status>() {
					public ResponseList<Status> receiveItems(Paging paging) throws TwitterException {
						return twitter().getMentionsTimeline(paging);
					}

					@Override
					public long getId(Status item) {
						return item.getId();
					}

					@Override
					public void createAction(Status item) {
						twitterActionFactory.receiveMention(item);
					}	
				}, instance.latestMention, rateLimitPolicy, "statuses/mention_timeline");
				
				// look for new retweets
				instance.latestRetweet = pullItems(new PullItemHandler<Status>() {
					public ResponseList<Status> receiveItems(Paging paging) throws TwitterException {
						return twitter().getRetweetsOfMe(paging);
					}										

					@Override
					public List<Status> postProcessItems(List<Status> items)
							throws TwitterException {					
						List<Status> retweets = new ArrayList<Status>();
						for (Status tweet: items) {
							retweets.addAll(twitter().getRetweets(tweet.getId()));
						}
						return retweets;
					}

					@Override
					public long getId(Status item) {
						return item.getRetweetedStatus().getId();
					}

					@Override
					public void createAction(Status item) {
						twitterActionFactory.receiveRetweet(item);
					}	
				}, instance.latestRetweet, rateLimitPolicy, "statuses/retweets_of_me");				
				
				// look for new favorits			
				// TODO only possible via streaming API -> required continuous supervision
				
				
				// look for new directmessages
				instance.lastestDirectMessage = pullItems(new PullItemHandler<DirectMessage>() {
					public ResponseList<DirectMessage> receiveItems(Paging paging) throws TwitterException {
						return twitter().getDirectMessages(paging);
					}

					@Override
					public long getId(DirectMessage item) {
						return item.getId();
					}

					@Override
					public void createAction(DirectMessage item) {
						twitterActionFactory.receiveDirectMessage(item);
					}	
				}, instance.lastestDirectMessage, rateLimitPolicy, "direct_messages");										
			}			
		} catch (TwitterException e) {
			addRatelimit("*", e.getRateLimitStatus());
			throw new RuntimeException(e);
		} finally {
			instance.save();
		}
	}
	
	private <E> long pullItems(PullItemHandler<E> handler, long mostRecentKnownItemID, RateLimitPolicy policy, String ratelimit) throws TwitterException {		
		long oldestReceivedItemID = -1;
		if (mostRecentKnownItemID <= 0) {
			mostRecentKnownItemID = 1;
		}
		long newMostRecentKnownItemID = mostRecentKnownItemID;
		while (waitForRatelimitForBackgroundTask(policy, ratelimit)) {
			Paging paging = null;
			if (oldestReceivedItemID == -1) {
				paging = new Paging(1, 100, mostRecentKnownItemID);
			} else {
				paging = new Paging(1, 100, mostRecentKnownItemID, oldestReceivedItemID - 1);
			}
			ResponseList<E> receivedItems = handler.receiveItems(paging);
			addRatelimit(ratelimit, receivedItems.getRateLimitStatus());
			if (receivedItems.isEmpty()) {
				return newMostRecentKnownItemID;
			}
			for(E item: handler.postProcessItems(receivedItems)) {
				handler.createAction(item);
				long id = handler.getId(item);
				if (oldestReceivedItemID == -1 || id < oldestReceivedItemID) {
					oldestReceivedItemID = id;
				}
				if (id > newMostRecentKnownItemID) {
					newMostRecentKnownItemID = id;
				}
			}
		}
		return newMostRecentKnownItemID;
	}
	
	private abstract class PullItemHandler<E> {
		public abstract ResponseList<E> receiveItems(Paging paging) throws TwitterException;
		public List<E> postProcessItems(List<E> items) throws TwitterException {
			return items;
		}
		public abstract long getId(E item);
		public abstract void createAction(E item);
	}
	
	public void performAction(Action action, RateLimitPolicy rateLimitPolicy) {
		if (action.service != Service.twitter) {
			throw new IllegalArgumentException();
		}
		
		try {
			TwitterUser twitterUser = action.target.twitterUser;
			if(action.actionType == ActionType.publicMessage) {
				twitter().updateStatus(action.message);
			} else if (action.actionType == ActionType.retweet) {
				twitter().retweetStatus(action.replyToId);
			} else if (action.actionType == ActionType.message) {
				waitForRatelimitForBackgroundTask(rateLimitPolicy, "direct_messages/sent");
				DirectMessage result = twitter().sendDirectMessage(twitterUser.id, action.message);
				addRatelimit("direct_messages/sent", result.getRateLimitStatus());
			} else if (action.actionType == ActionType.beFriend) {
				twitter().createFriendship(twitterUser.id);
				twitterMe.addFriend(twitterUser.id);
			} else if (action.actionType == ActionType.unFriend) {
				twitter().destroyFriendship(twitterUser.id);
				twitterMe.removeFriend(twitterUser.id);
			} else if (action.actionType == ActionType.like) {
				twitter().createFavorite(action.replyToId);
			}
			
			if (action.actionType == ActionType.beFriend || action.actionType == ActionType.unFriend) {
				twitterMe.save();
				waitForRatelimitForBackgroundTask(rateLimitPolicy, "users/show");
				User t4jUser = twitter().showUser(twitterUser.id);				
				addRatelimit("users/show", t4jUser.getRateLimitStatus());
				TwitterUser.update(twitterUser, twitterMe, t4jUser);				
			}
			
			action.executedAt = new Date();
		} catch (TwitterException e) {
			throw new RuntimeException(e);
		}
	}
}
