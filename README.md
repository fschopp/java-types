Skeletal implementation of interface
[`javax.lang.model.util.Types`](http://docs.oracle.com/javase/8/docs/api/javax/lang/model/util/Types.html),
plus concrete realization backed by core Java Reflection API, akin to
[JDK Enhancement Proposal (JEP) 119](http://openjdk.java.net/jeps/119)

## Status
[![Build Status](https://travis-ci.org/fschopp/java-types.svg?branch=master)](https://travis-ci.org/fschopp/java-types)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.florianschoppmann.java/java-types/badge.svg?style=flat)](http://search.maven.org/#search|gav|1|g:net.florianschoppmann.java%20AND%20a:java-types)

## Overview

- source compatible with Java 7 and 8
- algorithms, relations, and properties of Java Language Specification
  implemented exclusively in terms of
  [`javax.lang.model`](https://docs.oracle.com/javase/8/docs/api/javax/lang/model/package-summary.html)
  interfaces; therefore supports different concrete implementations of
  `javax.lang.model` interfaces
- besides the generic skeletal implementation of `javax.lang.model.util.Types`,
  also includes a concrete implementation backed by the Java Reflection API
- implemented in plain Java with no dependencies; easily embeddable in projects
  that have Java types as part of their domain model (for instance,
  domain-specific languages on top of the JVM that need some sort of support
  for Java generic types)
- contract tests for the methods and classes that clients of the library need
  to provide in order to support other implementations of `javax.lang.model`
  interfaces
- tests have virtually
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

## Documentation

- [API documentation](http://fschopp.github.io/java-types/apidocs/index.html)
- [Maven-generated project documentation](http://fschopp.github.io/java-types)

## Usage Examples

The following examples show some use cases of class `ReflectionTypes`, the Java
Reflection API-based implementation of `javax.lang.model.util.Types` provided
by this project. While the bulk of the functionality of `ReflectionTypes` is
provided by an abstract, skeletal-implementation class called `AbstractTypes`,
class `ReflectionTypes` provides methods `typeElement()` and `typeMirror()`
that facilitate converting Java classes and generic types to `TypeElement` and
`TypeMirror` instances.

### Test whether one type is a subtype of another

(see JLS §4.10 and its references)

```java
ReflectionTypes types = ReflectionTypes.getInstance();
// listSuperNumberType: List<? super Number>
DeclaredType listSuperNumberType = types.getDeclaredType(
    types.typeElement(List.class),
    types.getWildcardType(null, types.typeMirror(Number.class))
);
// iterableExtendsNumberType: Iterable<? extends Number>
DeclaredType iterableExtendsNumberType = types.getDeclaredType(
    types.typeElement(Iterable.class),
    types.getWildcardType(types.typeMirror(Number.class), null)
);
// iterableType: Iterable<?>
DeclaredType iterableType = types.getDeclaredType(
    types.typeElement(Iterable.class),
    types.getWildcardType(null, null)
);
assert types.isSubtype(listSuperNumberType, iterableType);
assert types.isSubtype(iterableExtendsNumberType, iterableType);
assert !types.isSubtype(listSuperNumberType, iterableExtendsNumberType);
```

### Resolve actual type arguments

(see JLS §4.5, §8.1, and their references)

```java
ReflectionTypes types = ReflectionTypes.getInstance();
// actual type arguments to Comparable, given the (raw) subtype ScheduledFuture
List<? extends TypeMirror> typeArguments = types.resolveActualTypeArguments(
    types.typeElement(Comparable.class),
    types.typeMirror(ScheduledFuture.class)
);
assert typeArguments.equals(
    Collections.singletonList(types.typeMirror(Delayed.class))
);
```

### Capture conversion with mutually recursive type-variable bounds

(taken from [Example 8.1.2-1](http://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#d5e11077)
of JLS 8, see also JLS §5.1.10 and its references)

```java
// The following are top-level definitions
interface ConvertibleTo<T> {
    T convert();
}
class ReprChange<T extends ConvertibleTo<S>,
                 S extends ConvertibleTo<T>> {
    T t;
    void set(S s) { t = s.convert();    }
    S get()       { return t.convert(); }
}
class Amount implements ConvertibleTo<Integer> {
    @Override public Integer convert() { return 42; }
}

// [...]

ReflectionTypes types = ReflectionTypes.getInstance();
// reprChangeType: ReprChange<Amount, ?>
DeclaredType reprChangeType = types.getDeclaredType(
    types.typeElement(ReprChange.class),
    types.typeMirror(Amount.class),
    types.getWildcardType(null, null)
);
TypeMirror convertedType = types.capture(reprChangeType);

TypeVariable capturedS
    = (TypeVariable) ((DeclaredType) convertedType).getTypeArguments().get(1);
// convertibleToAmountType: ConvertibleTo<Amount>
DeclaredType convertibleToAmountType = types.getDeclaredType(
    types.typeElement(ConvertibleTo.class),
    types.typeMirror(Amount.class)
);
assert capturedS.getUpperBound().equals(convertibleToAmountType);
```
