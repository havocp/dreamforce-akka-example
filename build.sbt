import com.typesafe.startscript.StartScriptPlugin

seq(StartScriptPlugin.startScriptForClassesSettings: _*)

name := "dreamforce-akka-example"

version := "1.0"

scalaVersion := "2.9.2"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq("io.netty" % "netty" % "3.5.3.Final",
                            "com.typesafe.akka" % "akka-actor" % "2.0.2")
