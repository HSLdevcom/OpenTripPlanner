# Shapes

The shape of a trip pattern is the path a vehicle travels between its stops. It is stored as a
`CompactLineStringSequence`: one `CompactLineString` per hop (stop to stop) plus a table of
cumulative distances. The shape is not used in routing. It gives the geometry and distance of
transit legs and the geometries of the GraphQL APIs.

## Ownership

The shape belongs to the transit model object it describes, so it lives exactly as long as that
object and needs no lifecycle of its own.

- `TripPattern.getShape()` holds the shape of the pattern. It is never null.
- `TripPatternBuilder.withShape(...)` sets it. `build()` checks with `TripShapeValidator` that it
  has one hop per pair of consecutive stops. Without a shape, the builder uses straight lines
  between the stops (tests, added trips, SIRI extra calls).
- `TripPattern.copy()` keeps the shape. If the stop pattern changes so that the hop count no longer
  matches, the caller must set a new shape (or `null` for straight lines).

## Data flow

1. **Graph build:** the GTFS `GeometryProcessor` (from `shapes.txt`) and the NeTEx
   `ServiceLinkMapper` (from ServiceLink projections) create the hop shapes of a pattern, and
   `TripShapeFactory.fromLineStrings` compacts them. A pattern without shape data gets straight
   lines.
2. **Real-time:** `TripPatternCache` creates a pattern when an update changes the stops of a trip,
   and sets its shape at creation with `RealTimeTripShapeFactory` (`updater/trip/shape`). The shape
   is derived from the pattern it replaces; a pattern that replaces none gets straight lines. The
   cached pattern keeps its shape for as long as the pattern is cached, also across `FULL_DATASET`
   updates, and a pattern that is no longer used is dropped together with its shape.

Identical hops and identical distance tables are deduplicated through the `DeduplicatorService`, so
many patterns share them.

## Glossary

| Term                        | Meaning                                                                        |
| --------------------------- | ------------------------------------------------------------------------------ |
| Hop                         | The segment between two consecutive stops of a pattern.                        |
| Hop shape                   | The `LineString` of a single hop; a trip pattern shape holds one per hop.      |
| Trip pattern shape          | All hops of a pattern, as a `CompactLineStringSequence`.                       |
| `CompactLineStringSequence` | Immutable list of hops plus cumulative distances; decodes hops on demand.      |
| `CompactLineString`         | One hop in packed form (package-private); equal hops are shared.               |
| `PackedLineStringCodec`     | Encodes/decodes the packed byte form; lives in the `street` module.            |
| `TripShapeFactory`          | Builds a `CompactLineStringSequence` from line strings or from straight lines. |
| `TripShapeValidator`        | Checks that a shape has one hop per pair of consecutive stops.                 |
| `RealTimeTripShapeFactory`  | Derives the shape of a real-time pattern from the pattern it replaces.         |
