package apis;

import models.Action;
import models.Action.ActionType;
import models.Action.Direction;
import models.Action.Service;
import models.Presence;
import models.TwitterUser;
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
		twitterConnection.waitForRatelimitForBackgroundTask(RateLimitPolicy.wait, "users/show");
		User t4jUser = twitterConnection.twitter().showUser(id);
		twitterConnection.addRatelimit("users/show", t4jUser.getRateLimitStatus());
		TwitterUser twitterUser = TwitterUser.get(twitterConnection.getTwitterMe(), t4jUser);
		Presence presence = twitterUser.getPresence();
		action.target = presence;
		presence.actions.add(action);
		
		action.save();    	
    	presence.save();    	
    	twitterUser.save();
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
}
