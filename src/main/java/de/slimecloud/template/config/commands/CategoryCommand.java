package de.slimecloud.template.config.commands;

import de.mineking.discordutils.commands.Command;
import de.mineking.discordutils.commands.CommandManager;
import de.mineking.discordutils.commands.context.ICommandContext;
import de.slimecloud.template.config.GuildConfig;
import de.slimecloud.template.config.engine.CategoryInfo;
import de.slimecloud.template.config.engine.ConfigField;
import de.slimecloud.template.main.Bot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class CategoryCommand extends Command<ICommandContext> {
	public CategoryCommand(@NotNull Bot bot, @NotNull CommandManager<ICommandContext, ?> manager, @Nullable Field field, @NotNull Function<GuildConfig, Object> instance, @NotNull CategoryInfo category, @NotNull Field[] fields) {
		super(manager, category.command(), category.description());

		Map<String, Field> fieldMap = new HashMap<>();
		Set<Field> required = new HashSet<>();
		Set<Field> optional = new HashSet<>();

		for (Field f : fields) {
			//Ignore fields without @ConfigField
			ConfigField info = f.getAnnotation(ConfigField.class);
			if (info == null) continue;

			f.setAccessible(true);

			fieldMap.put(info.command(), f);

			if (info.required()) required.add(f);
			else optional.add(f);
		}

		//Add enable / disable
		if (field != null) {
			addSubcommand(new DisableCommand(bot, manager, field, category));
			addSubcommand(new EnableCommand(bot, manager, instance, field, category, fieldMap, required, optional));
		}
	}

	@NotNull
	public static CategoryCommand getCommand(@NotNull Bot bot, @NotNull CommandManager<ICommandContext, ?> manager, @NotNull Field field) {
		CategoryInfo info = field.getAnnotation(CategoryInfo.class);
		if (info == null) throw new IllegalArgumentException("Category is missing annotation!");

		return new CategoryCommand(bot, manager, field, c -> {
			try {
				//Get category
				Object temp = field.get(c);

				//Create category if not present
				if (temp == null) {
					temp = field.getType().getConstructor().newInstance();
					field.set(c, temp);

					bot.updateGuildCommands(c.getGuild());
				}

				return temp;
			} catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}, info, field.getType().getDeclaredFields());
	}

	@Override
	public void performCommand(@NotNull ICommandContext context) throws Exception {
	}
}
