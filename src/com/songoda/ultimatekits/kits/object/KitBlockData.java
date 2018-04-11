package com.songoda.ultimatekits.kits.object;

import org.bukkit.Location;

public class KitBlockData {

    private boolean hologram, particles, items;

    private final Kit kit;
    private final Location location;

    public KitBlockData(Kit kit, Location location, boolean hologram, boolean particles, boolean items) {
        this.kit = kit;
        this.location = location;
        this.hologram = hologram;
        this.particles = particles;
        this.items = items;
    }

    public KitBlockData(Kit kit, Location location) {
        this(kit, location, false, false, false);
    }

    public Kit getKit() {
        return kit;
    }

    public Location getLocation() {
        return location.clone();
    }

    public boolean showHologram() {
        return hologram;
    }

    public void setShowHologram(boolean hologram) {
        this.hologram = hologram;
    }

    public boolean hasParticles() {
        return particles;
    }

    public void setHasParticles(boolean particles) {
        this.particles = particles;
    }

    public boolean isDisplayingItems() {
        return items;
    }

    public void setDisplayingItems(boolean items) {
        this.items = items;
    }

//    public void removeKitFromBlock(Player player) {
//        try {
//            removeDisplayItems();
//
//            String kit = KitPreview.getInstance().getConfig().getString("data.block." + locationStr);
//            KitPreview.getInstance().getConfig().set("data.holo." + locationStr, null);
//            KitPreview.getInstance().getConfig().set("data.particles." + locationStr, null);
//            KitPreview.getInstance().getConfig().set("data.displayitems." + locationStr, null);
//            KitPreview.getInstance().saveConfig();
//            KitPreview.getInstance().holo.updateHolograms();
//            KitPreview.getInstance().getConfig().set("data.block." + locationStr, null);
//            KitPreview.getInstance().saveConfig();
//            KitPreview.getInstance().holo.updateHolograms();
//            player.sendMessage(TextComponent.formatText(KitPreview.getInstance().references.getPrefix() + "&8Kit &9" + kit + " &8unassigned from: &a" + location.getBlock().getType().toString() + "&8."));
//        } catch (Exception ex) {
//            Debugger.runReport(ex);
//        }
//    }
//
//
//    public void removeDisplayItems() {
//        try {
//            for (Entity e : location.getChunk().getEntities()) {
//                if (e.getType() != EntityType.DROPPED_ITEM && e.getLocation().getX() != location.getX() && e.getLocation().getZ() != location.getZ()) {
//                    continue;
//                }
//                Item i = (Item) e;
//                i.remove();
//            }
//        } catch (Exception ex) {
//            Debugger.runReport(ex);
//        }
//    }
}
