package utils;

import java.net.UnknownHostException;

import models.Action;
import models.Presence;
import models.TwitterMe;
import models.TwitterUser;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import com.mongodb.MongoClient;

public class DataStoreConnection {
	
	private static Datastore datastore = null;
	
	public static Datastore datastore() {
		if (datastore == null) {
			Morphia morphia = new Morphia();
			try {
				datastore = morphia.createDatastore(new MongoClient("localhost"), "develSNRMS");
			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			} 

			morphia.map(Action.class, Presence.class, TwitterUser.class, TwitterMe.class);
			datastore.ensureIndexes();
			datastore.ensureCaps();
		}
		
		return datastore;
	}
}
