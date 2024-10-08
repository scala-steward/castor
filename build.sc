import mill._, scalalib._, scalajslib._, scalanativelib._, publish._
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.4.0`
import de.tobiasroeser.mill.vcs.version.VcsVersion
import $ivy.`com.github.lolgab::mill-mima::0.1.1`
import com.github.lolgab.mill.mima._
import mill.scalalib.api.ZincWorkerUtil.isScala3

val communityBuildDottyVersion = sys.props.get("dottyVersion").toList

val scala212 = "2.12.19"
val scala213 = "2.13.13"
val scala3 = "3.3.3"

val scalaVersions = scala3 :: scala213 :: scala212 :: communityBuildDottyVersion

trait MimaCheck extends Mima {
  def mimaPreviousVersions = VcsVersion.vcsState().lastTag.toSeq
}

object castor extends Module {
  trait PlatformModule {
    def platformSegment: String = this match {
      case _: ScalaJSModule => "js"
      case _: ScalaNativeModule => "native"
      case _ => "jvm"
    }
  }
  trait ActorModule extends CrossScalaModule with PublishModule with MimaCheck with PlatformModule {
    def publishVersion = VcsVersion.vcsState().format()

    def pomSettings = PomSettings(
      description = artifactName(),
      organization = "com.lihaoyi",
      url = "https://github.com/com-lihaoyi/castor",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("com-lihaoyi", "castor"),
      developers = Seq(
        Developer("lihaoyi", "Li Haoyi","https://github.com/lihaoyi")
      )
    )

    def artifactName = "castor"
    def millSourcePath = super.millSourcePath / os.up

    def sources = T.sources(
      millSourcePath / "src",
      millSourcePath / s"src-$platformSegment"
    )

    def ivyDeps = Agg(ivy"com.lihaoyi::sourcecode::0.4.1")
  }
  trait ActorTestModule extends ScalaModule with TestModule.Utest with PlatformModule {
    def sources = T.sources(
      millSourcePath / "src",
      millSourcePath / s"src-$platformSegment"
    )
    def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.8.3")
  }

  object js extends Cross[ActorJsModule](scalaVersions)
  trait ActorJsModule extends ActorModule with ScalaJSModule {
    def scalaJSVersion = "1.16.0"
    override def sources = T.sources {
      super.sources() ++ Seq(PathRef(millSourcePath / "src-js-native"))
    }
    object test extends ScalaJSTests with ActorTestModule {
      def scalaVersion = crossScalaVersion
    }
  }
  object jvm extends Cross[ActorJvmModule](scalaVersions)
  trait ActorJvmModule extends ActorModule {
    object test extends ScalaTests with ActorTestModule{
      def ivyDeps = super.ivyDeps() ++ Agg(
        ivy"com.lihaoyi::os-lib:0.9.1"
      )
    }
  }
  object native extends Cross[ActorNativeModule](scalaVersions)
  trait ActorNativeModule extends ActorModule with ScalaNativeModule {
    def scalaNativeVersion = "0.5.4"
    // Enable after first release for Scala Native 0.5
    def mimaPreviousArtifacts = T { Agg.empty }
    override def sources = T.sources {
      super.sources() ++ Seq(PathRef(millSourcePath / "src-js-native"))
    }
    object test extends ScalaNativeTests with ActorTestModule
  }
}
