package net.florianschoppmann.java.reflect;

import net.florianschoppmann.java.type.AnnotatedConstruct;

import javax.lang.model.type.TypeMirror;

/**
 * Common super-interface of all {@link TypeMirror} implementations in this package.
 *
 * <p>This is an interface, and not an abstract class, because some implementations are enums and not classes.
 */
interface ReflectionTypeMirror extends TypeMirror, AnnotatedConstruct { }
