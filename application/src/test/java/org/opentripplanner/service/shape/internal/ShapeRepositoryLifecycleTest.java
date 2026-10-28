package org.opentripplanner.service.shape.internal;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.ScheduledShapeData;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.service.shape.model.TripGeometryFactory;
import org.opentripplanner.street.geometry.WgsCoordinate;

class ShapeRepositoryLifecycleTest {

  private static final FeedScopedId PATTERN_1 = new FeedScopedId("F", "P1");
  private static final FeedScopedId PATTERN_2 = new FeedScopedId("F", "P2");

  private static final CompactLineStringSequence TRIP_PATTERN_GEOMETRY_1 =
    TripGeometryFactory.straightLines(
      List.of(new WgsCoordinate(60.0, 10.0), new WgsCoordinate(60.01, 10.0)),
      DeduplicatorService.NOOP
    );
  private static final CompactLineStringSequence TRIP_PATTERN_GEOMETRY_2 =
    TripGeometryFactory.straightLines(
      List.of(new WgsCoordinate(61.0, 10.0), new WgsCoordinate(61.01, 10.0)),
      DeduplicatorService.NOOP
    );

  private final DefaultShapeRepository buffer = new DefaultShapeRepository(
    ScheduledShapeData.empty()
  );
  private final ShapeRepositoryLifecycle lifecycle = new ShapeRepositoryLifecycle(buffer);

  @Test
  void copyOnWriteReturnsTheLongLivedBuffer() {
    var snapshot = lifecycle.freeze(buffer);

    assertThat(lifecycle.copyOnWrite(snapshot)).isSameInstanceAs(buffer);
  }

  @Test
  void initialSnapshotIsEmpty() {
    var snapshot = lifecycle.freeze(buffer);

    assertThat(snapshot.getTripPatternGeometry(PATTERN_1)).isNull();
  }

  @Test
  void freezeContainsStoredGeometry() {
    buffer.setTripPatternGeometry(PATTERN_1, TRIP_PATTERN_GEOMETRY_1);

    var snapshot = lifecycle.freeze(buffer);

    assertThat(snapshot.getTripPatternGeometry(PATTERN_1)).isEqualTo(TRIP_PATTERN_GEOMETRY_1);
    assertThat(snapshot.getTripPatternGeometry(PATTERN_2)).isNull();
  }

  @Test
  void snapshotIsNotAffectedByLaterWritesToTheBuffer() {
    buffer.setTripPatternGeometry(PATTERN_1, TRIP_PATTERN_GEOMETRY_1);
    var snapshot = lifecycle.freeze(buffer);

    lifecycle.copyOnWrite(snapshot).setTripPatternGeometry(PATTERN_2, TRIP_PATTERN_GEOMETRY_2);

    assertThat(snapshot.getTripPatternGeometry(PATTERN_2)).isNull();
    var next = lifecycle.freeze(buffer);
    assertThat(next.getTripPatternGeometry(PATTERN_1)).isEqualTo(TRIP_PATTERN_GEOMETRY_1);
    assertThat(next.getTripPatternGeometry(PATTERN_2)).isEqualTo(TRIP_PATTERN_GEOMETRY_2);
  }
}
