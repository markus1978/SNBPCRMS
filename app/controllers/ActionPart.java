package controllers;

import models.Action;
import play.mvc.Controller;
import play.mvc.Result;

import com.avaje.ebean.Page;

public class ActionPart extends Controller {
	public static Result list(String query) {    	    	
    	Page<Action> page = null;
    	try {
	    	page = Action.find	    		
	    		.orderBy("scheduledFor desc")
	    		.findPagingList(20)
	    		.setFetchAhead(false)
	    		.getPage(0);			
	    	return ok(views.html.action.list.render(query, page));
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}		
	}
	
	public static Result ajaxPage(String query, long cursor) {
		Page<Action> page = null;
    	try {
	    	page = Action.find
	    		.orderBy("scheduledFor desc")
	    		.findPagingList(20)
	    		.setFetchAhead(false)
	    		.getPage((int)cursor);			
	    	return ok(views.html.action.page.render(query, page));
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}
	}
}
