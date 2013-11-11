package utils;

import java.net.UnknownHostException;

import models.Action;
import models.Presence;
import models.TwitterMe;
import models.TwitterUser;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import play.api.Play;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;

public class DataStoreConnection {
	
	private static Datastore datastore = null;
	private static Morphia morphia = null;
	
	public static <T> T map(BasicDBObject source, Class<T> theClass) {		
		return morphia().fromDBObject(theClass, source);
	}
	
	private static Morphia morphia() {
		if (morphia == null) {
			morphia = new Morphia();
			morphia.map(Action.class, Presence.class, TwitterUser.class, TwitterMe.class);
		}
		return morphia;
	}
	
	public static Datastore datastore() {
		if (datastore == null) {
			Morphia morphia = morphia();
			try {
				String dbName = "SNRMS";
				if (!Play.isProd(Play.current())) {
					dbName = "devel" + dbName;
				}
				datastore = morphia.createDatastore(new MongoClient("localhost"), dbName);
			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			} 

			datastore.ensureIndexes();			
			// text indexes
			datastore.getCollection(TwitterUser.class).ensureIndex(new BasicDBObject("description", "text"));
			
			datastore.ensureCaps();
		}
		
		return datastore;
	}
}
