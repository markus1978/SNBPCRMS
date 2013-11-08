package controllers;

import models.Action;
import models.Page;
import play.mvc.Controller;
import play.mvc.Result;

public class ActionPart extends Controller {
	public static Result list(String query) {    	    	
    	Page<Action> page = null;
    	try {
	    	page = Action.find(query, 20, 0);	
			for (Action action: page) {
				if (!action.isRead) {
					action.markRead();
				}
			}
	    	return ok(views.html.action.list.render(query, page));
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}		
	}
	
	public static Result ajaxPage(String query, long cursor) {
		Page<Action> page = null;
    	try {
	    	page = Action.find(query, 20, cursor);
	    	for (Action action: page) {
				if (!action.isRead) {
					action.markRead();
				}
			}
	    	return ok(views.html.action.page.render(query, page));
    	} catch (Exception e) {
    		return internalServerError(e.getMessage());
    	}
	}
	
	public static Result check(String query) {
		TwitterPart.check();
		return list(query);
	}
}
