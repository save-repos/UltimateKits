package com.songoda.ultimatekits.conversion;

import com.songoda.ultimatekits.KitPreview;
import com.songoda.ultimatekits.conversion.hooks.EssentialsHook;
import com.songoda.ultimatekits.conversion.hooks.UltimateCoreHook;
import com.songoda.ultimatekits.utils.Methods;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Convert {

    private final KitPreview instance;

    private Hook hook;

    public Convert(KitPreview instance) {
        this.instance = instance;
        if (instance.getServer().getPluginManager().getPlugin("Essentials") != null) {
            hook = new EssentialsHook();
        } else if (instance.getServer().getPluginManager().getPlugin("UltimateCore") != null) {
            hook = new UltimateCoreHook();
        }
        convertKits();
    }


    public void convertKits() {
        Set<String> kits = hook.getKits();

        if (instance.getKitFile().getConfig().contains("Kits")) return;

        for (String kit : kits) {
            List<String> serializedItems = new ArrayList<>();
            for (ItemStack item : hook.getItems(kit)) {
                serializedItems.add(Methods.serializeItemStack(item));
            }
            instance.getKitFile().getConfig().set("Kits." + kit + ".items", serializedItems);
            instance.getKitFile().getConfig().set("Kits." + kit + ".delay", hook.getDelay(kit));
            instance.getKitFile().getConfig().set("Kits." + kit + ".price", 0D);
        }
        instance.getKitFile().saveConfig();
    }
}
