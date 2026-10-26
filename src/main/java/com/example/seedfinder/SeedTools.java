package com.example.seedfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.biome.BiomeManager;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.LongStream;

/**
 * The server never sends the real world seed, but it does send a SHA-256 based
 * "hashed seed" (used for biome zoom). The client stores it inside BiomeManager.
 * Any candidate seed can be verified EXACTLY by hashing it and comparing.
 */
public final class SeedTools {
    private SeedTools() {}

    public static final long MASK48 = (1L << 48) - 1;
    public static final AtomicBoolean CANCEL = new AtomicBoolean(false);

    /** Reads the hashed seed the server sent to this client. Call on the main thread. */
    public static OptionalLong hashedSeed() {
        try {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) return OptionalLong.empty();
            BiomeManager bm = level.getBiomeManager();
            // BiomeManager has exactly one non-static long field: the hashed seed.
            for (Field f : BiomeManager.class.getDeclaredFields()) {
                if (f.getType() == long.class && !Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    return OptionalLong.of(f.getLong(bm));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return OptionalLong.empty();
    }

    public static boolean matches(long candidate, long hashed) {
        return BiomeManager.obfuscateSeed(candidate) == hashed;
    }

    /** Vanilla rule: numeric text is used as-is, anything else uses String.hashCode(). */
    public static long seedFromText(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return text.hashCode();
        }
    }

    /** Given the lower 48 bits (structure seed), finds the upper 16 bits. */
    public static List<Long> expandStructureSeed(long structureSeed, long hashed) {
        List<Long> found = new ArrayList<>();
        long low = structureSeed & MASK48;
        for (long hi = 0; hi < 65536L; hi++) {
            long seed = (hi << 48) | low;
            if (matches(seed, hashed)) found.add(seed);
        }
        return found;
    }

    /** Checks every seed in [from, to] in parallel. */
    public static List<Long> scan(long from, long to, long hashed) {
        CANCEL.set(false);
        return LongStream.rangeClosed(from, to).parallel()
                .filter(s -> !CANCEL.get() && matches(s, hashed))
                .boxed().toList();
    }
}
