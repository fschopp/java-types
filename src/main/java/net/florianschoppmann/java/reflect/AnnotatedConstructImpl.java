package net.florianschoppmann.java.reflect;

import net.florianschoppmann.java.type.AnnotatedConstruct;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;
import java.util.List;

abstract class AnnotatedConstructImpl implements AnnotatedConstruct {
    static UnsupportedOperationException unsupportedException() {
        return new UnsupportedOperationException(String.format(
            "Annotations not currently supported by %s.", ReflectionTypes.class
        ));
    }

    @Override
    public final List<? extends AnnotationMirror> getAnnotationMirrors() {
        throw unsupportedException();
    }

    @Override
    public final <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        throw unsupportedException();
    }

    @Override
    public final <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
        throw unsupportedException();
    }
}
