package controllers;

import twitter4j.TwitterException;

public class CouldNotCompleteException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CouldNotCompleteException(TwitterException e) {
		super(e);
	}

}
