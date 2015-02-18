---
layout: docs
title: General
page_title: Client - General
prev_section: features
next_section: client-java
permalink: /docs/client-general/
---

The client libraries provide the necessary classes to consume an ATOMPub feed. The library provides a feed processor that fetches feed pages and offers them for consumption.

The library user is responsible for:

- a feed provider
- persisting the latest feed position

The library provides a default implementation for a feed provider that uses the [Play WS API](https://www.playframework.com/documentation/latest/ScalaWS).
