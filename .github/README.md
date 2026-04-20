<h1 align="center">
  <img src="/doc/user/images/otp-logo.svg" width="120" /><br>
  OpenTripPlanner (OTP) - Digitransit Fork
</h1>
<p align="center">
  <b>
    HSL OTP Deployments ・ 
    <a href="https://dev.reittiopas.fi/etusivu">DEV</a> ・
    <a href="https://reittiopas.hsl.fi/etusivu">PROD</a>
  </b>
</p>
<p align="center">
  <b>
    HSL OTP Debug ・ 
    <a href="https://dev-hsl-debug.digitransit.fi/">DEV</a> ・
    <a href="https://hsl-debug.digitransit.fi/">PROD</a>
  </b>
</p>
<p align="center">
  <b>
    OTP GraphQL Explorer ・ 
    <a href="https://dev-api.digitransit.fi/graphiql/hsl">DEV</a> ・
    <a href="https://api.digitransit.fi/graphiql/hsl">PROD</a>
  </b>
</p>

# Overview of CI and Branch Configuration

This is the Digitransit fork of [OpenTripPlanner](https://github.com/opentripplanner/OpenTripPlanner).
The [`digitransit_ext_config`](https://github.com/HSLdevcom/OpenTripPlanner/tree/digitransit_ext_config) branch contains the Digitransit CI configuration and scripts.

> **The contents of the `v2` branch are reset often and nothing is configured to be retained in the branch permanently!
It should not be used as a base for changes.
Always use the upstream [`dev-2.x`](https://github.com/opentripplanner/OpenTripPlanner) branch as a base and make contributions there.**

## Relevant Branches

This repository contains the current dev version in-use in the `v2` branch.
The current prod version comes from the [latest release](https://github.com/HSLdevcom/OpenTripPlanner/releases/latest).
There is also a `custom-release` branch that is used for building versions of OTP that differ from the `v2` branch.

## CI Configuration

Checkout [`.github/workflows`](/.github/workflows/). The configured CI pipelines build docker images for dev and prod. The built images can be found here: https://hub.docker.com/r/hsldevcom/opentripplanner.

## [`merge_upstream.sh`](https://github.com/HSLdevcom/OpenTripPlanner/blob/digitransit_ext_config/merge_upstream.sh)

This script can be used to merge and push changes from e.g. `upstream/dev-2.x` to the `v2` or `custom-release` branches.
You need to have `python3` and `jq` installed.

## [`digitransit/RELEASE_CHANGELOG.md`](/digitransit/RELEASE_CHANGELOG.md)

The generated changelog diff can be found in this file, if it exists.
