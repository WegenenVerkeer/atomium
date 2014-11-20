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
    <groupId>be.vlaanderen.awv</groupId>
    <artifactId>atomium-server</artifactId>
    <version>{{site.version}}</version>
</dependency>
{% endhighlight %}

### SBT

{% highlight scala %}
libraryDependencies += "be.vlaanderen.awv" % "atomium-server" % "{{site.version}}"
{% endhighlight %}