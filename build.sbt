name := "TweetAggregator"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)     

libraryDependencies += "oauth.signpost" % "signpost-core" % "1.2.1.2"

libraryDependencies += "oauth.signpost" % "signpost-commonshttp4" % "1.2.1.1"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.0-alpha4"

play.Project.playScalaSettings
