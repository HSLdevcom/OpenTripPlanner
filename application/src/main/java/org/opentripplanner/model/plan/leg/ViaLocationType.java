package org.opentripplanner.model.plan.leg;

/**
 * Categorization for a via location.
 */
public enum ViaLocationType {
  /**
   * Location is a stop location where the passenger boards a vehicle or alights from a vehicle, or
   * a coordinate which is visited.
   */
  VISIT,
  /**
   * Stop location must be visited on-board a transit vehicle or the journey must alight or board at
   * the location.
   */
  PASS_THROUGH,
}
