package com.github.groundbreakingmc.smoke;

import com.github.groundbreakingmc.mylib.collections.expiring.ExpiringSet;
import com.google.common.collect.ImmutableSet;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * TODO
 * 1. Добавить забивки
 */
public final class Smoke extends JavaPlugin implements Listener {

    private final Set<PotionEffect> effects;
    private final ExpiringSet<UUID> cooldowns;
    private final Particle.DustOptions dustOptions;

    public Smoke() {
        this.effects = ImmutableSet.of(
                new PotionEffect(PotionEffectType.DARKNESS, 60, 0),
                new PotionEffect(PotionEffectType.SLOW, 60, 0)
        );

        this.cooldowns = new ExpiringSet<>(5, TimeUnit.SECONDS);

        this.dustOptions = new Particle.DustOptions(Color.WHITE, 3);
    }

    @Override
    public void onEnable() {
        super.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) {
            return;
        }

        final Player player = event.getPlayer();
        if (this.shisha(event)) {
            this.processShisha(event);
        } else if (event.useItemInHand() != Event.Result.DENY && this.wape(player)) {
            this.process(player, player.getEyeLocation(), false);
        }
    }

    private boolean shisha(final PlayerInteractEvent event) {
        return !event.isCancelled()
                && event.hasBlock()
                && event.getClickedBlock().getType() == Material.BREWING_STAND;
    }

    private boolean wape(final Player player) {
        final ItemStack itemStack = player.getInventory().getItemInMainHand();
        final Material material = itemStack.getType();
        if (material != Material.GOAT_HORN || player.hasCooldown(material)) {
            return false;
        }

        player.setCooldown(material, 140);
        return true;
    }

    private void processShisha(final PlayerInteractEvent event) {
        final Location location = event.getInteractionPoint();

        if (this.shisha(location)) {
            return;
        }

        event.setCancelled(true);
        this.process(event.getPlayer(), location, true);
    }

    private boolean shisha(final Location location) {
        return location.clone().subtract(0, 1, 0).getBlock().getType() != Material.BLACK_WOOL;
    }

    private void process(final Player player, final Location location, final boolean shisha) {
        if (!player.hasPermission("smoke.use")) {
            return;
        }

        if (shisha) {
            if (this.cooldowns.contains(player.getUniqueId())) {
                return;
            }

            this.cooldowns.add(player.getUniqueId());
        }

        location.getWorld().spawnParticle(Particle.REDSTONE, location, 25, 0, 0, 0, 0, this.dustOptions);
        player.addPotionEffects(this.effects);
    }
}
