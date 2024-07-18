package org.opentripplanner.routing.api.request;

import java.util.List;

public class RequestModesBuilder {

  private List<StreetMode> accessModes;
  private List<StreetMode> egressModes;
  private List<StreetMode> directModes;
  private List<StreetMode> transferModes;

  RequestModesBuilder(RequestModes origin) {
    this.accessModes = origin.accessModes;
    this.egressModes = origin.egressModes;
    this.directModes = origin.directModes;
    this.transferModes = origin.transferModes;
  }

  public List<StreetMode> accessModes() {
    return accessModes;
  }

  public RequestModesBuilder withAccessMode(StreetMode accessMode) {
    this.accessModes = List.of(accessMode);
    return this;
  }

  public RequestModesBuilder withAccessModes(List<StreetMode> accessModes) {
    this.accessModes = accessModes;
    return this;
  }

  public List<StreetMode> egressModes() {
    return egressModes;
  }

  public RequestModesBuilder withEgressMode(StreetMode egressMode) {
    this.egressModes = List.of(egressMode);
    return this;
  }

  public RequestModesBuilder withEgressModes(List<StreetMode> egressModes) {
    this.egressModes = egressModes;
    return this;
  }

  public List<StreetMode> directModes() {
    return directModes;
  }

  public RequestModesBuilder withDirectMode(StreetMode directMode) {
    this.directModes = List.of(directMode);
    return this;
  }

  public RequestModesBuilder withDirectModes(List<StreetMode> directModes) {
    this.directModes = directModes;
    return this;
  }

  public List<StreetMode> transferModes() {
    return transferModes;
  }

  public RequestModesBuilder withTransferMode(StreetMode transferMode) {
    this.transferModes = List.of(transferMode);
    return this;
  }

  public RequestModesBuilder withTransferModes(List<StreetMode> transferModes) {
    this.transferModes = transferModes;
    return this;
  }

  public RequestModesBuilder withAllStreetModes(StreetMode streetMode) {
    return withAccessMode(streetMode)
      .withEgressMode(streetMode)
      .withDirectMode(streetMode)
      .withTransferMode(streetMode);
  }

  public RequestModes build() {
    return new RequestModes(this);
  }
}
