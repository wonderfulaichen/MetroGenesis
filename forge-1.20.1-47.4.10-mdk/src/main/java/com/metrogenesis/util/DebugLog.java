package com.metrogenesis.util;

import org.slf4j.Logger;

/**
 * Utility to guard verbose diagnostic logging behind a toggle.
 * Toggle is hardcoded to false for now; flip to true during debugging.
 */
public final class DebugLog {
    private DebugLog() {
    }

    private static final boolean ENABLED = false;
    private static final boolean CACHE_ENABLED = false;

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static boolean isCacheEnabled() {
        return CACHE_ENABLED;
    }

    public static void info(Logger logger, String message, Object... args) {
        if (!isEnabled()) return;
        logger.info(message, args);
    }

    public static void cache(Logger logger, String message, Object... args) {
        if (!isCacheEnabled()) return;
        logger.info("[cache] " + message, args);
    }
}
