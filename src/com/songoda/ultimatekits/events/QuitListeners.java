package com.songoda.ultimatekits.events;

import com.songoda.ultimatekits.KitPreview;
import com.songoda.ultimatekits.utils.Debugger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuitListeners implements Listener {

    private final KitPreview instance;

    public QuitListeners(KitPreview instance) {
        this.instance = instance;
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        try {
            instance.inEditor.remove(event.getPlayer());
            instance.whereAt.remove(event.getPlayer().getUniqueId());
        } catch (Exception ex) {
            Debugger.runReport(ex);
        }
    }
}

