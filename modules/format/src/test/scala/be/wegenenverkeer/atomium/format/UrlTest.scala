package be.wegenenverkeer.atomium.format

import org.scalatest.{FunSuite, Matchers}

class UrlTest extends FunSuite with Matchers {



  test("Can build http path") {

    val url = Url("http://foo.bar") / "test"

    url.path shouldBe "http://foo.bar/test"

  }

  test("Can build http path while avoiding double slashes slash in additional path") {

    val url = Url("http://foo.bar") / "/test"

    url.path shouldBe "http://foo.bar/test"

  }

  test("Can build http path while avoiding double slashes slash already in base") {

    val url = Url("http://foo.bar/") / "test"

    url.path shouldBe "http://foo.bar/test"

  }

  test("Can build http path while avoiding double slashes slash in both") {

    val url = Url("http://foo.bar/") / "/test"

    url.path shouldBe "http://foo.bar/test"

  }

  test("Can build https path") {

    val url = Url("https://foo.bar") / "test"

    url.path shouldBe "https://foo.bar/test"

  }

  test("Can build https path while avoiding double slashes slash in additional path") {

    val url = Url("https://foo.bar") / "/test"

    url.path shouldBe "https://foo.bar/test"

  }

  test("Can build https path while avoiding double slashes slash already in base") {

    val url = Url("https://foo.bar/") / "test"

    url.path shouldBe "https://foo.bar/test"

  }

  test("Can build https path while avoiding double slashes slash in both") {

    val url = Url("https://foo.bar/") / "/test"

    url.path shouldBe "https://foo.bar/test"

  }

}
