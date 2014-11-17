---
layout: docs
title: General
page_title: Server - General
prev_section: client-samples
next_section: server-java
permalink: /docs/server-general/
---

The server modules provide support for creating a server that allows you to publish ATOMPub feeds.

The server part consists of three modules:

- server
- server-jdbc
- server-mongo

The server library provides a feed service. A feed service can be used to

- add new elements to the feed
- retrieve a page of the feed

A feed service is not responsible for persistence of the entries and pages in feeds, this is delegated to a feed store.

The feed service implementation is written in Scala, but the library also provides a Java wrapper.

A feed store is responsible for the persistence of feeds. There are currently two implementations:

- a feed store that stores data in a relational database
- a feed store that stores data in a Mongo database

Both persistence libraries provide a Scala and Java variant. The Java implementation is a simple wrapper around the Scala implementation.

