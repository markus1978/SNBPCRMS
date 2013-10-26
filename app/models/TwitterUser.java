package models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

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
	
	@Constraints.Required
	public boolean isFriend;

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
}
