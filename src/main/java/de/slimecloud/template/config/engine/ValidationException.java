package de.slimecloud.template.config.engine;

public class ValidationException extends RuntimeException {
	public ValidationException(Exception e) {
		super(e);
	}
}
