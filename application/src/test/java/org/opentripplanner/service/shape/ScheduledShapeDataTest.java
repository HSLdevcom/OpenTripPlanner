package org.opentripplanner.service.shape;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.service.shape.model.TripGeometryFactory;
import org.opentripplanner.street.geometry.WgsCoordinate;

class ScheduledShapeDataTest {

  private static final CompactLineStringSequence TRIP_PATTERN_GEOMETRY =
    TripGeometryFactory.straightLines(
      List.of(new WgsCoordinate(60.0, 10.0), new WgsCoordinate(60.01, 10.0)),
      DeduplicatorService.NOOP
    );

  @Test
  void emptyHasNoGeometry() {
    var empty = ScheduledShapeData.empty();

    assertThat(empty.getTripPatternGeometry(id("A"))).isNull();
  }

  @Test
  void ofCopiesTheMapAndSharesTheGeometry() {
    var geometriesByTripPattern = new HashMap<>(java.util.Map.of(id("A"), TRIP_PATTERN_GEOMETRY));

    var data = ScheduledShapeData.of(geometriesByTripPattern);
    geometriesByTripPattern.put(id("B"), TRIP_PATTERN_GEOMETRY);

    assertThat(data.getTripPatternGeometry(id("A"))).isSameInstanceAs(TRIP_PATTERN_GEOMETRY);
    assertThat(data.getTripPatternGeometry(id("B"))).isNull();
  }

  @Test
  void nullKeysAreRejected() {
    assertThrows(NullPointerException.class, () ->
      ScheduledShapeData.empty().getTripPatternGeometry(null)
    );
  }
}
