package models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

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
	
	public boolean iOS = false;	
	public boolean android = false;
	public boolean pc = false;
	public boolean consoles = false;
	public boolean more = false;
	
	@Constraints.Required
	public String name;
	    
	public String channelURLsBlob = "";
	
	public String contactURLsBlob = "";
	
	@Transient
	public List<String> channelURLs;
	@Transient
	public List<String> contactURLs;
	
	public Date lastActivity = new Date();
	
	private String serializeURLs(List<String> urls) {
		StringBuffer result = new StringBuffer();
		for (String url: urls) {
			url = url.trim();
			if (url.length() > 0) {
				result.append(url);
				result.append("%&§");
			}
		}
		return result.toString();
	}
	
	private List<String> deserializeURLs(String urlString) {
		return Arrays.asList(urlString.split("%&§"));
	}
	
	public void setChannelURLs(List<String> urls) {		
		channelURLsBlob = serializeURLs(urls);
	}
	
	public void setContactURLs(List<String> urls) {		
		contactURLsBlob = serializeURLs(urls);
	}

	public List<String>  getChannelURLs() {
		this.channelURLs = deserializeURLs(channelURLsBlob);
		return channelURLs;
	}

	public List<String>  getContactURLs() {
		this.contactURLs = deserializeURLs(contactURLsBlob);
		return contactURLs;
	}

	@Constraints.Required
	public Date added;

}
