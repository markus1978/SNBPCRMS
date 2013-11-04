package controllers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import models.Presence;
import play.mvc.Controller;
import play.mvc.Result;

import com.avaje.ebean.Page;
import com.fasterxml.jackson.databind.JsonNode;

public class PresencePart extends Controller {
	
	public static Result list(String query) {    	    	
    	Page<Presence> page = null;
    	try {
	    	page = Presence.find
	    		.findPagingList(20)
	    		.setFetchAhead(false)
	    		.getPage(0);			
	    	return ok(views.html.presence.list.render(query, page));
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}		
	}
	
	public static Result ajaxPage(String query, long cursor) {
		Page<Presence> page = null;
    	try {
	    	page = Presence.find
	    		.findPagingList(20)
	    		.setFetchAhead(false)
	    		.getPage((int)cursor);			
	    	return ok(views.html.presence.page.render(query, page));
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}
	}
	
	public static Result ajaxActions(long id) {
		return ok(views.html.presence.actions.render(Presence.find.byId(id).actions, true));
	}
	
	public static Result ajaxUpdate(long id) {
		try {
	    	updatePresenceWithJson(id);	    	
	    	return ok("Update complete");
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}
	}
	
	private static Presence updatePresenceWithJson(long id) {
		Presence presence = Presence.find.byId(id);
    	JsonNode json = request().body().asJson();    	
    	
    	List<String> channelURLs = null;
    	List<String> contactURLs = null;
    	
    	Iterator<Entry<String, JsonNode>> fields = json.fields();
    	while (fields.hasNext()) {
			Entry<String, JsonNode> next = fields.next();
			String key = next.getKey();
			if (key.equals("category")) {
				presence.category = models.Presence.Category.valueOf(next.getValue().asText());
			} else if (key.equals("name")) {
				presence.name = next.getValue().asText();
			} else if (key.equals("tier")) {
				presence.tier = models.Presence.Tier.valueOf(next.getValue().asText());
			} else if (key.equals("iOS")) {
				presence.iOS = next.getValue().asBoolean();
			} else if (key.equals("android")) {
				presence.android = next.getValue().asBoolean();
			} else if (key.equals("pc")) {
				presence.pc = next.getValue().asBoolean();
			} else if (key.equals("consoles")) {
				presence.consoles = next.getValue().asBoolean();
			} else if (key.equals("more")) {
				presence.more = next.getValue().asBoolean();
			} else if (key.startsWith("channelURLs")) {
				if (channelURLs == null) {
					channelURLs = new ArrayList<String>();
				}
				channelURLs.add(next.getValue().asText());
			} else if (key.startsWith("contactURLs")) {
				if (contactURLs == null) {
					contactURLs = new ArrayList<String>();
				}
				contactURLs.add(next.getValue().asText());
			}
		}

    	if (contactURLs != null) {
    		presence.setContactURLs(contactURLs);
    	}
    	if (channelURLs != null) {
    		presence.setChannelURLs(channelURLs);
    	}
    	presence.save();
    	
    	return presence;
	}
	
	public static Result ajaxUpdateDetails(long id) {
		try {
	    	Presence presence = updatePresenceWithJson(id);
	    	
	    	return ok(views.html.presence.embedded.render(presence, null));
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}
	}
	
	public static Result ajaxDetails(long id) {
		try {
			Presence presence = Presence.find.byId(id);
			presence.getContactURLs().size();
			presence.getChannelURLs().size();
			return ok(views.html.presence.details.render(presence));	
		} catch (Exception e) {
			return internalServerError(e.getMessage());
		}	
	}
	
	public static Result ajaxEmbedded(long id) {
		try {
			return ok(views.html.presence.embedded.render(Presence.find.byId(id), null));	
		} catch (Exception e) {
			return internalServerError(e.getMessage());
		}	
	}
}
