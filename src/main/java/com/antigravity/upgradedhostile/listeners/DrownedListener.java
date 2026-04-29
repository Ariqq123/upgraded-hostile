package com.antigravity.upgradedhostile.listeners;

import com.antigravity.upgradedhostile.handlers.DrownedHandler;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

public class DrownedListener implements Listener {

    private final DrownedHandler drownedHandler;

    public DrownedListener(DrownedHandler drownedHandler) {
        this.drownedHandler = drownedHandler;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTridentHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof Drowned drowned)) return;
        if (!(event.getHitEntity() instanceof Player player)) return;

        drownedHandler.attemptHarpoon(drowned, player, trident.getItemStack());
    }
}
