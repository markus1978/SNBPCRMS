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
				}
			}
	    		
	    	presence.save();
	    	
	    	return ok("Update complete");
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}
	}
}
