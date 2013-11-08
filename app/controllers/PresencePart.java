package controllers;

import models.Page;
import models.Presence;
import play.mvc.Controller;
import play.mvc.Result;
import utils.JsonUpdateMapper;

public class PresencePart extends Controller {
	
	public static Result list(String query) {    	    	    	
    	Page<Presence> page = Presence.find(query, 20, 0);			
    	return ok(views.html.presence.list.render(query, page));
	}
	
	public static Result ajaxPage(String query, long cursor) {		    	
		Page<Presence> page = Presence.find(query, 20, (int)cursor);	    				
    	return ok(views.html.presence.page.render(query, page));    	
	}
	
	public static Result ajaxUpdate(String id) {
		Presence presence = Presence.find(id);
		JsonUpdateMapper.update(request().body().asJson(), presence);
		presence.save();
		return ok();		
	}
	
	public static Result ajaxUpdateDetails(String id, String reloadTemplate) {
		Presence presence = Presence.find(id);
		JsonUpdateMapper.update(request().body().asJson(), presence);
		presence.save();
		
		if (reloadTemplate.equals("embedded")) {
    		return ok(views.html.presence.embedded.render(presence, null));	
    	} else if (reloadTemplate.equals("row")) {
    		return ok(views.html.presence.row.render(presence));
    	} else {
    		throw new RuntimeException("Unknown template");
    	}			
	}
		
	public static Result ajaxDetails(String id) {
		return ok(views.html.presence.details.render(Presence.find(id)));		
	}
	
	public static Result ajaxEmbedded(String id) {
		return ok(views.html.presence.embedded.render(Presence.find(id), null));		
	}
	
	public static Result ajaxDelete(String id) {
		Presence.find(id).delete();
		return ok(views.html.presence.embedded.render(null, null));
	}
}
