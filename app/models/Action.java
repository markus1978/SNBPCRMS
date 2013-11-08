package models;

import java.util.Date;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.utils.IndexDirection;

import utils.DataStoreConnection;

@Entity("actions")
public class Action {

	public enum ActionType { beFriend, unFriend, message, retweet, publicMessage, like };
	public enum Service { twitter, facebook, youtube };
	public enum Direction { receive, send, global };

	@Id private ObjectId id;	
	public ActionType actionType = null;
	public Service service = null;	
	public Direction direction = null;	
	public String message = null;
	public Long replyToId = null;	
	@Indexed(value=IndexDirection.DESC) public Date scheduledFor = null;	
	public Date executedAt = null;	
	public boolean isExecuted = false;		
	@Reference public Presence target = null;
	public boolean isRead = true;
	
	public static Action create(ActionType actionType, Service service, Direction direction) {
		Action action = new Action();
		action.actionType = actionType;
		action.service = service;
		action.direction = direction;
		
		action.scheduledFor = new Date();
		
		return action;
	}
	
	public void save() {
		DataStoreConnection.datastore().save(this);
	}
	
	public void updateTarget(Presence target) {
		DataStoreConnection.datastore().update(
			this,
			DataStoreConnection.datastore()
				.createUpdateOperations(Action.class)
					.add("target", target)
		);
	}
	
	public static Page<Action> find(String whereConditions, int count, long offset) {
		Query<Action> query = DataStoreConnection.datastore()
				.find(Action.class);
		if (whereConditions != null && !whereConditions.trim().equals("")) {
			query = query.where("function() { return (" + whereConditions + "); }");
		}
		query
				.order("-scheduledFor")
				.offset((int)offset)
				.limit(count)
				.disableCursorTimeout();
		
		return new MongoDBPage<Action>(query);
	}
	
	public static Action find(String id) {
		return DataStoreConnection.datastore().get(Action.class, new ObjectId(id));
	}
	
	public boolean isStored() {
		return id != null;
	}

	public String getId() {
		return id == null ? null : id.toStringMongod();
	}

	public void markRead() {
		UpdateOperations<Action> setRead = DataStoreConnection.datastore().createUpdateOperations(Action.class).set("isRead", true);
		DataStoreConnection.datastore().update(this, setRead);
	}

	public void delete() {
		DataStoreConnection.datastore().delete(this);
	}
}
