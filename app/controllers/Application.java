package controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import twitter4j.RateLimitStatus;
import views.html.index;

import com.fasterxml.jackson.databind.JsonNode;

public class Application extends Controller {
	
	// this is actually somewhat bad, since everything should be pretty stateless	
	public static Map<String, RateLimitStatus> ratelimits = new HashMap<String, RateLimitStatus>();
	public static ConcurrentLinkedQueue<String> log = new ConcurrentLinkedQueue<String>();
	
	public static Result ratelimits() {
		return ok(views.html.ratelimit.render(ratelimits));			
	}
	
	public static Result log() {
		JsonNode json = Json.toJson(log);
		log.clear();		
		return ok(json);
	}

    public static Result index() {
        return ok(index.render("This is SNBPCRMS."));
    }    
            
    protected static void waitForRatelimitForBackgroundTask(String... ratelimits) {
		boolean approved = false;
		while (!approved) {
			approved = true;
			for(String ratelimit: ratelimits) {
				RateLimitStatus rateLimitStatus = Application.ratelimits.get(ratelimit);
				if (rateLimitStatus != null) {
					if (rateLimitStatus.getRemaining() <= 3) {
						approved = false;
						try {
							Thread.sleep(rateLimitStatus.getSecondsUntilReset()*1000);
						} catch (InterruptedException e) {						
							e.printStackTrace();
						}
					}
				}
			}
		}
	}


}
