---
layout: docs
title: Java
page_title: Server - Java
prev_section: server-general
next_section: server-scala
permalink: /docs/server-java/
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