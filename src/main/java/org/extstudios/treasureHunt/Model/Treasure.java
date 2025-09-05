package org.extstudios.treasureHunt.Model;

public record Treasure(
        String id,
        String world,
        int x,
        int y,
        int z,
        String material,
        String command
){}
