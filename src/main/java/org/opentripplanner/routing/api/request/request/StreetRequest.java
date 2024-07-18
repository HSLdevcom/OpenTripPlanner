package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import java.util.List;
import org.opentripplanner.routing.api.request.StreetMode;

// TODO VIA: Javadoc
public class StreetRequest implements Cloneable, Serializable {

  private List<StreetMode> modes;

  public StreetRequest() {
    this(StreetMode.WALK);
  }

  public StreetRequest(StreetMode mode) {
    this.modes = List.of(mode);
  }

  public void setMode(StreetMode mode) {
    this.modes = List.of(mode);
  }

  public void setModes(List<StreetMode> modes) {
    this.modes = modes;
  }

  public List<StreetMode> modes() {
    return modes;
  }

  public StreetMode searchMode() {
    if (modes.size() == 1) {
      return modes.getFirst();
    }
    // TODO support for more than two modes within a search might be implemented here
    return modes.stream().filter(mode -> mode != StreetMode.WALK).findFirst().get();
  }

  public StreetRequest clone() {
    try {
      return (StreetRequest) super.clone();
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
