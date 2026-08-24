<!--
  NOTE! Part of this document is generated. Make sure you edit the template, not the generated doc.

   - Template directory is:  /doc/templates
   - Generated directory is: /doc/user
-->

GTFS feeds contain _schedule_ data that is published by an agency or operator in advance. The feed
does not account for unexpected service changes or traffic disruptions that occur from day to day.
Thus, this kind of data is also referred to as 'static' data or 'scheduled' arrival and departure
times.

[GTFS-Realtime](https://gtfs.org/realtime/) complements GTFS with additional kinds of feeds. In
contrast to the base GTFS schedule feed, they provide _real-time_ updates (_'dynamic'_ data) and are
updated from minute to minute.

## Alerts

Alerts are text messages attached to GTFS objects, informing riders of disruptions and changes. The
information is downloaded in a single HTTP request and polled regularly.

<!-- INSERT: real-time-alerts -->

## TripUpdates via HTTP(S)

TripUpdates report on the status of scheduled trips as they happen, providing observed and predicted
arrival and departure times for the remainder of the trip. The information is downloaded in a single
HTTP request and polled regularly.

<!-- INSERT: stop-time-updater -->

##### `TripProperties.shape_id` support

OTP resolves `TripUpdate.TripProperties.shape_id` against standalone `Shape` FeedEntities carried in
the same GTFS-RT message (matched by the `Shape`'s own `shape_id` field, not the enclosing
`FeedEntity.id`), for trip updates of any `schedule_relationship` (`SCHEDULED`, `NEW`, `ADDED`,
`REPLACEMENT`, `DUPLICATED`). The referenced shape is decoded from its `encoded_polyline` and used
to build the pattern's hop geometries.

Resolving `shape_id` against shapes already defined in the feed's static `shapes.txt` is not yet
supported. If a `shape_id` cannot be resolved, OTP falls back to the default hop geometries
(straight lines between stops for a newly created pattern).

## Streaming TripUpdates via MQTT

This updater connects to an MQTT broker and processes TripUpdates in a streaming fashion. This means
that they will be applied individually in near-real-time rather than in batches at a certain
interval.

This system powers the real-time updates in Helsinki and more information can be found
[on Github](https://github.com/HSLdevcom/transitdata).

<!-- INSERT: mqtt-gtfs-rt-updater -->

## Vehicle Positions

VehiclePositions give the location of some or all vehicles currently in service, in terms of
geographic coordinates or position relative to their scheduled stops. The information is downloaded
in a single HTTP request and polled regularly.

<!-- INSERT: vehicle-positions -->
