package org.opentripplanner.service.shape.dataimport;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.street.geometry.GeometryUtils.makeLineString;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.graph.kryosupport.KryoBuilder;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.service.shape.model.TripGeometryFactory;
import org.opentripplanner.transit.model.framework.Deduplicator;

class ShapeDataImportTest {

  private static final CompactLineStringSequence TRIP_PATTERN_GEOMETRY =
    TripGeometryFactory.fromLineStrings(
      List.of(makeLineString(10.0, 59.0, 10.1, 59.1, 10.2, 59.0)),
      new Deduplicator()
    );

  @Test
  void storesGeometry() {
    var subject = new ShapeDataImport();
    assertThat(subject.getTripPatternGeometry(id("A"))).isNull();

    subject.setTripPatternGeometry(id("A"), TRIP_PATTERN_GEOMETRY);

    assertThat(subject.getTripPatternGeometry(id("A"))).isEqualTo(TRIP_PATTERN_GEOMETRY);
  }

  @Test
  void convertsToScheduledData() {
    var subject = new ShapeDataImport();
    subject.setTripPatternGeometry(id("A"), TRIP_PATTERN_GEOMETRY);

    var scheduled = subject.toScheduledShapeData();

    assertThat(scheduled.getTripPatternGeometry(id("A"))).isEqualTo(TRIP_PATTERN_GEOMETRY);
  }

  @Test
  void modificationsAfterConversionFail() {
    var subject = new ShapeDataImport();
    subject.toScheduledShapeData();

    assertThrows(IllegalStateException.class, () ->
      subject.setTripPatternGeometry(id("A"), TRIP_PATTERN_GEOMETRY)
    );
  }

  @Test
  void geometrySurvivesSerialization() {
    var deduplicator = new Deduplicator();
    var hops = List.of(makeLineString(10.0, 59.0, 10.1, 59.1, 10.2, 59.0));
    var subject = new ShapeDataImport();
    subject.setTripPatternGeometry(
      id("A"),
      TripGeometryFactory.fromLineStrings(hops, deduplicator)
    );
    subject.setTripPatternGeometry(
      id("B"),
      TripGeometryFactory.fromLineStrings(hops, deduplicator)
    );

    var read = roundTrip(subject);

    var a = read.getTripPatternGeometry(id("A"));
    assertThat(a.get(0)).isEqualTo(hops.getFirst());
  }

  @Test
  void deserializedImportIsNotFrozen() {
    var subject = new ShapeDataImport();
    subject.toScheduledShapeData();

    var read = roundTrip(subject);

    read.setTripPatternGeometry(id("A"), TRIP_PATTERN_GEOMETRY);
    assertThat(read.getTripPatternGeometry(id("A"))).isEqualTo(TRIP_PATTERN_GEOMETRY);
  }

  private static ShapeDataImport roundTrip(ShapeDataImport subject) {
    var kryo = KryoBuilder.create();
    var bytes = new ByteArrayOutputStream();
    try (var out = new Output(bytes)) {
      kryo.writeClassAndObject(out, subject);
    }
    return (ShapeDataImport) kryo.readClassAndObject(new Input(bytes.toByteArray()));
  }
}
