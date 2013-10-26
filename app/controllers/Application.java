package controllers;

import java.util.ArrayList;
import java.util.List;

import com.avaje.ebean.Page;

import models.PagedUserHolderList;
import models.TwitterUser;
import play.db.ebean.Model.Finder;
import play.mvc.Controller;
import play.mvc.Result;
import twitter4j.PagableResponseList;
import twitter4j.Relationship;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import views.html.*;
import views.html.defaultpages.error;

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
    		PagedUserHolderList result = null;
    		if (query.startsWith("friends:")) {
    			String screenName = query.substring("friends:".length()).trim();
    			result = friends(screenName, cursor);
    		} else if (query.startsWith("sql: ")){
    			String sql = query.substring("sql:".length()).trim();
    			result = sql(sql, cursor);
    		} else {
    			return ok(index.render("Did not understand query."));
    		}

    		return ok(views.html.users.render(query, result));
    	} catch (TwitterException e) {
    		return ok(index.render("Something went wrong: " + e.getErrorMessage() + "."));
    	}
    }
    
    public static Result importAll(String query) {
    	return ok(index.render("Import All is not done yet."));
    }
    
    private static PagedUserHolderList friends(String screenName, long cursor) throws TwitterException {
    	User twitterUser = twitter().showUser(screenName);
		PagableResponseList<User> pagableTwitterUsers = twitter().getFriendsList(screenName, cursor);
		PagedUserHolderList userList = PagedUserHolderList.create(twitter, twitterUser, pagableTwitterUsers, twitterUser.getFriendsCount());
		return userList;
    }
    
    private static PagedUserHolderList sql(String sql, long cursor) throws TwitterException {
    	Finder<Long, TwitterUser> finder = new Finder<Long, TwitterUser>(Long.class, TwitterUser.class);
    	
    	Page<TwitterUser> page = finder.where(sql)
    		.findPagingList(10)
    		.setFetchAhead(false)
    		.getPage(cursor == -1 ? 0 : (int)cursor);
    	
		PagedUserHolderList userList = PagedUserHolderList.create(twitter, twitter.showUser("mscheidgen"), page);
		return userList;
    }
}
