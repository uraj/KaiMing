# KaiMing
KaiMing is a binary analysis framework. The goal is to provide an open source
implementation of various static analysis algorithms for COTS x86 binaries. 

The dependencies of this project is managed by Maven. In detail, the project
requires the following components:

+ Java 8
+ Scala 2.11, with parser combinator
+ ScalaTest 2.26
+ JUnit 4.11

The project can be directly imported into Eclipse, but you may need a few
Eclipse plug-ins to work with the imported code.

Since the project is managed by Maven, you can type the following commands
in the project directory to execute test cases. The dependencies should be
resolved by Maven automatically.

`$ mvn test`
