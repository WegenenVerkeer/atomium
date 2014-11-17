---
layout: docs
title: Mongo
page_title: Server - Mongo
prev_section: server-persistence-jdbc
next_section: server-samples
permalink: /docs/server-persistence-mongo/
---

The `atomium-server-mongo` module provides a feed store implementation that stores the feeds in a [Mongo](http://www.mongodb.org/) database.

## Add dependency

### Maven

{% highlight xml %}
<dependency>
    <groupId>be.vlaanderen.awv</groupId>
    <artifactId>atomium-server-mongo</artifactId>
    <version>{{site.version}}</version>
</dependency>
{% endhighlight %}

### SBT

{% highlight scala %}
libraryDependencies += "be.vlaanderen.awv" % "atomium-server-mongo" % "{{site.version}}"
{% endhighlight %}
