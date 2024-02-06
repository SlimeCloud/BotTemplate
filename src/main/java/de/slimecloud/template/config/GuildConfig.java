package de.slimecloud.template.config;

import de.slimecloud.template.config.engine.CategoryInfo;
import de.slimecloud.template.config.engine.ConfigField;
import de.slimecloud.template.config.engine.ConfigFieldType;
import de.slimecloud.template.config.engine.Info;
import de.slimecloud.template.main.Main;
import de.slimecloud.template.main.Bot;
import de.slimecloud.template.util.ColorUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.*;

@Slf4j
@CategoryInfo(name = "Standard", command = "general", description = "Generelle Konfiguration des Servers")
@ToString
public class GuildConfig {
	@NotNull
	public static GuildConfig readFromFile(@NotNull Bot bot, long guild) {
		String path = bot.getConfig().getGuildStorage().replace("%guild%", String.valueOf(guild));

		try {
			File file = new File(path);

			if (!file.exists()) return new GuildConfig().configure(bot, path, guild);

			try (FileReader reader = new FileReader(file)) {
				return Main.json.fromJson(reader, GuildConfig.class).configure(bot, path, guild);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@ToString.Exclude
	private transient Bot bot;

	private transient long guild;
	private transient String path;

	@Setter
	@ConfigField(name = "Farbe", command = "color", description = "Die primäre Farbe des Servers", type = ConfigFieldType.COLOR)
	private String color;

	@Setter
	@ConfigField(name = "Team", command = "team", description = "Die Team-Rolle", type = ConfigFieldType.ROLE)
	private Long teamRole;
	
	@Setter
	@ConfigField(name = "Log-Kanal", command = "log", description = "Kanal, in dem Log-Nachrichten für den Bot gesendet werden", type = ConfigFieldType.MESSAGE_CHANNEL)
	private Long logChannel;

	@NotNull
	private GuildConfig configure(@NotNull Bot bot, @NotNull String path, long guild) {
		this.bot = bot;
		this.guild = guild;
		this.path = path;

		return this;
	}

	@NotNull
	public Bot getBot() {
		return bot;
	}

	@NotNull
	public Guild getGuild() {
		return bot.getJda().getGuildById(guild);
	}

	@Nullable
	public Color getColor() {
		return color == null || color.isEmpty() ? null : ColorUtil.parseColor(color);
	}

	@NotNull
	public Optional<Role> getTeamRole() {
		return Optional.ofNullable(teamRole).map(bot.getJda()::getRoleById);
	}

	@NotNull
	public Optional<GuildMessageChannel> getLogChannel() {
		return Optional.ofNullable(logChannel).map(id -> bot.getJda().getChannelById(GuildMessageChannel.class, id));
	}

	@NotNull
	public GuildConfig save() {
		try {
			File file = new File(path);

			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}

			try (FileWriter writer = new FileWriter(file)) {
				Main.formattedJson.toJson(this, writer);
			}
		} catch (IOException e) {
			logger.error("Failed to write guild config", e);
		}
		return this;
	}
}
