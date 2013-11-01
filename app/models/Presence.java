package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import play.data.validation.Constraints;
import play.db.ebean.Model;

@Entity
public class Presence extends Model {

	private static final long serialVersionUID = 1L;
	
	public enum Tier { one, two, three, notAssigned };
	public enum Category { publication, developer, user, notAssigned };
	public enum Status { neutral, wishlist, ignored, notAssigned };
	
	public static Finder<Long,Presence> find = new Finder<Long,Presence>(Long.class, Presence.class); 
	
	@Id
	public Long id;
	
	@OneToOne(mappedBy="presence")
	public TwitterUser twitterUser;
	
	@OneToMany(mappedBy="target", cascade=CascadeType.ALL)
	public List<Action> actions = new ArrayList<Action>();
	
	@Constraints.Required
	public Tier tier = Tier.notAssigned;
	
	@Constraints.Required
	public Category category = Category.notAssigned;
	
	public String name;
	
	@Constraints.Required
	public Date added;

}
