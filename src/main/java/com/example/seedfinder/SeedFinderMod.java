package com.example.seedfinder;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only mod: registers the /seedfinder client commands. */
@Mod(value = SeedFinderMod.MODID, dist = Dist.CLIENT)
public class SeedFinderMod {
    public static final String MODID = "seedfinder";

    public SeedFinderMod(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(SeedCommands::register);
    }
}
