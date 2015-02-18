---
layout: docs
title: Scala
page_title: Server - Scala
prev_section: server-java
next_section: server-persistence-jdbc
permalink: /docs/server-scala/
---

## Add dependency

### Maven

{% highlight xml %}
<dependency>
    <groupId>be.wegenenverkeer</groupId>
    <artifactId>atomium-server</artifactId>
    <version>{{site.version}}</version>
</dependency>
{% endhighlight %}

### SBT

{% highlight scala %}
libraryDependencies += "be.wegenenverkeer" % "atomium-server" % "{{site.version}}"
{% endhighlight %}