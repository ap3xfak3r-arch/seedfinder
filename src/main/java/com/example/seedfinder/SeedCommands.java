package com.example.seedfinder;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.List;
import java.util.OptionalLong;

public final class SeedCommands {
    private SeedCommands() {}

    static void say(String text) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.gui.getChat().addMessage(Component.literal("[SeedFinder] " + text)));
    }

    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("seedfinder")
            .then(Commands.literal("info").executes(ctx -> {
                OptionalLong h = SeedTools.hashedSeed();
                if (h.isEmpty()) { say("Join a world or server first."); return 0; }
                say("Hashed seed from server: " + h.getAsLong());
                return 1;
            }))
            .then(Commands.literal("check")
                .then(Commands.argument("seed", LongArgumentType.longArg()).executes(ctx -> {
                    OptionalLong h = SeedTools.hashedSeed();
                    if (h.isEmpty()) { say("Join a world or server first."); return 0; }
                    long s = LongArgumentType.getLong(ctx, "seed");
                    say(SeedTools.matches(s, h.getAsLong()) ? "MATCH! The seed is " + s : s + " is NOT the seed.");
                    return 1;
                })))
            .then(Commands.literal("text")
                .then(Commands.argument("word", StringArgumentType.greedyString()).executes(ctx -> {
                    OptionalLong h = SeedTools.hashedSeed();
                    if (h.isEmpty()) { say("Join a world or server first."); return 0; }
                    String w = StringArgumentType.getString(ctx, "word");
                    long s = SeedTools.seedFromText(w);
                    say(SeedTools.matches(s, h.getAsLong())
                        ? "MATCH! \"" + w + "\" gives seed " + s
                        : "\"" + w + "\" (seed " + s + ") is NOT the seed.");
                    return 1;
                })))
            .then(Commands.literal("expand")
                .then(Commands.argument("structureSeed", LongArgumentType.longArg()).executes(ctx -> {
                    OptionalLong h = SeedTools.hashedSeed();
                    if (h.isEmpty()) { say("Join a world or server first."); return 0; }
                    long hashed = h.getAsLong();
                    long low = LongArgumentType.getLong(ctx, "structureSeed");
                    say("Trying all 65536 upper-bit combinations...");
                    Thread t = new Thread(() -> {
                        List<Long> r = SeedTools.expandStructureSeed(low, hashed);
                        say(r.isEmpty() ? "No match. That structure seed is wrong."
                                        : "FOUND full seed(s): " + r);
                    }, "seedfinder-expand");
                    t.setDaemon(true);
                    t.start();
                    return 1;
                })))
            .then(Commands.literal("scan")
                .then(Commands.argument("from", LongArgumentType.longArg())
                .then(Commands.argument("to", LongArgumentType.longArg()).executes(ctx -> {
                    OptionalLong h = SeedTools.hashedSeed();
                    if (h.isEmpty()) { say("Join a world or server first."); return 0; }
                    long hashed = h.getAsLong();
                    long from = LongArgumentType.getLong(ctx, "from");
                    long to = LongArgumentType.getLong(ctx, "to");
                    say("Scanning " + from + " to " + to + " (/seedfinder cancel to stop)...");
                    Thread t = new Thread(() -> {
                        List<Long> r = SeedTools.scan(from, to, hashed);
                        say(r.isEmpty() ? "Scan finished, nothing found." : "FOUND seed(s): " + r);
                    }, "seedfinder-scan");
                    t.setDaemon(true);
                    t.start();
                    return 1;
                }))))
            .then(Commands.literal("cancel").executes(ctx -> {
                SeedTools.CANCEL.set(true);
                say("Cancel requested.");
                return 1;
            }))
        );
    }
}
