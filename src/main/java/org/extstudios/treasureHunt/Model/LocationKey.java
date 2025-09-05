package org.extstudios.treasureHunt.Model;


import org.bukkit.Location;

import java.util.Objects;

public final class LocationKey {
    public final String world;
    public final int x,y,z;

    private LocationKey(String world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static LocationKey of(String world, int x, int y, int z) {
        return new LocationKey(world, x, y, z);
    }
    public static LocationKey of(Location loc) {return of(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @Override public boolean equals (Object o) {
        if (this == o) return true;
        if (!(o instanceof LocationKey that)) return false;
        return x==that.x && y==that.y && z==that.z && Objects.equals(world, that.world);
    }

    @Override public int hashCode() {return Objects.hash(world,x,y,z);}
    @Override public String toString(){return world+": "+x+" "+y+" "+z;}
}
