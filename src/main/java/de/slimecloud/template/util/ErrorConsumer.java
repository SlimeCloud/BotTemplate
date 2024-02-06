package de.slimecloud.template.util;

public interface ErrorConsumer<T> {
	void accept(T arg) throws Exception;
}
