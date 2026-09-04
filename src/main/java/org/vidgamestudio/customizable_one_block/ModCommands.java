package org.vidgamestudio.customizable_one_block;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Customizable_one_block.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(net.minecraft.commands.Commands.literal("oneblock")
                .requires(source -> source.hasPermission(2))
                .then(net.minecraft.commands.Commands.literal("reload")
                        .executes(context -> {
                            ModConfig.load();
                            context.getSource().sendSuccess(() -> Component.literal("§a[OneBlock] Конфигурация успешно перезагружена!"), true);
                            return 1;
                        })
                )
                .then(Commands.literal("hand")
                        .then(Commands.argument("chance", IntegerArgumentType.integer(1, 100))
                                .executes(context -> executeOnlyChance(
                                        context,
                                        IntegerArgumentType.getInteger(context, "chance")
                                ))
                                .then(Commands.argument("min", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("max", IntegerArgumentType.integer(1))
                                                .executes(context -> executeWithMinMax(
                                                        context,
                                                        IntegerArgumentType.getInteger(context, "chance"),
                                                        IntegerArgumentType.getInteger(context, "min"),
                                                        IntegerArgumentType.getInteger(context, "max")
                                                ))
                                        )
                                )
                        )
                )
        );
    }

    private static int executeOnlyChance(CommandContext<CommandSourceStack> context, int chance) {
        CommandSourceStack source = context.getSource();
        String itemRegistryName = getItemInHandId(source);
        if (itemRegistryName == null) return 0;

        MutableComponent clickableItem = Component.literal(itemRegistryName+":"+chance)
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, itemRegistryName+":"+chance))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Нажми, чтобы скопировать ID")))
                        .withColor(ChatFormatting.GREEN)
                );

        MutableComponent fullMessage = Component.literal("Предмет: ")
                .append(clickableItem);

        source.sendSuccess(() -> fullMessage, false);
        return 1;
    }

    private static int executeWithMinMax(CommandContext<CommandSourceStack> context, int chance, int min, int max) {
        CommandSourceStack source = context.getSource();
        if (min > max) {
            source.sendFailure(Component.literal("Ошибка: min не может быть больше max!"));
            return 0;
        }
        String itemRegistryName = getItemInHandId(source);
        if (itemRegistryName == null) return 0;

        MutableComponent clickableItem = Component.literal(min+";"+itemRegistryName+";"+max+":"+chance)
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, min+";"+itemRegistryName+";"+max+":"+chance))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Нажми, чтобы скопировать ID")))
                        .withColor(ChatFormatting.GREEN)
                );

        MutableComponent fullMessage = Component.literal("Предмет: ")
                .append(clickableItem);

        source.sendSuccess(() -> fullMessage, false);
        return 1;
    }

    private static String getItemInHandId(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Эту команду может использовать только игрок!"));
            return null;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || stack.is(Items.AIR)) {
            source.sendFailure(Component.literal("Ошибка: Вы должны держать предмет в руке!"));
            return null;
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

}
