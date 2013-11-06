package controllers;

import java.util.Date;
import java.util.List;

import models.Action;
import models.Action.ActionType;
import models.Action.Direction;
import models.Action.Service;
import models.Page;
import models.Presence;
import models.TwitterUser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.JsonUpdateMapper;
import apis.TwitterConnection;
import apis.TwitterConnection.RateLimitPolicy;

import com.fasterxml.jackson.databind.JsonNode;

public class TwitterPart extends Controller {
	
	public static Result ajaxTimeline(long userId, long maxId) {
		List<twitter4j.Status> userTimelinePage = TwitterConnection.timeline(userId, maxId, RateLimitPolicy.fail);    		    
		return ok(views.html.twitter.timelinePage.render(userId, userTimelinePage));    	
    }
	
    public static Result list(String query) {
		Page<TwitterUser> result = evaluateQuery(query, -1, RateLimitPolicy.fail);    		
		return ok(views.html.twitter.list.render(query, result));    	
    }
    
    public static Result ajaxPage(String query, long cursor) {
    	Page<TwitterUser> result = evaluateQuery(query, cursor, RateLimitPolicy.fail);
    	return ok(views.html.twitter.page.render(query, result));
    }
    
    private static Page<TwitterUser> evaluateQuery(String query, long cursor, RateLimitPolicy rateLimitPolicy) {    	
    	if (query.startsWith("friends:")) {
			String screenName = query.substring("friends:".length()).trim();
			return TwitterConnection.friends(screenName, cursor, rateLimitPolicy);
		} else if (query.startsWith("followers:")) {
			String screenName = query.substring("followers:".length()).trim();
			return TwitterConnection.followers(screenName, cursor, rateLimitPolicy);
		} else if (query.startsWith("suggested:")) {
			String categorySlug = query.substring("suggested:".length()).trim();
			if (categorySlug.equals("")) {
				categorySlug = null;
			}
			return TwitterConnection.suggestions(categorySlug, rateLimitPolicy);
		} else {
			return TwitterUser.find(query.trim(), 20, cursor);								
		}
    }
    
    public static Result importAll(final String query) {
    	TwitterConnection.startBackgroundImport(query);
	    return ok("initiated import all for: '" + query + "'");
    }
    
    
    public static Result follow(long id) {
    	return ok(views.html.twitter.row.render(createAndSaveAction(id, ActionType.beFriend, null)));
    }
    
    public static Result unFollow(long id) {
    	return ok(views.html.twitter.row.render(createAndSaveAction(id, ActionType.unFriend, null)));
    }
    
    public static Result retweet(long userId, final long statusId) {    	    	
		twitter4j.Status status = TwitterConnection.status(statusId, RateLimitPolicy.fail);
		final String body = status.getText();
		return ok(views.html.twitter.row.render(createAndSaveAction(userId, ActionType.retweet, new Callback<Action>() {
			@Override
			public void invoke(Action arg) {
				arg.replyToId = statusId;
				arg.message = body;
			}    			
		})));
    }
    
    public static Result favor(long userId, final long statusId) {    	
		twitter4j.Status status = TwitterConnection.status(statusId, RateLimitPolicy.fail);
		final String body = status.getText();
		return ok(views.html.twitter.row.render(createAndSaveAction(userId, ActionType.like, new Callback<Action>() {
			@Override
			public void invoke(Action arg) {
				arg.replyToId = statusId;
				arg.message = body;
			}    			
		})));
    }
    
    public static Result ajaxCreateDirectMessage(long targetId) {
		TwitterUser twitterUser = TwitterUser.find(targetId);
		Action action = createAction(ActionType.message);
		action.target = twitterUser.getPresence();		
		return ok(views.html.action.details.render(action, true));    	
    }
    
    public static Result ajaxCreateMentionTweet(long targetId) {    	
		TwitterUser twitterUser = TwitterUser.find(targetId);
		Action action = createAction(ActionType.publicMessage);
		action.target = twitterUser.getPresence();
		action.message = "@" + twitterUser.screenName + " ";   		
		return ok(views.html.action.details.render(action, true));    	
    }
    
    public static Result ajaxCreateTweet() {
		Action action = createAction(ActionType.publicMessage);
		action.direction = Direction.global;
		return ok(views.html.action.details.render(action, true));    	
    }
    
    public interface Callback<T> {
    	void invoke(T arg);
    }
    
    private static Action createAction(ActionType actionType) {    
    	final Action action = Action.create(actionType, Service.twitter, Direction.send);
    	action.service = Service.twitter;
    	action.direction = Direction.send;
    	action.actionType = actionType;
    	
    	action.scheduledFor = new Date();		    	
    	return action;
    }
    
    public static Result createPresence(long id) {
    	TwitterUser twitterUser = update(id, new Callback<TwitterUser>() {
			@Override
			public void invoke(TwitterUser twitterUser) {
				Presence presence = twitterUser.getPresence();	    	
				presence.save();	
			}    		
    	});
    	return ok(views.html.presence.embedded.render(twitterUser.presence, null));    	    
    }
    
    private static TwitterUser update(long id, Callback<TwitterUser> updateTwitterUserCallback) {
    	TwitterUser twitterUser = TwitterUser.find(id);
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
	    		    	
			return ok(views.html.twitter.row.render(twitterUser));
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}
    }
	
	public static Result ajaxSendDirectMessage(long targetId) {
		return ajaxSendMessage(targetId, ActionType.message);
	}
	
	public static Result ajaxSendMentionTweet(long targetId) {
		return ajaxSendMessage(targetId, ActionType.publicMessage);
	}
	
	public static Result ajaxSendTweet() {		
		Action action = createAction(ActionType.publicMessage);
		action.direction = Direction.global;
		
		JsonNode json = request().body().asJson();
		JsonUpdateMapper.update(json, action);	    		    	
    	action.save();

    	return ok();
	}
	
	private static Result ajaxSendMessage(long targetId, ActionType actionType) {
    	TwitterUser twitterUser = createAndSaveAction(targetId, actionType, new Callback<Action>() {
			@Override
			public void invoke(Action action) {
				JsonNode json = request().body().asJson();    
				JsonUpdateMapper.update(json, action);		    					
			}    		
    	});	 
    	return ok(views.html.twitter.row.render(twitterUser));
	}
	
    private static TwitterUser createAndSaveAction(long id, ActionType actionType, Callback<Action> editAction) {    	
    	final TwitterUser twitterUser = TwitterUser.find(id);    	
    	final Action action = createAction(actionType);
    	
    	if (editAction != null) {
    		editAction.invoke(action);
    	}
    	
    	Presence presence = twitterUser.getPresence();
    	presence.actions.add(action);
		presence.actions = presence.actions;
		if (presence.lastActivity.getTime() < action.scheduledFor.getTime()) {
			presence.lastActivity = action.scheduledFor;
		}
    	
		action.save();    	
    	presence.save();    	
    	twitterUser.save();
    	    	
    	return twitterUser;
    }
}
