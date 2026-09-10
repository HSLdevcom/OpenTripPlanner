package org.opentripplanner.transit.service;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.geometry.CompactLineStringSequence;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.network.TripPatternGeometryFactory;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Default implementation of {@link TripPatternGeometryService}, backed by a
 * {@link TripPatternGeometryRepository}.
 * <p>
 * If a pattern has no geometry registered in the repository (for example, it was constructed
 * without going through the code paths that populate the repository), a straight-line geometry is
 * synthesized on the fly from the pattern's {@code StopPattern}, mirroring the historical fallback
 * behaviour of {@code TripPatternBuilder}.
 */
public class DefaultTripPatternGeometryService implements TripPatternGeometryService, Serializable {

  private final TripPatternGeometryRepository repository;

  public DefaultTripPatternGeometryService(TripPatternGeometryRepository repository) {
    this.repository = Objects.requireNonNull(repository);
  }

  @Override
  public LineString getPatternGeometry(TripPattern pattern) {
    var sequence = resolveSequence(pattern, null);
    return sequence.concatenate(0, sequence.size());
  }

  @Override
  public LineString getHopGeometry(TripPattern pattern, int stopPosInPattern) {
    return resolveSequence(pattern, null).get(stopPosInPattern);
  }

  @Override
  public LineString geometryBetween(
    TripPattern pattern,
    int boardingStopPosition,
    int alightingStopPosition
  ) {
    return resolveSequence(pattern, null).concatenate(boardingStopPosition, alightingStopPosition);
  }

  @Override
  public int distanceBetween(
    TripPattern pattern,
    int boardingStopPosition,
    int alightingStopPosition
  ) {
    return resolveSequence(pattern, null).distanceBetween(
      boardingStopPosition,
      alightingStopPosition
    );
  }

  @Override
  public LineString getTripGeometry(TripPattern pattern, Trip trip) {
    var sequence = resolveSequence(pattern, trip);
    return sequence.concatenate(0, sequence.size());
  }

  @Override
  public LineString geometryBetween(
    TripPattern pattern,
    Trip trip,
    int boardingStopPosition,
    int alightingStopPosition
  ) {
    return resolveSequence(pattern, trip).concatenate(boardingStopPosition, alightingStopPosition);
  }

  @Override
  public int distanceBetween(
    TripPattern pattern,
    Trip trip,
    int boardingStopPosition,
    int alightingStopPosition
  ) {
    return resolveSequence(pattern, trip).distanceBetween(
      boardingStopPosition,
      alightingStopPosition
    );
  }

  /**
   * Resolve the geometry sequence to use for the given pattern and (optional) trip: the trip's
   * override if one is registered, otherwise the pattern's registered default, otherwise a
   * synthesized straight-line sequence.
   */
  private CompactLineStringSequence resolveSequence(TripPattern pattern, @Nullable Trip trip) {
    if (trip != null) {
      var override = repository.getTripGeometryOverride(trip.getId());
      if (override != null) {
        return override;
      }
    }
    var patternDefault = repository.getPatternGeometry(pattern.getId());
    if (patternDefault != null) {
      return patternDefault;
    }
    return TripPatternGeometryFactory.buildHopGeometries(pattern.getStopPattern(), null);
  }
}
