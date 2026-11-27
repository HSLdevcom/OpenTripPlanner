package org.opentripplanner.transit.model.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.street.geometry.GeometryUtils.makeLineString;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.shape.CompactLineStringSequence;
import org.opentripplanner.transit.model.shape.TripShapeFactory;
import org.opentripplanner.transit.model.site.RegularStop;

class TripPatternTest {

  private static final String ID = "1";
  private static final String NAME = "short name";
  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();

  private static final Route ROUTE = TransitRepositoryForTest.route("routeId").build();
  public static final RegularStop STOP_A = TEST_MODEL.stop("A").build();
  public static final RegularStop STOP_X = TEST_MODEL.stop("X").build();
  public static final RegularStop STOP_B = TEST_MODEL.stop("B").build();
  public static final RegularStop STOP_Y = TEST_MODEL.stop("Y").build();
  public static final RegularStop STOP_C = TEST_MODEL.stop("C").build();
  private static final StopPattern STOP_PATTERN = TransitRepositoryForTest.stopPattern(
    STOP_A,
    STOP_B,
    STOP_C
  );

  private static final List<LineString> HOP_SHAPES = List.of(
    makeLineString(STOP_A.getCoordinate(), STOP_X.getCoordinate(), STOP_B.getCoordinate()),
    makeLineString(STOP_B.getCoordinate(), STOP_Y.getCoordinate(), STOP_C.getCoordinate())
  );

  private static final CompactLineStringSequence SHAPE = TripShapeFactory.fromLineStrings(
    HOP_SHAPES,
    DeduplicatorService.NOOP
  );

  private static final TripPattern SUBJECT = TripPattern.of(id(ID))
    .withName(NAME)
    .withRoute(ROUTE)
    .withStopPattern(STOP_PATTERN)
    .withShape(SHAPE)
    .build();

  @Test
  void copy() {
    assertEquals(ID, SUBJECT.getId().getId());

    // Make a copy, and set the same name (nothing is changed)
    var copy = SUBJECT.copy().withName(NAME).build();

    assertSame(SUBJECT, copy);

    // Copy and change name
    copy = SUBJECT.copy().withName("v2").build();

    // The two objects are not the same instance, but are equal(same id)
    assertNotSame(SUBJECT, copy);
    assertEquals(SUBJECT, copy);

    assertEquals(ID, copy.getId().getId());
    assertEquals("v2", copy.getName());
    assertEquals(ROUTE, copy.getRoute());
    assertEquals(STOP_PATTERN, copy.getStopPattern());
    assertSame(SHAPE, copy.getShape());
    assertEquals(HOP_SHAPES.get(1), copy.getShape().get(1));
  }

  @Test
  void defaultShapeIsStraightLinesBetweenTheStops() {
    var pattern = TripPattern.of(id("straight"))
      .withRoute(ROUTE)
      .withStopPattern(STOP_PATTERN)
      .build();

    var shape = pattern.getShape();
    assertEquals(2, shape.size());
    assertEquals(makeLineString(STOP_A.getCoordinate(), STOP_B.getCoordinate()), shape.get(0));
    assertEquals(makeLineString(STOP_B.getCoordinate(), STOP_C.getCoordinate()), shape.get(1));
  }

  @Test
  void shapeWithWrongNumberOfHopsIsRejected() {
    var shape = TripShapeFactory.fromLineStrings(
      List.of(HOP_SHAPES.get(0)),
      DeduplicatorService.NOOP
    );
    var builder = TripPattern.of(id("invalid"))
      .withRoute(ROUTE)
      .withStopPattern(STOP_PATTERN)
      .withShape(shape);

    Assertions.assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  void sameAs() {
    assertTrue(SUBJECT.sameAs(SUBJECT.copy().build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withId(id("X")).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withName("X").build()));
    assertFalse(
      SUBJECT.sameAs(
        SUBJECT.copy().withRoute(TransitRepositoryForTest.route("anotherId").build()).build()
      )
    );
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withMode(TransitMode.RAIL).build()));
    // The copy keeps the shape, which is validated against the stop pattern; changing the stop
    // pattern without clearing the inherited shape would be a caller bug.
    assertFalse(
      SUBJECT.sameAs(
        SUBJECT.copy().withStopPattern(TEST_MODEL.stopPattern(11)).withShape(null).build()
      )
    );
  }

  @Test
  void initNameShouldThrow() {
    Assertions.assertThrows(IllegalStateException.class, () -> SUBJECT.initName("abc"));
  }

  @Test
  void shouldAddName() {
    var name = "xyz";
    var noNameYet = TripPattern.of(id(ID)).withRoute(ROUTE).withStopPattern(STOP_PATTERN).build();

    noNameYet.initName(name);

    assertEquals(name, noNameYet.getName());
  }

  @Test
  void shouldResolveMode() {
    var patternWithoutExplicitMode = TripPattern.of(id(ID))
      .withRoute(ROUTE)
      .withStopPattern(STOP_PATTERN)
      .build();

    assertEquals(patternWithoutExplicitMode.getMode(), ROUTE.getMode());
  }

  @Test
  void containsAnyStopId() {
    assertFalse(SUBJECT.containsAnyStopId(List.of()));
    assertFalse(SUBJECT.containsAnyStopId(List.of(id("not-in-pattern"))));
    assertTrue(SUBJECT.containsAnyStopId(List.of(STOP_A.getId())));
    assertTrue(SUBJECT.containsAnyStopId(List.of(STOP_A.getId(), id("not-in-pattern"))));
  }
}
