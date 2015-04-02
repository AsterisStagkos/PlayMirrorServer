name := "play-ws-test"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
"com.newrelic.agent.java" % "newrelic-agent" % "3.7.0"
)     

libraryDependencies += "postgresql" % "postgresql" % "9.1-901-1.jdbc4"

play.Project.playJavaSettings
