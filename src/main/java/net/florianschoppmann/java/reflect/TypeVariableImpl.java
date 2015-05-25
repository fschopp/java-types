package net.florianschoppmann.java.reflect;

import javax.annotation.Nullable;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import java.util.Objects;

final class TypeVariableImpl extends AnnotatedConstructImpl implements ReflectionTypeMirror, TypeVariable {
    private boolean frozen = false;

    private final TypeParameterElementImpl typeParameterElement;
    @Nullable private final WildcardTypeImpl capturedTypeArgument;
    @Nullable private ReflectionTypeMirror upperBound;
    @Nullable private ReflectionTypeMirror lowerBound;

    TypeVariableImpl(TypeParameterElementImpl typeParameterElement, @Nullable WildcardTypeImpl capturedTypeArgument) {
        Objects.requireNonNull(typeParameterElement);

        this.typeParameterElement = typeParameterElement;
        this.capturedTypeArgument = capturedTypeArgument;
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        requireFrozen();
        assert upperBound != null && lowerBound != null : "must be non-null when frozen";

        TypeVariableImpl other = (TypeVariableImpl) otherObject;
        return typeParameterElement.equals(other.typeParameterElement)
            && Objects.equals(capturedTypeArgument, other.capturedTypeArgument)
            && upperBound.equals(other.upperBound)
            && lowerBound.equals(other.lowerBound);
    }

    @Override
    public int hashCode() {
        requireFrozen();

        return Objects.hash(typeParameterElement, capturedTypeArgument, upperBound, lowerBound);
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> visitor, @Nullable P parameter) {
        return visitor.visitTypeVariable(this, parameter);
    }

    @Override
    public String toString() {
        return ReflectionTypes.getInstance().toString(this);
    }

    void requireUnfrozen() {
        if (frozen) {
            throw new IllegalStateException(String.format(
                "Tried to modify instance of %s after it became effectively immutable.", getClass()
            ));
        }
    }

    void requireFrozen() {
        if (!frozen) {
            throw new IllegalStateException(String.format(
                "Instance of %s used before object construction finished.", getClass()
            ));
        }
    }

    @Override
    public TypeParameterElementImpl asElement() {
        return typeParameterElement;
    }

    @Override
    public ReflectionTypeMirror getUpperBound() {
        requireFrozen();
        assert upperBound != null : "must be non-null when frozen";
        return upperBound;
    }

    @Override
    public ReflectionTypeMirror getLowerBound() {
        requireFrozen();
        assert lowerBound != null : "must be non-null when frozen";
        return lowerBound;
    }

    void setUpperAndLowerBounds(ReflectionTypeMirror newUpperBound, ReflectionTypeMirror newLowerBound) {
        requireUnfrozen();
        Objects.requireNonNull(newUpperBound);
        Objects.requireNonNull(newLowerBound);

        upperBound = newUpperBound;
        lowerBound = newLowerBound;
        frozen = true;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.TYPEVAR;
    }

    @Nullable
    WildcardTypeImpl getCapturedTypeArgument() {
        return capturedTypeArgument;
    }
}
