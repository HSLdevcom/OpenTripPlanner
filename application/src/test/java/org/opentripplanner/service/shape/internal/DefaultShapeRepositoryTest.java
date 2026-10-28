package org.opentripplanner.service.shape.internal;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.ScheduledShapeData;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.service.shape.model.TripGeometryFactory;
import org.opentripplanner.street.geometry.WgsCoordinate;

class DefaultShapeRepositoryTest {

  private static final FeedScopedId SCHEDULED = new FeedScopedId("F", "scheduled");
  private static final FeedScopedId REAL_TIME = new FeedScopedId("F", "rt");
  private static final FeedScopedId OTHER = new FeedScopedId("F", "other");

  private static final CompactLineStringSequence SCHEDULED_TRIP_PATTERN_GEOMETRY = line(60.0);
  private static final CompactLineStringSequence REAL_TIME_TRIP_PATTERN_GEOMETRY = line(61.0);
  private static final CompactLineStringSequence OVERRIDING_TRIP_PATTERN_GEOMETRY = line(62.0);

  private final DefaultShapeRepository repository = new DefaultShapeRepository(
    ScheduledShapeData.of(Map.of(SCHEDULED, SCHEDULED_TRIP_PATTERN_GEOMETRY))
  );

  private static CompactLineStringSequence line(double lat) {
    return TripGeometryFactory.straightLines(
      List.of(new WgsCoordinate(lat, 10.0), new WgsCoordinate(lat + 0.01, 10.0)),
      DeduplicatorService.NOOP
    );
  }

  @Test
  void initialSnapshotContainsTheScheduledData() {
    var snapshot = repository.createSnapshot();

    assertThat(snapshot.getTripPatternGeometry(SCHEDULED)).isEqualTo(
      SCHEDULED_TRIP_PATTERN_GEOMETRY
    );
    assertThat(snapshot.getTripPatternGeometry(REAL_TIME)).isNull();
  }

  @Test
  void snapshotContainsRealTimeGeometry() {
    repository.setTripPatternGeometry(REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);

    var snapshot = repository.createSnapshot();

    assertThat(snapshot.getTripPatternGeometry(REAL_TIME)).isEqualTo(
      REAL_TIME_TRIP_PATTERN_GEOMETRY
    );
    assertThat(snapshot.getTripPatternGeometry(SCHEDULED)).isEqualTo(
      SCHEDULED_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void realTimeGeometryOverridesScheduled() {
    repository.setTripPatternGeometry(SCHEDULED, OVERRIDING_TRIP_PATTERN_GEOMETRY);

    assertThat(repository.getTripPatternGeometry(SCHEDULED)).isEqualTo(
      OVERRIDING_TRIP_PATTERN_GEOMETRY
    );
    assertThat(repository.createSnapshot().getTripPatternGeometry(SCHEDULED)).isEqualTo(
      OVERRIDING_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void snapshotIsNotAffectedByLaterWrites() {
    var snapshot = repository.createSnapshot();

    repository.setTripPatternGeometry(OTHER, REAL_TIME_TRIP_PATTERN_GEOMETRY);

    assertThat(snapshot.getTripPatternGeometry(OTHER)).isNull();
    assertThat(repository.getTripPatternGeometry(OTHER)).isEqualTo(REAL_TIME_TRIP_PATTERN_GEOMETRY);
  }

  @Test
  void scheduledOnlyHasNoRealTimeData() {
    var snapshot = DefaultShapeRepository.scheduledOnly(
      ScheduledShapeData.of(Map.of(SCHEDULED, SCHEDULED_TRIP_PATTERN_GEOMETRY))
    );

    assertThat(snapshot.getTripPatternGeometry(SCHEDULED)).isEqualTo(
      SCHEDULED_TRIP_PATTERN_GEOMETRY
    );
    assertThat(snapshot.getTripPatternGeometry(REAL_TIME)).isNull();
  }
}
