package de.slimecloud.template.util;

import de.slimecloud.template.config.engine.ValidationException;
import de.slimecloud.template.util.types.AtomicString;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringUtil {
	private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("((?<=[a-z])(?=[A-Z]))|((?<=[A-Z])(?=[A-Z][a-z]))");

	@NotNull
	public static String[] parseCamelCase(@NotNull String s) {
		return Arrays.stream(CAMEL_CASE_PATTERN.split(s))
				.map(String::toLowerCase)
				.toArray(String[]::new);
	}

	@NotNull
	public static String prettifyCamelCase(@NotNull String s) {
		return Arrays.stream(parseCamelCase(s))
				.map(StringUtils::capitalize)
				.collect(Collectors.joining(" "));
	}

	@NotNull
	public static String format(@NotNull String s, @NotNull Map<String, Object> args) {
		if (s.isEmpty()) return args.values().toString();

		AtomicString format = new AtomicString(s);
		args.forEach((k, v) -> format.set(format.get().replace("%" + k + "%", String.valueOf(v))));

		return format.get();
	}

	@Contract("null -> false")
	public static boolean isInteger(@Nullable String s) {
		if (s == null || s.isBlank()) return false;

		try {
			Integer.parseInt(s);
			return true;
		} catch (NumberFormatException | NullPointerException e) {
			return false;
		}
	}

	@Contract("null -> false")
	public static boolean isNumeric(@Nullable String s) {
		if (s == null || s.isBlank()) return false;

		try {
			Double.parseDouble(s);
			return true;
		} catch (NumberFormatException | NullPointerException e) {
			return false;
		}
	}

	@Contract("null -> false")
	public static boolean isValidURL(@Nullable String url) {
		if (url == null || url.isBlank()) return false;

		try {
			new URL(url).toURI();
			return true;
		} catch (MalformedURLException | URISyntaxException e) {
			return false;
		}
	}

	@NotNull
	public static String extractUrl(@NotNull OptionMapping value) {
		try {
			if (isValidURL(value.getAsString())) value.getAsString();
			throw new ValidationException(null);
		} catch (Exception e) {
			throw new ValidationException(e);
		}
	}

	@NotNull
	public static Enum<?> extractEnum(@NotNull Class<?> type, @NotNull OptionMapping value) {
		return Arrays.stream(type.getEnumConstants())
				.map(e -> (Enum<?>) e)
				.filter(e -> e.name().equals(value.getAsString()))
				.findFirst().orElseThrow(() -> new ValidationException(null));
	}

	@NotNull
	public static String padRight(@NotNull String s, int n) {
		return padRight(s, ' ', n);
	}

	@NotNull
	public static String padLeft(@NotNull String s, int n) {
		return padLeft(s, ' ', n);
	}

	@NotNull
	public static String padRight(@NotNull String s, char padChar, int n) {
		if (s.length() >= n) return s;
		return s + String.valueOf(padChar).repeat(n - s.length());
	}

	@NotNull
	public static String padLeft(@NotNull String s, char padChar, int n) {
		if (s.length() >= n) return s;
		return String.valueOf(padChar).repeat(n - s.length()) + s;
	}


	@NotNull
	public static String createProgressBar(double value, int length, char pad, String delimiter) {
		return padRight("━".repeat((int) (value * length)) + "╸" + delimiter, pad, length + delimiter.length() + 1);
	}

	@NotNull
	public static String createProgressBar(double value, int length) {
		return "\033[32m" + createProgressBar(value, length, '━', "\033[30m╺") + "\033[0m";
	}
}
