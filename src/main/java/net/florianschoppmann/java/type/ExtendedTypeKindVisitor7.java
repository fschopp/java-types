package net.florianschoppmann.java.type;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.UnknownTypeException;
import javax.lang.model.util.TypeKindVisitor7;

import static javax.lang.model.SourceVersion.RELEASE_7;

/**
 * A visitor of types that supports {@link IntersectionType}, but otherwise behaves as {@link TypeKindVisitor7}.
 *
 * <p>This visitor provides a new method {@link #visitIntersection(IntersectionType, Object)} in order to visit
 * instances of {@link IntersectionType}. At some time in the future, when Java 7 compatibility is no longer
 * required, {@link IntersectionType} and this class will be removed and replaced by their Java 8 equivalents.
 *
 * @param <R> The return type of this visitor's methods. Use {@link Void} for visitors that do not need to return
 *     results.
 * @param <P> The type of the additional parameter to this visitor's methods. Use {@code Void} for visitors that do
 *     not need an additional parameter.
 */
@SupportedSourceVersion(RELEASE_7)
public class ExtendedTypeKindVisitor7<R, P> extends TypeKindVisitor7<R, P> {
    /**
     * Constructor for concrete subclasses to call; uses {@code null} for the default value.
     */
    protected ExtendedTypeKindVisitor7() {
        super(null);
    }

    /**
     * Constructor for concrete subclasses to call; uses the argument for the default value.
     *
     * @param defaultValue the value to assign to {@link #DEFAULT_VALUE}
     */
    protected ExtendedTypeKindVisitor7(R defaultValue) {
        super(defaultValue);
    }

    /**
     * Visits an unknown kind of type or an {@link IntersectionType}, which did not exist in the {@link TypeMirror}
     * hierarchy of Java 7.
     *
     * <p>This method is final. Use {@link #visitOther(TypeMirror, Object)} to override the default behavior when
     * visiting an unknown type.
     *
     * @param typeMirror the type to visit
     * @param parameter a visitor-specified parameter
     * @return a visitor-specified result
     * @throws javax.lang.model.type.UnknownTypeException a visitor implementation may optionally throw this exception
     */
    @Override
    public final R visitUnknown(TypeMirror typeMirror, P parameter) {
        if (typeMirror instanceof IntersectionType && ((IntersectionType) typeMirror).isIntersectionType()) {
            return visitIntersection((IntersectionType) typeMirror, parameter);
        } else {
            return visitOther(typeMirror, parameter);
        }
    }

    /**
     * Visits an unknown kind of type. This can occur if the language evolves and new kinds of types are added to the
     * {@link TypeMirror} hierarchy.
     *
     * <p>The default implementation of this method in {@code TypeKindVisitor7WithIntersectionType} will always throw
     * {@link javax.lang.model.type.UnknownTypeException}. This behavior is not required of a subclass.
     *
     * @param typeMirror the type to visit
     * @param parameter a visitor-specified parameter
     * @return a visitor-specified result
     * @throws javax.lang.model.type.UnknownTypeException a visitor implementation may optionally throw this exception
     */
    public R visitOther(TypeMirror typeMirror, P parameter) {
        throw new UnknownTypeException(typeMirror, parameter);
    }

    /**
     * Visits an intersection type by calling {@link #defaultAction(TypeMirror, Object)}.
     *
     * @param intersectionType the intersection type to visit
     * @param parameter a visitor-specified parameter
     * @return the result of {@code defaultAction}
     */
    public R visitIntersection(IntersectionType intersectionType, P parameter) {
        return defaultAction(intersectionType, parameter);
    }
}
