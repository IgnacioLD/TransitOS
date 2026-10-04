package com.glossostudio.transitos.feature.planner

/** Origin and destination to open the planner with, e.g. from a saved route. */
data class PlannerPrefill(val originStopId: String, val destinationStopId: String)
