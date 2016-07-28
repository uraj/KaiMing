# KaiMing
KaiMing is a binary analysis framework. The goal is to provide an open source
implementation of various static analysis algorithms for COTS binaries. Till
now, KaiMing supports the following architectures:
+ ARM
+ AArch64

The dependencies of this project is managed by sbt. In detail, the project
requires the following components:
+ Scala 2.11, with parser combinator
+ ScalaTest
+ scodec
+ Graph for Scala
+ enumeratum

The project can be directly imported into Eclipse after you generate the
required Eclipse configuration files by typing 

`$ sbt eclipse`

You may need a few Eclipse plug-ins to work with the imported code.
Library dependencies should be resolved by sbt automatically.

To compile, type

`$ sbt compile`

To run the shipped test cases, type

`$ sbt test`
