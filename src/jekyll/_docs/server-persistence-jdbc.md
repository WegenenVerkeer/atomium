---
layout: docs
title: Relational database
page_title: Server - JDBC
prev_section: server-scala
next_section: server-persistence-mongo
permalink: /docs/server-persistence-jdbc/
---

The `atomium-server-jdbc` module provides a feed store implementation that stores the feeds in a relational database, using the the [Slick](http://slick.typesafe.com/) library.

## Add dependency

### Maven

{% highlight xml %}
<dependency>
    <groupId>be.vlaanderen.awv</groupId>
    <artifactId>atomium-server-jdbc</artifactId>
    <version>{{site.version}}</version>
</dependency>
{% endhighlight %}

### SBT

{% highlight scala %}
libraryDependencies += "be.vlaanderen.awv" % "atomium-server-jdbc" % "{{site.version}}"
{% endhighlight %}

## TODO

- configuration
- database structure
