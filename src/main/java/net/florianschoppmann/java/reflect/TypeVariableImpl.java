package net.florianschoppmann.java.reflect;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import java.util.Objects;

final class TypeVariableImpl extends AnnotatedConstructImpl implements ReflectionTypeMirror, TypeVariable {
    private boolean frozen = false;

    private final TypeParameterElementImpl typeParameterElement;
    private final WildcardTypeImpl capturedTypeArgument;
    private ReflectionTypeMirror upperBound;
    private ReflectionTypeMirror lowerBound;

    TypeVariableImpl(TypeParameterElementImpl typeParameterElement, WildcardTypeImpl capturedTypeArgument) {
        Objects.requireNonNull(typeParameterElement);

        this.typeParameterElement = typeParameterElement;
        this.capturedTypeArgument = capturedTypeArgument;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        requireFrozen();

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
    public <R, P> R accept(TypeVisitor<R, P> visitor, P parameter) {
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

        return upperBound;
    }

    @Override
    public ReflectionTypeMirror getLowerBound() {
        requireFrozen();

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

    WildcardTypeImpl getCapturedTypeArgument() {
        return capturedTypeArgument;
    }
}
