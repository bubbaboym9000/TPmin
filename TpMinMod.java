package com.example.tpmin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.Locale;
import java.util.Set;

/**
 * Adds /tpmin, which teleports the player to -9,223,372,036,854,775,808 (Long.MIN_VALUE).
 *
 *   /tpmin     sets both X and Z
 *   /tpmin x   sets X only
 *   /tpmin z   sets Z only
 *
 * Y is left alone. Vanilla clamps positions in several places, so the command reports where
 * the player actually ended up next to the position that was requested.
 */
public final class TpMinMod implements ModInitializer {
    public static final String MOD_ID = "tpmin";

    // -2^63 is exactly representable as a double.
    private static final double TARGET = (double) Long.MIN_VALUE;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpmin")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(ctx -> teleport(ctx, true, true))
                .then(Commands.literal("x").executes(ctx -> teleport(ctx, true, false)))
                .then(Commands.literal("z").executes(ctx -> teleport(ctx, false, true))));
    }

    private static int teleport(CommandContext<CommandSourceStack> ctx, boolean setX, boolean setZ)
            throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();

        double x = setX ? TARGET : player.getX();
        double z = setZ ? TARGET : player.getZ();
        ServerLevel level = (ServerLevel) player.level();

        // Called as a statement on purpose: works whether this overload returns boolean or void.
        player.teleportTo(level, x, player.getY(), z, Set.of(), player.getYRot(), player.getXRot(), true);

        String message = String.format(Locale.ROOT,
                "Requested X=%s Z=%s, now at X=%s Z=%s",
                whole(x), whole(z), whole(player.getX()), whole(player.getZ()));
        source.sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    /** Prints a double with every digit, no scientific notation. */
    private static String whole(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }
}
