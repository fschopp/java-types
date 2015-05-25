package net.florianschoppmann.java.type;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Abstract skeletal implementation of {@link Types}.
 *
 * <p>This class provides a skeletal implementation of the {@link Types} interface. Specifically, it implements all
 * methods pertaining to §4.10 (subtyping) in the Java Language Specification (JLS). Concrete subclasses are expected to
 * implement the abstract methods in this class, which are responsible for creating appropriate type-mirror instances.
 * This class does not place any additional constraints on the concrete {@link TypeMirror} and {@link Element}
 * implementations, so mutability and thread-safety are implementation-defined. However, this class crucially relies on
 * the {@code equals} method being well-defined. That is, {@link Element} objects that have equal names and equal
 * enclosing elements must compare equal. Likewise, {@link TypeMirror} objects that contain equal values must compare
 * equal. In particular, multiple instances created by one of the {@code get}-methods must compare equal when the given
 * arguments compare equal.
 *
 * <p>Besides subtype-related methods, this class also provides method
 * {@link #resolveActualTypeArguments(TypeElement, TypeMirror)} for resolving formal type parameters to actual type
 * arguments. For instance, given type {@code List<String>}, this method determines the actual type argument for the
 * formal type parameter of {@code Collection<E>} (that is, {@code String} in this simple example).
 *
 * <p>Unless explicitly stated otherwise, all methods in this class expect non-null arguments. Passing null where not
 * expected will cause a {@link NullPointerException} to be thrown. Implementations typically place additional
 * restrictions on method arguments not captured by the types of the formal parameters (which stem from
 * {@link javax.lang.model} and its subpackages). While the details are implementation-defined, typically this means
 * that arguments must have been crated by the same implementation, or otherwise an {@link IllegalArgumentException}
 * will be thrown. Implementations must override {@link #requireValidType(TypeMirror)} and
 * {@link #requireValidElement(Element)}, as these methods are expected to perform any necessary validation.
 */
public abstract class AbstractTypes implements Types {
    private static final List<TypeKind> REFERENCE_TYPES = Collections.unmodifiableList(Arrays.asList(
        TypeKind.ARRAY, TypeKind.DECLARED, TypeKind.NULL, TypeKind.TYPEVAR
    ));
    private static final int DEFAULT_STRING_BUILDER_SIZE = 256;

    private final SubstitutionVisitor substitutionVisitor = new SubstitutionVisitor();
    private final ErasureVisitor erasureVisitor = new ErasureVisitor();
    private final ToStringVisitor toStringVisitor = new ToStringVisitor();
    private final SubtypeVisitor subtypeVisitor = new SubtypeVisitor();
    private final DeclaredTypeSubtypeVisitor declaredTypeSubtypeVisitor = new DeclaredTypeSubtypeVisitor();

    /**
     * Verifies that the given {@link Element} is valid for use with this class.
     *
     * <p>The meaning of valid is implementation-defined, but typically a valid {@link Element} must have been created
     * by the implementation that belongs to the current {@code AbstractTypes} instance.
     *
     * @param element element
     * @throws NullPointerException if the argument is null
     * @throws IllegalArgumentException if the given {@link Element} cannot be used with this class
     */
    protected abstract void requireValidElement(Element element);

    /**
     * Verifies that the given {@link TypeMirror} is valid for use with this class, or that it is {@code null}.
     *
     * <p>The meaning of valid is implementation-defined, but typically a valid {@link TypeMirror} must have been
     * created by the implementation that belongs to the current {@code AbstractTypes} instance. A {@code null} argument
     * is always valid. The rationale is that {@code null} {@link TypeMirror} arguments have a special meaning for some
     * methods such as {@link #getWildcardType(TypeMirror, TypeMirror)} or
     * {@link #createTypeVariable(TypeParameterElement, WildcardType)}.
     *
     * @param type type mirror, may be {@code null}
     * @throws IllegalArgumentException if the given {@link TypeMirror} instance is non-null and it cannot be used with
     *     this class
     */
    protected abstract void requireValidType(@Nullable TypeMirror type);

    /**
     * Verifies that the given array is non-null and contains valid types that are not null.
     *
     * @param types array of types
     * @throws NullPointerException if the given array or any of its elements are null
     * @throws IllegalArgumentException if {@link #requireValidType(TypeMirror)} throws an exception for one of the
     *     array elements
     */
    protected final void requireValidTypes(TypeMirror[] types) {
        for (TypeMirror typeArg: types) {
            Objects.requireNonNull(typeArg, "TypeMirror array must not contain null elements.");
            requireValidType(typeArg);
        }
    }

    /**
     * Returns a type mirror corresponding to the given Java reflection type.
     *
     * <p>Subclasses are required to return the appropriate {@link DeclaredType} instances for the following
     * {@link Class} instances:
     * <ul><li>
     *     {@link Object}
     * </li><li>
     *     {@link Serializable}
     * </li><li>
     *     {@link Cloneable}
     * </li></ul>
     *
     * <p>Support for other types is not required and implementation-defined.
     *
     * @param type type as represented by Java Reflection API
     * @throws UnsupportedOperationException If the given type is not one of the above {@link Class} objects and
     *     this type-utilities implementation does not support mirroring arbitrary Java reflection types.
     * @return the type mirror corresponding to the given reflection type
     */
    protected abstract TypeMirror typeMirror(Type type);

    /**
     * Internal class that contains both the substitution map passed to {@link #substitute(TypeMirror, Map)} and the
     * set of fresh type variables created at the beginning of that method.
     */
    private static final class Substitutions {
        private final Map<TypeParameterElement, ? extends TypeMirror> map;
        private final Map<TypeParameterElement, TypeVariable> freshTypeVariables;

        private Substitutions(Map<TypeParameterElement, ? extends TypeMirror> map,
                Map<TypeParameterElement, TypeVariable> freshTypeVariables) {
            this.map = map;
            this.freshTypeVariables = freshTypeVariables;
        }
    }

    /**
     * Visitor of a type mirror. Returns a new type mirror after performing the substitutions passed as visitor
     * argument.
     *
     * <p>This visitor is only used within this class and only on <em>valid</em> {@link TypeMirror} instances. Hence, it
     * can be asserted that the visitor parameter is always non-null.
     *
     * @see #substitutionVisitor
     * @see #requireValidType(TypeMirror)
     */
    private final class SubstitutionVisitor extends ExtendedTypeKindVisitor7<TypeMirror, Substitutions> {
        private TypeMirror[] substituteInList(List<? extends TypeMirror> types, Substitutions substitutions) {
            TypeMirror[] substituted = new TypeMirror[types.size()];
            int i = 0;
            for (TypeMirror type: types) {
                substituted[i] = type.accept(this, substitutions);
                ++i;
            }
            return substituted;
        }

        @Override
        public TypeMirror visitDeclared(DeclaredType declaredType, @Nullable Substitutions substitutions) {
            assert substitutions != null;
            TypeMirror enclosingType = declaredType.getEnclosingType();
            TypeElement typeDeclaration = (TypeElement) declaredType.asElement();
            TypeMirror[] substitutedArguments = substituteInList(declaredType.getTypeArguments(), substitutions);
            if (enclosingType.getKind() == TypeKind.DECLARED) {
                return getDeclaredType((DeclaredType) enclosingType, typeDeclaration, substitutedArguments);
            } else {
                return getDeclaredType(typeDeclaration, substitutedArguments);
            }
        }

        @Override
        public TypeMirror visitArray(ArrayType arrayType, @Nullable Substitutions substitutions) {
            assert substitutions != null;
            return getArrayType(arrayType.getComponentType().accept(this, substitutions));
        }

        @Override
        public TypeMirror visitTypeVariable(TypeVariable typeVariable, @Nullable Substitutions substitutions) {
            assert substitutions != null;
            TypeParameterElement formalTypeParameter = (TypeParameterElement) typeVariable.asElement();
            @Nullable TypeVariable freshTypeVariable = substitutions.freshTypeVariables.get(formalTypeParameter);
            if (freshTypeVariable != null && formalTypeParameter.asType().equals(typeVariable)) {
                return freshTypeVariable;
            }

            @Nullable TypeMirror substitution = substitutions.map.get(formalTypeParameter);
            if (substitution != null) {
                return substitution;
            }

            return getTypeVariable(
                formalTypeParameter,
                typeVariable.getUpperBound().accept(this, substitutions),
                typeVariable.getLowerBound().accept(this, substitutions),
                capturedTypeArgument(typeVariable)
            );
        }

        @Override
        public TypeMirror visitWildcard(WildcardType wildcardType, @Nullable Substitutions substitutions) {
            assert substitutions != null;
            @Nullable TypeMirror extendsBounds = wildcardType.getExtendsBound();
            @Nullable TypeMirror superBound = wildcardType.getSuperBound();

            return getWildcardType(
                extendsBounds != null
                    ? extendsBounds.accept(this, substitutions)
                    : null,
                superBound != null
                    ? superBound.accept(this, substitutions)
                    : null
            );
        }

        @Override
        public TypeMirror visitIntersection(IntersectionType intersectionType, @Nullable Substitutions substitutions) {
            assert substitutions != null;
            return getIntersectionType(substituteInList(intersectionType.getBounds(), substitutions));
        }

        @Override
        protected TypeMirror defaultAction(TypeMirror type, Substitutions substitutions) {
            return type;
        }
    }

    /**
     * Replaces formal type parameters in the given type.
     *
     * <p>This method requires that {@code type} does not contain transitive references to itself, unless through
     * {@link DeclaredType#asElement()} → {@link TypeElement#asType()} or {@link TypeVariable#asElement()} →
     * {@link TypeParameterElement#asType()}. Otherwise, this method might run into an infinite recursion, resulting in
     * a {@link StackOverflowError}.
     *
     * <p>Moreover, this method requires that any type variable transitively referenced by {@code substitutionMap} must
     * not contain a transitive reference (through {@link TypeVariable#getUpperBound()} or
     * {@link TypeVariable#getLowerBound()}) to itself. Instead, any instance of {@link TypeVariable} (transitively)
     * referenced by a value in {@code substitutionMap} must be the result of {@link TypeParameterElement#asType()}.
     *
     * <p>This method creates a fresh type variable for each formal type parameter that is to be substituted by a type
     * variable for the same formal type parameter. For instance, suppose {@code T extends Object} is a formal type
     * parameter, and {@code substitutionMap} specifies to replace it with the type variable {@code T extends U<T>}. In
     * this case, {@link #createTypeVariable(TypeParameterElement, WildcardType)} will be called with the formal type
     * parameter {@code T extends Object} as (first) argument. Once all fresh types have been created,
     * {@link #setTypeVariableBounds(TypeVariable, TypeMirror, TypeMirror)} will then be called with {@code U<T>} as
     * upper bound, where {@code T} is the fresh type variable {@code T extends U<T>}.
     *
     * @param type type in which the type parameters will be replaced recursively, guaranteed non-null
     * @param substitutionMap mapping from formal type parameters to substituted type, guaranteed non-null
     * @return new port type, guaranteed non-null
     */
    protected TypeMirror substitute(TypeMirror type, Map<TypeParameterElement, ? extends TypeMirror> substitutionMap) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(substitutionMap);
        requireValidType(type);
        for (TypeMirror substitutionType: substitutionMap.values()) {
            Objects.requireNonNull(substitutionType, "Substitution type cannot be null.");
            requireValidType(substitutionType);
        }

        Map<TypeParameterElement, TypeVariable> freshTypeVariables = new LinkedHashMap<>();
        for (Map.Entry<TypeParameterElement, ? extends TypeMirror> entry: substitutionMap.entrySet()) {
            TypeMirror value = entry.getValue();
            if (value.getKind() == TypeKind.TYPEVAR) {
                TypeParameterElement formalTypeParameter = entry.getKey();
                TypeVariable typeVariable = (TypeVariable) value;
                if (entry.getKey().equals(typeVariable.asElement())) {
                    assert !freshTypeVariables.containsKey(formalTypeParameter);

                    freshTypeVariables.put(
                        formalTypeParameter,
                        createTypeVariable(formalTypeParameter, capturedTypeArgument(typeVariable))
                    );
                }
            }
        }

        Substitutions substitutions = new Substitutions(substitutionMap, freshTypeVariables);
        for (Map.Entry<TypeParameterElement, TypeVariable> entry: freshTypeVariables.entrySet()) {
            TypeVariable substitution = (TypeVariable) substitutionMap.get(entry.getKey());
            setTypeVariableBounds(
                entry.getValue(),
                substitution.getUpperBound().accept(substitutionVisitor, substitutions),
                substitution.getLowerBound().accept(substitutionVisitor, substitutions)
            );
        }

        return type.accept(substitutionVisitor, substitutions);
    }

    /**
     * Returns the actual type arguments of a type declaration given a subtype (typically with its own actual type
     * arguments).
     *
     * <p>This method "projects" the actual type arguments of {@code subtype}, as well as all actual type arguments of
     * super types in the type hierarchy between {@code typeElement} and {@code subType}, onto the type declaration
     * represented by {@code typeElement}.
     *
     * <p>For example, {@code typeElement} may be the (generic) type declaration {@code Comparable<T>}, and
     * {@code subType} may be the (non-generic) type {@link Integer}. The result in this case would be a singleton list
     * containing the type {@link Integer}.
     *
     * <p>More generally, resolution works as follows: First, the shortest inheritance path from {@code subType} to
     * {@code typeElement} is found. Note that while Java allows multiple inheritance for interfaces, JLS §8.1.5
     * disallows inheriting from the same interface with different type parameters (both directly and transitively).
     * Hence, the shortest path contains all information that is necessary to resolve formal type parameters to actual
     * parameters. This method then propagates the actual type arguments bottom-up along the inheritance path.
     * Note that the inheritance path consists of {@link DeclaredType} instances, and it may consist of generic types,
     * non-generic types, and raw types.
     *
     * <p>If the inheritance path contains a raw type <em>before</em> the last path element, this method proceeds
     * by using the "prototypical" type returned by {@link Element#asType()} instead. Correspondingly, it is possible
     * that the returned list may contain type variables from a type declaration along the inheritance path. However, if
     * the <em>last</em> inheritance path element is a raw type, the returned list will be empty. Otherwise, if a
     * non-null non-empty {@link List} is returned, it is guaranteed to have the same number of elements as
     * {@code typeElement.getTypeParameters()}.
     *
     * @param typeElement type declaration
     * @param subType potential subtype of {@code typeElement}, must be a non-generic type declaration, raw type,
     *     generic type declaration, or parameterized type
     * @return actual type arguments for the formal parameters of {@code typeElement} (empty list if the <em>last</em>
     *     path element in the inheritance path from {@code subType} to {@code typeElement} is a raw type), or
     *     {@code null} if {@code subType} is not a subtype of {@code typeElement}
     * @throws IllegalArgumentException if the arguments do not satisfy the constraints mentioned above
     */
    @Nullable
    public final List<? extends TypeMirror> resolveActualTypeArguments(TypeElement typeElement, TypeMirror subType) {
        requireValidElement(Objects.requireNonNull(typeElement));
        requireValidType(Objects.requireNonNull(subType));

        if (subType.getKind() != TypeKind.DECLARED) {
            return null;
        }

        DeclaredType declaredSubType = (DeclaredType) subType;

        // getShortestPathToSuperType() will throw an exception if subType does not satisfy the constraints mentioned
        // above.
        @Nullable List<DeclaredType> path = getShortestPathToSuperType(typeElement, declaredSubType);
        if (path == null) {
            return null;
        }

        // Early exit if there is nothing to resolve. However, we must not move this early exit any earlier, because
        // we do want to return null if subType is not a subtype of typeElement.
        if (typeElement.getTypeParameters().isEmpty()) {
            return Collections.emptyList();
        }

        Iterator<DeclaredType> pathIterator = path.iterator();
        DeclaredType current = pathIterator.next();
        while (pathIterator.hasNext()) {
            TypeElement currentTypeElement = (TypeElement) current.asElement();

            // Check whether "current" is a raw type. This may happen in the first loop iteration if subType is a raw
            // type, or in subsequent iterations if the type that was previously "current" (during the last iteration
            // of the for-loop) derived from a raw type. If yes, use instead the "prototypical" type returned by
            // Element#asType().
            if (current.getTypeArguments().isEmpty() && !currentTypeElement.getTypeParameters().isEmpty()) {
                current = (DeclaredType) currentTypeElement.asType();
            }

            List<? extends TypeParameterElement> currentFormalParameters = currentTypeElement.getTypeParameters();
            List<? extends TypeMirror> currentActualParameters = current.getTypeArguments();

            Map<TypeParameterElement, TypeMirror> currentFormalToActual = new LinkedHashMap<>();
            for (int index = 0; index < currentFormalParameters.size(); ++index) {
                currentFormalToActual.put(currentFormalParameters.get(index), currentActualParameters.get(index));
            }

            current = (DeclaredType) substitute(pathIterator.next(), currentFormalToActual);
        }
        return current.getTypeArguments();
    }

    /**
     * Visitor of a type mirror. Returns whether the visited type mirror is a subtype of the visitor argument (of type
     * {@link DeclaredType}).
     *
     * <p>This visitor is only used within this class and only on <em>valid</em> {@link TypeMirror} instances. Hence, it
     * can be asserted that the visitor parameter is always non-null.
     *
     * @see #declaredTypeSubtypeVisitor
     * @see #requireValidType(TypeMirror)
     */
    private final class DeclaredTypeSubtypeVisitor extends ExtendedTypeKindVisitor7<Boolean, DeclaredType> {
        private DeclaredTypeSubtypeVisitor() {
            super(false);
        }

        /**
         * Returns whether the first declared type is a subtype of the second declared type.
         *
         * <p>This method proceeds by computing the actual type arguments when {@code subType} is projected onto the
         * type declaration corresponding to {@code superType}. It then tests if all actual type arguments of
         * {@code subType} are <em>contained</em> in those of {@code superType}.
         */
        @Override
        public Boolean visitDeclared(DeclaredType subType, @Nullable DeclaredType superType) {
            assert superType != null;
            DeclaredType actualSubType = subType;

            // First test if there subType has at least one wildcard type argument. In that case, we need to perform a
            // capture conversion first.
            // Note that this is the right place to do capture conversion: JLS §8.1.4 and §9.1.3 state about class types
            // and interfaces type listed in the extends or implements clause of a class/interface declaration:
            // - "If the ClassType has type arguments, it must denote a well-formed parameterized type (§4.5), and none
            // of the type arguments may be wildcard type arguments, or a compile-time error occurs."
            // - "If an InterfaceType has type arguments, it must denote a well-formed parameterized type (§4.5), and
            // none of the type arguments may be wildcard type arguments, or a compile-time error occurs."
            // Hence, wildcards do not appear on the "inheritance path" between subType and superType.
            for (TypeMirror subTypeArgument: subType.getTypeArguments()) {
                if (subTypeArgument.getKind() == TypeKind.WILDCARD) {
                    actualSubType = (DeclaredType) capture(subType);
                    break;
                }
            }

            // Resolve the actual type parameters of subType when projected onto the superType
            TypeElement superTypeDeclaration = (TypeElement) superType.asElement();
            @Nullable List<? extends TypeMirror> projectedTypeArguments
                = resolveActualTypeArguments(superTypeDeclaration, actualSubType);

            if (projectedTypeArguments == null) {
                // subType is not a subtype of the type declaration
                return false;
            }

            List<? extends TypeMirror> superTypeArguments = superType.getTypeArguments();
            if (projectedTypeArguments.isEmpty() && !superTypeArguments.isEmpty()) {
                // the projection of subType onto superType resulted in a raw type, which is neither a subtype of any
                // parametrized type of the generic type declaration of superType, nor the generic type declaration
                // itself
                return false;
            }

            // Note that superType could be a raw type, in which case superTypeArguments is empty. In that case, the
            // loop would not be executed at all.
            Iterator<? extends TypeMirror> projectedTypeArgumentsIterator = projectedTypeArguments.iterator();
            for (TypeMirror to: superTypeArguments) {
                TypeMirror from = projectedTypeArgumentsIterator.next();
                if (!contains(to, from)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns whether the array type is a subtype of the declared type.
         *
         * <p>According to JLS §4.10.3, an array type can only be a subtype of a declared type if the latter represents
         * one of {@link Object}, {@link Cloneable}, or {@link Serializable}.
         */
        @Override
        public Boolean visitArray(ArrayType subType, @Nullable DeclaredType superType) {
            assert superType != null;
            return typeMirror(Object.class).equals(superType)
                || typeMirror(Cloneable.class).equals(superType)
                || typeMirror(Serializable.class).equals(superType);
        }

        /**
         * Returns whether the type variable is a subtype of the declared type.
         *
         * <p>According to JLS §4.10.2, the direct supertypes of a type variable are the types listed in its bound.
         * Hence, this method returns true if {@link TypeVariable#getUpperBound()} is a subtype of {@code superType}.
         */
        @Override
        public Boolean visitTypeVariable(TypeVariable subType, @Nullable DeclaredType superType) {
            assert superType != null;
            return isSubtype(subType.getUpperBound(), superType);
        }

        /**
         * Returns whether the intersection type is a subtype of the declared type.
         *
         * <p>According to JLS §4.10.2, the direct supertypes of an intersection type {@code T_1 & ... T_n} are
         * {@code T_1}, ..., {@code T_n}. Hence, this method returns true if at least one of
         * {@link IntersectionType#getBounds()} is a subtype of {@code superType}.
         */
        @Override
        public Boolean visitIntersection(IntersectionType subType, @Nullable DeclaredType superType) {
            assert superType != null;
            for (TypeMirror bound: subType.getBounds()) {
                if (isSubtype(bound, superType)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Visitor of a type mirror. Returns whether the visited type mirror is a supertype of the visitor argument.
     *
     * <p>This visitor does not have to deal with the null-type, which has been dealt with before. It has to make a
     * decision for {@link ArrayType}, {@link DeclaredType}, {@link PrimitiveType}, and {@link TypeVariable}.
     *
     * <p>Java 8 introduces {@code IntersectionType}, but this code currently uses Java 7. Moreover, there are other
     * types that are currently not supported, such as {@link javax.lang.model.type.UnionType}. Finally,
     * {@link WildcardType} is not a type, but only a type argument, so it is not necessary to be dealt with here.
     * Likewise, {@link NoType} is not used to model proper types, but only empty bounds, non-existence of interface
     * super classes, etc.
     *
     * <p>This visitor is only used within this class and only on <em>valid</em> {@link TypeMirror} instances. Hence, it
     * can be asserted that the visitor parameter is always non-null.
     *
     * @see #subtypeVisitor
     * @see #requireValidType(TypeMirror)
     */
    private final class SubtypeVisitor extends ExtendedTypeKindVisitor7<Boolean, TypeMirror> {
        private SubtypeVisitor() {
            super(false);
        }

        /**
         * Returns whether the array type is a super type of the type given as second argument.
         *
         * <p>According to JLS §4.10.3, array component types are covariant; for instance, {@code Integer[]} is a proper
         * subtype of {@code Number[]}. Moreover, all subtypes of an array type are again array types. Hence, this
         * method simply reduces the problem to testing if {@code subType} is also an array type and then applying
         * {@link AbstractTypes#isSubtype(TypeMirror, TypeMirror)} to the component types.
         */
        @Override
        public Boolean visitArray(ArrayType superType, @Nullable TypeMirror subType) {
            assert subType != null;
            return subType.getKind() == TypeKind.ARRAY
                && isSubtype(((ArrayType) subType).getComponentType(), superType.getComponentType());
        }

        /**
         * Returns whether the declared type is a super type of the type given as second argument.
         *
         * <p>This method has {@link DeclaredTypeSubtypeVisitor} visit {@code subType}.
         */
        @Override
        public Boolean visitDeclared(DeclaredType superType, @Nullable TypeMirror subType) {
            assert subType != null;
            return subType.accept(declaredTypeSubtypeVisitor, superType);
        }

        private final List<TypeKind> numericKindEnumValues = Collections.unmodifiableList(Arrays.asList(
            TypeKind.DOUBLE, TypeKind.FLOAT, TypeKind.LONG, TypeKind.INT, TypeKind.SHORT, TypeKind.BYTE
        ));
        private final int intIndex = numericKindEnumValues.indexOf(TypeKind.INT);

        /**
         * Returns whether the primitive type is a supertype of the given type.
         */
        @Override
        public Boolean visitPrimitive(PrimitiveType superType, @Nullable TypeMirror subType) {
            assert subType != null;
            if (!subType.getKind().isPrimitive()) {
                return false;
            }

            int superTypeIndex = numericKindEnumValues.indexOf(superType.getKind());
            int subTypeIndex = numericKindEnumValues.indexOf(subType.getKind());
            return (subType.getKind() == TypeKind.CHAR && 0 <= superTypeIndex && superTypeIndex <= intIndex)
                || (0 <= superTypeIndex && superTypeIndex <= subTypeIndex);
        }

        /**
         * Returns whether the type variable is a super type of the given type.
         *
         * <p>A type variable is only a supertype of its lower bound.
         */
        @Override
        public Boolean visitTypeVariable(TypeVariable superType, @Nullable TypeMirror subType) {
            assert subType != null;
            return isSameType(superType.getLowerBound(), subType);
        }

        /**
         * Returns whether the given intersection type is a super type of the given type.
         *
         * <p>While one might expect that the set of supertypes of an intersection type {@code T_1 & ... & T_n} includes
         * the intersection of any (non-empty) subset of {@code T_1}, ..., {@code T_n}, this seems is not specified by
         * JLS §4.10 (which only says that "the direct supertypes of an intersection type {@code T_1 & ... & T_n} are
         * {@code T_i} (1 ≤ i ≤ n)"). See also issue
         * <a href="https://bugs.openjdk.java.net/browse/JDK-6718388">JDK-6718388</a>.
         *
         * <p>Therefore, an intersection type is only a supertype of itself.
         */
        @Override
        public Boolean visitIntersection(IntersectionType superType, @Nullable TypeMirror subType) {
            assert subType != null;
            return isSameType(superType, subType);
        }
    }

    /**
     * Returns whether the first type is a subtype of the second type, as specified by JLS §4.10.
     *
     * <p>The subtype relationship is transitive and reflexive.
     *
     * @param t1 the first type
     * @param t2 the second type
     * @return {@code true} if and only if the first type is a subtype of the second
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if given an executable or package type
     */
    @Override
    public final boolean isSubtype(TypeMirror t1, TypeMirror t2) {
        requireValidType(Objects.requireNonNull(t1));
        requireValidType(Objects.requireNonNull(t2));

        // §4.10.2: The direct supertypes of the null type are all reference types other than the null type itself.
        if (t1.getKind() == TypeKind.NULL && REFERENCE_TYPES.contains(t2.getKind())) {
            return true;
        }

        return t2.accept(subtypeVisitor, t1);
    }

    /**
     * Returns whether the first type argument <em>contains</em> the second type argument, as specified by JLS §4.5.1.
     *
     * <p>Using the JLS notation, this method returns true if {@code t2 <= t1}. As JLS §4.10 states, "subtyping does not
     * extend through parameterized types." Hence, this method is necessarily different from
     * {@link #isSubtype(TypeMirror, TypeMirror)}. In particular, the super-type relationship does not include types
     * with covariant type arguments.
     *
     * @param t1 the first type
     * @param t2 the second type
     * @return {@code true} if and only if the first type contains the second
     * @throws IllegalArgumentException if given an executable or package type
     */
    @Override
    public final boolean contains(TypeMirror t1, TypeMirror t2) {
        Objects.requireNonNull(t1);
        Objects.requireNonNull(t2);
        requireValidType(t1);
        requireValidType(t2);

        // We need to cover these cases (JLS §4.5.1):
        //
        // (a) wildcard t2 <= wildcard t1
        // 1. ? extends T <= ? extends S if T <: S
        // 2. ? extends T <= ?
        // 3. ? super T <= ? super S if S <: T
        // 4. ? super T <= ?
        // 5. ? super T <= ? extends Object
        //
        // (b) other type t2 <= other type t1
        // 1. T <= T
        //
        // (c) other type t2 <= wildcard t1
        // 1. T <= ? extends T
        // 2. T <= ? super T

        if (t1.getKind() == TypeKind.WILDCARD) {
            @Nullable TypeMirror t1ExtendsBound = ((WildcardType) t1).getExtendsBound();
            @Nullable TypeMirror t1SuperBound = ((WildcardType) t1).getSuperBound();
            boolean t1HasExtendsBound = t1ExtendsBound != null;
            boolean t1HasSuperBound = t1SuperBound != null;

            if (t2.getKind() == TypeKind.WILDCARD) {
                // Handle (a).
                @Nullable TypeMirror t2ExtendsBound = ((WildcardType) t2).getExtendsBound();
                @Nullable TypeMirror t2SuperBound = ((WildcardType) t2).getSuperBound();

                if (t2ExtendsBound != null) {
                    if (t1ExtendsBound != null) {
                        // (a) 1.
                        return isSubtype(t2ExtendsBound, t1ExtendsBound);
                    } else if (t1SuperBound == null) {
                        // (a) 2.
                        return true;
                    }
                    // Note that "? super S" never contains a type argument of form "? extends T"
                    return false;
                } else if (t2SuperBound != null) {
                    if (t1SuperBound != null) {
                        // (a) 3.
                        return isSubtype(t1SuperBound, t2SuperBound);
                    } else {
                        // (a) 4. and 5.: Handle case "? super T <= ?" (always true) or "? super S <= ? extends T" (only
                        // if T is Object)
                        return t1ExtendsBound == null
                            || isSameType(t1ExtendsBound, typeMirror(Object.class));
                    }
                } else {
                    // Handle special case of (a), namely "? <= ? extends T" (only if T is Object), "? <= ? super T"
                    // (always false), or "? <= ?" (which is equivalent to "? extends Object <= ? extends Object" and
                    // therefore true).
                    return t1SuperBound == null && (
                        t1ExtendsBound == null || isSameType(t1ExtendsBound, typeMirror(Object.class))
                    );
                }
            } else {
                // Handle (c). Reduce to case (a).
                if (t1HasExtendsBound) {
                    // (c) 1.
                    return contains(t1, getWildcardType(t2, null));
                } else if (t1HasSuperBound) {
                    // (c) 2.
                    return contains(t1, getWildcardType(null, t2));
                }
                // Combining (c) 1. with (a) 2. or (c) 2. with (a) 4., we immediately have "T <= ?"
                return true;
            }
        } else {
            // Handle (b).
            return isSameType(t1, t2);
        }
    }

    /**
     * Returns the direct super types of the given type declaration, as defined by JLS §4.10.2.
     */
    private List<DeclaredType> directSupertypesOfTypeDeclaration(TypeElement typeElement) {
        TypeMirror superClass = typeElement.getSuperclass();
        List<? extends TypeMirror> interfaces = typeElement.getInterfaces();
        List<DeclaredType> newSuperTypes = new ArrayList<>(1 + interfaces.size());
        if (superClass.getKind() == TypeKind.DECLARED) {
            newSuperTypes.add((DeclaredType) superClass);
        }
        for (TypeMirror superInterface: interfaces) {
            newSuperTypes.add((DeclaredType) superInterface);
        }
        if (typeElement.getKind() == ElementKind.INTERFACE && interfaces.isEmpty()) {
            newSuperTypes.add((DeclaredType) typeMirror(Object.class));
        }
        return newSuperTypes;
    }

    /**
     * Internal class to keep state for the Dijkstra shortest-path algorithm in
     * {@link #getShortestPathToSuperType(TypeElement, DeclaredType)}.
     */
    private static final class TypeDeclarationVertexState {
        private int distance;
        private boolean visited;
        private final TypeElement typeElement;

        /**
         * The type as contained in {@code previous.typeDeclaration.getSuperTypes()}.
         */
        private final DeclaredType declaredType;

        @Nullable private TypeDeclarationVertexState previous;

        private TypeDeclarationVertexState(int distance, boolean visited, TypeElement typeElement,
                DeclaredType declaredType) {
            this.distance = distance;
            this.visited = visited;
            this.typeElement = typeElement;
            this.declaredType = declaredType;
        }

        /**
         * Returns the path induced by this node, starting with {@code derived} and ending with {@link #declaredType}.
         *
         * <p>The path is obtained by following {@link #previous} until {@code null}. Each element in the path (except
         * for the first) is the {@link #declaredType} of the current instance.
         *
         * @param derived the first element in the path
         * @return the path induced by this node
         */
        private List<DeclaredType> toPath(DeclaredType derived) {
            DeclaredType[] path = new DeclaredType[distance + 1];
            int count = path.length;
            TypeDeclarationVertexState pathElement = this;
            while (pathElement.previous != null) {
                --count;
                path[count] = pathElement.declaredType;
                pathElement = pathElement.previous;
            }
            path[0] = derived;
            return Arrays.asList(path);
        }
    }

    /**
     * Returns the shortest inheritance path between a type declaration and a subtype (starting with
     * {@code derived} and ending with {@code base}).
     *
     * <p>Each element in the returned path is a non-generic type, a raw type, or a parameterized type. The
     * {@link DeclaredType} at position {@code i} is always contained in the result of
     * {@link #directSupertypesOfTypeDeclaration(TypeElement)} applied to the type declaration of the type at position
     * {@code (i - 1)}.
     *
     * <p>This methods runs a Dijkstra shortest-path algorithm. It relies on {@link TypeElement#equals(Object)}
     * being well-defined (two object representing the same type declaration must compare equal). Consequently, if
     * {@link DeclaredType} instances have an identity, it must be guaranteed that there are no two instances
     * representing the same type declaration.
     *
     * @param base base type declaration
     * @param derived derived type
     * @return If there is an inheritance path from {@code derived} to {@code base}, then a {@code List<DeclaredType>}
     *     {@code p} such that {@code p.get(0).equals(toGenericType(derived))} and
     *     {@code toRawTypeDeclaration(p.get(p.size() - 1)).equals(base)} are {@code true}. Otherwise, {@code null} to
     *     indicate that there is no such path.
     */
    @Nullable
    private List<DeclaredType> getShortestPathToSuperType(TypeElement base, DeclaredType derived) {
        TypeElement typeElement = (TypeElement) derived.asElement();

        Set<TypeElement> boundary = new LinkedHashSet<>();
        Map<TypeElement, TypeDeclarationVertexState> dijkstraState = new HashMap<>();

        // Distance from derived to itself is 0
        dijkstraState.put(typeElement, new TypeDeclarationVertexState(0, false, typeElement, derived));
        // Start off with derived
        boundary.add(typeElement);

        // Invariants:
        // - boundary only contains nodes that have *not* been visited
        // - For all visited nodes, the shortest path is known
        while (!boundary.isEmpty()) {
            // shortest := vertex in boundary with smallest distance from typeElement
            @Nullable TypeDeclarationVertexState shortest = null;
            for (TypeElement currentDeclaration: boundary) {
                TypeDeclarationVertexState current = dijkstraState.get(currentDeclaration);
                if (shortest == null || current.distance < shortest.distance) {
                    shortest = current;
                }
            }
            // Since boundary is non-empty, shortest was assigned in the previous loop. Also note that due to the above
            // invariant, shortest has not been visited.
            assert shortest != null && !shortest.visited;

            // Terminate if we found base. Since shortest.distance is non-decreasing over the loop iterations, it is
            // impossible to find a shorter path in future iterations.
            if (shortest.typeElement.equals(base)) {
                return shortest.toPath(derived);
            }

            // Remove shortest from boundary.
            boundary.remove(shortest.typeElement);
            shortest.visited = true;

            for (DeclaredType superType: directSupertypesOfTypeDeclaration(shortest.typeElement)) {
                // A direct super type of a type declaration is either a non-generic type declaration or a raw type (in
                // both cases represented as DeclaredType with no actual type parameters) or a parameterized type
                TypeElement superDeclaration = (TypeElement) superType.asElement();
                @Nullable TypeDeclarationVertexState stats = dijkstraState.get(superDeclaration);

                if (stats == null) {
                    stats = new TypeDeclarationVertexState(Integer.MAX_VALUE, false, superDeclaration, superType);
                    dijkstraState.put(superDeclaration, stats);
                }

                int alt = shortest.distance + 1;
                if (!stats.visited && alt < stats.distance) {
                    stats.distance = alt;
                    stats.previous = shortest;
                    boundary.add(superDeclaration);
                }
            }
        }
        return null;
    }

    /**
     * Visitor of a type mirror. Returns the erasure of the visited type mirror.
     *
     * @see #erasureVisitor
     */
    private final class ErasureVisitor extends ExtendedTypeKindVisitor7<TypeMirror, Void> {
        @Override
        public TypeMirror visitDeclared(DeclaredType declaredType, @Nullable Void ignored) {
            TypeMirror originalEnclosingType = declaredType.getEnclosingType();
            @Nullable DeclaredType newEnclosingType = originalEnclosingType.getKind() == TypeKind.NONE
                ? null
                : (DeclaredType) erasure(declaredType.getEnclosingType());
            return getDeclaredType(newEnclosingType, (TypeElement) declaredType.asElement());
        }

        /**
         * Returns the array type corresponding to the erasure of the component type.
         */
        @Override
        public TypeMirror visitArray(ArrayType arrayType, @Nullable Void ignored) {
            return getArrayType(erasure(arrayType.getComponentType()));
        }

        /**
         * Returns the erasure of the leftmost bound of the given type variable.
         *
         * <p>The erasure of a type variable is the erasure of its leftmost bound (JLS §4.6). If multiple bounds are
         * present, the upper bound is modelled as an intersection type. The erasure of an intersection type is
         * guaranteed to have see right form (see {@link #visitIntersection(IntersectionType, Void)}).
         */
        @Override
        public TypeMirror visitTypeVariable(TypeVariable typeVariable, @Nullable Void ignored) {
            return erasure(typeVariable.getUpperBound());
        }

        /**
         * Returns the erasure of the leftmost member of the given intersection type.
         *
         * <p>While JLS §4.6 does not mention intersection types (and thus, strictly speaking, the erasure of an
         * intersection type should be the unmodified intersection type itself), this implementation computes the
         * erasure of an intersection type as the erasure of its left-most type.
         */
        @Override
        public TypeMirror visitIntersection(IntersectionType intersectionType, @Nullable Void ignored) {
            return erasure(intersectionType.getBounds().get(0));
        }

        /**
         * Returns the given type itself.
         *
         * <p>JLS §4.6 specifies: "The erasure of every other type is the type itself."
         */
        @Override
        protected TypeMirror defaultAction(TypeMirror type, Void ignored) {
            return type;
        }
    }

    /**
     * Returns the erasure of a type, as specified by JLS §4.6.
     *
     * @param type the type to be erased
     * @return the erasure of the given type
     * @throws IllegalArgumentException if given a package type
     */
    @Override
    public final TypeMirror erasure(TypeMirror type) {
        Objects.requireNonNull(type);
        requireValidType(type);

        return type.accept(erasureVisitor, null);
    }

    /**
     * Returns the element corresponding to a type.
     *
     * <p>The type may be a {@code DeclaredType} or {@code TypeVariable}. Returns {@code null} if the type is not one
     * with a corresponding element.
     *
     * @param type the type
     * @return the element corresponding to the given type
     */
    @Override
    public final Element asElement(TypeMirror type) {
        Objects.requireNonNull(type);
        requireValidType(type);

        if (type.getKind() == TypeKind.DECLARED) {
            return ((DeclaredType) type).asElement();
        } else if (type.getKind() == TypeKind.TYPEVAR) {
            return ((TypeVariable) type).asElement();
        } else {
            return null;
        }
    }

    /**
     * Returns whether the two given type arguments represent the same type.
     *
     * <p>If either of the arguments to this method represents a wildcard, this method will return false. As a
     * consequence, a wildcard is not the same type as itself.
     *
     * @param t1 the first type
     * @param t2 the second type
     * @return {@code true} if and only if the two types are the same
     */
    @Override
    public final boolean isSameType(TypeMirror t1, TypeMirror t2) {
        requireValidType(Objects.requireNonNull(t1));
        requireValidType(Objects.requireNonNull(t2));

        return t1.getKind() != TypeKind.WILDCARD && t1.equals(t2);
    }

    /**
     * Returns the greatest lower bound (glb) of a wildcard extends bound and an upper bound of a type parameter.
     *
     * <p>This method is only called from {@link #capture(TypeMirror)}. JLS §5.1.10 defines the greatest lower bound
     * {@code glb(V_1, ..., V_m)} as {@code V_1 & ... & V_m}. Unfortunately, the specification provides no clarity
     * whether intersection types are allowed to be nested. This implementation takes the interpretation that
     * intersection types should not be nested. Therefore, the bounds contained in {@code originalUpperBound} are
     * unwrapped.
     *
     * @param wildcardExtendsBound extends bound of the wildcard type argument
     * @param originalUpperBound original upper bound of the type parameter
     * @return the greatest lower bound
     */
    private static TypeMirror[] greatestLowerBound(TypeMirror wildcardExtendsBound, TypeMirror originalUpperBound) {
        @Nullable TypeMirror[] result = null;
        if (originalUpperBound instanceof IntersectionType) {
            IntersectionType originalIntersectionBound = (IntersectionType) originalUpperBound;
            if (originalIntersectionBound.isIntersectionType()) {
                List<? extends TypeMirror> originalBounds = originalIntersectionBound.getBounds();
                result = new TypeMirror[1 + originalBounds.size()];
                int i = 0;
                for (TypeMirror originalBound: originalBounds) {
                    ++i;
                    result[i] = originalBound;
                }
            }
        }

        if (result == null) {
            result = new TypeMirror[2];
            result[1] = originalUpperBound;
        }

        result[0] = wildcardExtendsBound;
        return result;
    }

    /**
     * Returns the caputure conversion of (just) the given wildcard argument.
     *
     * <p>This method is only called by {@link #capture(TypeMirror)}.
     */
    private TypeVariable captureWildcardArgument(WildcardType wildcardArgument, TypeParameterElement typeParameter) {
        TypeVariable originalTypeVariable = (TypeVariable) typeParameter.asType();

        // Denoted U_i in JLS 5.1.10
        TypeMirror originalUpperBound = originalTypeVariable.getUpperBound();

        // Both of the following are denoted B_i in JLS 5.1.10 (in "? extends B_i" and "? super B_i", respectively)
        @Nullable TypeMirror wildcardExtendsBound = wildcardArgument.getExtendsBound();
        @Nullable TypeMirror wildcardSuperBound = wildcardArgument.getSuperBound();

        TypeMirror newUpperBound;
        TypeMirror newLowerBound;

        // There exists a capture conversion from a parameterized type G<T_1,...,T_n> (§4.5) to a parameterized type
        // G<S_1, ..., S_n>, where, for 1 <= i <= n:
        if (wildcardExtendsBound == null && wildcardSuperBound == null) {
            // If T_i is a wildcard type argument (§4.5.1) of the form ?, then S_i is a fresh type variable whose
            // upper bound is U_i[A_1 := S_1, ..., A_n := S_n] and whose lower bound is the null type (§4.1).
            newUpperBound = originalUpperBound;
            newLowerBound = getNullType();
        } else if (wildcardSuperBound == null) {
            // If T_i is a wildcard type argument of the form ? extends B_i, then S_i is a fresh type variable whose
            // upper bound is glb(B_i, U_i[A_1 := S_1, ..., A_n := S_n]) and whose lower bound is the null type.
            //
            // glb(V_1, ..., V_m) is defined as V_1 & ... & V_m.
            // It is a compile-time error if, for any two classes (not interfaces) V_i and V_j, V_i is not a
            // subclass of V_j or vice versa.
            newUpperBound = getIntersectionType(greatestLowerBound(wildcardExtendsBound, originalUpperBound));
            newLowerBound = getNullType();
        } else {
            // If T_i is a wildcard type argument of the form ? super B_i, then S_i is a fresh type variable whose
            // upper bound is U_i[A_1 := S1, ..., A_n := S_n] and whose lower bound is B_i.
            assert wildcardExtendsBound == null;

            newUpperBound = originalUpperBound;
            newLowerBound = wildcardSuperBound;
        }

        return getTypeVariable(typeParameter, newUpperBound, newLowerBound, wildcardArgument);
    }

    /**
     * Returns the capture conversion of the given type, as specified by JLS §5.1.10.
     *
     * @param type the type to be converted
     * @return the result of applying capture conversion
     * @throws IllegalArgumentException if given an executable or package type
     */
    @Override
    public final TypeMirror capture(TypeMirror type) {
        Objects.requireNonNull(type);
        requireValidType(type);

        // JLS §5.1.10 states: "Capture conversion on any type other than a parameterized type (§4.5) acts as an
        // identity conversion (§5.1.1)."
        if (type.getKind() != TypeKind.DECLARED) {
            return type;
        }

        DeclaredType declaredType = (DeclaredType) type;
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.isEmpty()) {
            return declaredType;
        }

        TypeElement typeDeclaration = (TypeElement) declaredType.asElement();
        Iterator<? extends TypeMirror> typeArgumentIterator = typeArguments.iterator();
        Iterator<? extends TypeParameterElement> typeParameterIterator = typeDeclaration.getTypeParameters().iterator();
        TypeMirror[] newArguments = new TypeMirror[typeArguments.size()];
        Map<TypeParameterElement, TypeMirror> substitutions = new LinkedHashMap<>();
        for (int index = 0; index < newArguments.length; ++index) {
            TypeMirror typeArgument = typeArgumentIterator.next();
            TypeParameterElement typeParameter = typeParameterIterator.next();
            TypeMirror substitution;

            if (typeArgument.getKind() != TypeKind.WILDCARD) {
                newArguments[index] = typeArgument;
                substitution = typeArgument;
            } else {
                // For the intermediate declared type (see below), we need the original type variable corresponding to
                // the formal type parameter. Only original type variables will be replaced by the substitutionVisitor.
                newArguments[index] = typeParameter.asType();
                substitution = captureWildcardArgument((WildcardType) typeArgument, typeParameter);
            }
            substitutions.put(typeParameter, substitution);
        }

        TypeMirror enclosingType = declaredType.getEnclosingType();

        // Construct intermediateDeclaredType that already has type variables in its argument list instead of wildcard
        // arguments.
        DeclaredType intermediateDeclaredType;
        if (enclosingType.getKind() == TypeKind.DECLARED) {
            intermediateDeclaredType = getDeclaredType((DeclaredType) enclosingType, typeDeclaration, newArguments);
        } else {
            intermediateDeclaredType = getDeclaredType(typeDeclaration, newArguments);
        }

        return substitute(intermediateDeclaredType, substitutions);
    }

    /**
     * Returns a new type variable that corresponds to the given formal type parameter and that has the given actual
     * upper and lower bounds.
     *
     * <p>This method is primarily needed during capture conversion, in order to create a fresh type variable that
     * overrides the bounds of the formal type parameter it represents. This method is also called during substitution.
     * As an example, given a formal type parameter {@code T extends Object} and an upper bound {@code Number}, this
     * method returns the type variable {@code T} with upper bound {@code Number}.
     *
     * <p>This method is not suited for creating type variables with recursive type bounds if these bounds override the
     * bounds of the formal type parameter (as only happens during capture conversion). In order to create such a type
     * variable, this method may be used to create an interim type variable, where the (overridden) upper and lower
     * bounds should only reference the type variable returned by {@link TypeParameterElement#asType()}. As a second
     * step, {@link #substitute(TypeMirror, Map)} may then be used to substitute the original type variable with the
     * interim type variable. The result will be a fresh type variable with the overridden bounds, and these bounds
     * will reference the fresh type variable instead of the original type variable.
     *
     * @param typeParameter the formal type parameter
     * @param upperBound the upper bound for the new type variable, may contain recursive references to
     *     {@code typeParameter.asType()}
     * @param lowerBound the lower bound for the new type variable, may contain recursive references to
     *     {@code typeParameter.asType()}
     * @param capturedTypeArgument the wildcard type argument that new type variable captures as part of a capture
     *     conversion (§5.1.10 JLS), or {@code null} if the new type variable is not the result of a capture conversion
     * @return the new type variable
     * @throws NullPointerException if any of the first three arguments is null
     */
    protected TypeVariable getTypeVariable(TypeParameterElement typeParameter, TypeMirror upperBound,
            TypeMirror lowerBound, @Nullable WildcardType capturedTypeArgument) {
        Objects.requireNonNull(typeParameter);
        Objects.requireNonNull(upperBound);
        Objects.requireNonNull(lowerBound);
        requireValidType(upperBound);
        requireValidType(lowerBound);
        requireValidType(capturedTypeArgument);

        TypeVariable typeVariable = createTypeVariable(typeParameter, capturedTypeArgument);
        setTypeVariableBounds(typeVariable, upperBound, lowerBound);
        return typeVariable;
    }

    /**
     * Creates a new <em>unfinished</em> type variable for the given formal parameter.
     *
     * <p>Whenever this method is called within this class, the returned type variable is guaranteed to be passed to
     * {@link #setTypeVariableBounds(TypeVariable, TypeMirror, TypeMirror)} before being used as a {@link TypeVariable}
     * instance. That is, the returned type variable is considered to be under construction until being passed to
     * {@link #setTypeVariableBounds(TypeVariable, TypeMirror, TypeMirror)}, and only after that method call, the type
     * variable will have to satisfy the contract specified by interface {@link TypeVariable} and its super-interfaces.
     *
     * <p>Before {@link #setTypeVariableBounds(TypeVariable, TypeMirror, TypeMirror)} is called on the returned
     * {@link TypeVariable}, calling either {@link TypeVariable#getUpperBound()} or {@link TypeVariable#getLowerBound()}
     * must trigger an {@link IllegalStateException}.
     *
     * <p>Note that the previous paragraph does not guarantee that
     * {@link #setTypeVariableBounds(TypeVariable, TypeMirror, TypeMirror)} is always called on the newly returned
     * type-variable instance. If any exception occurs before the new type-variable could be used, then
     * {@link #setTypeVariableBounds(TypeVariable, TypeMirror, TypeMirror)} may not be called (even if the exception is
     * unrelated to the construction of the new type-variable instance).
     *
     * <p>The {@link TypeVariable} interface does not provide access to captured wildcard type arguments. It can be
     * retrieved by calling {@link #capturedTypeArgument(TypeVariable)} instead.
     *
     * @param typeParameter the formal type parameter
     * @param capturedTypeArgument the wildcard type argument that new type variable captures as part of a capture
     *     conversion (§5.1.10 JLS), or {@code null} if the new type variable is not the result of a capture conversion
     * @return new unfinished type variable for the given formal parameter, which may not yet satisfy the contracts
     *     of {@link TypeVariable}
     * @see #setTypeVariableBounds(TypeVariable, TypeMirror, TypeMirror)
     * @see #capturedTypeArgument(TypeVariable)
     * @throws NullPointerException if {@code typeParameter} is null
     */
    protected abstract TypeVariable createTypeVariable(TypeParameterElement typeParameter,
        @Nullable WildcardType capturedTypeArgument);

    /**
     * Sets the bounds of a type variable previously returned by
     * {@link #createTypeVariable(TypeParameterElement, WildcardType)}.
     *
     * <p>Before an (unfinished) type-variable instance returned by
     * {@link #createTypeVariable(TypeParameterElement, WildcardType)} is used by this class, this method is guaranteed
     * to be called exactly once.
     *
     * <p>Implementations that create effectively immutable {@link TypeMirror} instances may use this method to "freeze"
     * the given type-variable instance.
     *
     * @param typeVariable type variable previously returned by
     *     {@link #createTypeVariable(TypeParameterElement, WildcardType)}
     * @param upperBound Upper bound for the given type variable. If no explicit upper bound is used, a
     *     {@link DeclaredType} representing {@link Object} will be passed.
     * @param lowerBound Lower bound for the given type variable. This may a {@link NullType} instance, unless capture
     *     conversion produced a type variable with a non-trivial lower bound.
     * @see #createTypeVariable(TypeParameterElement, WildcardType)
     */
    protected abstract void setTypeVariableBounds(TypeVariable typeVariable, TypeMirror upperBound,
        TypeMirror lowerBound);

    /**
     * Returns the captured wildcard type argument of the given type variable, or null if the given type variable is not
     * the result of a capture conversion.
     *
     * <p>This method returns the wildcard type argument that was previously passed to
     * {@link #createTypeVariable(TypeParameterElement, WildcardType)}.
     *
     * @param typeVariable the type variable that may be the result of a capture conversion
     * @return the captured wildcard type argument, or null if not applicable
     */
    @Nullable
    protected abstract WildcardType capturedTypeArgument(TypeVariable typeVariable);

    /**
     * Returns a new intersection type. At least one bounds needs to be given.
     *
     * @param bounds the bounds of the new intersection type
     * @return the new intersection type
     * @throws IllegalArgumentException if the given array is empty
     */
    public abstract IntersectionType getIntersectionType(TypeMirror... bounds);

    /**
     * Visitor of {@link TypeMirror} instances that appends the {@link String} representation to the
     * {@link StringBuilder} instance passed as visitor argument.
     *
     * <p>This visitor is only used within this class and only on <em>valid</em> {@link TypeMirror} instances. Hence, it
     * can be asserted that the visitor parameter is always non-null.
     *
     * @see #requireValidType(TypeMirror)
     */
    private final class ToStringVisitor extends ExtendedTypeKindVisitor7<Void, StringBuilder> {
        @Override
        public Void visitPrimitive(PrimitiveType primitiveType, @Nullable StringBuilder stringBuilder) {
            assert stringBuilder != null;
            stringBuilder.append(primitiveType.getKind().toString().toLowerCase());
            return null;
        }

        @Override
        public Void visitNull(NullType nullType, @Nullable StringBuilder stringBuilder) {
            assert stringBuilder != null;
            stringBuilder.append("null");
            return null;
        }

        @Override
        public Void visitNoType(NoType noType, @Nullable StringBuilder stringBuilder) {
            assert stringBuilder != null;
            stringBuilder.append(noType.getKind().toString().toLowerCase());
            return null;
        }

        @Override
        public Void visitDeclared(DeclaredType declaredType, @Nullable StringBuilder stringBuilder) {
            assert stringBuilder != null;
            TypeMirror enclosingType = declaredType.getEnclosingType();
            TypeElement typeElement = (TypeElement) declaredType.asElement();
            if (enclosingType.getKind() == TypeKind.DECLARED) {
                visitDeclared((DeclaredType) enclosingType, stringBuilder);
                stringBuilder.append('.').append(typeElement.getSimpleName());
            } else {
                stringBuilder.append(typeElement.getQualifiedName());
            }

            List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            if (!typeArguments.isEmpty()) {
                stringBuilder.append('<');
                appendList(stringBuilder, typeArguments, ", ");
                stringBuilder.append('>');
            }
            return null;
        }

        @Override
        public Void visitArray(ArrayType arrayType, @Nullable StringBuilder stringBuilder) {
            assert stringBuilder != null;
            arrayType.getComponentType().accept(this, stringBuilder);
            stringBuilder.append("[]");
            return null;
        }

        @Override
        public Void visitTypeVariable(TypeVariable typeVariable, @Nullable StringBuilder stringBuilder) {
            assert stringBuilder != null;
            @Nullable WildcardType capturedTypeArgument = capturedTypeArgument(typeVariable);
            if (capturedTypeArgument != null) {
                stringBuilder.append("capture<");
                capturedTypeArgument.accept(this, stringBuilder);
                stringBuilder.append('>');
            } else {
                stringBuilder.append(typeVariable.asElement().getSimpleName());
            }
            return null;
        }

        @Override
        public Void visitWildcard(WildcardType wildcardTypeArgument, @Nullable StringBuilder stringBuilder) {
            assert stringBuilder != null;
            stringBuilder.append('?');
            @Nullable TypeMirror extendsBound = wildcardTypeArgument.getExtendsBound();
            if (extendsBound != null) {
                stringBuilder.append(" extends ");
                extendsBound.accept(this, stringBuilder);
            }
            @Nullable TypeMirror superBound = wildcardTypeArgument.getSuperBound();
            if (superBound != null) {
                stringBuilder.append(" super ");
                superBound.accept(this, stringBuilder);
            }
            return null;
        }

        @Override
        public Void visitIntersection(IntersectionType intersectionType, @Nullable StringBuilder stringBuilder) {
            assert stringBuilder != null;
            appendList(stringBuilder, intersectionType.getBounds(), " & ");
            return null;
        }

        private void appendList(StringBuilder stringBuilder, List<? extends TypeMirror> types, String glue) {
            assert !types.isEmpty();

            boolean first = true;
            for (TypeMirror type: types) {
                if (first) {
                    first = false;
                } else {
                    stringBuilder.append(glue);
                }
                type.accept(this, stringBuilder);
            }
        }
    }

    /**
     * Returns the canonical string representation of the given type.
     *
     * @param type type
     * @return canonical string representation of the given type
     */
    public final String toString(TypeMirror type) {
        requireValidType(Objects.requireNonNull(type));

        StringBuilder stringBuilder = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
        type.accept(toStringVisitor, stringBuilder);
        return stringBuilder.toString();
    }
}
