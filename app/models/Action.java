package models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import play.data.validation.Constraints;
import play.db.ebean.Model;

@Entity
public class Action extends Model {

	private static final long serialVersionUID = 1L;

	public enum ActionType { beFriend, unFriend, message, retweet, publicMessage };
	public enum Service { twitter, facebook, youtube };
	public enum Direction { receive, send, global };
	
	public static Finder<Long,Action> find = new Finder<Long,Action>(Long.class, Action.class);
	
	public static String abr(ActionType type) {
		if (type == ActionType.beFriend) {
			return "o";
		} else if (type == ActionType.unFriend) {
			return "x";
		} else if (type == ActionType.message) {
			return "m";
		} else if (type == ActionType.retweet) {
			return "R";
		} else {
			return "ERROR";
		}
	}
	
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
	public Presence target;
}
