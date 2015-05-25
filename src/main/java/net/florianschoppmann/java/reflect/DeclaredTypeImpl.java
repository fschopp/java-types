package net.florianschoppmann.java.reflect;

import javax.annotation.Nullable;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;
import java.util.List;
import java.util.Objects;

final class DeclaredTypeImpl extends AnnotatedConstructImpl implements ReflectionTypeMirror, DeclaredType {
    private final ReflectionTypeMirror enclosingType;
    private final TypeElementImpl typeElement;
    private final ImmutableList<? extends ReflectionTypeMirror> typeArguments;

    DeclaredTypeImpl(ReflectionTypeMirror enclosingType, TypeElementImpl typeElement,
            List<? extends ReflectionTypeMirror> typeArguments) {
        Objects.requireNonNull(enclosingType);
        Objects.requireNonNull(typeElement);
        Objects.requireNonNull(typeArguments);

        this.enclosingType = enclosingType;
        this.typeElement = typeElement;
        this.typeArguments = ImmutableList.copyOf(typeArguments);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        DeclaredTypeImpl other = (DeclaredTypeImpl) otherObject;
        return enclosingType.equals(other.enclosingType)
            && typeElement.equals(other.typeElement)
            && typeArguments.equals(other.typeArguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enclosingType, typeElement, typeArguments);
    }

    @Override
    public String toString() {
        return ReflectionTypes.getInstance().toString(this);
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> visitor, @Nullable P parameter) {
        return visitor.visitDeclared(this, parameter);
    }

    @Override
    public TypeElementImpl asElement() {
        return typeElement;
    }

    @Override
    public ReflectionTypeMirror getEnclosingType() {
        return enclosingType;
    }

    @Override
    public List<? extends ReflectionTypeMirror> getTypeArguments() {
        return typeArguments;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.DECLARED;
    }
}
