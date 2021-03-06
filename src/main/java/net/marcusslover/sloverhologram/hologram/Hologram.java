package net.marcusslover.sloverhologram.hologram;

import net.marcusslover.sloverhologram.SloverHologram;
import net.minecraft.server.v1_12_R1.EntityArmorStand;
import net.minecraft.server.v1_12_R1.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_12_R1.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_12_R1.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.*;

@SuppressWarnings("WeakerAccess")
public class Hologram {
    private final SloverHologram plugin = SloverHologram.getInstance();

    private final String name;
    private List<String> lines;
    private Location location;
    private final Chunk chunk;
    private final boolean custom;

    private final Map<UUID, List<EntityArmorStand>> entities = new HashMap<>();

    /**
     * A method which creates a new hologram object
     * @param name name of the hologram
     * @param lines lines of the hologram
     * @param location location of the hologram
     */
    public Hologram(final String name, final List<String> lines, final Location location, final boolean custom) {
        this.name = name;
        this.lines = lines;
        this.location = location;
        this.chunk = location.getChunk();
        this.custom = custom;

        if (!custom) {
            plugin.hologramList.add(this);
        } else {
            plugin.getAPI().getHologramMap().put(name, this);
        }
    }

    /**
     * A method which checks either the hologram is API-created
     * or manually created in-game using the command.
     * @return true or false
     */
    public boolean isCustom() {
        return custom;
    }

    /**
     * A method which returns chunk the hologram is in
     * @return the chunk
     */
    public Chunk getChunk() {
        return chunk;
    }

    /**
     * A method which returns the location
     * @return location of the hologram
     */
    public Location getLocation() {
        return this.location;
    }

    /**
     * A method which sets location of hologram
     * @param location new location
     */
    public void setLocation(final Location location) {
        this.location = location;
    }

    /**
     * A method which returns hologram line
     * @param i number
     * @return actual string line of the hologram
     */
    public String getLine(final int i) {
        return lines.get(i);
    }

    /**
     * A method which returns lines of the hologram
     * @return lines of the hologram
     */
    public List<String> getLines() {
        return this.lines;
    }

    /**
     * A method which returns name of the hologram
     * @return name of the hologram
     */
    public String getName() {
        return this.name;
    }

    /**
     * A method which sets lines of the hologram
     * @param stringList a new list of lines
     */
    public void setLines(final List<String> stringList) {
        this.lines = stringList;
    }

    /**
     * A method which make the hologram appear
     * to a certain player
     * @param player the player
     */
    public void show(final Player player) {
        if (this.location.getWorld() == null) {
            return;
        }

        if (this.location.getWorld().equals(player.getWorld())) {
            if (this.getLocation().getChunk().isLoaded()) {
                this.generateHologram(player);
            }
        }
    }

    /**
     * A method which make the hologram disappear
     * to a certain player
     * @param player the player
     */
    public void remove(final Player player) {
        this.destroyHologram(player);
    }

    /**
     * A method which generates the hologram
     * and sends the packets to a certain player
     * @param player the player packet will be send to
     */
    private void generateHologram(final Player player) {
        List<EntityArmorStand> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, List<EntityArmorStand>> set : entities.entrySet()) {
            if (set.getKey() == player.getUniqueId()) {
                for (EntityArmorStand entityArmorStand : set.getValue()) {


                    PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(entityArmorStand.getBukkitEntity().getEntityId());
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
                    toRemove.add(entityArmorStand);
                }
            }
        }

        Location loc = this.location.clone();
        double space = plugin.getHologramManager().space();
        if (!entities.containsKey(player.getUniqueId())) {
            entities.put(player.getUniqueId(), new ArrayList<>());
        }
        List<EntityArmorStand> list = entities.get(player.getUniqueId());
        toRemove.forEach(list::remove);
        for (int i = 0; i < this.lines.size(); i++) {
            EntityArmorStand entityArmorStand = getHologramLine(loc, this.getLine(i));
            PacketPlayOutSpawnEntityLiving packet = new PacketPlayOutSpawnEntityLiving(entityArmorStand);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
            loc.add(0, -space, 0);
            list.add(entityArmorStand);
        }
        entities.put(player.getUniqueId(), list);
    }

    /**
     * A method which destroys the hologram
     * and sends the packets to a certain player
     * @param player the player packet will be send to
     */
    private void destroyHologram(final Player player) {
        if (!entities.containsKey(player.getUniqueId())) {
            return;
        }

        if (!this.getLocation().getChunk().isLoaded()) {
            return;
        }

        for (Map.Entry<UUID, List<EntityArmorStand>> set : entities.entrySet()) {
            if (set.getKey() == player.getUniqueId()) {
                for (EntityArmorStand entityArmorStand : set.getValue()) {
                    PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(entityArmorStand.getBukkitEntity().getEntityId());
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
                }
            }
        }
        entities.remove(player.getUniqueId());
    }

    /**
     * A method that completely destroys the hologram.
     */
    public void destroy() {
        try {
            entities.forEach((key, value) -> {
                Player player = Bukkit.getPlayer(key);
                if (player != null) {
                    destroyHologram(player);
                }
            });
        } catch (Exception ignored) {}

        if (isCustom()) {
            plugin.getAPI().getHologramMap().remove(name);
        } else {
            plugin.hologramList.remove(this);
        }
    }

    /**
     * A method with returns crafted armor stand
     * @return crafted armor stand
     */
    private EntityArmorStand getHologramLine(final Location loc, final String value) {
        WorldServer worldServer = ((CraftWorld) this.location.getWorld()).getHandle();
        EntityArmorStand entityArmorStand = new EntityArmorStand(worldServer);

        entityArmorStand.setSilent(true);
        entityArmorStand.setMarker(false);
        entityArmorStand.setNoGravity(true);
        entityArmorStand.setSmall(true);
        entityArmorStand.setInvisible(true);
        entityArmorStand.setCustomNameVisible(true);
        entityArmorStand.setCustomName(getColor(value));
        entityArmorStand.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

        return entityArmorStand;
    }

    /**
     * A method which returns colored string
     * @param string a normal string
     * @return colored string
     */
    protected String getColor(final String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    /**
     * A method which returns all armorstands of the hologram
     * @return the map with entities
     */
    public Map<UUID, List<EntityArmorStand>> getEntities() {
        return entities;
    }
}
