package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import play.data.validation.Constraints;
import play.db.ebean.Model;

@Entity
public class TwitterUser extends Model {

	private static final long serialVersionUID = 1L;
	
	public enum Tier { one, two, three, notAssigned };
	public enum Category { publication, developer, user, notAssigned };
	public enum Status { neutral, wishlist, ignored, notAssigned };

	@Id
	public Long id;

	@Constraints.Required
	public String screenName;
	
	@Constraints.Required
	public Date added;
	
	@Constraints.Required
	public Date lastUpdated;
	
	@Constraints.Required
	public boolean isFollower;
	
	public Date isFollowerSince;
	
	public int timesHasBeenFollower = 0;
	
	@Constraints.Required
	public boolean isFriend;
	
	public Date isFriendSince;
	
	public int timesHasBeenFriend = 0;

	@Constraints.Required
	public int followersCount;
	
	@Constraints.Required
	public int friendsCount;
	
	@Constraints.Required
	public Tier tier = Tier.notAssigned;
	
	@Constraints.Required
	public Category category = Category.notAssigned;
	
	@Constraints.Required
	public Status status = Status.notAssigned;

	@Constraints.Required
	public String description;
	
	public static Finder<Long,TwitterUser> find = new Finder<Long,TwitterUser>(Long.class, TwitterUser.class); 
	
	@OneToMany(mappedBy="target", cascade=CascadeType.ALL)
	public List<Action> actions = new ArrayList<Action>();
}
