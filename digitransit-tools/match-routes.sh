#!/usr/bin/env bash

# This script reads GTFS file as input (argument 1) and outputs route ids matching to
# regular expression of second argument.

if [ -z "$1" ]
then
    >&2 echo
    >&2 echo "Usage:"
    >&2 echo
    >&2 echo "    $0 GTFS_FILE [REGEXP]"
    >&2 echo
    exit 1
fi

GTFS_INPUT=$1
REGEXP=${2:-'^(21(4[3-9]|5[0-9]|6[0-5])(A|[B-Z]A).*|2321|7.*)$'}

# create sandbox directory
WD=$(mktemp -d -t gtfs-XXXXXX)
ROUTE_FILE="$WD/routes.txt"
ROUTE_ID_FILE="$WD/route-ids.txt"

# read route_ids in routes.txt from gtfs package
>&2 echo " 📁 Reading file: $GTFS_INPUT"
unzip "$GTFS_INPUT" routes.txt -d "$WD" > /dev/null 2>&1
tail -n +2 "$ROUTE_FILE" | cut -d, -f1 | sort | uniq > "$ROUTE_ID_FILE"

ROUTE_COUNT=$(wc -l < "$ROUTE_ID_FILE")
>&2 echo " 💡 Number of routes found from '$GTFS_INPUT': $ROUTE_COUNT"
>&2 echo " 🔎 Matching against /$REGEXP/..."
>&2 echo
grep -E $REGEXP "$ROUTE_ID_FILE"

# clean up
rm -rf "$WD"
