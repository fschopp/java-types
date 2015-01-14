package net.florianschoppmann.java.reflect;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class TypeElementImpl extends ElementImpl implements TypeElement, ReflectionParameterizable {
    private final Class<?> clazz;
    private final ImmutableList<TypeParameterElementImpl> typeParameters;
    private ReflectionElement enclosingElement;
    private ReflectionTypeMirror superClass;
    private List<ReflectionTypeMirror> interfaces;
    private List<ElementImpl> enclosedElements;

    /**
     * Cache the type returned by {@link #asType()}.
     *
     * <p>Similarly to {@link String#hashCode()}, caching is not synchronized. This means that different threads may
     * (at least theoretically) see different values for this field. However, this is not a problem because all such
     * values would compare equal. Moreover, §17.7 JLS specifies that "Writes to and reads of references are always
     * atomic, regardless of whether they are implemented as 32-bit or 64-bit values". Hence, even if caches were
     * updated, every access to this field would yield a well-defined result.
     */
    private DeclaredTypeImpl type;

    TypeElementImpl(Class<?> clazz) {
        this.clazz = Objects.requireNonNull(clazz);

        List<TypeParameterElementImpl> newTypeParameters = new ArrayList<>();
        for (TypeVariable<?> parameter: clazz.getTypeParameters()) {
            newTypeParameters.add(new TypeParameterElementImpl(parameter, this));
        }
        typeParameters = ImmutableList.copyOf(newTypeParameters);
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        return clazz.equals(((TypeElementImpl) otherObject).clazz);
    }

    @Override
    public int hashCode() {
        return clazz.hashCode();
    }

    @Override
    public String toString() {
        return clazz.toString();
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> visitor, P parameter) {
        return visitor.visitType(this, parameter);
    }

    @Override
    public List<ElementImpl> getEnclosedElements() {
        requireFinished();

        return enclosedElements;
    }

    @Override
    public NestingKind getNestingKind() {
        throw new UnsupportedOperationException(String.format(
            "Nesting kind not currently supported by %s.", ReflectionTypes.class
        ));
    }

    @Override
    public NameImpl getQualifiedName() {
        return new NameImpl(clazz.getCanonicalName());
    }

    @Override
    public Name getSimpleName() {
        return new NameImpl(clazz.getSimpleName());
    }

    @Override
    public TypeMirror getSuperclass() {
        requireFinished();

        return superClass;
    }

    @Override
    public List<? extends TypeMirror> getInterfaces() {
        requireFinished();

        return interfaces;
    }

    @Override
    public List<TypeParameterElementImpl> getTypeParameters() {
        return typeParameters;
    }

    @Override
    public ReflectionElement getEnclosingElement() {
        requireFinished();

        if (enclosingElement == null) {
            throw new UnsupportedOperationException("getEnclosingElement() not supported for top-level classes.");
        } else {
            return enclosingElement;
        }
    }

    @Override
    public DeclaredTypeImpl asType() {
        requireFinished();

        DeclaredTypeImpl localType = type;
        if (localType == null) {
            List<TypeVariableImpl> prototypicalTypeArguments = new ArrayList<>(typeParameters.size());
            for (TypeParameterElementImpl typeParameter: typeParameters) {
                prototypicalTypeArguments.add(typeParameter.asType());
            }

            ReflectionTypeMirror enclosingType = enclosingElement == null
                ? NoTypeImpl.NONE
                : enclosingElement.asType();
            localType = new DeclaredTypeImpl(enclosingType, this, prototypicalTypeArguments);
            type = localType;
        }
        return localType;
    }

    @Override
    public ElementKind getKind() {
        if (clazz.isEnum()) {
            return ElementKind.ENUM;
        } else if (clazz.isAnnotation()) {
            return ElementKind.ANNOTATION_TYPE;
        } else if (clazz.isInterface()) {
            return ElementKind.INTERFACE;
        } else {
            return ElementKind.CLASS;
        }
    }

    @Override
    protected void finishDerivedFromElement(MirrorContext mirrorContext) {
        Class<?> enclosingClass = clazz.getEnclosingClass();
        enclosingElement = enclosingClass == null
            ? null
            : mirrorContext.typeDeclaration(enclosingClass);

        Class<?>[] declaredClasses = clazz.getDeclaredClasses();
        List<ElementImpl> newEnclosedElements = new ArrayList<>(typeParameters.size() + declaredClasses.length);
        newEnclosedElements.addAll(typeParameters);
        for (Class<?> declaredClass: declaredClasses) {
            newEnclosedElements.add(mirrorContext.typeDeclaration(declaredClass));
        }
        enclosedElements = ImmutableList.copyOf(newEnclosedElements);

        Type genericSuperClass = clazz.getGenericSuperclass();
        superClass = genericSuperClass == null
            ? NoTypeImpl.NONE
            : mirrorContext.mirror(genericSuperClass);
        interfaces = mirrorContext.mirror(clazz.getGenericInterfaces());
        for (TypeParameterElementImpl typeParameter: typeParameters) {
            typeParameter.finish(mirrorContext);
        }

        // Field 'type' is lazily initialized in order to break a dependency chain: Constructing type requires
        // enclosingElement.asType(), which at this point may not yet be available.
    }
}
