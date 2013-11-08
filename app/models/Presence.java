package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.utils.IndexDirection;

import utils.DataStoreConnection;


@Entity
public class Presence {
	
	public enum Tier { one, two, three, notAssigned };
	public enum Category { publication, developer, user, notAssigned };
	public enum Status { neutral, wishlist, ignored, notAssigned };
	 	
	@Id private ObjectId id;
	
	@Reference public TwitterUser twitterUser;	
	@Reference public List<Action> actions = new ArrayList<Action>();
	
	public Tier tier = Tier.notAssigned;
	public Category category = Category.notAssigned;
	
	public boolean iOS = false;	
	public boolean android = false;
	public boolean pc = false;
	public boolean consoles = false;
	public boolean more = false;
	
	public String name;    
	public List<String> channelURLs = new ArrayList<String>();
	public List<String> contactURLs = new ArrayList<String>();
			
	@Indexed(value=IndexDirection.DESC) public Date lastActivity;				
	@Indexed(value=IndexDirection.DESC) public Date added;
	
	public static Presence create(TwitterUser twitterUser) {		
		Presence presence = new Presence();
		presence.name = twitterUser.screenName;
		presence.added = new Date();
		presence.lastActivity = presence.added;
		presence.channelURLs.add(TwitterUser.getProfileURL(twitterUser));
		presence.twitterUser = twitterUser;
		return presence;
	}
	
	public void save() {
		DataStoreConnection.datastore().save(this);
	}
	
	public static Page<Presence> find(String whereConditions, int count, int offset) {
		Query<Presence> query = DataStoreConnection.datastore()
				.find(Presence.class);
		if (whereConditions != null && !whereConditions.trim().equals("")) {
			query = query.where("function() { return (" + whereConditions + "); }");
		}
		query
				.order("-lastActivity")
				.offset(offset)
				.limit(count)
				.disableCursorTimeout();
		
		return new MongoDBPage<Presence>(query);
	}
	
	public static Presence find(String id) {
		return DataStoreConnection.datastore().get(Presence.class, new ObjectId(id));
	}
	
	public boolean isStored() {
		return id != null;
	}
	
	public String getId() {
		return id == null ? null : id.toStringMongod();
	}
}
