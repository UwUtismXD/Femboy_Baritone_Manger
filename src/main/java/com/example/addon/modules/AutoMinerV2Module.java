package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class AutoMinerV2Module extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<List<net.minecraft.block.Block>> blocksToMine = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks-to-mine")
        .description("Select one or more blocks for Baritone to mine.")
        .filter(block -> !block.getTranslationKey().equals("block.minecraft.air"))
        .build()
    );

    private final Setting<String> periodicCommand = sgGeneral.add(new StringSetting.Builder()
        .name("periodic-command")
        .description("Command to run when idle for 10 seconds (without /).")
        .defaultValue("help")
        .build()
    );

    private static final int TICKS_STOPPED_THRESHOLD = 20 * 10; // 10 seconds

    private Vec3d lastPos = null;
    private int stoppedTicks = 0;
    private boolean initialized = false;

    public AutoMinerV2Module() {
        super(AddonTemplate.CATEGORY, "auto-miner-v2", "Automatically mines selected blocks and runs a command when idle for 10 seconds.");
    }

    private void sendMineCommand(List<net.minecraft.block.Block> blocks) {
        if (!isActive() || blocks == null || blocks.isEmpty()) return;
        if (mc.player == null || mc.world == null) return;
        String joined = blocks.stream().map(block -> block.getTranslationKey().replace("block.minecraft.", "")).reduce((a, b) -> a + " " + b).orElse("");
        ChatUtils.sendPlayerMsg("#mine " + joined);
    }

    private void runPeriodicCommand() {
        String cmd = periodicCommand.get().trim();
        if (!cmd.isEmpty()) {
            ChatUtils.sendPlayerMsg("/" + cmd);
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (!initialized) {
            if (isActive()) {
                toggle();
            }
            initialized = true;
            return;
        }

        if (!isActive()) return;
        if (mc.player == null || mc.world == null) return;

        Vec3d currentPos = mc.player.getPos();
        if (lastPos != null && currentPos.equals(lastPos)) {
            stoppedTicks++;
            if (stoppedTicks >= TICKS_STOPPED_THRESHOLD) {
                runPeriodicCommand();
                stoppedTicks = 0;
            }
        } else {
            stoppedTicks = 0;
        }
        lastPos = currentPos;
    }

    @Override
    public void onActivate() {
        stoppedTicks = 0;
        lastPos = null;
        mc.execute(() -> sendMineCommand(blocksToMine.get()));
    }

    @Override
    public void onDeactivate() {
        if (mc.player != null && mc.world != null) {
            ChatUtils.sendPlayerMsg("#stop");
        }
    }
}