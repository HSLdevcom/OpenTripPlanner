# `digitransit-tools`

This directory contains *Digitransit* specific command line tools to help with
service operation.

## `match-routes.sh`

A tool which reads a GTFS file (.zip) as input and outputs all route ids which
match to optional regular expressions.

Usage:

```sh
./match-routes.sh GTFS_FILE [REGEXP]
```

Default value for `REGEXP` is `^(21(4[3-9]|5[0-9]|6[0-5])(A|[B-Z]A).*|2321)$`.
This expression matches to Espoo bus lines which should be avoided if reasonable
alternate itinerary with subway exists.
