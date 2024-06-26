package dev.geri.shops.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;

public class Data {

    private ArrayList<Shop> shops;
    private HashMap<String, Container> containers;

    public transient boolean hasChanges = false;
    private transient final HashMap<UUID, Container> pendingCopies = new HashMap<>();

    /**
     * @return The string formatted location that we can use a sort of hash
     */
    public String formatLocation(Location location) {
        return "%s:%s,%s,%s".formatted(location.getWorld() != null ? location.getWorld().getName() : null, location.getX(), location.getY(), location.getZ());
    }

    /**
     * Parse a raw location as a Bukkit object
     *
     * @return The location or null if there are _any_ issues
     */
    public Location parseLocation(String location) {
        try {
            String[] split = location.split(":");
            String[] coords = split[1].split(",");
            return new Location(Bukkit.getWorld(split[0]), Double.parseDouble(coords[0]), Double.parseDouble(coords[1]), Double.parseDouble(coords[2]));
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * Save a container at a specific location
     */
    public void saveContainer(Location location, Container container) {
        this.hasChanges = true;
        container.setShop(getShop(container.shopName()));
        this.containers.put(this.formatLocation(location), container);
    }

    /**
     * Get a container at a specific location
     * Note that this will NOT adjust for any
     * special cases, such as double chests.
     *
     * @return The container or null if not found
     */
    public Container getContainer(Location location) {
        return this.containers.get(this.formatLocation(location));
    }

    /**
     * Delete a container at a given location. If the shop does
     * not have any other containers linked, it will also be deleted
     */
    public void removeContainer(Location location) {
        this.hasChanges = true;
        String key = this.formatLocation(location);
        Container container = this.containers.get(key);
        if (container == null) return;

        this.containers.remove(key);

        // If this was the last container for the shop,
        // untrack that as well
        List<Container> shopContainers = this.containers.values().stream().filter(c -> c.shopName() != null && c.shopName().equals(container.shopName())).toList();
        if (shopContainers.isEmpty()) {
            this.removeShop(container.shopName());
        }
    }

    /**
     * Get a shop by its name
     *
     * @return The shop or null if not found
     */
    public Shop getShop(String name) {
        return this.shops.stream().filter(s -> Objects.equals(s.name(), name)).findFirst().orElse(null);
    }

    /**
     * Save a shop by its name
     */
    public void saveShop(Shop shop) {
        this.removeShop(shop.name());
        this.shops.add(shop);
    }

    /**
     * Delete a shop by its name
     */
    public void removeShop(String name) {
        this.shops.removeIf(s -> s.name().equals(name));
    }

    /**
     * Initialise all the containers with their shops
     */
    public void init() {
        if (this.shops == null) this.shops = new ArrayList<>();
        if (this.containers == null) this.containers = new HashMap<>();
        this.containers.values().forEach(container -> {
            container.setShop(getShop(container.shopName()));
        });
    }

    /**
     * @return The raw list of shops
     */
    public ArrayList<Shop> shops() {
        return this.shops;
    }

    /**
     * @return The raw list of containers
     */
    public Map<String, Container> containers() {
        return this.containers;
    }

    /**
     * Set a new copy action for a specific player
     * @param playerUuid The player who is copying
     * @param container The container that is being copied
     */
    public void setPendingCopy(UUID playerUuid, Container container) {
        this.pendingCopies.put(playerUuid, container);
    }

    /**
     * Get a pending copy action's container
     * @param playerUuid The player who is copying
     * @return The container or null if not found
     */
    public Container getPendingCopy(UUID playerUuid) {
        return this.pendingCopies.get(playerUuid);
    }

    /**
     * Remove the current copy action for a player
     */
    public void removePendingCopy(UUID playerUuid) {
        this.pendingCopies.remove(playerUuid);
    }

}
