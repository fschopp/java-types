/**
 * Provides an abstract skeletal implementation of class {@link javax.lang.model.util.Types}, together with classes and
 * interfaces that ensure source compatibility with both Java 7 and 8.
 *
 * <p>This package only provides the algorithms, relations, and properties documented in the Java Language Specification
 * (JLS), but no concrete {@link javax.lang.model.element.Element} and {@link javax.lang.model.type.TypeMirror}
 * implementations. Instead, this package supports all implementations that follow the contracts set forth in
 * {@link javax.lang.model.util.Types} and {@link net.florianschoppmann.java.type.AbstractTypes}.
 */
@NonNullByDefault
package net.florianschoppmann.java.type;
