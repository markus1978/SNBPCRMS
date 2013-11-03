package controllers;

import java.util.Iterator;
import java.util.Map.Entry;

import models.Presence;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;

public class PresencePart extends Controller {
	public static Result ajaxActions(long id) {
		return ok(views.html.presence.actions.render(Presence.find.byId(id).actions));
	}
	
	public static Result ajaxUpdate(long id) {
		try {
	    	Presence presence = Presence.find.byId(id);
	    	JsonNode json = request().body().asJson();    	
	    	
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
				}
			}
	    		
	    	presence.save();
	    	
	    	return ok("Update complete");
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}
	}
	
	public static Result ajaxDetails(long id) {
		try {
			return ok(views.html.presence.details.render(Presence.find.byId(id)));	
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
