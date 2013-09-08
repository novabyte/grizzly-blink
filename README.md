Grizzly-Blink
=============

[Grizzly NIO framework](http://grizzly.java.net/) filters to process the
 [Blink](http://blinkprotocol.org/) protocol serialized messages.

Grizzly-Blink is created and maintained by Chris Molozian (@novabyte).
<br/>
Code licensed under the [Apache License v2.0](http://www.apache.org/licenses/LICENSE-2.0).
 Documentation licensed under [CC BY 3.0](http://creativecommons.org/licenses/by/3.0/).

## Usage ##

Grizzly-Blink is not __yet__ available on [Maven Central](http://search.maven.org/).

### Filter Types ###

At the moment there is only one filter to choose from: __BlinkCompactFilter__.
 This filter implements the [Blink Compact](http://blog.blinkprotocol.org/2013/02/blink-native-binary-format-introduction.html)
 protocol.

There are future plans to add support for the __Blink Native__ version of the
 Blink protocol specification once support for it is added to
 [jblink](https://github.com/pantor-engineering/jblink).

## Example ##

The filter needs a `Schema` type to use to encode and decode messages.

The example below demonstrates the
 [`Hello` protocol](https://github.com/novabyte/grizzly-blink/tree/master/src/test/blink/Hello.blink),
 all messages sent/received by the filter chain will (de)serialize to the `Hello`
 protocol format:

```java
final Schema schema = new Schema();
SchemaReader.readFromString(
        "namespace HelloSpec\n" +
        "Hello/1 ->\n" +
        "  string Greeting", schema);
schema.finalizeSchema();

final FilterChainBuilder serverFilterBuilder = FilterChainBuilder.stateless()
        .add(new TransportFilter())
        .add(new BlinkCompactFilter(schema, "some.package.for.blink.code"))
```

For more detailed examples of how to integrate this filter into your code have a
 look at the [test cases](https://github.com/novabyte/grizzly-blink/tree/master/src/test/java/me/cmoz/grizzly/blink).

## Developer Notes ##

The codebase requires the [Gradle](http://gradle.org) build tool at version
 `1.6+` and the Java compiler at version `1.6.0` or greater.

The main external dependency for the project is [Grizzly NIO](http://grizzly.java.net/),
 at `2.3.5` or greater. At the moment [jblink](https://github.com/pantor-engineering/jblink)
 is being bundled within this library until it's packaged properly and available
 on [Maven Central](http://search.maven.org/).

For a full list of dependencies see the [build script](https://github.com/novabyte/grizzly-blink/blob/master/build.gradle).
 All dependencies are downloaded by Gradle during the build process.

### Building the codebase ###

The codebase requires [NodeJS](http://nodejs.org/) to use the __blinkc__
 compiler, it generates source files that map the protocol specification to Java
 (similarly to how Protobuf-Java works). You'll also need to clone the repository
 with all git submodules to include the `blinkc.js` dependency:

```
git clone --recursive git://github.com/novabyte/grizzly-blink.git
```

A list of all possible build targets can be displayed by Gradle with
 `gradle tasks`.

In a regular write-compile-test cycle use `gradle test`.

A list of all project dependencies can be displayed by Gradle with
 `gradle dependencies`.

It is recommended to run Gradle with the
 [Build Daemon](http://docs.codehaus.org/display/GRADLE/Gradle+Build+Daemon)
 enabled to improve performance. e.g. `gradle --daemon` once the daemon is
 running it can be stopped with `gradle --stop`.

## Contribute ##

All contributions to the documentation and the codebase are very welcome. Feel
 free to open issues on the tracker wherever the documentation needs improving.

Also, pull requests are always welcome! `:)`
