package controllers;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import models.PagedUserHolderList;
import models.TwitterUser;
import models.TwitterUser.Category;
import models.TwitterUser.Tier;
import play.data.Form;
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

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Page;
import com.avaje.ebean.text.json.JsonContext;
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
    		PagedUserHolderList result = evaluateQuery(query, cursor);
    		if (result == null) {
    			return ok(index.render("Could not evaluate query '" + query + "'"));
    		}

    		return ok(views.html.users.render(query, result));
    	} catch (TwitterException e) {
    		return ok(index.render("Something went wrong: " + e.getErrorMessage() + "."));
    	}
    }
    
    private static PagedUserHolderList evaluateQuery(String query, long cursor) throws TwitterException {
    	PagedUserHolderList result = null;
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
	    	            	PagedUserHolderList result = evaluateQuery(query, cursor);
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
    
    private static PagedUserHolderList friends(String screenName, long cursor) throws TwitterException {
    	User twitterUser = twitter().showUser(screenName);
		PagableResponseList<User> pagableTwitterUsers = twitter().getFriendsList(screenName, cursor);
		PagedUserHolderList userList = PagedUserHolderList.create(twitter(), twitterUser, pagableTwitterUsers, twitterUser.getFriendsCount());
		return userList;
    }
    
    private static PagedUserHolderList followers(String screenName, long cursor) throws TwitterException {
    	User twitterUser = twitter().showUser(screenName);
		PagableResponseList<User> pagableTwitterUsers = twitter().getFollowersList(screenName, cursor);
		PagedUserHolderList userList = PagedUserHolderList.create(twitter(), twitterUser, pagableTwitterUsers, twitterUser.getFollowersCount());
		return userList;
    }
    
    private static PagedUserHolderList sql(String sql, long cursor) throws TwitterException {
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
    	
		PagedUserHolderList userList = PagedUserHolderList.create(twitter(), twitter().showUser("mscheidgen"), page);
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
}
