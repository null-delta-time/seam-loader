package sh.ndt.seam.mod.example;

import sh.ndt.seam.api.ConfigEntry;
import sh.ndt.seam.api.ModInitializer;
import sh.ndt.seam.api.SeamApi;

import java.util.Arrays;

public final class ExampleMod implements ModInitializer {

    private static final String MOD_ID = "example";
    private int ticks = 0;

    @Override
    public void onInitialize() {
        SeamApi.registerConfig(MOD_ID, Arrays.asList(
            new ConfigEntry("show_ticks", "Show Tick Counter", ConfigEntry.Type.BOOLEAN, Boolean.TRUE)
        ));

        SeamApi.onClientTick(() -> ticks++);
        SeamApi.appendToTitle(() -> {
            if (!Boolean.TRUE.equals(SeamApi.getConfigValue(MOD_ID, "show_ticks"))) return "";
            return " [t:" + ticks + "]";
        });
        System.out.println("[ExampleMod] initialized");
    }
}
