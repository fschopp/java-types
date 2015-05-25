package net.florianschoppmann.java.type;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to define that all contained entities for which a {@code null} annotation is otherwise lacking should be
 * considered as {@link Nonnull}.
 *
 * <p>This annotation can be applied to all element types.
 */
@TypeQualifierDefault({
    ElementType.ANNOTATION_TYPE,
    ElementType.CONSTRUCTOR,
    ElementType.FIELD,
    ElementType.LOCAL_VARIABLE,
    ElementType.METHOD,
    ElementType.PACKAGE,
    ElementType.PARAMETER,
    ElementType.TYPE
})
@Nonnull
@Retention(RetentionPolicy.RUNTIME)
public @interface NonNullByDefault { }
