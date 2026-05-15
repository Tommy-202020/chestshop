package dev.minecore.asteroidChests.models;

import java.util.Locale;

public enum ShopMode {
    BUY,
    SELL,
    BOTH;

    public static ShopMode fromInput(String input) {
        if (input == null) {
            return null;
        }
        try {
            return ShopMode.valueOf(input.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}