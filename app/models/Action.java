package models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import play.data.validation.Constraints;
import play.db.ebean.Model;

@Entity
public class Action extends Model {

	public enum ActionType { beFriend, unFriend, message, retweet };
	public enum Service { twitter };
	public enum Direction { receive, send, global };
	
	@Id
	public long id;
	
	@Constraints.Required
	public Service service;
	@Constraints.Required
	public ActionType actionType;
	@Constraints.Required
	public Direction direction;
	
	public String message;
	public Date scheduledFor;
	public Date executedAt;
	
	@Constraints.Required
	public boolean executed;	
	
	@ManyToOne
	public TwitterUser target;
}
