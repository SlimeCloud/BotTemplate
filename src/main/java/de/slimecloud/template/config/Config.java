package de.slimecloud.template.config;

import de.slimecloud.template.config.engine.Required;
import de.slimecloud.template.main.Main;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
public class Config {
	@Required
	private String defaultColor;

	private Long developerRole;

	@Required
	private String guildStorage;

	@Required
	private ActivityConfig activity;

	@Required
	private final List<LogForwarding> logForwarding = Collections.emptyList();

	private final List<Long> timeoutIgnore = Collections.emptyList();

	private String githubRepository;

	@NotNull
	public static Config readFromFile(@NotNull String path) throws Exception {
		try (FileReader reader = new FileReader(path)) {
			Config config = Main.json.fromJson(reader, Config.class);
			check(config);
			return config;
		}
	}

	private static void check(Object instance) throws IllegalAccessException {
		for (Field f : instance.getClass().getDeclaredFields()) {
			f.setAccessible(true);
			Object value = f.get(instance);

			if (value != null && value.getClass().getName().startsWith("de.slimecloud")) check(value);
			if (value == null && f.isAnnotationPresent(Required.class))
				throw new IllegalArgumentException("Required config value " + f.getName() + " has no value");
		}
	}
}
