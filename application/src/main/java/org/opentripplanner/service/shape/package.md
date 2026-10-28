# Shape service

Owns the geometry of trip patterns. The geometry of a pattern is a `CompactLineStringSequence` with
one `CompactLineString` per hop (stop to stop) plus cumulative distances.

- `ShapeService` – read access, used by routing, itinerary mapping and the GraphQL APIs. It is
  backed by a `ShapeRepositorySnapshot`.
- `ShapeDataImport` (`dataimport`) – mutable, written at graph build time and serialized with the
  graph. `toScheduledShapeData()` freezes it into the immutable `ScheduledShapeData` (like
  `TimetableDataImport` / `ScheduledTimetableData`).
- `ShapeRepository` – the long-lived, transactional buffer. It owns the `ScheduledShapeData` and the
  geometries of patterns created by real-time updates, and publishes immutable
  `ShapeRepositorySnapshot`s at commit time (like `TimetableRepository`). A snapshot reads the
  real-time geometries first and falls back to the scheduled ones.

Identical hops are deduplicated, so equal hops share one pointer (Kryo reference tracking keeps that
across serialization). Real-time modified patterns get their geometry from the pattern they were
copied from, see `RealTimeShapeResolver`.
