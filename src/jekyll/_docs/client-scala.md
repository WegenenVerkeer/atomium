---
layout: docs
title: Scala
page_title: Client - Scala
prev_section: client-java
next_section: server-general
permalink: /docs/client-scala/
---

Client library for handling atomium feeds (which are atom-like feeds in JSON).

## Add dependency

### Maven

{% highlight xml %}
<dependency>
    <groupId>be.vlaanderen.awv</groupId>
    <artifactId>atomium-client-scala</artifactId>
    <version>{{site.version}}</version>
</dependency>
{% endhighlight %}

### SBT

{% highlight scala %}
libraryDependencies += "be.vlaanderen.awv" % "atomium-client-scala" % "{{site.version}}"
{% endhighlight %}
