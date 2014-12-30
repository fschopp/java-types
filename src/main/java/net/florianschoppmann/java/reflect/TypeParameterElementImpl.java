package net.florianschoppmann.java.reflect;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeParameterElement;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Objects;

final class TypeParameterElementImpl extends ElementImpl implements TypeParameterElement {
    private final TypeVariable<?> reflectionTypeVariable;
    private final ElementImpl genericElement;
    private final TypeVariableImpl typeVariable;
    private List<ReflectionTypeMirror> bounds;

    TypeParameterElementImpl(TypeVariable<?> reflectionTypeVariable, ElementImpl genericElement) {
        this.reflectionTypeVariable = Objects.requireNonNull(reflectionTypeVariable);
        this.genericElement = Objects.requireNonNull(genericElement);
        typeVariable = new TypeVariableImpl(this, null);
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        return reflectionTypeVariable.equals(((TypeParameterElementImpl) otherObject).reflectionTypeVariable);
    }

    @Override
    public int hashCode() {
        return reflectionTypeVariable.hashCode();
    }

    @Override
    public String toString() {
        return reflectionTypeVariable.toString();
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> visitor, P parameter) {
        return visitor.visitTypeParameter(this, parameter);
    }

    @Override
    public ElementImpl getGenericElement() {
        return genericElement;
    }

    @Override
    public List<ReflectionTypeMirror> getBounds() {
        requireFinished();

        return bounds;
    }

    @Override
    public ElementImpl getEnclosingElement() {
        return genericElement;
    }

    @Override
    public TypeVariableImpl asType() {
        return typeVariable;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.TYPE_PARAMETER;
    }

    @Override
    public Name getSimpleName() {
        return new NameImpl(reflectionTypeVariable.getName());
    }

    @Override
    public ImmutableList<? extends ReflectionElement> getEnclosedElements() {
        return ImmutableList.emptyList();
    }

    @Override
    protected void finishDerivedFromElement(MirrorContext mirrorContext) {
        bounds = mirrorContext.mirror(reflectionTypeVariable.getBounds());
        ReflectionTypeMirror bound = bounds.size() == 1
            ? bounds.get(0)
            : new IntersectionTypeImpl(bounds);
        typeVariable.setUpperAndLowerBounds(bound, NullTypeImpl.INSTANCE);
    }
}
