package be.be.wegenenverkeer.ws

import java.io.File

import com.typesafe.config.ConfigFactory
import play.api._
import play.api.libs.ws.ning.NingWSPlugin
import play.core.SourceMapper
import resource.Resource

import scala.language.implicitConversions

class ManagedPlayApp {

  private lazy val _config = Configuration(ConfigFactory.load())

  private var playAppOpt: Option[Application] = None

  private def newApp() = new Application {

    override val plugins: Seq[Plugin] = Seq(new NingWSPlugin(this))

    override def configuration: Configuration = _config

    override def path: File = new File(".")

    override def mode: Mode.Mode = Mode.Prod

    override def classloader: ClassLoader = getClass.getClassLoader

    override def global: GlobalSettings = new GlobalSettings {}

    override def sources: Option[SourceMapper] = None

  }

  implicit def playApp = playAppOpt
                         .getOrElse(sys.error("Play Application not started!"))

  def config = playApp.configuration

  def onStart() : Unit = playAppOpt match {
    case None => // create and start app
      val app = newApp()
      app.plugins.foreach(_.onStart())
      playAppOpt = Some(app)
    case _ => ()
  }

  def onStop() : Unit = playAppOpt match {
    case Some(app) => app.plugins.foreach(_.onStop())
    case None => ()
  }
}

object ManagedPlayApp {
  implicit def managedPlayAppResource(managedPlayApp:ManagedPlayApp) = new Resource[ManagedPlayApp] {
    override def open(app: ManagedPlayApp): Unit = app.onStart()
    override def close(app: ManagedPlayApp): Unit = app.onStop()
  }
}
