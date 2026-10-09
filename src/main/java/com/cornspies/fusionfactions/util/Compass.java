package com.cornspies.fusionfactions.util;

// 8-way compass directions for messages, e.g. supply drop announcements and radar alerts.
public final class Compass {
    private static final String[] DIRECTIONS = { "north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west" };

    // The direction of an offset, as a key for "fusionfactions.direction.<key>". North is -Z, east is +X.
    public static String direction(int dx, int dz) {
        double degrees = Math.toDegrees(Math.atan2(dx, -dz));
        return DIRECTIONS[Math.floorMod(Math.round(degrees / 45.0), DIRECTIONS.length)];
    }

    private Compass() {
    }
}
