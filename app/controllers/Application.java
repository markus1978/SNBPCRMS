package controllers;

import java.util.concurrent.ConcurrentLinkedQueue;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;
import apis.TwitterConnection;

import com.fasterxml.jackson.databind.JsonNode;

public class Application extends Controller {
	
	// this is actually somewhat bad, since everything should be pretty stateless	
	public static ConcurrentLinkedQueue<String> log = new ConcurrentLinkedQueue<String>();
	
	public static Result ratelimits() {
		return ok(views.html.ratelimit.render(TwitterConnection.ratelimits()));			
	}
	
	public static Result log() {
		JsonNode json = Json.toJson(log);
		log.clear();		
		return ok(json);
	}

    public static Result index() {
        return ok(index.render("This is SNBPCRMS."));
    }    
            
 
}
