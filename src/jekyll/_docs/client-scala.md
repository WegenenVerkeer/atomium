---
layout: docs
title: Scala
page_title: Client - Scala
prev_section: client-java
next_section: client-samples
permalink: /docs/client-scala/
---

Client library for handling atomium feeds (which are atom-like feeds in JSON).

## Add dependency

### Maven

{% highlight xml %}
<dependency>
    <groupId>be.wegenenverkeer</groupId>
    <artifactId>atomium-client-scala</artifactId>
    <version>{{site.version}}</version>
</dependency>
{% endhighlight %}

### SBT

{% highlight scala %}
libraryDependencies += "be.wegenenverkeer" % "atomium-client-scala" % "{{site.version}}"
{% endhighlight %}

## Play WS API feed provider
 
If you want to use this implementation you will need to add a dependency on the Play WS API library.

{% highlight scala %}
libraryDependencies += "com.typesafe.play" %%  "play-ws" % "2.3.0"
{% endhighlight %}
