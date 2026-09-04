package org.vidgamestudio.customizable_one_block;

import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Customizable_one_block.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(net.minecraft.commands.Commands.literal("oneblock")
                .requires(source -> source.hasPermission(2)) // Только для операторов
                .then(net.minecraft.commands.Commands.literal("reload")
                        .executes(context -> {
                            ModConfig.load();
                            context.getSource().sendSuccess(() -> Component.literal("§a[OneBlock] Конфигурация успешно перезагружена!"), true);
                            return 1;
                        })
                )
        );
    }

}
