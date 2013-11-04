package controllers;

import java.util.Date;

import models.Action;
import models.Action.ActionType;
import models.Action.Direction;
import models.Action.Service;
import models.Presence;
import models.TwitterUser;
import models.TwitterUserPage;
import play.db.ebean.Model.Finder;
import play.libs.Akka;
import play.mvc.Controller;
import play.mvc.Result;
import scala.concurrent.duration.Duration;
import twitter4j.IDs;
import twitter4j.PagableResponseList;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;

import com.avaje.ebean.Page;

public class TwitterPart extends Controller {

	private static final String consumerKey = "uObLVPxuBJqfrEOEB3ms1g";
	private static final String consumerSecret = "IAYcfFI5Xhq6g0McCdPVM5EEFOqq8PUkPH7KQu58w";
	private static final String accessToken = "127843079-7SDkbTQjdCK761FrZxd68ED6ktEMbSGn8pQTbe1h";
	private static final String accessSecret = "H32HL32hwiA1TOFfXQ7BA5GKFHJBoGl6qJYCYuBT2Wd4z";
	
	// this is actually somewhat bad, since everything should be pretty stateless
	private static Twitter twitter = null;
	
	public static Twitter twitter() {
		if (twitter == null) {
			TwitterFactory factory = new TwitterFactory();
			twitter = factory.getInstance();
			twitter.setOAuthConsumer(consumerKey, consumerSecret);
			twitter.setOAuthAccessToken(new AccessToken(accessToken, accessSecret));	
		}		
		return twitter;
	}
	
    public static Result users(String query, long cursor) {
    	try {
    		TwitterUserPage result = evaluateQuery(query, cursor, false);
    		if (result == null) {
    			return ok(views.html.index.render("Could not evaluate query '" + query + "'"));
    		}

    		return ok(views.html.twitter.users.render(query, result));
    	} catch (TwitterException e) { 
    		if (e.getRateLimitStatus() != null) {
    			Application.ratelimits.put("*", e.getRateLimitStatus());	
    		}    		
    		return ok(views.html.index.render("Something went wrong: " + e.getErrorMessage() + ". Retry at " + new Date(e.getRateLimitStatus().getResetTimeInSeconds()*(long)1000)));
    	}
    }
    
    public static Result ajaxUsers(String query, long cursor) {
    	try {
    		TwitterUserPage result = evaluateQuery(query, cursor, false);
    		if (result == null) {
    			return internalServerError("Could not evaluate query.");
    		}
    		return ok(views.html.twitter.userPage.render(query, result));
    	} catch (TwitterException e) {
    		if (e.getRateLimitStatus() != null) {
    			Application.ratelimits.put("*", e.getRateLimitStatus());	
    		}   
    		return internalServerError("Twitter exception: " + e.getMessage());
    	}
    }
    
    public static Result ajaxTimeline(long userId, long maxId) {
    	try {
    		ResponseList<twitter4j.Status> userTimelinePage = null;
    		if (maxId == -1) {
    			userTimelinePage = twitter().getUserTimeline(userId);    			
    		} else {
    			Paging paging = new Paging();
    			paging.setMaxId(maxId);
    			userTimelinePage = twitter().getUserTimeline(paging);    			
    		}
    		Application.ratelimits.put("statuses/user_timeline", userTimelinePage.getRateLimitStatus());
    		return ok(views.html.twitter.timelinePage.render(userId, userTimelinePage));
    	} catch (TwitterException e) {
    		if (e.getRateLimitStatus() != null) {
    			Application.ratelimits.put("*", e.getRateLimitStatus());	
    		}   
    		return internalServerError("Twitter exception: " + e.getMessage());
    	}
    }

    private static TwitterUserPage evaluateQuery(String query, long cursor, boolean runInBackground) throws TwitterException {
    	TwitterUserPage result = null;
		if (query.startsWith("friends:")) {
			String screenName = query.substring("friends:".length()).trim();
			result = friends(screenName, cursor, runInBackground);
		} else if (query.startsWith("followers:")) {
			String screenName = query.substring("followers:".length()).trim();
			result = followers(screenName, cursor, runInBackground);
		} else if (query.startsWith("suggested:")) {
			String categorySlug = query.substring("suggested:".length()).trim();
			if (categorySlug.equals("")) {
				categorySlug = null;
			}
			result = suggestions(categorySlug, runInBackground);
		} else {
			String sql = query.trim();
			result = sql(sql, cursor, runInBackground);
			if (result == null) {
				Application.log.add("Could not execute query '" + query + "'");
			}
		}
		return result;
    }
    
