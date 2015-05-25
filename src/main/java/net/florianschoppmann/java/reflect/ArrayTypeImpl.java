package net.florianschoppmann.java.reflect;

import javax.annotation.Nullable;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;
import java.util.Objects;

final class ArrayTypeImpl extends AnnotatedConstructImpl implements ReflectionTypeMirror, ArrayType {
    private final ReflectionTypeMirror componentType;

    ArrayTypeImpl(ReflectionTypeMirror componentType) {
        Objects.requireNonNull(componentType);

        this.componentType = componentType;
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        return componentType.equals(((ArrayTypeImpl) otherObject).componentType);
    }

    @Override
    public int hashCode() {
        // Do not return just componentType.hashCode() because both this instance and componentType are of type
        // ReflectionTypeMirror.
        return Objects.hash(getClass(), componentType.hashCode());
    }

    @Override
    public String toString() {
        return ReflectionTypes.getInstance().toString(this);
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> visitor, @Nullable P parameter) {
        return visitor.visitArray(this, parameter);
    }

    @Override
    public ReflectionTypeMirror getComponentType() {
        return componentType;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.ARRAY;
    }
}
