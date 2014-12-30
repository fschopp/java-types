package net.florianschoppmann.java.type;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Represents a construct that can be annotated, equivalent to {@code javax.lang.model.AnnotatedConstruct} in JDK 8.
 *
 * <p>This interface's sole purpose is to ensure source compatibility with Java 7 and 8, despite the fact that
 * {@code javax.lang.model.AnnotatedConstruct} was retrofitted into the Java language model hierarchy in JDK 8. See the
 * <a href="http://docs.oracle.com/javase/8/docs/api/javax/lang/model/AnnotatedConstruct.html">JDK 8 JavaDoc for
 * {@code javax.lang.model.AnnotatedConstruct}</a> for more information.
 *
 * <p>Implementations may implement both {@code javax.lang.model.AnnotatedConstruct} and this interface. At some time
 * in the future, when Java 7 compatibility is no longer required, this interface will be deprecated and eventually
 * removed in favor of {@code javax.lang.model.AnnotatedConstruct}.
 */
public interface AnnotatedConstruct {
    /**
     * Returns the annotations that are <em>directly present</em> on this construct.
     *
     * @return the annotations <em>directly present</em> on this construct; an empty list if there are none
     */
    List<? extends AnnotationMirror> getAnnotationMirrors();

    /**
     * Returns this construct's annotation of the specified type if such an annotation is <em>present</em>, else
     * {@code null}.
     *
     * @param <A> the annotation type
     * @param annotationType the {@code Class} object corresponding to the annotation type
     * @return this construct's annotation for the specified annotation type if present, else {@code null}
     */
    <A extends Annotation> A getAnnotation(Class<A> annotationType);

    /**
     * Returns annotations that are <em>associated</em> with this construct.
     *
     * @param <A> the annotation type
     * @param annotationType the {@code Class} object corresponding to the annotation type
     * @return this construct's annotations for the specified annotation type if present on this construct, else an
     *     empty array
     */
    <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType);
}
