name := "kafka-topology-builder-scala"

version := "0.1"

scalaVersion := "2.12.8"

resolvers += "Sonatype Nexus releases" at "https://oss.sonatype.org/content/repositories/releases"


libraryDependencies += "org.sellmerfud" %% "optparse" % "2.2"

libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.8"
libraryDependencies += "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.9.8"

mainClass in assembly := Some("TopologyBuilder")
