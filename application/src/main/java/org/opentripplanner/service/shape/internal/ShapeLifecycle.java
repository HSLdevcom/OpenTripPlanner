package org.opentripplanner.service.shape.internal;

import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;
import org.opentripplanner.service.shape.ShapeRepository;
import org.opentripplanner.service.shape.ShapeSnapshot;

/**
 * Copy-on-write / freeze lifecycle for the shape repository. Each transaction that writes geometry
 * gets a new mutable repository initialized from the last committed snapshot, and a new immutable
 * snapshot is published when the transaction commits. Edits made to a repository that is never
 * frozen are discarded, which is what makes transaction rollback possible.
 * <p>
 * What is copied here is only the real-time override layer. The static layer — the pattern index,
 * the per-trip index and the interned hop pool, all of which come from graph build and never change
 * afterwards — is passed on by reference. See {@link ShapeRepository} for why that distinction is
 * part of the contract rather than an implementation choice.
 */
public class ShapeLifecycle implements RepositoryLifecycle<ShapeSnapshot, ShapeRepository> {

  @Override
  public ShapeRepository copyOnWrite(ShapeSnapshot snapshot) {
    // the cast is safe: all snapshots are created by freeze() below
    return ((DefaultShapeSnapshot) snapshot).copyOnWrite();
  }

  @Override
  public ShapeSnapshot freeze(ShapeRepository repository) {
    // the cast is safe: all repositories are created by copyOnWrite() above or by the module
    return ((DefaultShapeRepository) repository).freeze();
  }
}
