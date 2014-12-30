package net.florianschoppmann.java.reflect;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.florianschoppmann.java.type.AbstractTypes;
import net.florianschoppmann.java.type.IntersectionType;

import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link javax.lang.model.util.Types} backed by the Java Reflection API.
 *
 * <p>All {@link Element} and {@link TypeMirror} objects returned by this class are immutable and therefore thread-safe.
 * Likewise, only a stateless (and thus thread-safe) singleton instance of this class is available via
 * {@link #getInstance()}.
 *
 * <p>Currently unsupported are (resulting in an {@link java.lang.UnsupportedOperationException}):
 * <ul><li>
 *     Type parameters in method declarations. See {@link #typeMirror(Type)} for details.
 * </li><li>
 *     {@link #directSupertypes(TypeMirror)}
 * </li><li>
 *     {@link #asMemberOf(javax.lang.model.type.DeclaredType, javax.lang.model.element.Element)}
 * </li><li>
 *     {@link #isAssignable(TypeMirror, TypeMirror)}
 * </li><li>
 *     {@link #isSubsignature(javax.lang.model.type.ExecutableType, javax.lang.model.type.ExecutableType)}
 * </li></ul>
 */
public final class ReflectionTypes extends AbstractTypes {
    private static final ReflectionTypes INSTANCE = new ReflectionTypes();

    private final ImmutableList<PrimitiveTypeImpl> primitiveTypes;
    private final ImmutableList<TypeElementImpl> boxedTypeDeclarations;
    {
        List<PrimitiveTypeImpl> newTypes = new ArrayList<>(TypeKind.values().length);
        List<TypeElementImpl> newDeclarations = new ArrayList<>(TypeKind.values().length);

        newTypes.addAll(Collections.<PrimitiveTypeImpl>nCopies(TypeKind.values().length, null));
        newDeclarations.addAll(Collections.<TypeElementImpl>nCopies(TypeKind.values().length, null));

        addPrimitive(newTypes, newDeclarations, TypeKind.DOUBLE, PrimitiveTypeImpl.DOUBLE, Double.class);
        addPrimitive(newTypes, newDeclarations, TypeKind.FLOAT, PrimitiveTypeImpl.FLOAT, Float.class);
        addPrimitive(newTypes, newDeclarations, TypeKind.LONG, PrimitiveTypeImpl.LONG, Long.class);
        addPrimitive(newTypes, newDeclarations, TypeKind.INT, PrimitiveTypeImpl.INT, Integer.class);
        addPrimitive(newTypes, newDeclarations, TypeKind.SHORT, PrimitiveTypeImpl.SHORT, Short.class);
        addPrimitive(newTypes, newDeclarations, TypeKind.BYTE, PrimitiveTypeImpl.BYTE, Byte.class);
        addPrimitive(newTypes, newDeclarations, TypeKind.CHAR, PrimitiveTypeImpl.CHAR, Character.class);
        addPrimitive(newTypes, newDeclarations, TypeKind.BOOLEAN, PrimitiveTypeImpl.BOOLEAN, Boolean.class);

        primitiveTypes = ImmutableList.copyOf(newTypes);
        boxedTypeDeclarations = ImmutableList.copyOf(newDeclarations);
    }

    private ReflectionTypes() { }

    /**
     * Returns the singleton instance of this class.
     *
     * <p>Since this class does not contain any state, and since it is immutable, the returned instance is thread-safe.
     *
     * @return the singleton instance of this class
     */
    public static ReflectionTypes getInstance() {
        return INSTANCE;
    }

    @Override
    protected void requireValidElement(Element element) {
        Objects.requireNonNull(element);
        if (!(element instanceof ReflectionElement)) {
            throw new IllegalArgumentException(String.format(
                "Expected %s instance that was created by %s, but got instance of %s.",
                Element.class.getSimpleName(), ReflectionTypes.class, element.getClass()
            ));
        }
    }

    @Override
    protected void requireValidType(TypeMirror type) {
        if (!(type instanceof ReflectionTypeMirror) && type != null) {
            throw new IllegalArgumentException(String.format(
                "Expected %s instance that was created by %s, but got instance of %s.",
                TypeMirror.class.getSimpleName(), ReflectionTypes.class, type.getClass()
            ));
        }
    }

    private void addPrimitive(List<PrimitiveTypeImpl> newPrimitiveTypes,
        List<TypeElementImpl> newBoxedTypeDeclarations, TypeKind kind, PrimitiveTypeImpl primitiveType,
        Class<?> clazz) {
        newPrimitiveTypes.set(kind.ordinal(), primitiveType);
        newBoxedTypeDeclarations.set(kind.ordinal(), ((DeclaredTypeImpl) typeMirror(clazz)).asElement());
    }

    @Override
    public TypeElement boxedClass(PrimitiveType primitiveType) {
        return boxedTypeDeclarations.get(primitiveType.getKind().ordinal());
    }

    @Override
    public PrimitiveType unboxedType(TypeMirror type) {
        requireValidType(Objects.requireNonNull(type));

        if (type.getKind() != TypeKind.DECLARED) {
            throw new IllegalArgumentException(String.format(
                "Expected type mirror of kind %s, but got %s.", TypeKind.DECLARED, type
            ));
        }

        Name name = ((DeclaredTypeImpl) type).asElement().getQualifiedName();
        if (name.contentEquals(Double.class.getName())) {
            return PrimitiveTypeImpl.DOUBLE;
        } else if (name.contentEquals(Float.class.getName())) {
            return PrimitiveTypeImpl.FLOAT;
        } else if (name.contentEquals(Long.class.getName())) {
            return PrimitiveTypeImpl.LONG;
        } else if (name.contentEquals(Integer.class.getName())) {
            return PrimitiveTypeImpl.INT;
        } else if (name.contentEquals(Short.class.getName())) {
            return PrimitiveTypeImpl.SHORT;
        } else if (name.contentEquals(Byte.class.getName())) {
            return PrimitiveTypeImpl.BYTE;
        } else if (name.contentEquals(Character.class.getName())) {
            return PrimitiveTypeImpl.CHAR;
        } else if (name.contentEquals(Boolean.class.getName())) {
            return PrimitiveTypeImpl.BOOLEAN;
        } else {
            throw new IllegalArgumentException(String.format("Expected boxed type, but got %s.", type));
        }
    }

    /**
     * Returns a type mirror for the given {@link Class} object.
     */
    private ReflectionTypeMirror mirrorClass(Class<?> clazz, MirrorContext mirrorContext) {
        if (clazz.isArray()) {
            return new ArrayTypeImpl(mirrorContext.mirror(clazz.getComponentType()));
        } else if (clazz.isPrimitive()) {
            return (ReflectionTypeMirror) getPrimitiveType(TypeKind.valueOf(clazz.getName().toUpperCase()));
        } else {
            // raw type
            Class<?> enclosingClass = clazz.getEnclosingClass();
            ReflectionTypeMirror enclosingType = enclosingClass == null
                ? NoTypeImpl.NONE
                : mirrorContext.mirror(enclosingClass);
            return new DeclaredTypeImpl(enclosingType, mirrorContext.typeDeclaration(clazz),
                Collections.<ReflectionTypeMirror>emptyList());
        }
    }

    /**
     * Returns a type mirror for the given {@link ParameterizedType} object.
     */
    private static DeclaredTypeImpl mirrorParameterizedType(ParameterizedType parameterizedType,
            MirrorContext mirrorContext) {
        Class<?> rawClass = (Class<?>) parameterizedType.getRawType();
        TypeElementImpl typeDeclaration = mirrorContext.typeDeclaration(rawClass);
        Type ownerType = parameterizedType.getOwnerType();
        ReflectionTypeMirror ownerTypeMirror = ownerType == null
            ? NoTypeImpl.NONE
            : mirrorContext.mirror(ownerType);
        return new DeclaredTypeImpl(ownerTypeMirror, typeDeclaration,
            mirrorContext.mirror(parameterizedType.getActualTypeArguments()));
    }

    /**
     * Returns a type mirror for the given {@link WildcardType} object.
     *
     * <p>The following preconditions are guaranteed by the JLS and the JavaDoc of package {@link java.lang.reflect}:
     * <ul><li>
     *     {@link java.lang.reflect.WildcardType#getUpperBounds()} specifies: "Note that if no upper bound is explicitly
     *     declared, the upper bound is {@code Object}."
     * </li><li>
     *     While {@link java.lang.reflect.WildcardType#getUpperBounds()} and
     *     {@link java.lang.reflect.WildcardType#getLowerBounds()} return an arrays, JLS §4.5.1 (at least up to
     *     version 8) only supports a single ReferenceType for both bounds.
     * </li></ul>
     */
    private static WildcardTypeImpl mirrorWildcardType(WildcardType wildcardType, MirrorContext mirrorContext) {
        Type[] upperBounds = wildcardType.getUpperBounds();
        Type[] lowerBounds = wildcardType.getLowerBounds();

        // See JavaDoc for an explanation of the following assert statement.
        assert upperBounds.length == 1 && lowerBounds.length <= 1
            && (lowerBounds.length == 0 || Object.class.equals(upperBounds[0]));

        ReflectionTypeMirror extendsBounds;
        if (Object.class.equals(upperBounds[0])) {
            extendsBounds = null;
        } else {
            extendsBounds = mirrorContext.mirror(upperBounds[0]);
        }

        ReflectionTypeMirror superBound;
        if (lowerBounds.length == 0) {
            superBound = null;
        } else {
            superBound = mirrorContext.mirror(lowerBounds[0]);
        }
        return new WildcardTypeImpl(extendsBounds, superBound);
    }

    static void requireCondition(boolean condition, String formatString, Object argument) {
        if (!condition) {
            throw new IllegalStateException(String.format(formatString, argument));
        }
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH", justification = "requireCondition() asserts non-null")
    private static TypeVariableImpl mirrorTypeVariable(TypeVariable<?> typeVariable, MirrorContext mirrorContext) {
        GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
        if (genericDeclaration instanceof Class<?>) {
            TypeElementImpl typeDeclaration = mirrorContext.typeDeclaration((Class<?>) genericDeclaration);
            TypeParameterElementImpl element = null;
            for (TypeParameterElementImpl typeParameter: typeDeclaration.getTypeParameters()) {
                if (typeParameter.getSimpleName().contentEquals(typeVariable.getName())) {
                    element = typeParameter;
                    break;
                }
            }
            requireCondition(element != null,
                "Could not find the type-parameter element that corresponds to type variable %s.", typeVariable);
            return element.asType();
        } else {
            throw new UnsupportedOperationException("Method or constructor type parameters not supported.");
        }
    }

    ReflectionTypeMirror mirrorInternal(Type type, MirrorContext mirrorContext) {
        ReflectionTypeMirror typeMirror = null;
        if (type instanceof Class<?>) {
            typeMirror = mirrorClass((Class<?>) type, mirrorContext);
        } else if (type instanceof ParameterizedType) {
            typeMirror = mirrorParameterizedType((ParameterizedType) type, mirrorContext);
        } else if (type instanceof GenericArrayType) {
            typeMirror = new ArrayTypeImpl(
                mirrorContext.mirror(((GenericArrayType) type).getGenericComponentType())
            );
        } else if (type instanceof WildcardType) {
            typeMirror = mirrorWildcardType((WildcardType) type, mirrorContext);
        } else if (type instanceof TypeVariable<?>) {
            typeMirror = mirrorTypeVariable((TypeVariable<?>) type, mirrorContext);
        }
        requireCondition(typeMirror != null,
            "Expected Class, ParameterizedType, GenericArrayType, WildcardType, or TypeVariable instance, but got %s.",
            type);
        return typeMirror;
    }

    /**
     * Returns a type element corresponding to the given {@link Class} object.
     *
     * @param clazz class object
     * @return type element corresponding to the given {@link Class} object
     * @throws IllegalArgumentException if the given class represents a primitive or array type
     * @throws UnsupportedOperationException if a generic declaration is referenced that is not of type {@link Class},
     *     see {@link #typeMirror(java.lang.reflect.Type)} for details
     */
    public TypeElement typeElement(Class<?> clazz) {
        if (clazz.isArray() || clazz.isPrimitive()) {
            throw new IllegalArgumentException(String.format("Expected class or interface type, but got %s.", clazz));
        }

        return ((DeclaredTypeImpl) typeMirror(clazz)).asElement();
    }

    /**
     * Returns a type mirror corresponding to the given Java reflection type.
     *
     * <p>Type parameters in method declarations are not currently supported. That is, if the given type references a
     * {@link java.lang.reflect.TypeVariable} instance that has a {@link java.lang.reflect.Constructor} or
     * {@link java.lang.reflect.Method} as generic declaration, an {@link java.lang.UnsupportedOperationException}
     * will be thrown.
     *
     * @param type type as represented by Java Reflection API
     * @return type mirror corresponding to the given Java reflection type
     * @throws UnsupportedOperationException if a generic declaration is referenced that is not of type {@link Class}
     */
    @Override
    public TypeMirror typeMirror(Type type) {
        Map<Class<?>, TypeElementImpl> typeDeclarations = new LinkedHashMap<>();
        Map<Class<?>, TypeElementImpl> newTypeDeclarations = new LinkedHashMap<>();
        MirrorContext mirrorContext = new MirrorContext(this, typeDeclarations, newTypeDeclarations);

        TypeMirror typeMirror = mirrorInternal(type, mirrorContext);
        while (!newTypeDeclarations.isEmpty()) {
            List<TypeElementImpl> typeDeclarationsToFinish = new ArrayList<>(newTypeDeclarations.values());
            typeDeclarations.putAll(newTypeDeclarations);
            newTypeDeclarations.clear();
            for (TypeElementImpl typeDeclaration: typeDeclarationsToFinish) {
                typeDeclaration.finish(mirrorContext);
            }
        }
        return typeMirror;
    }

    private static ImmutableList<ReflectionTypeMirror> toList(TypeMirror[] types) {
        List<ReflectionTypeMirror> list = new ArrayList<>(types.length);
        for (TypeMirror type: types) {
            list.add((ReflectionTypeMirror) type);
        }
        return ImmutableList.copyOf(list);
    }

    @Override
    public DeclaredType getDeclaredType(DeclaredType containing, TypeElement typeElem, TypeMirror... typeArgs) {
        // Note that containing may be null
        requireValidType(containing);
        requireValidElement(typeElem);
        requireValidTypes(typeArgs);

        ReflectionTypeMirror newContainingType = containing == null
            ? NoTypeImpl.NONE
            : (ReflectionTypeMirror) containing;
        return new DeclaredTypeImpl(newContainingType, (TypeElementImpl) typeElem, toList(typeArgs));
    }

    @Override
    public DeclaredType getDeclaredType(TypeElement typeElem, TypeMirror... typeArgs) {
        requireValidElement(typeElem);
        requireValidTypes(typeArgs);

        return new DeclaredTypeImpl(NoTypeImpl.NONE, (TypeElementImpl) typeElem, toList(typeArgs));
    }

    @Override
    public NoType getNoType(TypeKind kind) {
        Objects.requireNonNull(kind);
        if (kind == TypeKind.VOID) {
            return NoTypeImpl.VOID;
        } else if (kind == TypeKind.NONE) {
            return NoTypeImpl.NONE;
        } else {
            throw new IllegalArgumentException(String.format("Expected one of %s, but got %s.",
                Arrays.asList(TypeKind.VOID, TypeKind.NONE), kind));
        }
    }

    @Override
    public NullType getNullType() {
        return NullTypeImpl.INSTANCE;
    }

    @Override
    public ArrayType getArrayType(TypeMirror componentType) {
        Objects.requireNonNull(componentType);
        requireValidType(componentType);

        return new ArrayTypeImpl((ReflectionTypeMirror) componentType);
    }

    @Override
    protected javax.lang.model.type.TypeVariable createTypeVariable(TypeParameterElement typeParameter,
        javax.lang.model.type.WildcardType capturedTypeArgument) {

        requireValidElement(Objects.requireNonNull(typeParameter));
        requireValidType(capturedTypeArgument);

        return new TypeVariableImpl((TypeParameterElementImpl) typeParameter, (WildcardTypeImpl) capturedTypeArgument);
    }

    @Override
    protected javax.lang.model.type.WildcardType capturedTypeArgument(javax.lang.model.type.TypeVariable typeVariable) {
        requireValidType(Objects.requireNonNull(typeVariable));

        return ((TypeVariableImpl) typeVariable).getCapturedTypeArgument();
    }

    @Override
    public IntersectionType getIntersectionType(TypeMirror... bounds) {
        Objects.requireNonNull(bounds);
        if (bounds.length == 0) {
            throw new IllegalArgumentException("Expected at least one bound.");
        }
        requireValidTypes(bounds);

        List<ReflectionTypeMirror> newBounds = new ArrayList<>(bounds.length);
        for (TypeMirror bound: bounds) {
            newBounds.add((ReflectionTypeMirror) bound);
        }

        return new IntersectionTypeImpl(newBounds);
    }

    @Override
    protected void setTypeVariableBounds(javax.lang.model.type.TypeVariable typeVariable, TypeMirror upperBound,
        TypeMirror lowerBound) {

        requireValidType(Objects.requireNonNull(typeVariable));
        requireValidType(Objects.requireNonNull(upperBound));
        requireValidType(Objects.requireNonNull(lowerBound));

        ((TypeVariableImpl) typeVariable).setUpperAndLowerBounds(
            (ReflectionTypeMirror) upperBound,
            (ReflectionTypeMirror) lowerBound
        );
    }

    @Override
    public PrimitiveType getPrimitiveType(TypeKind kind) {
        PrimitiveTypeImpl primitiveType = primitiveTypes.get(kind.ordinal());
        if (primitiveType == null) {
            throw new IllegalArgumentException(String.format("Expected primitive kind, but got %s.", kind));
        }
        return primitiveType;
    }

    @Override
    public javax.lang.model.type.WildcardType getWildcardType(TypeMirror extendsBound, TypeMirror superBound) {
        // Note that extendsBound and superBound are allowed to be null.
        requireValidType(extendsBound);
        requireValidType(superBound);

        return new WildcardTypeImpl((ReflectionTypeMirror) extendsBound, (ReflectionTypeMirror) superBound);
    }

    private static UnsupportedOperationException unsupportedException() {
        return new UnsupportedOperationException(
            "isAssignable(), isSubsignature(), asMemberOf(), and directSupertypes() not currently supported."
        );
    }

    @Override
    public boolean isAssignable(TypeMirror t1, TypeMirror t2) {
        throw unsupportedException();
    }

    @Override
    public boolean isSubsignature(ExecutableType m1, ExecutableType m2) {
        throw unsupportedException();
    }

    @Override
    public TypeMirror asMemberOf(DeclaredType containing, Element element) {
        throw unsupportedException();
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException whenever this method is called
     */
    @Override
    public List<? extends TypeMirror> directSupertypes(TypeMirror t) {
        throw unsupportedException();
    }
}
