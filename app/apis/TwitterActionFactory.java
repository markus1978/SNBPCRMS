package apis;

import java.util.Date;

import models.Action;
import models.Action.ActionType;
import models.Action.Direction;
import models.Action.Service;
import models.Presence;
import models.TwitterUser;
import twitter4j.DirectMessage;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.User;
import apis.TwitterConnection.RateLimitPolicy;

public class TwitterActionFactory {

	private final TwitterConnection twitterConnection;

	public TwitterActionFactory(TwitterConnection twitterConnection) {
		super();
		this.twitterConnection = twitterConnection;
	}
	
	private void createFollow(long id, boolean follow, boolean receive) throws TwitterException {
		Action action = Action.create(follow ? ActionType.beFriend : ActionType.unFriend, Service.twitter, receive ? Direction.receive : Direction.send);
		
		TwitterUser twitterUser = null; 
		try {	
			twitterConnection.waitForRatelimitForBackgroundTask(RateLimitPolicy.wait, "users/show");
			User t4jUser = twitterConnection.twitter().showUser(id);
			twitterConnection.addRatelimit("users/show", t4jUser.getRateLimitStatus());
			twitterUser = TwitterUser.get(twitterConnection.getTwitterMe(), t4jUser);
		} catch (TwitterException e) {
			if (!twitterConnection.handleTwitterException(e, id)) {
				return; // action is dropped and not created
			}
		}	
		
		if (twitterUser == null) {
			// twitter user could not be retrieved from twitter, look into our db
			twitterUser = TwitterUser.find(id);
		}
		
		if (twitterUser == null) {
			// twitter user is not available
			// save nothing, action is dropped
		} else {
			Presence presence = twitterUser.getPresence();
			action.target = presence;
			presence.actions.add(action);
			action.isRead = false;
			
			action.save();    	
	    	presence.save();    	
	    	twitterUser.save();
		}
	}
	
	private void createReceiveMessage(User user, String text, long id, ActionType type, Date time) {
		Action action = Action.create(type, Service.twitter, Direction.receive);

		TwitterUser twitterUser = TwitterUser.get(twitterConnection.getTwitterMe(), user);
		Presence presence = twitterUser.getPresence();
		action.target = presence;
		presence.actions.add(action);
		action.message = text;
		action.replyToId = id;
		action.isRead = false;
		action.isExecuted = true;
		action.scheduledFor = time;
		action.executedAt = time;
		
		action.save();    	
    	presence.save();    	
    	twitterUser.save();
		
	}
	
	private void createReceiveMessage(Status status, ActionType type) {
		createReceiveMessage(status.getUser(), status.getText(), status.getId(), type, status.getCreatedAt());
	}
	
	public void receiveFollow(long id) throws TwitterException {
		createFollow(id, true, true);
	}

	public void sendFollow(long id) throws TwitterException  {
		createFollow(id, true, false);
	}

	public void sendUnFollow(long id) throws TwitterException  {
		createFollow(id, false, false);
	}

	public void receiveUnFollow(long id) throws TwitterException  {
		createFollow(id, false, true);
	}

	public void receiveMention(Status mention) {
		createReceiveMessage(mention, ActionType.publicMessage);
	}

	public void receiveRetweet(Status retweet) {
		createReceiveMessage(retweet, ActionType.retweet);		
	}

	public void receiveDirectMessage(DirectMessage directMessage) {
		createReceiveMessage(directMessage.getSender(), directMessage.getText(), directMessage.getId(), ActionType.message, directMessage.getCreatedAt());
	}
}
