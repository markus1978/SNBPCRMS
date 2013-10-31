package controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import models.Action;
import models.Action.ActionType;
import models.Action.Direction;
import models.Action.Service;
import models.TwitterUser;
import models.TwitterUser.Category;
import models.TwitterUser.IUserHolder;
import models.TwitterUser.Tier;
import models.TwitterUserPage;
import play.db.ebean.Model.Finder;
import play.libs.Akka;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import scala.concurrent.duration.Duration;
import twitter4j.IDs;
import twitter4j.PagableResponseList;
import twitter4j.Paging;
import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import views.html.index;
import views.html.user;

import com.avaje.ebean.Page;
import com.fasterxml.jackson.databind.JsonNode;

public class Application extends Controller {
	
	private static final String consumerKey = "uObLVPxuBJqfrEOEB3ms1g";
	private static final String consumerSecret = "IAYcfFI5Xhq6g0McCdPVM5EEFOqq8PUkPH7KQu58w";
	private static final String accessToken = "127843079-7SDkbTQjdCK761FrZxd68ED6ktEMbSGn8pQTbe1h";
	private static final String accessSecret = "H32HL32hwiA1TOFfXQ7BA5GKFHJBoGl6qJYCYuBT2Wd4z";
	
	// this is actually somewhat bad, since everything should be pretty stateless
	private static Twitter twitter = null;
	public static Map<String, RateLimitStatus> ratelimits = new HashMap<String, RateLimitStatus>();
	private static ConcurrentLinkedQueue<String> log = new ConcurrentLinkedQueue<String>();
	
	public static Twitter twitter() {
		if (twitter == null) {
			TwitterFactory factory = new TwitterFactory();
			twitter = factory.getInstance();
			twitter.setOAuthConsumer(consumerKey, consumerSecret);
			twitter.setOAuthAccessToken(new AccessToken(accessToken, accessSecret));	
		}		
		return twitter;
	}
	
	public static Result ratelimits() {
		return ok(views.html.ratelimit.render(ratelimits));			
	}
	
	public static Result log() {
		JsonNode json = Json.toJson(log);
		log.clear();		
		return ok(json);
	}

    public static Result index() {
        return ok(index.render("This is SNBPCRMS."));
    }
                
    public static Result users(String query, long cursor) {
    	try {
    		TwitterUserPage result = evaluateQuery(query, cursor, false);
    		if (result == null) {
    			return ok(index.render("Could not evaluate query '" + query + "'"));
    		}

    		return ok(views.html.users.render(query, result));
    	} catch (TwitterException e) { 
    		if (e.getRateLimitStatus() != null) {
    			ratelimits.put("*", e.getRateLimitStatus());	
    		}    		
    		return ok(index.render("Something went wrong: " + e.getErrorMessage() + ". Retry at " + new Date(e.getRateLimitStatus().getResetTimeInSeconds()*(long)1000)));
    	}
    }
    
