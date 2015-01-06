package me.ferrybig.java.bukkit.plugins.dailychests;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

/**
 *
 * @author Fernando
 */
public class BlockLocation implements ConfigurationSerializable, Serializable {

    private static final long serialVersionUID = 0x4726489562L;
    
    public static BlockLocation deserialize(Map<String, Object> args) {
        int x = 0;
        int y = 0;
        int z = 0;
        String w = null;

        if (args.containsKey("x")) {
            x = (Integer) args.get("x");
        }
        if (args.containsKey("y")) {
            y = (Integer) args.get("y");
        }
        if (args.containsKey("z")) {
            z = (Integer) args.get("z");
        }
        if (args.containsKey("w")) {
            w = args.get("w").toString();
        }

        return new BlockLocation(w, x, y, z);
    }

    /**
     * Tries to parse a Block Location from a String value
     *
     * @param value to parse
     * @return Block Location, or null if it could not be parsed
     */
    public static BlockLocation parseLocation(String value) {
        if (value.length() < 10) {
            return null;
        }
        try {
            StringBuilder world = new StringBuilder(value.length() - 9);
            String[] bits = value.split("_");
            int z = Integer.parseInt(bits[bits.length - 1]);
            int y = Integer.parseInt(bits[bits.length - 2]);
            int x = Integer.parseInt(bits[bits.length - 3]);
            // get world
            int worldbitcount = bits.length - 3;
            for (int i = 0; i < worldbitcount; i++) {
                world.append(bits[i]);
                if (i != worldbitcount - 1) {
                    world.append('_');
                }
            }
            return new BlockLocation(world.toString(), x, y, z);
        } catch (Throwable t) {
            return null;
        }
    }
    public final String world;
    public final int x, y, z;

    /**
     * Initializes a new Block Location using a Block
     *
     * @param block to use
     */
    public BlockLocation(Block block) {
        this(block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    /**
     * Initializes a new Block Location using a Location
     *
     * @param location to use
     */
    public BlockLocation(Location location) {
        this(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Initializes a new Block Location using a world and x/y/z coordinate
     *
     * @param world to use the name of
     * @param x - coordinate
     * @param y - coordinate
     * @param z - coordinate
     */
    public BlockLocation(World world, final int x, final int y, final int z) {
        this(world == null ? null : world.getName(), x, y, z);
    }

    /**
     * Initializes a new Block Location using a world and x/y/z coordinate
     *
     * @param world name to use
     * @param x - coordinate
     * @param y - coordinate
     * @param z - coordinate
     */
    public BlockLocation(final String world, final int x, final int y, final int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (object instanceof BlockLocation) {
            BlockLocation bloc = (BlockLocation) object;
            return bloc.x == this.x && bloc.y == this.y && bloc.z == this.z && bloc.world.equals(this.world);
        } else {
            return false;
        }
    }

    /**
     * Gets the Block this Location is in
     *
     * @return the Block, or null if the world this location is in is not loaded
     */
    public Block getBlock() {
        World world = this.getWorld();
        return world == null ? null : world.getBlockAt(this.x, this.y, this.z);
    }

    /**
     * Gets the chunk this Block Location is in<br> <b>Will load the chunk if it
     * isn't loaded</b>
     *
     * @return the Chunk, or null if the world this location is in is not loaded
     */
    public Chunk getChunk() {
        World world = this.getWorld();
        if (world == null) {
            return null;
        }
        return world.getChunkAt(this.x >> 4, this.z >> 4);
    }

    /**
     * Gets the Location from this Block Location
     *
     * @return the Location of the middle of the Block, or null if the world is
     * not loaded
     */
    public Location getLocation() {
        World world = this.getWorld();
        return world == null ? null : new Location(world, this.x + 0.5, this.y + 0.5, this.z + 0.5);
    }

    /**
     * Gets the World this Location is in
     *
     * @return the World, or null if it is not loaded
     */
    public World getWorld() {
        return Bukkit.getServer().getWorld(this.world);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + this.world.hashCode();
        hash = 53 * hash + (this.x ^ (this.x >> 16));
        hash = 53 * hash + (this.y ^ (this.y >> 16));
        hash = 53 * hash + (this.z ^ (this.z >> 16));
        return hash;
    }

    /**
     * Checks if this Location is in the World specified
     *
     * @param world to check
     * @return True if it is in the world, False if not
     */
    public boolean isIn(World world) {
        if (this.world == null && world == null) {
            return true;
        } else if (this.world == null || world == null) {
            return false;
        } else {
            return this.world.equals(world.getName());
        }
    }

    /**
     * Checks if this Block Location is within the boundaries of a chunk
     *
     * @param chunk to check
     * @return True if within, False if not
     */
    public boolean isIn(Chunk chunk) {
        if (chunk != null && this.isIn(chunk.getWorld())) {
            return (this.x >> 4) == chunk.getX() && (this.z >> 4) == chunk.getZ();
        }
        return false;
    }

    /**
     * Checks if this Block Location is within the boundaries of a cuboid
     *
     * @param point1 of the Cuboid
     * @param point2 of the Cuboid
     * @return True if within, False if not
     */
    public boolean isIn(BlockLocation point1, BlockLocation point2) {
        if (point1 != null && point2 != null && point1.world.equals(point2.world) && point1.world.equals(this.world)) {
            int value;
            value = Math.min(point1.x, point2.x);
            if (this.x < value || this.x > (value + Math.abs(point1.x - point2.x))) {
                return false;
            }
            value = Math.min(point1.y, point2.y);
            if (this.y < value || this.y > (value + Math.abs(point1.y - point2.y))) {
                return false;
            }
            value = Math.min(point1.z, point2.z);
            if (this.z < value || this.z > (value + Math.abs(point1.z - point2.z))) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Checks if this location is loaded
     *
     * @return True if it is loaded, False if not
     */
    public boolean isLoaded() {
        return Bukkit.getServer().getWorld(this.world) != null;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> result = new TreeMap<String, Object>();

        result.put("x", x);
        result.put("y", y);
        result.put("z", z);
        result.put("w", this.world);
        return result;
    }

    @Override
    public String toString() {
        return this.world + "_" + this.x + "_" + this.y + "_" + this.z;
    }
}
