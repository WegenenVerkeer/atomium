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
- server-slick
- server-mongo

The server library provides a feed service. A feed service can be used to

- add new elements to the feed
- retrieve a page of the feed

A feed service is not responsible for persistence of the entries and pages in feeds, this is delegated to a feed store.

The feed service implementation is written in Scala, but the library also provides a Java wrapper.

Feed pages provide 'previous' and 'next' links by which a client can navigate through the whole feed.

Following a 'previous' link will move forwards through the feed, this means moving towards the head of the feed
and this will retrieve more recent feed entries

Following a 'next' link will move backwards through the feed, this means moving towards the last page of the feed
and this will retrieve older feed entries

A feed store is responsible for the persistence of feeds. There are currently two implementations:

- a feed store that stores data in a relational database
- a feed store that stores data in a Mongo database

Both persistence libraries provide a Scala and Java variant. The Java implementation is a simple wrapper around the Scala implementation.

There is also an AbstractFeedStore base class that can be used to implement your own feedStore implementation,
using your own persistence technology. Using this class will make sure that you the paging (providing 'next'/'previous' links)
 will work correctly.

