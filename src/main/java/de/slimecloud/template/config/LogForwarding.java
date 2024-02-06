package de.slimecloud.template.config;

import de.mineking.discordutils.console.RedirectTarget;
import de.slimecloud.template.config.engine.Required;
import de.slimecloud.template.main.Bot;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record LogForwarding(@Required Type type, @Required String id) {
	enum Type {
		USER {
			@NotNull
			@Override
			public RedirectTarget<Bot> build(@NotNull String id) {
				return RedirectTarget.directMessage(Long.parseLong(id));
			}
		},
		CHANNEL {
			@NotNull
			@Override
			public RedirectTarget<Bot> build(@NotNull String id) {
				return RedirectTarget.channel(Long.parseLong(id));
			}
		},
		WEBHOOK {
			@NotNull
			@Override
			public RedirectTarget<Bot> build(@NotNull String value) {
				return RedirectTarget.webhook(value);
			}
		};

		@NotNull
		public abstract RedirectTarget<Bot> build(@NotNull String value);
	}

	@NotNull
	public RedirectTarget<Bot> build() {
		return RedirectTarget.pingRoleOnError(type.build(id), bot -> Optional.ofNullable(bot.getConfig().getDeveloperRole()).map(bot.getJda()::getRoleById));
	}
}
