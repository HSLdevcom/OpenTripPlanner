# Digitransit OTP Release Summary

## Changelog _20260825_ vs _upstream/dev-2.x_

### Added PRs
- SIRI: Map terminal times as provided by the feed [#7864](https://github.com/opentripplanner/OpenTripPlanner/pull/7864)
- Replace raptor stop index with position [#7811](https://github.com/opentripplanner/OpenTripPlanner/pull/7811)
- Fix TransitAlertService is null for vector tiles [#7902](https://github.com/opentripplanner/OpenTripPlanner/pull/7902)
- Deprecate RealtimeState in both APIs [#7748](https://github.com/opentripplanner/OpenTripPlanner/pull/7748)
- Remove UNDEFINED alert severity [#7883](https://github.com/opentripplanner/OpenTripPlanner/pull/7883)
- Share GBFS manifest loading and send configured headers with the manifest request [#7884](https://github.com/opentripplanner/OpenTripPlanner/pull/7884)
- Reject GTFS-RT added trips referring to unknown stops [#7897](https://github.com/opentripplanner/OpenTripPlanner/pull/7897)
- Treat unstated NeTEx DaysOfWeek as valid for all days [#7871](https://github.com/opentripplanner/OpenTripPlanner/pull/7871)
- Improve how vehicle rental networks and vehicles are presented in the debug client [#7888](https://github.com/opentripplanner/OpenTripPlanner/pull/7888)
- Direction of line restriction in SIRI-SX [#7903](https://github.com/opentripplanner/OpenTripPlanner/pull/7903)
- Support for SIRI partial replacedBy [#7873](https://github.com/opentripplanner/OpenTripPlanner/pull/7873)
- Expose scheduled quay [#7739](https://github.com/opentripplanner/OpenTripPlanner/pull/7739)
- Document the transaction framework and cleanup module test [#7894](https://github.com/opentripplanner/OpenTripPlanner/pull/7894)
- Frequency-based vehicle position matching [#7927](https://github.com/opentripplanner/OpenTripPlanner/pull/7927)
- Move per-network GBFS configuration into a shared gbfs section of otp-config.json [#7886](https://github.com/opentripplanner/OpenTripPlanner/pull/7886)
- Compact node storage to reduce graph build memory [#7925](https://github.com/opentripplanner/OpenTripPlanner/pull/7925)
- Add `runningTimeRanges` filter to canceled trips/calls queries in the GTFS API [#7899](https://github.com/opentripplanner/OpenTripPlanner/pull/7899)
- Change APIs to properly support alerts with multiple active periods [#7926](https://github.com/opentripplanner/OpenTripPlanner/pull/7926)
- Fix GTFS GraphQL API's stop call's real-time estimated times [#7953](https://github.com/opentripplanner/OpenTripPlanner/pull/7953)
- Remove street notes feature [#7959](https://github.com/opentripplanner/OpenTripPlanner/pull/7959)
- Add alertConnection query to the GTFS GraphQL API [#7924](https://github.com/opentripplanner/OpenTripPlanner/pull/7924)
