package org.opentripplanner.routing.core;

/**
 * FareType should be extendable for third-party usage
 *
 * java.nio.file.OpenOption is a good example for this requirement
 */
public interface FareType {
  String name();
}