    public static Result importAll(final String query) {
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
    	            					Application.waitForRatelimitForBackgroundTask("friends/ids");
		    	            			idsResponse = twitter().getFriendsIDs(screenName, idsResponse == null ? -1 : idsResponse.getNextCursor());
		    	            			Application.ratelimits.put("friends/ids", idsResponse.getRateLimitStatus());
    	            				} else {
    	            					Application.waitForRatelimitForBackgroundTask("followers/ids");
		    	            			idsResponse = twitter().getFollowersIDs(screenName, idsResponse == null ? -1 : idsResponse.getNextCursor());
		    	            			Application.ratelimits.put("followers/ids", idsResponse.getRateLimitStatus());
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
	    	            				System.out.println(ids.length);
	    	            				Application.waitForRatelimitForBackgroundTask("users/lookup");
	    	            				ResponseList<User> users = twitter().lookupUsers(ids);
	    	            				Application.ratelimits.put("users/lookup", users.getRateLimitStatus());
	    	            				for (User t4jUser: users) {
	    	            					TwitterUser.update(twitter(), null, t4jUser);
	    	            				}
	    	            				totalUsersImported += ids.length;
	    	            				Application.log.add("Imported " + totalUsersImported + " users.");
	    	            			}
    	            			}    	            			
    	            		} else {
	    	            		int userCount = 0;
		    	            	long cursor = -1;
		    	            	TwitterUserPage result = evaluateQuery(query, cursor, true);
		    	            	userCount += result.size();
		    	            	Application.log.add("Imported " + userCount + " users.");
		    	            	while (result.hasNext()) {
		    	            		result = evaluateQuery(query, result.getNextCursor(), true);
		    	            		userCount += result.size();
		    	            		Application.log.add("Imported " + userCount + " users.");
		    	            	}
    	            		}
    	            	} catch (TwitterException e) {
    	            		if (e.getRateLimitStatus() != null) {
    	            			Application.ratelimits.put("*", e.getRateLimitStatus());	
    	            		}   
    	            	} catch (Exception e) {
    	            		e.printStackTrace();
    	            		Application.log.add("Exception: " + e.getLocalizedMessage());
    	            	}
    	            }
    	        },
    	        Akka.system().dispatcher()
    	);
	    return ok("initiated import all for: '" + query + "'");
    }
    
    private static TwitterUserPage suggestions(String categorySlug, boolean runInBackground) throws TwitterException {
		if (runInBackground) {
			Application.waitForRatelimitForBackgroundTask("users/suggestions/:slug");
		}
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
    	Application.ratelimits.put("users/suggestions/:slug", response.getRateLimitStatus());
		TwitterUserPage userList = TwitterUserPage.create(twitter(), response);
		return userList;
    }
    
    private static TwitterUserPage friends(String screenName, long cursor, boolean backgroundTask) throws TwitterException {
    	if (backgroundTask) {
    		Application.waitForRatelimitForBackgroundTask("users/show", "friends/list");
    	}
    	
    	User twitterUser = twitter().showUser(screenName);
		PagableResponseList<User> pagableTwitterUsers = twitter().getFriendsList(screenName, cursor);
		TwitterUserPage userList = TwitterUserPage.create(twitter(), pagableTwitterUsers, twitterUser.getFriendsCount());
		Application.ratelimits.put("users/show", twitterUser.getRateLimitStatus());
		Application.ratelimits.put("friends/list", pagableTwitterUsers.getRateLimitStatus());
		return userList;
    }

	private static TwitterUserPage followers(String screenName, long cursor, boolean runInBackground) throws TwitterException {
		if (runInBackground) {
			Application.waitForRatelimitForBackgroundTask("users/show", "followers/list");
		}
    	User twitterUser = twitter().showUser(screenName);
		PagableResponseList<User> pagableTwitterUsers = twitter().getFollowersList(screenName, cursor);
		TwitterUserPage userList = TwitterUserPage.create(twitter(), pagableTwitterUsers, twitterUser.getFollowersCount());
		Application.ratelimits.put("users/show", twitterUser.getRateLimitStatus());
		Application.ratelimits.put("followers/list", pagableTwitterUsers.getRateLimitStatus());
		return userList;
    }
    
    private static TwitterUserPage sql(String sql, long cursor, boolean runInBackground) throws TwitterException {
		if (runInBackground) {
			Application.waitForRatelimitForBackgroundTask("users/show", "users/lookup");
		}
    	
    	Finder<Long, TwitterUser> finder = new Finder<Long, TwitterUser>(Long.class, TwitterUser.class);
    	
    	Page<TwitterUser> page = null;
    	try {
	    	page = finder.where(sql)
	    		.findPagingList(20)
	    		.setFetchAhead(false)
	    		.getPage(cursor == -1 ? 0 : (int)cursor);
			TwitterUserPage userList = TwitterUserPage.create(twitter(), page);
			return userList;
    	} catch (TwitterException e) {
    		if (e.getRateLimitStatus() != null) {
    			Application.ratelimits.put("*", e.getRateLimitStatus());	
    		}   
    		return null;
    	} catch (Exception e) {
    		return null;
    	}
    }
    
    public static Result follow(long id) {
    	try {
    		return ok(views.html.twitter.user.render(createAction(id, ActionType.beFriend, null)));
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}
    }
    
    public static Result unFollow(long id) {
    	try {
    		return ok(views.html.twitter.user.render(createAction(id, ActionType.unFriend, null)));
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}
    }
    
    public static Result retweet(long userId, final long statusId) {
    	try {
    		return ok(views.html.twitter.user.render(createAction(userId, ActionType.retweet, new Callback<Action>() {
				@Override
				public void invoke(Action arg) {
					arg.message = Long.toString(statusId);
				}    			
    		})));
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}
    }
    
    public interface Callback<T> {
    	void invoke(T arg);
    }
    
    private static TwitterUser createAction(long id, ActionType actionType, Callback<Action> editAction) throws TwitterException {    	
    	final TwitterUser twitterUser = TwitterUser.find.byId(id);
    	
    	final Action action = new Action();
    	action.service = Service.twitter;
    	action.direction = Direction.send;
    	action.actionType = actionType;
    	
    	action.scheduledFor = new Date();
    	action.executed = false;
		
    	if (editAction != null) {
    		editAction.invoke(action);
    	}
    	
    	twitterUser.getPresence().actions.add(action);
    	twitterUser.getPresence().save();
    	twitterUser.save();
    	User t4jUser = twitter().showUser(id);       
    	Application.ratelimits.put("users/show", t4jUser.getRateLimitStatus());
    	return TwitterUser.update(twitter, twitterUser, t4jUser);
    }
    
    public static Result createPresence(long id) {
    	try {
	    	TwitterUser twitterUser = update(id, new Callback<TwitterUser>() {
				@Override
				public void invoke(TwitterUser twitterUser) {
					Presence presence = twitterUser.getPresence();	    	
					presence.save();	
				}    		
	    	});
	    	return ok(views.html.presence.embedded.render(twitterUser.presence, null));
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}    	
    }
    
    private static TwitterUser update(long id, Callback<TwitterUser> updateTwitterUserCallback) {
    	TwitterUser twitterUser = TwitterUser.find.byId(id);
    	updateTwitterUserCallback.invoke(twitterUser);	    
		twitterUser.save();
		return twitterUser;
    	
    }
    
	public static Result star(long id, final boolean isStarred) {
		try {
	    	TwitterUser twitterUser = update(id, new Callback<TwitterUser>() {
				@Override
				public void invoke(TwitterUser twitterUser) {
					twitterUser.isStarred = isStarred;
				}
			});
	    	
	    	User t4jUser = twitter().showUser(id);
	    	Application.ratelimits.put("users/show", t4jUser.getRateLimitStatus());
			twitterUser = TwitterUser.update(twitter(), twitterUser, t4jUser);
			return ok(views.html.twitter.user.render(twitterUser));
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}
    }
}