    public static Result ajaxUsers(String query, long cursor) {
    	try {
    		TwitterUserPage result = evaluateQuery(query, cursor, false);
    		if (result == null) {
    			return internalServerError("Could not evaluate query.");
    		}
    		return ok(views.html.userPage.render(query, result));
    	} catch (TwitterException e) {
    		if (e.getRateLimitStatus() != null) {
    			ratelimits.put("*", e.getRateLimitStatus());	
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
    		ratelimits.put("statuses/user_timeline", userTimelinePage.getRateLimitStatus());
    		return ok(views.html.timelinePage.render(userId, userTimelinePage));
    	} catch (TwitterException e) {
    		if (e.getRateLimitStatus() != null) {
    			ratelimits.put("*", e.getRateLimitStatus());	
    		}   
    		return internalServerError("Twitter exception: " + e.getMessage());
    	}
    }
    
    public static Result ajaxActions(long id) {    	
    	return ok(views.html.actions.render(TwitterUser.find.byId(id).actions));
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
				log.add("Could not execute query '" + query + "'");
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
	        	            			waitForRatelimitForBackgroundTask("friends/ids");
		    	            			idsResponse = twitter().getFriendsIDs(screenName, idsResponse == null ? -1 : idsResponse.getNextCursor());
		    	            			ratelimits.put("friends/ids", idsResponse.getRateLimitStatus());
    	            				} else {
    	            					waitForRatelimitForBackgroundTask("followers/ids");
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
	    	            				System.out.println(ids.length);
	    	            				waitForRatelimitForBackgroundTask("users/lookup");
	    	            				ResponseList<User> users = twitter().lookupUsers(ids);
	    	            				ratelimits.put("users/lookup", users.getRateLimitStatus());
	    	            				for (User t4jUser: users) {
	    	            					TwitterUser.update(twitter(), null, t4jUser);
	    	            				}
	    	            				totalUsersImported += ids.length;
	    	            				log.add("Imported " + totalUsersImported + " users.");
	    	            			}
    	            			}    	            			
    	            		} else {
	    	            		int userCount = 0;
		    	            	long cursor = -1;
		    	            	TwitterUserPage result = evaluateQuery(query, cursor, true);
		    	            	userCount += result.size();
		    	            	log.add("Imported " + userCount + " users.");
		    	            	while (result.hasNext()) {
		    	            		result = evaluateQuery(query, result.getNextCursor(), true);
		    	            		userCount += result.size();
		    	            		log.add("Imported " + userCount + " users.");
		    	            	}
    	            		}
    	            	} catch (TwitterException e) {
    	            		if (e.getRateLimitStatus() != null) {
    	            			ratelimits.put("*", e.getRateLimitStatus());	
    	            		}   
    	            	} catch (Exception e) {
    	            		e.printStackTrace();
    	            		log.add("Exception: " + e.getLocalizedMessage());
    	            	}
    	            }
    	        },
    	        Akka.system().dispatcher()
    	);
	    return ok("initiated import all for: '" + query + "'");
    }
    
    private static TwitterUserPage suggestions(String categorySlug, boolean runInBackground) throws TwitterException {
		if (runInBackground) {
			waitForRatelimitForBackgroundTask("users/suggestions/:slug");
		}
    	if (categorySlug == null) {
    		ResponseList<twitter4j.Category> suggestedUserCategories = twitter().getSuggestedUserCategories();
    		categorySlug = suggestedUserCategories.get(0).getSlug();
    		StringBuffer categories = new StringBuffer();
    		for (twitter4j.Category category: suggestedUserCategories) {
    			categories.append("'" + category.getName() + "':" + category.getSlug() + " ");
    		}
    		log.add("Suggestion categories are " + categories.toString());
    	}
    	ResponseList<twitter4j.User> response = twitter().getUserSuggestions(categorySlug);
		ratelimits.put("users/suggestions/:slug", response.getRateLimitStatus());
		TwitterUserPage userList = TwitterUserPage.create(twitter(), response);
		return userList;
    }
    
    private static TwitterUserPage friends(String screenName, long cursor, boolean backgroundTask) throws TwitterException {
    	if (backgroundTask) {
    		waitForRatelimitForBackgroundTask("users/show", "friends/list");
    	}
    	
    	User twitterUser = twitter().showUser(screenName);
		PagableResponseList<User> pagableTwitterUsers = twitter().getFriendsList(screenName, cursor);
		TwitterUserPage userList = TwitterUserPage.create(twitter(), pagableTwitterUsers, twitterUser.getFriendsCount());
		ratelimits.put("users/show", twitterUser.getRateLimitStatus());
		ratelimits.put("friends/list", pagableTwitterUsers.getRateLimitStatus());
		return userList;
    }
    
    private static void waitForRatelimitForBackgroundTask(String... ratelimits) {
		boolean approved = false;
		while (!approved) {
			approved = true;
			for(String ratelimit: ratelimits) {
				RateLimitStatus rateLimitStatus = Application.ratelimits.get(ratelimit);
				if (rateLimitStatus != null) {
					if (rateLimitStatus.getRemaining() <= 3) {
						approved = false;
						try {
							Thread.sleep(rateLimitStatus.getSecondsUntilReset()*1000);
						} catch (InterruptedException e) {						
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	private static TwitterUserPage followers(String screenName, long cursor, boolean runInBackground) throws TwitterException {
		if (runInBackground) {
			waitForRatelimitForBackgroundTask("users/show", "followers/list");
		}
    	User twitterUser = twitter().showUser(screenName);
		PagableResponseList<User> pagableTwitterUsers = twitter().getFollowersList(screenName, cursor);
		TwitterUserPage userList = TwitterUserPage.create(twitter(), pagableTwitterUsers, twitterUser.getFollowersCount());
		ratelimits.put("users/show", twitterUser.getRateLimitStatus());
		ratelimits.put("followers/list", pagableTwitterUsers.getRateLimitStatus());
		return userList;
    }
    
    private static TwitterUserPage sql(String sql, long cursor, boolean runInBackground) throws TwitterException {
		if (runInBackground) {
			waitForRatelimitForBackgroundTask("users/show", "users/lookup");
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
    			ratelimits.put("*", e.getRateLimitStatus());	
    		}   
    		return null;
    	} catch (Exception e) {
    		return null;
    	}
    }
    
    @BodyParser.Of(BodyParser.Json.class)
    public static Result update() {
    	JsonNode json = request().body().asJson();
    	Long id = json.get("id").asLong();
    	TwitterUser twitterUser = TwitterUser.find.byId(id);
    	
    	Iterator<Entry<String, JsonNode>> fields = json.fields();
    	while (fields.hasNext()) {
			Entry<String, JsonNode> next = fields.next();
			String key = next.getKey();
			if (key.equals("category")) {
				twitterUser.category = Category.valueOf(next.getValue().asText());
			} else if (key.equals("status")) {
				twitterUser.status = models.TwitterUser.Status.valueOf(next.getValue().asText());
			} else if (key.equals("tier")) {
				twitterUser.tier = Tier.valueOf(next.getValue().asText());
			}
		}
    		
    	twitterUser.save();
    	    
    	return ok("Updated user " + twitterUser.screenName + " with " + twitterUser.category);
    }
    
    public static Result follow(long id) {
    	try {
    		return ok(user.render(createAction(id, ActionType.beFriend, null)));
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}
    }
    
    public interface Callback<T> {
    	void invoke(T arg);
    }
    
    private static IUserHolder createAction(long id, ActionType actionType, Callback<Action> editAction) throws TwitterException {    	
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
    	
    	twitterUser.actions.add(action);
    	User t4jUser = twitter().showUser(id);       
    	ratelimits.put("users/show", t4jUser.getRateLimitStatus());
    	return TwitterUser.createHolder(twitter, twitterUser, t4jUser);
    }
    
    public static Result unFollow(long id) {
    	try {
    		return ok(user.render(createAction(id, ActionType.unFriend, null)));
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}
    }
    
    public static Result retweet(long userId, final long statusId) {
    	try {
    		return ok(user.render(createAction(userId, ActionType.retweet, new Callback<Action>() {
				@Override
				public void invoke(Action arg) {
					arg.message = Long.toString(statusId);
				}    			
    		})));
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}
    }
}
