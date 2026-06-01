package sh.ndt.seam.mod.example;

import sh.ndt.seam.api.ModInitializer;
import sh.ndt.seam.api.SeamApi;

public final class ExampleMod implements ModInitializer {

    private int ticks = 0;

    @Override
    public void onInitialize() {
        SeamApi.onClientTick(() -> ticks++);
        SeamApi.appendToTitle(() -> " [t:" + ticks + "]");
        System.out.println("[ExampleMod] initialized");
    }
}
