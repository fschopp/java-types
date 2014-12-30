Skeletal implementation of interface
[`javax.lang.model.util.Types`](http://docs.oracle.com/javase/8/docs/api/javax/lang/model/util/Types.html),
plus concrete realization backed by core Java Reflection API, akin to
[JDK Enhancement Proposal (JEP) 119](http://openjdk.java.net/jeps/119)

## Overview

- source compatible with Java 7 and 8
- algorithms, relations, and properties of Java Language Specification
  implemented exclusively in terms of
  [`javax.lang.model`](https://docs.oracle.com/javase/8/docs/api/javax/lang/model/package-summary.html)
  interfaces; therefore supports different concrete implementations of
  `javax.lang.model` interfaces
- implemented in plain Java with no dependencies; easily embeddable in projects
  that have Java types as part of their domain model (for instance,
  domain-specific languages on top of the JVM that need some sort of support
  for Java generic types)
- includes implementation of `javax.lang.model.util.Types` backed by Java
  Reflection API
- includes extensive unit tests with almost
  [full code coverage](http://fschopp.github.io/java-types/jacoco/index.html)
- contains a complete implementation of `javax.lang.model.util.Types` methods
  pertaining to JLS
  [§4](http://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html) (including
  primitive types, reference types, type variables, parameterized types, type
  erasure, raw types, intersection types, and subtyping) and
  [§5.1.10](http://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.10)
  (capture conversion)
- also includes method for resolving formal type parameters to actual type
  arguments (for example, determining the actual type argument to
  [`Comparable<T>`](http://docs.oracle.com/javase/8/docs/api/java/lang/Comparable.html),
  given type
  [`Integer`](http://docs.oracle.com/javase/8/docs/api/java/lang/Integer.html))
- methods pertaining to non-type elements (notably,
  [`ExecutableElement`](http://docs.oracle.com/javase/8/docs/api/javax/lang/model/element/ExecutableElement.html)
  and
  [`VariableElement`](http://docs.oracle.com/javase/8/docs/api/javax/lang/model/element/VariableElement.html))
  not currently implemented

## Status
[![Build Status](https://travis-ci.org/fschopp/java-types.svg?branch=master)](https://travis-ci.org/fschopp/java-types)

## License

[Revised BSD (3-Clause) License](LICENSE)

## Binary Releases

Published releases (compiled for Java 7 and up) are available on Maven Central.

```
<dependency>
    <groupId>net.florianschoppmann.java</groupId>
    <artifactId>java-types</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

```java
ReflectionTypes types = ReflectionTypes.getInstance();
List<? extends TypeMirror> typeArguments = types.resolveActualTypeArguments(
    types.typeElement(Comparable.class),
    types.typeMirror(Integer.class)
);
assert typeArguments.equals(
    Collections.singletonList(types.typeMirror(Integer.class))
);
```

Note that `typeElement()` and `typeMirror()` are provided by `ReflectionTypes`
(which is a Java core reflection-based implementation of
`javax.lang.model.util.Types`), whereas `resolveActualTypeArguments()` is a
`final` method in the `AbstractTypes` class provided by this project and is
implemented in an entirely generic fashion.
