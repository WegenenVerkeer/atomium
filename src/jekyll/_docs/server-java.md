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
    <groupId>be.vlaanderen.awv</groupId>
    <artifactId>atomium-server</artifactId>
    <version>{{site.version}}</version>
</dependency>
{% endhighlight %}

### SBT

{% highlight scala %}
libraryDependencies += "be.vlaanderen.awv" % "atomium-server" % "{{site.version}}"
{% endhighlight %}