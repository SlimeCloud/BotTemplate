package de.slimecloud.template.main;

import de.mineking.discordutils.DiscordUtils;
import de.mineking.discordutils.commands.Cache;
import de.mineking.discordutils.commands.Command;
import de.mineking.discordutils.commands.context.ICommandContext;
import de.mineking.javautils.database.DatabaseManager;
import de.slimecloud.template.config.ActivityConfig;
import de.slimecloud.template.config.Config;
import de.slimecloud.template.config.GuildConfig;
import de.slimecloud.template.config.LogForwarding;
import de.slimecloud.template.config.commands.ConfigCommand;
import de.slimecloud.template.main.extensions.ColorOptionParser;
import de.slimecloud.template.main.extensions.ColorTypeMapper;
import de.slimecloud.template.main.extensions.IDOptionParser;
import de.slimecloud.template.main.extensions.SnowflakeTypeMapper;
import de.slimecloud.template.util.ColorUtil;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class Bot extends ListenerAdapter {
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(0);

	private final Config config;
	private final Dotenv credentials;

	private final JDA jda;
	private final DiscordUtils<Bot> discordUtils;

	private final DatabaseManager database;

	public Bot(@NotNull Config config, @NotNull Dotenv credentials) throws IOException {
		this.config = config;
		this.credentials = credentials;

		//Verify token
		String token = credentials.get("DISCORD_TOKEN");
		if (token == null) throw new IllegalArgumentException("No token specified");

		//Initialize database if configured
		String databaseHost = credentials.get("DATABASE_HOST");
		if (databaseHost != null) {
			database = new DatabaseManager("jdbc:postgresql://" + databaseHost, credentials.get("DATABASE_USER"), credentials.get("DATABASE_PASSWORD"));
			database.getDriver().installPlugin(new PostgresPlugin());

			database.putData("bot", this);
			database.addMapper(new SnowflakeTypeMapper());
			database.addMapper(new ColorTypeMapper());

			//Initialize tables


		} else {
			logger.warn("Database credentials missing! Some features will be disabled!");

			database = null;
		}


		//Setup JDA
		JDABuilder builder = JDABuilder.createDefault(credentials.get("DISCORD_TOKEN"))
				//Show startup activity, see #startActivity
				.setStatus(OnlineStatus.IDLE)
				.setActivity(Activity.customStatus("Bot startet..."))

				//Configuration
				.enableIntents(EnumSet.allOf(GatewayIntent.class))
				.setMemberCachePolicy(MemberCachePolicy.ALL)

				//Listeners
				.addEventListeners(this);

		//Configure DiscordUtils
		discordUtils = DiscordUtils.create(builder, this)
				.mirrorConsole(config.getLogForwarding().stream().map(LogForwarding::build).toList())
				.useEventManager(null)
				.useUIManager(null)
				.useCommandManager(e -> () -> e, e -> () -> e, manager -> {
					manager.registerOptionParser(new ColorOptionParser());
					manager.registerOptionParser(new IDOptionParser());

					manager.registerCommand(new Command<>(manager, "test") {
						@Override
						public void performCommand(@NotNull ICommandContext context) throws Exception {
							throw new IOException();
						}
					});

					manager.registerCommand(ConfigCommand.class);

					//Register commands


					/*
					Automatically update comDiscordWrmands
					The parameter function loads the guild configuration and provides it as cache to all commands.
					Tish way, the configuration does not have to be reloaded for every registration check.

					This cache is also automatically passed to all future calls to updateGuildCommands
					 */
					manager.updateCommands(g -> new Cache().put("config", loadGuild(g)));
				})
				.useListManager(manager -> manager.setPageOption(new OptionData(OptionType.INTEGER, "seite", "Startseite").setMinValue(1)))
				.build();

		jda = discordUtils.getJDA();
	}

	@NotNull
	public Logger getLogger() {
		return logger;
	}

	public void updateGuildCommands(@NotNull Guild guild) {
		discordUtils.getCommandManager().updateGuildCommands(guild).queue();
	}

	@Override
	public void onReady(@NotNull ReadyEvent event) {
		startActivity();
	}

	private void startActivity() {
		jda.getPresence().setStatus(OnlineStatus.ONLINE);

		List<Activity> activities = config.getActivity().activities.stream()
				.map(ActivityConfig.ActivityEntry::build)
				.toList();

		//Schedule random activity updates
		executor.scheduleAtFixedRate(
				() -> jda.getPresence().setActivity(activities.get(Main.random.nextInt(activities.size()))),
				0, config.getActivity().interval, TimeUnit.SECONDS
		);
	}

	@Override
	public void onGuildReady(GuildReadyEvent event) {
		//Send startup message
		loadGuild(event.getGuild()).getLogChannel().ifPresent(channel ->
				channel.sendMessageEmbeds(
						new EmbedBuilder()
								.setTitle("Bot wurde gestartet")
								.setDescription("Der Bot hat sich mit der DiscordAPI (neu-) verbunden")
								.addField("Version", BuildInfo.version, true)
								.setColor(getColor(event.getGuild()))
								.setTimestamp(Instant.now())
								.build()
				).queue()
		);
	}

	public void scheduleDaily(int hour, @NotNull Runnable task) {
		long day = TimeUnit.DAYS.toSeconds(1);
		long initialDelay = Instant.now().atOffset(ZoneOffset.UTC)
				.withHour(hour)
				.withMinute(0)
				.withSecond(0)
				.toEpochSecond();

		if (initialDelay < 0) initialDelay += day;

		executor.scheduleAtFixedRate(() -> {
			try {
				task.run();
			} catch (Exception e) {
				logger.error("An error occurred in daily task", e);
			}
		}, initialDelay, day, TimeUnit.SECONDS);
	}

	@NotNull
	public GuildConfig loadGuild(long guild) {
		return GuildConfig.readFromFile(this, guild);
	}

	@NotNull
	public GuildConfig loadGuild(@NotNull Guild guild) {
		return loadGuild(guild.getIdLong());
	}

	@NotNull
	public Color getColor(long guild) {
		return Optional.ofNullable(loadGuild(guild).getColor()).orElseGet(() -> Color.decode(config.getDefaultColor()));
	}

	@NotNull
	public Color getColor(@Nullable Guild guild) {
		return Optional.ofNullable(guild)
				.map(g -> loadGuild(g).getColor())
				.or(() -> Optional.ofNullable(ColorUtil.parseColor(config.getDefaultColor())))
				.orElseThrow(); //Only happens if neither guild nor main config have valid configuration
	}

	//TODO: Add possibility to call this method to gracefully shutdown the bot
	public void shutdown() {
		try {
			logger.info("Shutting down...");

			executor.shutdownNow();

			jda.shutdown();
			if (!jda.awaitShutdown(Duration.ofSeconds(10))) {
				jda.shutdownNow();
				jda.awaitShutdown();
			}
		} catch (Exception e) {
			logger.error("Regular shutdown failed, forcing shutdown...", e);
			System.exit(1);
		}
	}
}
