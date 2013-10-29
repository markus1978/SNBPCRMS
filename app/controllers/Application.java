package controllers;

import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import models.Action;
import models.Action.ActionType;
import models.Action.Direction;
import models.Action.Service;
import models.TwitterUserPage;
import models.TwitterUser;
import models.TwitterUser.Category;
import models.TwitterUser.IUserHolder;
import models.TwitterUser.Tier;
import play.db.ebean.Model.Finder;
import play.libs.Akka;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import scala.concurrent.duration.Duration;
import twitter4j.PagableResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import views.html.index;
import views.html.list;
import views.html.user;

import com.avaje.ebean.Page;
import com.fasterxml.jackson.databind.JsonNode;

public class Application extends Controller {
	
	private static final String consumerKey = "uObLVPxuBJqfrEOEB3ms1g";
	private static final String consumerSecret = "IAYcfFI5Xhq6g0McCdPVM5EEFOqq8PUkPH7KQu58w";
	private static final String accessToken = "127843079-7SDkbTQjdCK761FrZxd68ED6ktEMbSGn8pQTbe1h";
	private static final String accessSecret = "H32HL32hwiA1TOFfXQ7BA5GKFHJBoGl6qJYCYuBT2Wd4z";
	
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
	
	private static ConcurrentLinkedQueue<String> log = new ConcurrentLinkedQueue<String>();
	
	public static Result log() {
		JsonNode json = Json.toJson(log);
		log.clear();
		return ok(json);
	}

    public static Result index() {
        return ok(index.render("Your new application is ready."));
    }
    
    public static Result add(String id) {
    	try {
    		User twitterUser = twitter().showUser(id);
    		
    		TwitterUser user = new TwitterUser();
    		user.id = twitterUser.getId();
    		user.screenName = twitterUser.getScreenName();
    		user.followersCount = twitterUser.getFollowersCount();
    		user.save();
        	
    		return ok(index.render("Added Twitter user with screen name " + user.screenName + "."));
    	} catch (TwitterException e) {
    		return ok(index.render("Something went wrong: " + e.getErrorMessage() + "."));
    	}
    }
    
    public static Finder<Long, TwitterUser> finder = new Finder<Long, TwitterUser>(Long.class, TwitterUser.class);
    
    public static Result list(int pageNumber, String sortBy, String order) {
    	 Page<TwitterUser> page = finder.where()
    		        .orderBy(sortBy + " " + order)
    		        .findPagingList(10)
    		        .setFetchAhead(false)
    		        .getPage(pageNumber);
    	return ok(list.render(page, sortBy, order));
    }
    
    public static Result users(String query, long cursor) {
    	try {
    		TwitterUserPage result = evaluateQuery(query, cursor);
    		if (result == null) {
    			return ok(index.render("Could not evaluate query '" + query + "'"));
    		}

    		return ok(views.html.users.render(query, result));
    	} catch (TwitterException e) {
    		return ok(index.render("Something went wrong: " + e.getErrorMessage() + "."));
    	}
    }
    
    public static Result ajaxUsers(String query, long cursor) {
    	try {
    		TwitterUserPage result = evaluateQuery(query, cursor);
    		if (result == null) {
    			return internalServerError("Could not evaluate query.");
    		}
    		return ok(views.html.userPage.render(query, result));
    	} catch (TwitterException e) {
    		return internalServerError("Twitter exception: " + e.getMessage());
    	}
    }
    
    private static TwitterUserPage evaluateQuery(String query, long cursor) throws TwitterException {
    	TwitterUserPage result = null;
		if (query.startsWith("friends:")) {
			String screenName = query.substring("friends:".length()).trim();
			result = friends(screenName, cursor);
		} else if (query.startsWith("followers:")) {
			String screenName = query.substring("followers:".length()).trim();
			result = followers(screenName, cursor);
		} else {
			String sql = query.trim();
			result = sql(sql, cursor);
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
    	            		int userCount = 0;
	    	            	long cursor = -1;
	    	            	TwitterUserPage result = evaluateQuery(query, cursor);
	    	            	userCount += result.size();
	    	            	log.add("Imported " + userCount + " users.");
	    	            	while (result.hasNext()) {
	    	            		result = evaluateQuery(query, result.getNextCursor());
	    	            		userCount += result.size();
	    	            		log.add("Imported " + userCount + " users.");
	    	            	}
    	            	} catch (TwitterException e) {
    	            		log.add("Twitter error: " + e.getLocalizedMessage());
    	            	}
    	            }
    	        },
    	        Akka.system().dispatcher()
    	);
	    return ok("initiated import all for: '" + query + "'");
    }
    
    private static TwitterUserPage friends(String screenName, long cursor) throws TwitterException {
    	User twitterUser = twitter().showUser(screenName);
		PagableResponseList<User> pagableTwitterUsers = twitter().getFriendsList(screenName, cursor);
		TwitterUserPage userList = TwitterUserPage.create(twitter(), pagableTwitterUsers, twitterUser.getFriendsCount());
		return userList;
    }
    
    private static TwitterUserPage followers(String screenName, long cursor) throws TwitterException {
    	User twitterUser = twitter().showUser(screenName);
		PagableResponseList<User> pagableTwitterUsers = twitter().getFollowersList(screenName, cursor);
		TwitterUserPage userList = TwitterUserPage.create(twitter(), pagableTwitterUsers, twitterUser.getFollowersCount());
		return userList;
    }
    
    private static TwitterUserPage sql(String sql, long cursor) throws TwitterException {
    	Finder<Long, TwitterUser> finder = new Finder<Long, TwitterUser>(Long.class, TwitterUser.class);
    	
    	Page<TwitterUser> page = null;
    	try {
	    	page = finder.where(sql)
	    		.findPagingList(20)
	    		.setFetchAhead(false)
	    		.getPage(cursor == -1 ? 0 : (int)cursor);
    	} catch (Exception e) {
    		return null;
    	}
    	
		TwitterUserPage userList = TwitterUserPage.create(twitter(), twitter().showUser("mscheidgen"), page);
		return userList;
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
    
    @BodyParser.Of(BodyParser.Json.class)
    public static Result follow() {
    	try {
    		return ok(user.render(createAction(ActionType.beFriend)));
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}
    }
    
    private static IUserHolder createAction(ActionType actionType) throws TwitterException {    	
    	JsonNode json = request().body().asJson();
    	Long id = json.get("id").asLong();
    	final TwitterUser twitterUser = TwitterUser.find.byId(id);
    	
    	final Action action = new Action();
    	action.service = Service.twitter;
    	action.direction = Direction.send;
    	action.actionType = ActionType.beFriend;
    	
    	action.scheduledFor = new Date();
    	action.executed = false;
    	
    	twitterUser.actions.add(action);
    	User t4jUser = twitter().showUser(id);        	
    	return TwitterUser.createHolder(twitter, twitterUser, t4jUser);
    }
    
    @BodyParser.Of(BodyParser.Json.class)
    public static Result unFollow() {
    	try {
    		return ok(user.render(createAction(ActionType.unFriend)));
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}
    }
}
