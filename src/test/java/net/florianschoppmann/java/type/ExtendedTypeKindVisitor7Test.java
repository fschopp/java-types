package net.florianschoppmann.java.type;

import net.florianschoppmann.java.reflect.ReflectionTypes;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnknownTypeException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link ExtendedTypeKindVisitor7}.
 */
public class ExtendedTypeKindVisitor7Test {
    private final ReflectionTypes types = ReflectionTypes.getInstance();

    private abstract static class AnnotatedConstructImpl implements AnnotatedConstruct {
        @Override
        public final List<? extends AnnotationMirror> getAnnotationMirrors() {
            throw new UnsupportedOperationException();
        }

        @Override
        public final <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class IntersectionTypeImpl extends AnnotatedConstructImpl implements IntersectionType {
        private final List<TypeMirror> bounds;
        private final boolean isIntersectionType;

        private IntersectionTypeImpl(List<TypeMirror> bounds, boolean isIntersectionType) {
            this.bounds = bounds;
            this.isIntersectionType = isIntersectionType;
        }

        @Override
        public List<? extends TypeMirror> getBounds() {
            return bounds;
        }

        @Override
        public boolean isIntersectionType() {
            return isIntersectionType;
        }

        @Override
        public TypeKind getKind() {
            return TypeKind.OTHER;
        }

        @Override
        public <R, P> R accept(TypeVisitor<R, P> visitor, P parameter) {
            return visitor.visitUnknown(this, parameter);
        }
    }

    private static final class OtherType extends AnnotatedConstructImpl implements TypeMirror {
        @Override
        public TypeKind getKind() {
            return TypeKind.OTHER;
        }

        @Override
        public <R, P> R accept(TypeVisitor<R, P> visitor, P parameter) {
            return visitor.visitUnknown(this, parameter);
        }
    }

    private static final class SomeVisitor extends ExtendedTypeKindVisitor7<String, String> {
        @Override
        public String visitOther(TypeMirror typeMirror, String parameter) {
            return "unknown";
        }
    }

    @Test
    public void test() {
        final List<TypeMirror> bounds
            = Arrays.asList(types.typeMirror(Serializable.class), types.typeMirror(Cloneable.class));

        ExtendedTypeKindVisitor7<String, String> visitor = new ExtendedTypeKindVisitor7<>("42");
        SomeVisitor someVisitor = new SomeVisitor();

        try {
            visitor.visitUnknown(new IntersectionTypeImpl(bounds, false), "24");
            Assert.fail("Expected exception.");
        } catch (UnknownTypeException ignored) { }

        try {
            visitor.visitUnknown(new OtherType(), "24");
            Assert.fail("Expected exception.");
        } catch (UnknownTypeException ignored) { }

        Assert.assertEquals(someVisitor.visitUnknown(new OtherType(), "24"), "unknown");

        Assert.assertEquals(visitor.visitUnknown(new IntersectionTypeImpl(bounds, true), "24"), "42");
    }
}
