package com.noclip.travel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.noclip.level.BackroomsLevel;
import com.noclip.level.Levels;
import com.noclip.sanity.Sanity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /noclip go|home|list|sanity — for testing and for server admins. */
public final class NoclipCommand {
    private NoclipCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("noclip")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("go")
                        .then(Commands.argument("level", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Levels.all().stream().map(l -> l.id), b))
                                .executes(c -> {
                                    ServerPlayer p = c.getSource().getPlayerOrException();
                                    String id = StringArgumentType.getString(c, "level");
                                    if (Levels.get(id) == null) {
                                        c.getSource().sendFailure(Component.literal("No such level: " + id));
                                        return 0;
                                    }
                                    return Travel.sendTo(p, id) ? 1 : 0;
                                })))
                .then(Commands.literal("home").executes(c -> Travel.sendTo(c.getSource().getPlayerOrException(), Travel.OVERWORLD) ? 1 : 0))
                .then(Commands.literal("list").executes(c -> {
                    StringBuilder sb = new StringBuilder();
                    for (BackroomsLevel l : Levels.all()) sb.append(l.id).append(" (").append(l.title).append(")  ");
                    c.getSource().sendSuccess(() -> Component.literal(Levels.count() + " levels: " + sb), false);
                    return Levels.count();
                }))
                .then(Commands.literal("sanity")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0, 100)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            Sanity.add(p, FloatArgumentType.getFloat(c, "value") - com.noclip.sanity.PlayerData.sanity(p));
                            return 1;
                        }))));
    }
}
