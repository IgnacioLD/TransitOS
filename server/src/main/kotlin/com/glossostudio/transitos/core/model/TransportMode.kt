package com.glossostudio.transitos.core.model

/**
 * Universal transport modes, mirrored from the app's `:core` model so the
 * server can emit JSON the app decodes directly. Adding a mode is a breaking
 * change for persisted data on the client; prefer mapping onto [OTHER].
 */
enum class TransportMode {
    METRO,
    TRAM,
    BUS,
    TRAIN,
    FERRY,
    CABLE_CAR,
    FUNICULAR,
    OTHER,
}
