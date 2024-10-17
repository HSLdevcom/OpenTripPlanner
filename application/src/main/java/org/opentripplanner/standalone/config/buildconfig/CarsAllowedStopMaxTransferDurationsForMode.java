package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_7;

import java.time.Duration;
import java.util.Map;
import org.opentripplanner.api.parameter.ApiRequestMode;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class CarsAllowedStopMaxTransferDurationsForMode {

  public static DurationForEnum<ApiRequestMode> map(
    NodeAdapter root,
    String parameterName,
    Duration maxTransferDuration
  ) {
    return withMaxDuration(
      maxTransferDuration,
      root
        .of(parameterName)
        .since(V2_7)
        .summary(
          "This is used for specifying a `maxTransferDuration` value for bikes and cars to use with transfers between stops that have trips with cars."
        )
        .description(
          """
This is a special parameter that only works on transfers between stops that have trips that allow cars.
The duration can be set for either 'BICYCLE' or 'CAR'.
For cars, this overrides the default `maxTransferDuration` for transfers between stops that have trips that allow cars.
For bicycles, this indicates that additional transfers should be calculated with the specified duration between stops that have trips that allow cars.

**Example**

```JSON
// build-config.json
{
  "carsAllowedStopMaxTransferDurationsForMode": {
    "CAR": "2h",
    "BICYCLE": "3h"
  } 
}
```
"""
        )
        .asEnumMap(ApiRequestMode.class, Duration.class)
    );
  }

  private static DurationForEnum<ApiRequestMode> withMaxDuration(
    Duration defaultValue,
    Map<ApiRequestMode, Duration> values
  ) {
    return DurationForEnum
      .of(ApiRequestMode.class)
      .withDefault(defaultValue)
      .withValues(values)
      .build();
  }
}
