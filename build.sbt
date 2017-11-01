name := "scaffoldtrends"

version := "1.0"

lazy val `scaffoldtrends` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice, openId )

libraryDependencies += "org.postgresql" % "postgresql" % "42.1.4"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0"

libraryDependencies += "com.typesafe.play" %% "play-iteratees" % "2.6.1"

libraryDependencies += "com.typesafe.play" %% "play-iteratees-reactive-streams" % "2.6.1"

// unmanagedResourceDirectories in Test +=  baseDirectory ( _ /"target/web/public/test" )

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"  