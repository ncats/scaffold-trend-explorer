name := "scaffoldtrends"

version := "1.0"

lazy val `scaffoldtrends` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(jdbc, ehcache, ws, specs2 % Test, guice, openId)

libraryDependencies += "org.postgresql" % "postgresql" % "42.1.4"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0"

libraryDependencies += "com.typesafe.play" %% "play-iteratees" % "2.6.1"

libraryDependencies += "com.typesafe.play" %% "play-iteratees-reactive-streams" % "2.6.1"

libraryDependencies ++= Seq("org.webjars" % "bootstrap" % "3.3.6"
  , "org.webjars" % "highcharts" % "6.0.2"
  , "org.webjars" % "datatables" % "1.10.12"
  , "org.webjars" % "datatables-plugins" % "1.10.12"
  , "org.webjars" % "jquery" % "3.2.1"
  , "org.webjars" % "jquery-ui" % "1.12.1"
  , "org.webjars" % "jquery-ui-themes" % "1.12.1"
  , "org.webjars" % "font-awesome" % "4.5.0"
  , "org.webjars" % "store.js" % "1.3.17-1")


// unmanagedResourceDirectories in Test +=  baseDirectory ( _ /"target/web/public/test" )

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "webjars" at "http://webjars.github.com/m2"

// some vars to generate BuildInfo
val branch = "git rev-parse --abbrev-ref HEAD".!!.trim
val commit = "git rev-parse --short HEAD".!!.trim
val author = s"git show --format=%an -s $commit".!!.trim
val buildDate = (new java.text.SimpleDateFormat("yyyyMMdd"))
  .format(new java.util.Date())
val appVersion = "%s-%s-%s".format(branch, buildDate, commit)


sourceGenerators in Compile += Def.task {
  println((sourceManaged in Compile).value)
  val file = (sourceManaged in Compile).value / "controllers" / "BuildInfo.scala"
  println(file)
  IO.write(file,
    """
      package controllers

      object BuildInfo {
        val BRANCH = "%s"
        val DATE = "%s"
        val COMMIT = "%s"
        val TIME = "%s"
        val AUTHOR = "%s"
      }
    """.format(branch, buildDate, commit, new java.util.Date(), author))
  Seq(file)
}.taskValue