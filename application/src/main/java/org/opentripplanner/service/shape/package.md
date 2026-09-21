# Shape Service

Owns the geometry of transit shapes: the path a vehicle takes between stops, for a trip pattern or
for one specific trip.

**Status: API design only.** The interfaces and value types here are complete and documented, and
the `internal/` classes are skeletons that establish the field layout and lifecycle seams. Nothing
is implemented, nothing is wired (no Dagger module, no `SerializedGraphObject` field), and no
existing code has been changed. `TripPattern` still owns geometry and every current caller still
reads it from there.

## Why this exists

Geometry is currently a `CompactLineStringSequence` field on `TripPattern`. Three problems follow
from that:

1. **A pattern can only hold one shape.** `GenerateTripPatternsOperation` builds hop geometries for
   the first trip that creates a pattern; every later trip with a different `shape_id` joins the
   same pattern and its shape is silently discarded. A route that is diverted for part of the day
   loses the diversion, and which of the two shapes survives depends on trip iteration order.
2. **There is nowhere to put per-trip or real-time geometry.** Real-time added and modified trips
   get geometry from `TripPatternBuilder.generateHopGeometriesFromOriginalTripPattern()`, which
   copies or straight-lines the original pattern's hops. A shape supplied by a real-time feed has no
   home.
3. **Deduplication is build-time only.** `GeometryProcessor` interns hop geometry by
   `ShapeSegmentKey(shapeId, fromDist, toDist)`, but that cache dies with the graph builder. Nothing
   in the serialized model shares hop geometry between patterns.

## Structure

Follows the `service/realtimevehicles` shape and the transaction framework's recommended model
(`framework/transaction/package.md`):

| Type              | Role                                                                               |
| ----------------- | ---------------------------------------------------------------------------------- |
| `ShapeSnapshot`   | Immutable, raw lookups by id. No transit-model knowledge, no fallback logic.       |
| `ShapeRepository` | Mutable, write-side, reached through a `WriteContext` on the single writer thread. |
| `ShapeService`    | Request-scoped read view that resolves trips and real-time patterns.               |
| `ShapeLifecycle`  | `copyOnWrite` / `freeze`.                                                          |

## Two storage layers

The repository holds data with two very different lifetimes, and the split is part of its contract:

| Layer               | Content                                                  | Lifetime                    | Serialized |
| ------------------- | -------------------------------------------------------- | --------------------------- | ---------- |
| Static              | pattern variants, per-trip shapes, the interned hop pool | written at graph build only | yes        |
| Real-time overrides | per-trip geometry from real-time updates                 | copied per transaction      | no         |

This is what makes copy-on-write affordable. A national feed has on the order of 10^5 patterns, and
`copyOnWrite` runs on every commit — commits happen on a periodic timer. Copying an index that size
per transaction is not viable, so the static layer is shared by reference (`StaticShapeLayer`) and
only the small override map is copied.

It also means one repository can be both serialized into the graph and registered with the
transaction framework, which the other transaction-framework repositories are not.

## Deduplication: hops, not shapes

`HopGeometry` — a single stop-to-stop segment — is the unit of interning. Interning whole shapes
only pays off when two trips share both a shape and a stop pattern. Interning hops also captures:

- an override that diverges from the pattern default in one hop, sharing every other hop;
- two patterns on the same corridor (a full run and a short-turn variant) sharing their common hops;
- two trips on the same shape with different stop patterns, sharing every coinciding hop.

Interning is invisible to callers: the write methods take plain `LineString`s and the repository is
responsible for the invariant. An invariant that callers can hand pre-interned objects into is one
that cannot be enforced.

## Two indexes, one set of sequences

`StaticShapeLayer` holds `pattern -> List<ShapeVariant>` and `trip -> HopGeometrySequence`. These
are two access paths over the same sequence instances, not two copies:

- the per-pattern index answers "what distinct geometries does this pattern have, and which trips
  use each" in one lookup, and makes the most-used default computable;
