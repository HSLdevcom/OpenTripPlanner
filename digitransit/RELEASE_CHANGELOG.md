# Digitransit OTP Release Summary

## Changelog _20260428_ vs _upstream/dev-2.x_

### Added PRs
- Add NO_DIRECT_MODE_CONNECTION routing error code for direct-only searches [#7494](https://github.com/opentripplanner/OpenTripPlanner/pull/7494)
- De-duplicate boardinglocations on areas [#7508](https://github.com/opentripplanner/OpenTripPlanner/pull/7508)
- Speed up tag lookup during OSM processing [#7536](https://github.com/opentripplanner/OpenTripPlanner/pull/7536)
- Include entrances that are part of a stop area relation, change build config field `includeOsmSubwayEntrances` to `includeOsmStationEntrances` [#7170](https://github.com/opentripplanner/OpenTripPlanner/pull/7170)
- Add application warmup feature to run routing queries during startup [#7509](https://github.com/opentripplanner/OpenTripPlanner/pull/7509)
- Add `startupRetryPeriod` to GBFS feed configuration [#7525](https://github.com/opentripplanner/OpenTripPlanner/pull/7525)
- Add support for transit group priority in the GTFS API [#7451](https://github.com/opentripplanner/OpenTripPlanner/pull/7451)
- Set default fares to  [#7545](https://github.com/opentripplanner/OpenTripPlanner/pull/7545)
- Reduce memory consumption when parsing very large NeTEx files [#7563](https://github.com/opentripplanner/OpenTripPlanner/pull/7563)
- Remove relation route name application [#7539](https://github.com/opentripplanner/OpenTripPlanner/pull/7539)
- Skip transit search when no access or egress mode is set [#7547](https://github.com/opentripplanner/OpenTripPlanner/pull/7547)
- Skip leg rebuild when fare/alert decoration would be a no-op [#7546](https://github.com/opentripplanner/OpenTripPlanner/pull/7546)
- Replace C2-based pass-through with via-connection chaining [#7496](https://github.com/opentripplanner/OpenTripPlanner/pull/7496)
- Improve Secret redaction for config File Logging [#7560](https://github.com/opentripplanner/OpenTripPlanner/pull/7560)
- Always generate hop geometries in the GTFS graph builder [#7571](https://github.com/opentripplanner/OpenTripPlanner/pull/7571)
- Coarsen speed and reluctance values at the request level [#7569](https://github.com/opentripplanner/OpenTripPlanner/pull/7569)
- Fix RAPTOR path building for flex patterns with duplicated stops [#7554](https://github.com/opentripplanner/OpenTripPlanner/pull/7554)
- Precompute cumulative pattern distance for O(1) leg distance lookup [#7559](https://github.com/opentripplanner/OpenTripPlanner/pull/7559)
