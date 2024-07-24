package org.opentripplanner.street.model.vertex;

public class StationEntranceVertex extends OsmVertex {

  public final String entranceName;

  public StationEntranceVertex(double x, double y, long nodeId, String entranceID) {
    super(x, y, nodeId);
    this.entranceName = entranceID;
  }

  public String getEntranceName() {
    return entranceName;
  }

  public String toString() {
    return "EntranceVertex(" + super.toString() + ")";
  }
}
