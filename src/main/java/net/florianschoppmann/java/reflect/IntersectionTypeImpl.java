package net.florianschoppmann.java.reflect;

import net.florianschoppmann.java.type.IntersectionType;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import java.util.List;
import java.util.Objects;

final class IntersectionTypeImpl extends AnnotatedConstructImpl implements IntersectionType, ReflectionTypeMirror {
    private final ImmutableList<ReflectionTypeMirror> bounds;

    IntersectionTypeImpl(List<ReflectionTypeMirror> bounds) {
        Objects.requireNonNull(bounds);

        this.bounds = ImmutableList.copyOf(bounds);
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        return bounds.equals(((IntersectionTypeImpl) otherObject).bounds);
    }

    @Override
    public int hashCode() {
        // Do not return just bounds.hashCode() because both this instance and componentType are of type
        // ReflectionTypeMirror.
        return Objects.hash(getClass(), bounds.hashCode());
    }

    @Override
    public String toString() {
        return ReflectionTypes.getInstance().toString(this);
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> visitor, P parameter) {
        return visitor.visitUnknown(this, parameter);
    }

    @Override
    public List<? extends TypeMirror> getBounds() {
        return bounds;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.OTHER;
    }

    @Override
    public boolean isIntersectionType() {
        return true;
    }
}
