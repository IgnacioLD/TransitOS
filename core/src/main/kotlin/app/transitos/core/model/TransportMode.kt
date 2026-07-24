package app.transitos.core.model

/**
 * Universal transport modes. Every provider maps its own taxonomy onto one of these.
 *
 * Adding a new mode is a breaking change for persisted data; prefer extending [OTHER]
 * via metadata until a dedicated mode is justified.
 */
public enum class TransportMode {
    METRO,
    TRAM,
    BUS,
    TRAIN,
    FERRY,
    CABLE_CAR,
    FUNICULAR,
    OTHER,
}