- the per-trip index answers "what geometry applies to this trip" in constant time, and holds
  entries only for trips that differ from their pattern default, so it stays small.

Keeping only the first forces a scan plus caller-side deduplication for the common query; keeping
only the second loses the trip-to-geometry association the default is selected by.

## Default = the most common shape

`ShapeRepository.indexStaticGeometries()` selects each pattern's default as the geometry used by the
most of its trips. Three things this depends on:

- **Deterministic tie-breaking.** Ties must be broken by a stable key, not encounter order, or graph
  builds stop being reproducible and snapshot tests flap.
- **Build cost.** Hop geometries must be computed for every trip rather than only the first. The
  existing `ShapeSegmentKey` cache absorbs most of the CPU cost, and hop interning bounds the
  steady-state memory, but neither has been measured.
- **It is an API-visible behaviour change.** `Pattern.geometry` and `Trip.tripGeometry` (GTFS
  GraphQL), `JourneyPattern.pointsOnLink` and `ServiceJourney.pointsOnLink` (Transmodel), and
  `Leg.legGeometry` / `Leg.distance` all change output for feeds whose patterns mix shapes.

## GTFS and NeTEx are not symmetric

NeTEx attaches ServiceLink projections to the JourneyPattern, so every ServiceJourney on a journey
pattern shares one geometry (`TripPatternMapper` →
`ServiceLinkMapper.getGeometriesByJourneyPattern`). **Per-trip static geometry is a GTFS-only
concept.** For NeTEx, `ShapeService.getTripGeometry` always resolves to the pattern default, and
`GeometrySource.STATIC_TRIP_SHAPE` never occurs.

## Feasibility notes for the implementation phase

**Low risk.** Routing is unaffected: geometry is not read by Raptor or `RoutingTripPattern`, so
moving it out carries no SpeedTest risk. The transaction framework imposes no obstacle — the
static/dynamic split is the same tradeoff its own `candyshop` example demonstrates between its two
lifecycle strategies.

**`CompactLineString` is package-private in the `street` module.** Its `of(LineString)` and
`toLineString(boolean)` methods have no access modifier, so the shape model cannot wrap it. This is
why `HopGeometry` is an interface that hides its storage form rather than a record over a compact
line string. Implementing it will require either widening that visibility or placing the compact
implementation inside `street`.

**`CompactLineStringSequence` cannot yet be built from interned hops.** Its only factory takes
`List<LineString>` and compacts them itself, which re-compacts and therefore defeats interning. A
factory accepting already-compacted members is a prerequisite for hop-level deduplication.

**`ScheduledTransitLeg` is the largest ripple.** It reads `tripPattern.distanceBetween(...)` eagerly
in its constructor and `tripPattern.geometryBetween(...)` lazily in `legGeometry()`. Once geometry
leaves `TripPattern` the leg needs either a request-scoped `ShapeService` reference or values
injected through `ScheduledTransitLegBuilder`. `ShapeService` supports both: the
trip-and-stop-position signatures can be called by a builder without holding a leg, and
`distanceMetersBetween` exists so that the per-leg hot path does not allocate a `Distance`.

**Real-time pattern creation loses its free geometry.** The reconstruction logic in
`TripPatternBuilder.generateHopGeometriesFromOriginalTripPattern()` (same stops → reuse; same
station → patch endpoints; otherwise straight line) is reachable today only because geometry is
still a field on `TripPattern`. `ShapeRepository.deriveGeometryForModifiedStopPattern` is the
interface seam that replaces it.

**Serialization version bump.** Adding a `ShapeRepository` field to `SerializedGraphObject` and
removing the geometry field from `TripPattern` both change the graph format, so
`otp.serialization.version.id` must be bumped when that happens. Not in this phase — nothing is
serialized yet.

**Unmeasured.** Memory delta from retaining every distinct shape variant instead of one per pattern;
graph-build time delta from computing geometry for all trips; whether flex patterns (`AreaStop` /
`GroupStop` hops, currently straight lines) need any special representation.
