package org.vidgamestudio.customizable_one_block;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Customizable_one_block.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModScoreboard {

    public static final String OBJECTIVE_NAME = "ob_broken";

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Scoreboard scoreboard = player.getServer().getScoreboard();
            Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);

            // Использовать DUMMY — безопасно, он никогда не будет гореть красным
            if (objective == null) {
                objective = scoreboard.addObjective(
                        OBJECTIVE_NAME,
                        ObjectiveCriteria.DUMMY,
                        Component.literal("§bБлоков сломано"),
                        ObjectiveCriteria.RenderType.INTEGER
                );
                scoreboard.setDisplayObjective(Scoreboard.DISPLAY_SLOT_LIST, objective);
            }
        }
    }

    public static void updatePlayerScore(ServerPlayer player, int totalBroken) {
        Scoreboard scoreboard = player.getServer().getScoreboard();
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective != null) {
            scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective).setScore(totalBroken);
        }
    }
}
