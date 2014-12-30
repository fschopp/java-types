package net.florianschoppmann.java.type;

import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
 * Represents an intersection type, equivalent to {@code javax.lang.model.type.IntersectionType} in JDK 8.
 *
 * <p>Implementations may implement both {@code javax.lang.model.type.IntersectionType} and this interface. At some time
 * in the future, when Java 7 compatibility is no longer required, this interface will be deprecated and eventually
 * removed in favor of {@code javax.lang.model.type.IntersectionType}.
 */
public interface IntersectionType extends TypeMirror, AnnotatedConstruct {
    /**
     * Return the bounds comprising this intersection type.
     *
     * @return the bounds of this intersection types.
     */
    List<? extends TypeMirror> getBounds();

    /**
     * Returns whether this type mirror represents an intersection type.
     *
     * <p>Since implementations may choose to implement multiple {@link TypeMirror} sub-interfaces at the same time,
     * this method exists so that an object can explicitly indicate whether it represents an intersection type.
     *
     * @return whether this type mirror represents an intersection type
     */
    boolean isIntersectionType();
}
