package com.railwayteam.railways.neoforge.datagen;

/**
 * Compatibility helpers for generated track names.
 */
public final class CompatLootTableProvider {
    private CompatLootTableProvider() {}

    public static String getCompatModForTrackName(String trackName) {
        return CompatDataGenUtils.parseTrackName(trackName)
            .map(CompatDataGenUtils.CompatTrackName::modId)
            .orElse(null);
    }
}
