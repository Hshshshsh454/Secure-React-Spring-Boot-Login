---
name: Maven Java runtime selection
description: Maven may launch on a different JDK than the compiler selected by PATH in this workspace.
---

When running Maven here, verify that Maven and `javac` use the same JDK. The Maven launcher initially selected Java 19 while Java 21 was installed and selected by `java`/`javac`; explicitly setting `JAVA_HOME` to the active compiler's JDK resolved this.

**Why:** The Spring Boot service targets Java 21, and a mismatched Maven runtime rejected the configured release before source compilation.

**How to apply:** Before Maven builds, tests, or artifact service commands, derive/set `JAVA_HOME` from the selected `javac` rather than relying on `mvn`'s default runtime.
