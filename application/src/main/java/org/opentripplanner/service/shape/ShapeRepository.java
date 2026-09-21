package org.opentripplanner.service.shape;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.model.GeometrySource;
import org.opentripplanner.service.shape.model.HopGeometry;
import org.opentripplanner.service.shape.model.HopGeometrySequence;
import org.opentripplanner.transit.model.network.StopPattern;

/**
 * The mutable repository for transit shape geometry. It is managed by the transaction framework: a
 * new repository initialized from the last committed {@link ShapeSnapshot} is created for each
 * transaction that writes geometry, obtained on the single writer thread through a
 * {@link org.opentripplanner.framework.transaction.api.WriteContext}, and a new immutable
 * {@link ShapeSnapshot} is published for the request threads at commit time.
 *
 * <h2>Two storage layers</h2>
 * Unlike the other transaction-framework repositories, this one holds data with two very different
 * lifetimes, and the split is part of its contract rather than an implementation detail:
 * <dl>
 *   <dt>The static layer</dt>
 *   <dd>Pattern defaults, static per-trip shapes, and the pool of interned {@link HopGeometry}. It
 *       is written only during graph build, is immutable afterwards, and is serialized into the
 *       graph. Every repository copy and every snapshot shares it <em>by reference</em>.</dd>
 *   <dt>The real-time override layer</dt>
 *   <dd>Per-trip geometry supplied or derived by real-time updates. Small, rewritten constantly,
 *       and not serialized.</dd>
 * </dl>
 * This split is what makes the copy-on-write lifecycle affordable. A national feed has on the order
 * of a hundred thousand patterns, and copying an index that size on every commit — commits happen on
 * a periodic timer — would not be viable. Because the static layer is shared rather than copied,
 * {@code copyOnWrite} only has to copy the override layer.
 * <p>
 * Methods below state which layer they touch. Calling a static-layer method after graph build is a
 * programming error.
 *
 * <h2>Deduplication</h2>
 * The repository interns hop geometry internally and callers do not participate: the write methods
 * take plain {@link LineString}s and the repository is responsible for compacting them, replacing
 * each with an already-stored equal instance where one exists, and assembling sequences from the
 * result.
 * <p>
 * Interning is kept off the API on purpose. It is a storage invariant, and an invariant that callers
 * can hand pre-interned objects into is an invariant that cannot be enforced. Hops, rather than
 * whole shapes, are the unit — see {@link HopGeometry} for why.
 */
public interface ShapeRepository {
  /**
   * Record the geometry of one trip, in the static layer.
   * <p>
   * Called once per trip during graph build — including for trips that turn out to share their
   * pattern's geometry, because which geometry is the most common cannot be known until every trip
   * has been seen. The repository interns the hops, so passing the same shape for a thousand trips
   * of a pattern costs one stored copy.
   * <p>
   * This is the change that lets a pattern keep more than one shape. Today geometry is built only
   * for the first trip that creates a pattern and every later trip's shape is silently dropped.
   *
   * @param patternId     the pattern the trip belongs to.
   * @param tripId        the trip.
   * @param shapeId       the shape the geometry came from, or {@code null} when the source data has
   *                      no identified shape — NeTEx ServiceLink projections and straight-line
   *                      fallbacks both have none.
   * @param hopGeometries one geometry per hop, so one fewer than the number of stops. Must be
   *                      end-to-end connected.
   * @param source        whether these are real shape geometry or a straight-line fallback.
   */
  void addTripGeometry(
    FeedScopedId patternId,
    FeedScopedId tripId,
    @Nullable FeedScopedId shapeId,
    List<LineString> hopGeometries,
    GeometrySource source
  );

  /**
   * Choose each pattern's default geometry and build the read indexes, closing the static layer to
   * further writes. Called once at the end of graph build.
   * <p>
   * The default is the geometry used by the most trips of the pattern. Ties must be broken by a
   * stable key rather than by encounter order, because graph builds have to be reproducible: two
   * builds of the same feed must select the same default, or snapshot tests and any downstream
   * artifact keyed on the geometry become unstable.
   * <p>
   * Note that this changes existing behaviour. Today the pattern's geometry is whichever shape
   * belonged to the first trip that happened to create the pattern, which for a diverted route is
   * frequently the less representative one.
   *
   * @throws IllegalStateException if called more than once.
   */
  void indexStaticGeometries();

  /**
   * Store a geometry supplied by a real-time update for one trip, in the override layer, replacing
   * any override already held for that trip.
   * <p>
   * The override is per trip, not per pattern: a real-time diversion applies to the vehicle that was
   * diverted, not to every trip that shares its stop pattern.
   */
  void setRealTimeGeometry(
    FeedScopedId tripId,
    FeedScopedId patternId,
    List<LineString> hopGeometries
  );

  /**
   * Drop the real-time override for one trip, so that it falls back to its static geometry.
   * Does nothing if there is no override.
   */
  void removeRealTimeGeometry(FeedScopedId tripId);

  /**
   * Drop every real-time override belonging to a feed, so that all of its trips fall back to their
   * static geometry. Used when a feed's real-time data is replaced wholesale or goes stale.
   */
  void removeRealTimeGeometriesForFeed(String feedId);

  /**
   * Derive a geometry for a stop pattern that a real-time update has modified, based on the
   * geometry of the pattern it was derived from.
   * <p>
   * A real-time update that changes which stops a trip serves invalidates the geometry hop by hop,
   * and the replacement has to be reconstructed: a hop between the same two stops keeps its
   * geometry, a hop between different stops of the same stations keeps its shape with the endpoints
   * moved, and anything else falls back to a straight line.
   * <p>
   * That reconstruction lives here rather than in the updater because it needs the static layer to
   * read from and the intern pool to write into. It is the piece that today comes for free from
   * {@code TripPatternBuilder}, which can reach the original pattern's geometry directly because the
   * geometry is still a field on {@code TripPattern}; once geometry moves out, the updater has no
   * such path and this method is what replaces it.
   * <p>
   * Returns the derived sequence without storing it; the caller decides whether it becomes an
   * override. Empty when the original pattern is unknown or has no geometry.
   *
   * @param originalPatternId the scheduled pattern the modified one was derived from.
   * @param newStopPattern    the stop pattern after the real-time modification.
   */
  Optional<HopGeometrySequence> deriveGeometryForModifiedStopPattern(
    FeedScopedId originalPatternId,
    StopPattern newStopPattern
  );
}
