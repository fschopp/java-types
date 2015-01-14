package net.florianschoppmann.java.type;

import org.testng.Assert;
import org.testng.ITest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Contract test for {@link AbstractTypes}.
 *
 * <p>This class should be instantiated and returned by a TestNG factory method (that is, a method annotated with
 * {@link org.testng.annotations.Factory}).
 */
public final class AbstractTypesContract implements ITest {
    private final AbstractTypesProvider provider;
    private AbstractTypes types;
    private final Map<Class<?>, TypeElement> typeElementMap = new LinkedHashMap<>();

    /**
     * Constructs a new contract-test instance with the given provider.
     *
     * @param provider provider of {@link AbstractTypes} instance, must not be null
     * @throws NullPointerException if the argument is null
     */
    public AbstractTypesContract(AbstractTypesProvider provider) {
        Objects.requireNonNull(provider);

        this.provider = provider;
    }

    @Override
    public String getTestName() {
        return provider.getClass().getName();
    }

    final TypeElement element(Class<?> clazz) {
        return Objects.requireNonNull(typeElementMap.get(clazz));
    }

    private PrimitiveType primitiveType(Class<?> clazz) {
        assert clazz.isPrimitive();

        if (clazz.equals(double.class)) {
            return types.getPrimitiveType(TypeKind.DOUBLE);
        } else if (clazz.equals(float.class)) {
            return types.getPrimitiveType(TypeKind.FLOAT);
        } else if (clazz.equals(long.class)) {
            return types.getPrimitiveType(TypeKind.LONG);
        } else if (clazz.equals(int.class)) {
            return types.getPrimitiveType(TypeKind.INT);
        } else if (clazz.equals(short.class)) {
            return types.getPrimitiveType(TypeKind.SHORT);
        } else if (clazz.equals(byte.class)) {
            return types.getPrimitiveType(TypeKind.BYTE);
        } else if (clazz.equals(boolean.class)) {
            return types.getPrimitiveType(TypeKind.BOOLEAN);
        } else if (clazz.equals(char.class)) {
            return types.getPrimitiveType(TypeKind.CHAR);
        } else {
            throw new IllegalArgumentException(String.format("Unexpected primitive type: %s", clazz));
        }
    }

    /**
     * Returns the type mirror corresponding to the given {@link Class} object.
     *
     * <p>If the given class is a generic declaration, the returned type will be a raw type (by calling
     * {@link AbstractTypes#getDeclaredType(TypeElement, TypeMirror...)} with an empty list of type arguments).
     *
     * @param clazz the {@link Class} object
     * @return the type mirror
     */
    private TypeMirror type(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return primitiveType(clazz);
        } else if (clazz.isArray()) {
            return types.getArrayType(type(clazz.getComponentType()));
        } else if (typeElementMap.containsKey(clazz)) {
            // If clazz represents a generic type declaration, this method returns the raw type.
            return types.getDeclaredType(typeElementMap.get(clazz));
        }

        throw new IllegalArgumentException(String.format("Unexpected type: %s", clazz));
    }

    private interface SimpleA { }

    private interface SimpleB extends SimpleA { }

    private interface SimpleC extends SimpleB { }

    private interface SimpleParameterized<T> extends Serializable { }

    private interface ExtendsParameterized<T extends SimpleC> { }

    private interface SubExtendsParameterized<T extends SimpleC> extends ExtendsParameterized<T> { }

    private interface RawSubExtendsParameterized<T> extends ExtendsParameterized { }

    private interface DiamondA<T, U> { }

    private interface DiamondB<T> extends DiamondA<T[], Integer[]> { }

    private interface DiamondC<U> extends DiamondA<String[], U[]> { }

    private interface DiamondD extends DiamondB<String>, DiamondC<Integer> { }

    private interface SubDiamondB<T> extends DiamondB<T[]> { }

    private interface SubSubDiamondB extends SubDiamondB<Integer[]> { }

    private abstract static class IntegerListSet implements Set<List<Integer>>, Comparable<IntegerListSet> { }

    private abstract static class ImmutableIntegerListSet extends IntegerListSet { }

    private abstract static class InterdependentRecursiveBoundA<
        T extends InterdependentRecursiveBoundA<T, U>,
        U extends T
    > { }

    private abstract static class InterdependentRecursiveBoundB
            extends InterdependentRecursiveBoundA<InterdependentRecursiveBoundB, InterdependentRecursiveBoundB>
            implements Serializable {
        private static final long serialVersionUID = -4490638717891611814L;
    }

    private static class OuterClass<T extends Number> {
        private final class InnerClass<U extends List<?> & Serializable> { }
    }

    /**
     * Performs initialization steps necessary in order to run the contract tests subsequently.
     */
    @BeforeClass
    public void setup() {
        provider.preContract();

        List<Class<?>> classesNeeded = Arrays.asList(
            Object.class, Enum.class, Cloneable.class, Serializable.class, Comparable.class,
            Collection.class, List.class, ArrayList.class, Set.class,
            Number.class, Integer.class,
            String.class,
            getClass(),
            IntegerListSet.class, ImmutableIntegerListSet.class,
            DiamondA.class, DiamondB.class, DiamondC.class, DiamondD.class, SubDiamondB.class, SubSubDiamondB.class,
            SimpleA.class, SimpleB.class, SimpleC.class,
            ExtendsParameterized.class, SubExtendsParameterized.class,
            RawSubExtendsParameterized.class,
            SimpleParameterized.class,
            InterdependentRecursiveBoundA.class, InterdependentRecursiveBoundB.class,
            OuterClass.class, OuterClass.InnerClass.class
        );

        for (Class<?> clazz: classesNeeded) {
            typeElementMap.put(clazz, null);
        }

        types = provider.getTypes(typeElementMap);
    }

    /**
     * Verifies the {@link TypeElement} instances in {@link #typeElementMap}.
     *
     * <p>The following is verified:
     * <ul><li>
     *     {@link TypeElement#asType()} returns "a <i>prototypical</i> type" which is "the element's invocation on the
     *     type variables corresponding to its own formal type parameters".
     * </li><li>
     *     {@link #type(Class)} returns a raw type if the given class object represents a generic type declaration.
     * </li><li>
     *     Passing the raw type returned by {@link #type(Class)} to
     *     {@link AbstractTypes#toString(TypeMirror)} yields a string equal to the class's name.
     * </li></ul>
     */
    @Test
    public void testSetup() {
        for (Map.Entry<Class<?>, TypeElement> entry: typeElementMap.entrySet()) {
            Class<?> clazz = entry.getKey();
            TypeElement typeElement = entry.getValue();

            assertTrue(typeElement.getKind().isClass() || typeElement.getKind().isInterface());
            DeclaredType prototypicalType = (DeclaredType) typeElement.asType();
            assertEquals(typeElement.getTypeParameters().size(), prototypicalType.getTypeArguments().size());

            for (TypeMirror typeArgument: prototypicalType.getTypeArguments()) {
                assertTrue(typeArgument.getKind() == TypeKind.TYPEVAR && typeArgument instanceof TypeVariable);
            }

            DeclaredType rawType = (DeclaredType) type(clazz);
            assertTrue(rawType.getTypeArguments().isEmpty());

            assertEquals(clazz.getCanonicalName(), types.toString(rawType));
        }
    }

    /**
     * Verifies that {@link AbstractTypes#requireValidElement(javax.lang.model.element.Element)} throws a
     * {@link NullPointerException} if passed null.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void requireValidElement() {
        types.requireValidElement(null);
    }

    /**
     * Verifies that {@link AbstractTypes#requireValidType(javax.lang.model.type.TypeMirror)} does not throw an
     * exception if passed null.
     */
    @Test
    public void requireValidType() {
        types.requireValidType(null);
    }

    /**
     * Verifies that {@link AbstractTypes#requireValidTypes(javax.lang.model.type.TypeMirror[])} throws exceptions if
     * passed null or an array with null element.
     */
    @Test
    public void requireValidTypes() {
        try {
            types.requireValidTypes(null);
            Assert.fail("Expected exception.");
        } catch (NullPointerException ignored) { }

        try {
            types.requireValidTypes(new TypeMirror[] { null });
            Assert.fail("Expected exception.");
        } catch (NullPointerException ignored) { }
    }

    private static void typeMirrorAssertExpectedName(TypeMirror typeMirror, Class<?> clazz) {
        assertEquals(typeMirror.getKind(), TypeKind.DECLARED);
        TypeElement element = (TypeElement) ((DeclaredType) typeMirror).asElement();
        assertEquals(element.getQualifiedName().toString(), clazz.getName());
    }

    /**
     * Verifies {@link AbstractTypes#typeMirror(java.lang.reflect.Type)}.
     */
    @Test
    public void typeMirror() {
        typeMirrorAssertExpectedName(types.typeMirror(Object.class), Object.class);
        typeMirrorAssertExpectedName(types.typeMirror(Serializable.class), Serializable.class);
        typeMirrorAssertExpectedName(types.typeMirror(Cloneable.class), Cloneable.class);
    }

    /**
     * Verifies {@link AbstractTypes#substitute(TypeMirror, Map)}.
     */
    @Test
    public void substitute() {
        TypeElement diamondADeclaration = element(DiamondA.class);
        Map<TypeParameterElement, TypeMirror> substitutions = new LinkedHashMap<>();
        substitutions.put(diamondADeclaration.getTypeParameters().get(0), type(String.class));
        DeclaredType expectedType = types.getDeclaredType(
            (DeclaredType) type(getClass()),
            diamondADeclaration,
            type(String.class), diamondADeclaration.getTypeParameters().get(1).asType()
        );
        assertEquals(types.substitute(diamondADeclaration.asType(), substitutions), expectedType);

        WildcardType extendsStringWildcardArgument = types.getWildcardType(type(String.class), null);
        assertEquals(types.substitute(extendsStringWildcardArgument, substitutions), extendsStringWildcardArgument);
    }

    /**
     * Verifies {@link AbstractTypes#resolveActualTypeArguments(TypeElement, TypeMirror)} when the second argument is
     * a generic type, a non-generic type, or a primitive type.
     */
    @Test
    public void resolveActualTypeArguments() {
        // Verify edge case: resolving type arguments on the same type
        assertEquals(types.resolveActualTypeArguments(element(List.class), type(List.class)), Collections.emptyList());

        // listOfIntegersType now represents List<Integer>
        DeclaredType listOfIntegersType = types.getDeclaredType(element(List.class), type(Integer.class));

        // Simple test without inheritance: List and List<Integer>
        assertEquals(
            types.resolveActualTypeArguments(element(List.class), listOfIntegersType),
            Collections.singletonList(type(Integer.class))
        );

        // Now with inheritance.
        assertEquals(
            types.resolveActualTypeArguments(element(Set.class), type(IntegerListSet.class)),
            Collections.singletonList(listOfIntegersType)
        );
        assertEquals(
            types.resolveActualTypeArguments(element(Set.class), type(ImmutableIntegerListSet.class)),
            Collections.singletonList(listOfIntegersType)
        );

        // Verify that non-generic type does not have actual type arguments
        assertEquals(
            types.resolveActualTypeArguments(element(IntegerListSet.class), type(ImmutableIntegerListSet.class)),
            Collections.emptyList()
        );

        // In case there is no subtype relationship, null is returned
        assertNull(
            types.resolveActualTypeArguments(element(ImmutableIntegerListSet.class), type(IntegerListSet.class))
        );
        assertNull(types.resolveActualTypeArguments(element(List.class), type(Collection.class)));
        assertNull(types.resolveActualTypeArguments(element(List.class), type(int.class)));
    }

    /**
     * Verifies {@link AbstractTypes#resolveActualTypeArguments(TypeElement, TypeMirror)} if the second argument is a
     * raw type.
     */
    @Test
    public void resolveActualTypeArgumentsRaw() {
        // Resolving unbound type parameters (because the subclass inherits from a raw type) returns an empty list
        assertEquals(
            types.resolveActualTypeArguments(
                element(ExtendsParameterized.class),
                type(RawSubExtendsParameterized.class)
            ),
            Collections.emptyList()
        );

        // Resolving the type argument of a raw type is the same as resolving the type arguments of the corresponding
        // prototypical type of the sub-type declaration.
        assertEquals(
            types.resolveActualTypeArguments(
                element(ExtendsParameterized.class),
                type(SubExtendsParameterized.class)
            ),
            types.resolveActualTypeArguments(
                element(ExtendsParameterized.class),
                element(SubExtendsParameterized.class).asType()
            )
        );
        assertEquals(
            ((DeclaredType) element(SubExtendsParameterized.class).asType()).getTypeArguments().get(0),
            element(SubExtendsParameterized.class).getTypeParameters().get(0).asType()
        );
    }

    /**
     * Verifies {@link AbstractTypes#resolveActualTypeArguments(TypeElement, TypeMirror)} if the second argument
     * contains an actual type argument that is an array type.
     */
    @Test
    public void resolveActualTypeArgumentsArrays() {
        // declaredType now represents DiamondB<String[]>
        DeclaredType declaredType = types.getDeclaredType(element(DiamondB.class), types.getArrayType(type(String.class)));
        assertEquals(
            types.resolveActualTypeArguments(element(DiamondA.class), declaredType),
            Arrays.asList(type(String[][].class), type(Integer[].class))
        );

        assertEquals(
            types.resolveActualTypeArguments(element(DiamondA.class), type(DiamondD.class)),
            Arrays.asList(type(String[].class), type(Integer[].class))
        );

        // Test raw type SubDiamondB. The super types of a raw type are the supertypes of the prototypical type (as defined by
        // TypeElement#asType())
        TypeMirror expectedTypeArgument
            = types.getArrayType(types.getArrayType(element(SubDiamondB.class).getTypeParameters().get(0).asType()));
        assertEquals(
            types.resolveActualTypeArguments(element(DiamondA.class), type(SubDiamondB.class)),
            Arrays.asList(expectedTypeArgument, type(Integer[].class))
        );

        assertEquals(
            types.resolveActualTypeArguments(element(DiamondA.class), type(SubSubDiamondB.class)),
            Arrays.asList(type(Integer[][][].class), type(Integer[].class))
        );
    }

    /**
     * Verifies {@link AbstractTypes#isSubtype(TypeMirror, TypeMirror)} if one of the types is a primitive type.
     */
    @Test
    public void testIsSubtypePrimitive() {
        // Primitives
        assertTrue(types.isSubtype(type(double.class), type(double.class)));
        assertTrue(types.isSubtype(type(float.class), type(double.class)));
        assertTrue(types.isSubtype(type(long.class), type(double.class)));
        assertTrue(types.isSubtype(type(int.class),  type(double.class)));
        assertTrue(types.isSubtype(type(short.class),type(double.class)));
        assertTrue(types.isSubtype(type(byte.class), type(double.class)));
        assertTrue(types.isSubtype(type(char.class), type(double.class)));

        assertTrue(types.isSubtype(type(int.class), type(int.class)));
        assertTrue(types.isSubtype(type(short.class), type(int.class)));
        assertTrue(types.isSubtype(type(byte.class), type(int.class)));
        assertTrue(types.isSubtype(type(char.class), type(int.class)));

        assertTrue(types.isSubtype(type(short.class), type(short.class)));
        assertTrue(types.isSubtype(type(byte.class), type(short.class)));
        assertFalse(types.isSubtype(type(char.class), type(short.class)));

        assertFalse(types.isSubtype(type(Object.class), type(int.class)));
        assertFalse(types.isSubtype(type(int.class), type(Object.class)));
        assertFalse(types.isSubtype(type(Integer.class), type(int.class)));
        assertFalse(types.isSubtype(type(int.class), type(Integer.class)));
        assertFalse(types.isSubtype(type(int[].class), type(int.class)));
        assertFalse(types.isSubtype(type(int.class), type(int[].class)));

        // JLS-mandated classes
        assertTrue(types.isSubtype(type(Object.class), type(Object.class)));
        assertTrue(types.isSubtype(type(Serializable.class), type(Object.class)));
        assertFalse(types.isSubtype(type(Object.class), type(Serializable.class)));

        // Elementary subtyping
        assertTrue(types.isSubtype(type(Number.class), type(Object.class)));
        assertFalse(types.isSubtype(type(Object.class), type(Number.class)));
    }

    /**
     * Verifies {@link AbstractTypes#isSubtype(TypeMirror, TypeMirror)} if one of the types is the null type.
     */
    @Test
    public void testIsSubtypeNull() {
        assertFalse(types.isSubtype(types.getNullType(), type(int.class)));
        assertFalse(types.isSubtype(type(int.class), types.getNullType()));

        assertFalse(types.isSubtype(type(Object.class), types.getNullType()));
        assertTrue(types.isSubtype(types.getNullType(), type(Object.class)));
    }

    /**
     * Verifies {@link AbstractTypes#isSubtype(TypeMirror, TypeMirror)} if one of the types is an array type.
     */
    @Test
    public void testIsSubtypeArray() {
        // arrayType represents List<String>[][]
        TypeMirror arrayType
            = types.getArrayType(types.getArrayType(types.getDeclaredType(element(List.class), type(String.class))));

        assertTrue(types.isSubtype(arrayType, arrayType));
        assertTrue(types.isSubtype(arrayType, type(Serializable.class)));
        assertTrue(types.isSubtype(arrayType, type(Cloneable.class)));
        assertTrue(types.isSubtype(arrayType, type(Object.class)));
        assertTrue(types.isSubtype(arrayType, type(Object[].class)));
        assertTrue(types.isSubtype(arrayType, type(Object[][].class)));

        assertFalse(types.isSubtype(arrayType, type(Number.class)));
        assertFalse(types.isSubtype(type(Number.class), arrayType));

        assertTrue(types.isSubtype(type(int[][].class), type(int[][].class)));
        assertTrue(types.isSubtype(type(char[][].class), type(int[][].class)));
        assertFalse(types.isSubtype(type(int[][].class), type(char[][].class)));
    }

    /**
     * Verifies {@link AbstractTypes#isSubtype(TypeMirror, TypeMirror)} if one of the types is an intersection type.
     */
    @Test
    public void testIsSubtypeIntersection() {
        TypeMirror serializableAndCloneableType
            = types.getIntersectionType(type(Serializable.class), type(Cloneable.class));

        assertTrue(types.isSubtype(serializableAndCloneableType, type(Serializable.class)));
        assertTrue(types.isSubtype(serializableAndCloneableType, type(Cloneable.class)));
        assertTrue(types.isSubtype(serializableAndCloneableType, serializableAndCloneableType));
        assertFalse(types.isSubtype(type(Cloneable.class), serializableAndCloneableType));

        TypeMirror serializableCloneableListType
            = types.getIntersectionType(type(Serializable.class), type(Cloneable.class), type(List.class));

        // Mathematically, the following should be true, but a subtype relationship is not mandated by the JLS.
        // Therefore, currently we use assertFalse.
        assertFalse(types.isSubtype(serializableCloneableListType, serializableAndCloneableType));
    }

    /**
     * Verifies {@link AbstractTypes#isSubtype(TypeMirror, TypeMirror)} if one of the types is a raw type.
     */
    @Test
    public void testIsSubtypeRaw() {
        // Test that raw type DiamondB is subtype of raw type DiamondA
        assertTrue(types.isSubtype(type(DiamondB.class), type(DiamondA.class)));

        // aWildcardType: DiamondA<? extends Object[], Integer[]>
        DeclaredType aWildcardType = types.getDeclaredType(
            element(DiamondA.class),
            types.getWildcardType(type(Object[].class), null),
            type(Integer[].class)
        );
        // According to JLS §4.10.2, DiamondA<? extends Object[], Integer[]>  :>  DiamondA<T[], Integer[]>  :>  DiamondB
        assertTrue(types.isSubtype(type(DiamondB.class), aWildcardType));


        // aType: DiamondA<Object[], Integer[]>
        DeclaredType aType = types.getDeclaredType(element(DiamondA.class), type(Object[].class), type(Integer[].class));
        // Since T[] (i.e., the first type argument to DiamondB's direct superclass) is not contained by Object[] (in
        // the sense of JLS §4.5.1), the raw type DiamondB is not a subtype of DiamondA<Object[], Integer[]>
        assertFalse(types.isSubtype(type(DiamondB.class), aType));

        // List is not a subtype of List<?>. While
        // List list = (List<?>) null
        // would compile without error (even without warning), this is not because of a subtype relationship, but only
        // allowed because of JLS §5.1.9 (unchecked conversion).
        DeclaredType rawListType = (DeclaredType) type(List.class);
        DeclaredType anyListType = types.getDeclaredType(element(List.class), types.getWildcardType(null, null));
        assertFalse(types.isSubtype(rawListType, anyListType));
        assertTrue(types.isSubtype(anyListType, rawListType));

        assertTrue(types.isSubtype(type(RawSubExtendsParameterized.class), type(ExtendsParameterized.class)));
        // rawParameterizedType: RawSubExtendsParameterized<Integer>
        DeclaredType rawParameterizedType
            = types.getDeclaredType(element(RawSubExtendsParameterized.class), type(Integer.class));
        assertTrue(types.isSubtype(rawParameterizedType, type(ExtendsParameterized.class)));
        // See explanation for List test case above.
        assertFalse(
            types.isSubtype(
                rawParameterizedType,
                types.getDeclaredType(element(ExtendsParameterized.class), types.getWildcardType(null, null))
            )
        );
    }

    @Test
    public void testIsSubtypeArrayTypeParameters() {
        // aType: DiamondA<? extends Serializable, ? extends Number[]>
        DeclaredType aType = types.getDeclaredType(
            element(DiamondA.class),
            types.getWildcardType(type(Serializable.class), null),
            types.getWildcardType(type(Number[].class), null)
        );
        // bType: DiamondB<?>
        DeclaredType bType = types.getDeclaredType(element(DiamondB.class), types.getWildcardType(null, null));
        assertTrue(types.isSubtype(bType, aType));

        // a2Type: DiamondA<? extends Object[], ? extends Number[]>
        DeclaredType a2Type = types.getDeclaredType(
            element(DiamondA.class),
            types.getWildcardType(type(Object[].class), null),
            types.getWildcardType(type(Number[].class), null)
        );
        // b2Type: DiamondB<Integer>
        DeclaredType b2Type = types.getDeclaredType(element(DiamondB.class), type(Integer.class));
        assertTrue(types.isSubtype(b2Type, a2Type));

        // a3Type: DiamondA<? extends Serializable, ? extends Number[]>
        DeclaredType a3Type = types.getDeclaredType(
            element(DiamondA.class),
            types.getWildcardType(type(Serializable.class), null),
            types.getWildcardType(type(Number[].class), null)
        );
        assertTrue(types.isSubtype(type(DiamondD.class), a3Type));
    }

    @Test
    public void testIsSubtypeSimpleTypeParameters() {
        // collectionOfNumbersType: Collection<Number>
        DeclaredType collectionOfNumbersType = types.getDeclaredType(element(Collection.class), type(Number.class));

        // JLS §4.10.2, raw type is supertype
        assertTrue(types.isSubtype(collectionOfNumbersType, type(Collection.class)));

        // A generic type is not a supertype of the raw type. Assignment is allowed, though, through an unchecked
        // conversion (JLS §5.1.9), which "causes a compile-time unchecked warning".
        assertFalse(types.isSubtype(type(Collection.class), collectionOfNumbersType));

        // collectionOfIntegersType: Collection<Integer>
        DeclaredType collectionOfIntegersType = types.getDeclaredType(element(Collection.class), type(Integer.class));

        // Collection<Integer> is not a subtype of Collection<Number>
        assertFalse(types.isSubtype(collectionOfIntegersType, collectionOfNumbersType));

        // wildcardCollectionType now represents Collection<? extends Number>
        DeclaredType wildcardCollectionType = types.getDeclaredType(
            element(Collection.class),
            types.getWildcardType(type(Number.class), null)
        );
        assertTrue(types.isSubtype(collectionOfIntegersType, wildcardCollectionType));
    }

    @Test
    public void testIsSubtypeWildcards() {
        // setType1: Set<List<Integer>>
        DeclaredType setType1 = types.getDeclaredType(
            element(Set.class),
            types.getDeclaredType(element(List.class), type(Integer.class))
        );
        assertTrue(types.isSubtype(type(IntegerListSet.class), setType1));

        // setType2: Set<? extends List<? extends Integer>>
        DeclaredType setType2 = types.getDeclaredType(
            element(Set.class),
            types.getWildcardType(
                types.getDeclaredType(element(List.class), types.getWildcardType(type(Integer.class), null)),
                null
            )
        );
        assertTrue(types.isSubtype(type(IntegerListSet.class), setType2));

        // setType3: Set<? super List<? super Integer>>
        DeclaredType setType3 = types.getDeclaredType(
            element(Set.class),
            types.getWildcardType(
                null,
                types.getDeclaredType(element(List.class), types.getWildcardType(null, type(Integer.class)))
            )
        );
        // setType4: Set<Collection<? super Integer>>
        DeclaredType setType4 = types.getDeclaredType(
            element(Set.class),
            types.getDeclaredType(element(Collection.class), types.getWildcardType(null, type(Integer.class)))
        );
        assertTrue(types.isSubtype(setType4, setType3));

        // setType5: Set<Collection<? super Number>>
        DeclaredType setType5 = types.getDeclaredType(
            element(Set.class),
            types.getDeclaredType(element(Collection.class), types.getWildcardType(null, type(Number.class)))
        );
        // Note that Set<Collection<? super Number>> is not a subtype of Set<? super List<? super Integer>>. Otherwise,
        // List<? super Number> would be a subtype of Collection<? super Integer>, implying that "? super Integer"
        // contains (§4.5.1) "? super Number", implying that Number is a subtype of Integer. A contradiction.
        assertFalse(types.isSubtype(setType5, setType3));

        // Also, IntegerListSet, which implements Set<List<Integer>>, is not a subtype of
        // Set<? super List<? super Integer>>. Otherwise, List<Integer> would be contained by
        // "? super List<? super Integer>", implying that List<? super Integer> is a subtype of List<Integer>. A
        // contradiction.
        assertFalse(types.isSubtype(type(IntegerListSet.class), setType3));

        // simpleParameterizedType1: ExtendsParameterized<? extends SimpleA>
        DeclaredType superType = types.getDeclaredType(
            element(ExtendsParameterized.class),
            types.getWildcardType(type(SimpleA.class), null)
        );
        // simpleParameterizedType2: ExtendsParameterized<? extends SimpleB>
        DeclaredType subType = types.getDeclaredType(
            element(ExtendsParameterized.class),
            types.getWildcardType(type(SimpleB.class), null)
        );
        assertTrue(types.isSubtype(subType, superType));
        // ExtendsParameterized<? extends SimpleA> is a subtype of
        // ExtendsParameterized<? extends SimpleB>, even
        // though SimpleB does not "contain" (JLS §4.5.1) SimpleA. Reason: The "contains"
        // relationship is irrelevant for the test, because the supertypes of
        // ExtendsParameterized<? extends SimpleB> are exactly those that are the supertypes of its capture
        // conversion (JLS §4.10.2).
        assertTrue(types.isSubtype(superType, subType));

        // extendsParameterized1: ExtendsParameterized<? super SimpleC>
        DeclaredType extendsParameterized1 = types.getDeclaredType(
            element(ExtendsParameterized.class),
            types.getWildcardType(null, type(SimpleC.class))
        );
        assertTrue(types.isSubtype(extendsParameterized1, superType));

        // extendsParameterized2: ExtendsParameterized<SimpleC>
        DeclaredType extendsParameterized2
            = types.getDeclaredType(element(ExtendsParameterized.class), type(SimpleC.class));
        assertTrue(types.isSubtype(extendsParameterized2, superType));

        // simpleParameterizedType1: SimpleParameterized<? extends SimpleA>
        DeclaredType simpleParameterizedType1 = types.getDeclaredType(
            element(SimpleParameterized.class),
            types.getWildcardType(type(SimpleA.class), null)
        );
        // simpleParameterizedType2 now represents SimpleParameterized<? extends SimpleB>
        DeclaredType simpleParameterizedType2 = types.getDeclaredType(
            element(SimpleParameterized.class),
            types.getWildcardType(type(SimpleB.class), null)
        );
        assertTrue(types.isSubtype(simpleParameterizedType2, simpleParameterizedType1));
        // Note that this is unlike above, because the type parameter of SimpleParameterized does not have an explicit
        // lower bound.
        assertFalse(types.isSubtype(simpleParameterizedType1, simpleParameterizedType2));
    }

    /**
     * Verifies {@link AbstractTypes#capture(TypeMirror)}.
     */
    @Test
    public void capture() {
        assertEquals(types.capture(type(Integer.class)), type(Integer.class));

        DeclaredType outerClassType = types.getDeclaredType(element(OuterClass.class), type(Integer.class));
        DeclaredType arrayListOfIntegersType = types.getDeclaredType(element(ArrayList.class), type(Integer.class));
        // innerClassType: OuterClass<Integer>.InnerClass<? extends ArrayList<Integer>>
        DeclaredType innerClassType = types.getDeclaredType(
            outerClassType,
            element(OuterClass.InnerClass.class),
            types.getWildcardType(arrayListOfIntegersType, null)
        );

        DeclaredType capturedType = (DeclaredType) types.capture(innerClassType);
        TypeVariable actualTypeArgument = (TypeVariable) capturedType.getTypeArguments().get(0);

        // intersectionType = glb(ArrayList<Integer>, List<?>, Serializable)
        IntersectionType intersectionType = (IntersectionType) actualTypeArgument.getUpperBound();
        assertTrue(isSubtypeOfOneOf(arrayListOfIntegersType, intersectionType.getBounds()));

        PrimitiveType intType = types.getPrimitiveType(TypeKind.INT);
        assertTrue(types.isSameType(types.capture(intType), intType));
    }

    @Test
    public void captureSingleRecursiveBound() {
        // enumType: Enum<?>
        DeclaredType enumType = types.getDeclaredType(element(Enum.class), types.getWildcardType(null, null));

        // capture: java.lang.Enum<capture<?>>
        DeclaredType capture = (DeclaredType) types.capture(enumType);

        assertEquals(capture.getTypeArguments().size(), 1);
        TypeVariable newTypeVariable = (TypeVariable) capture.getTypeArguments().get(0);
        DeclaredType upperBound = (DeclaredType) newTypeVariable.getUpperBound();
        assertEquals(upperBound.getKind(), TypeKind.DECLARED);

        // Since Enum has a recursive type bound, upperBound must represent Enum<capture<?>> as well!
        assertEquals(capture, upperBound);

        // The following should be implied, but explicit test does not hurt
        TypeElement upperBoundAsElement = (TypeElement) upperBound.asElement();
        assertTrue(upperBoundAsElement.getQualifiedName().contentEquals(Enum.class.getName()));
    }

    @Test
    public void captureInterdependentRecursiveBound() {
        // aType1: InterdependentRecursiveBoundA<?, ?>
        DeclaredType aType1 = types.getDeclaredType(
            element(InterdependentRecursiveBoundA.class),
            types.getWildcardType(null, null),
            types.getWildcardType(null, null)
        );
        DeclaredType aCapture1 = (DeclaredType) types.capture(aType1);
        TypeVariable captureForT1 = (TypeVariable) aCapture1.getTypeArguments().get(0);
        TypeVariable captureForU1 = (TypeVariable) aCapture1.getTypeArguments().get(1);

        DeclaredType captureForTUpperBound1 = (DeclaredType) captureForT1.getUpperBound();
        assertEquals(captureForTUpperBound1.getTypeArguments().get(0), captureForT1);
        assertEquals(captureForTUpperBound1.getTypeArguments().get(1), captureForU1);
        assertEquals(captureForU1.getUpperBound(), captureForT1);


        // aType2: InterdependentRecursiveBoundA<? super InterdependentRecursiveBoundB, ? extends Serializable>
        DeclaredType aType2 = types.getDeclaredType(
            element(InterdependentRecursiveBoundA.class),
            types.getWildcardType(null, type(InterdependentRecursiveBoundB.class)),
            types.getWildcardType(type(Serializable.class), null)
        );
        DeclaredType aCapture2 = (DeclaredType) types.capture(aType2);
        assertEquals(aCapture2.getTypeArguments().size(), 2);

        TypeVariable captureForT2 = (TypeVariable) aCapture2.getTypeArguments().get(0);
        TypeVariable captureForU2 = (TypeVariable) aCapture2.getTypeArguments().get(1);

        DeclaredType captureForTUpperBound2 = (DeclaredType) captureForT2.getUpperBound();
        assertEquals(captureForTUpperBound2.getTypeArguments().get(0), captureForT2);
        assertEquals(captureForTUpperBound2.getTypeArguments().get(1), captureForU2);
        DeclaredType captureForTLowerBound2 = (DeclaredType) captureForT2.getLowerBound();
        assertEquals(captureForTLowerBound2, type(InterdependentRecursiveBoundB.class));

        IntersectionType intersectionType = (IntersectionType) captureForU2.getUpperBound();
        assertTrue(intersectionType.getBounds().contains(captureForT2));
        assertTrue(intersectionType.getBounds().contains(type(Serializable.class)));

        // aType3: InterdependentRecursiveBoundA<InterdependentRecursiveBoundB, ?>
        DeclaredType aType3 = types.getDeclaredType(
            element(InterdependentRecursiveBoundA.class),
            type(InterdependentRecursiveBoundB.class),
            types.getWildcardType(null, null)
        );
        DeclaredType aCapture3 = (DeclaredType) types.capture(aType3);
        assertEquals(aCapture3.getTypeArguments().get(0), type(InterdependentRecursiveBoundB.class));
        TypeVariable captureForU3 = (TypeVariable) aCapture3.getTypeArguments().get(1);
        assertEquals(captureForU3.getUpperBound(), type(InterdependentRecursiveBoundB.class));
    }

    /**
     * Verifies that {@link AbstractTypes#contains(TypeMirror, TypeMirror)} throws expected exceptions.
     */
    @Test
    public void testContainsInvalidArguments() {
        try {
            types.contains(type(Object.class), null);
            Assert.fail("Exception expected.");
        } catch (NullPointerException ignore) { }

        try {
            types.contains(null, type(Object.class));
            Assert.fail("Exception expected.");
        } catch (NullPointerException ignore) { }

        try {
            types.contains(null, null);
            Assert.fail("Exception expected.");
        } catch (NullPointerException ignore) { }
    }

    /**
     * Verifies {@link AbstractTypes#contains(TypeMirror, TypeMirror)}.
     */
    @Test
    public void testContains() {
        WildcardType extendsWildcard = types.getWildcardType(null, null);
        WildcardType extendsObjectWildcard = types.getWildcardType(type(Object.class), null);
        WildcardType extendsIntegerWildcard = types.getWildcardType(type(Integer.class), null);
        WildcardType extendsNumberWildcard = types.getWildcardType(type(Number.class), null);
        WildcardType superIntegerWildcard = types.getWildcardType(null, type(Integer.class));
        WildcardType superNumberWildcard = types.getWildcardType(null, type(Number.class));

        // ? extends T <= ? extends S if T <: S
        assertTrue(types.contains(extendsNumberWildcard, extendsIntegerWildcard));
        assertFalse(types.contains(extendsIntegerWildcard, extendsNumberWildcard));

        // ? extends T <= ?
        assertTrue(types.contains(extendsWildcard, extendsIntegerWildcard));
        assertFalse(types.contains(extendsIntegerWildcard, extendsWildcard));

        // ? super T <= ? super S if S <: T
        assertTrue(types.contains(superIntegerWildcard, superNumberWildcard));
        assertFalse(types.contains(superNumberWildcard, superIntegerWildcard));

        // ? super T <= ?
        assertTrue(types.contains(extendsWildcard, superIntegerWildcard));
        assertFalse(types.contains(superIntegerWildcard, extendsWildcard));

        // ? super T <= ? extends Object
        assertTrue(types.contains(extendsObjectWildcard, superIntegerWildcard));
        assertFalse(types.contains(superIntegerWildcard, extendsObjectWildcard));
        assertFalse(types.contains(extendsNumberWildcard, superIntegerWildcard));

        // T <= T
        assertTrue(types.contains(type(Integer.class), type(Integer.class)));
        assertFalse(types.contains(type(Number.class), type(Integer.class)));
        assertFalse(types.contains(type(Integer.class), type(Number.class)));

        // T <= ? extends T
        assertTrue(types.contains(extendsIntegerWildcard, type(Integer.class)));
        assertFalse(types.contains(type(Integer.class), extendsIntegerWildcard));

        // T <= ? super T
        assertTrue(types.contains(superIntegerWildcard, type(Integer.class)));
        assertFalse(types.contains(type(Integer.class), superIntegerWildcard));

        // Verify transitive closure

        // T <= ?
        assertTrue(types.contains(extendsWildcard, type(Integer.class)));
        assertFalse(types.contains(type(Integer.class), extendsWildcard));

        // T <= ? extends Object
        assertTrue(types.contains(extendsObjectWildcard, type(Integer.class)));
        assertFalse(types.contains(type(Integer.class), extendsObjectWildcard));

        // ? <= ? super T
        assertFalse(types.contains(superNumberWildcard, extendsWildcard));

        // ? [extends Object] <= ? [extends Object]
        assertTrue(types.contains(extendsObjectWildcard, extendsObjectWildcard));
        assertTrue(types.contains(extendsObjectWildcard, extendsWildcard));
        assertTrue(types.contains(extendsWildcard, extendsWildcard));
        assertTrue(types.contains(extendsWildcard, extendsObjectWildcard));

        assertFalse(types.contains(extendsNumberWildcard, extendsObjectWildcard));
        assertFalse(types.contains(extendsNumberWildcard, extendsWildcard));
    }

    /**
     * Verifies that {@link AbstractTypes#erasure(TypeMirror)} throws expected exceptions.
     */
    @Test
    public void testErasureInvalidArguments() {
        try {
            types.erasure(null);
            Assert.fail("Exception expected.");
        } catch (NullPointerException ignore) { }
    }

    /**
     * Verifies {@link AbstractTypes#erasure(TypeMirror)}.
     */
    @Test
    public void testErasure() {
        // Parameterized type
        DeclaredType listOfStringsType = types.getDeclaredType(element(List.class), type(String.class));
        assertTrue(types.isSameType(types.erasure(listOfStringsType), type(List.class)));

        // Nested type
        DeclaredType outerClassType = types.getDeclaredType(element(OuterClass.class), type(Integer.class));
        DeclaredType arrayListOfIntegersType = types.getDeclaredType(element(ArrayList.class), type(Integer.class));
        DeclaredType innerClassType
            = types.getDeclaredType(outerClassType, element(OuterClass.InnerClass.class), arrayListOfIntegersType);

        DeclaredType expectedErasedNestedType
            = types.getDeclaredType((DeclaredType) type(OuterClass.class), element(OuterClass.InnerClass.class));
        assertTrue(types.isSameType(types.erasure(innerClassType), expectedErasedNestedType));

        // Array type
        TypeMirror arrayType = types.getArrayType(listOfStringsType);
        assertTrue(types.isSameType(types.erasure(arrayType), types.getArrayType(type(List.class))));

        // Type variable
        TypeElement listDeclaration = element(List.class);
        TypeVariable simpleTypeVariable = types.createTypeVariable(listDeclaration.getTypeParameters().get(0), null);
        types.setTypeVariableBounds(simpleTypeVariable, type(Number.class), types.getNullType());
        assertTrue(types.isSameType(types.erasure(simpleTypeVariable), type(Number.class)));

        TypeVariable multiBoundTypeVariable
            = types.createTypeVariable(listDeclaration.getTypeParameters().get(0), null);
        types.setTypeVariableBounds(multiBoundTypeVariable,
            types.getIntersectionType(type(List.class), type(Serializable.class)), types.getNullType());
        assertTrue(types.isSameType(types.erasure(multiBoundTypeVariable), type(List.class)));

        // Every other type
        PrimitiveType booleanType = types.getPrimitiveType(TypeKind.BOOLEAN);
        assertTrue(types.isSameType(types.erasure(booleanType), booleanType));
        assertTrue(types.isSameType(types.erasure(types.getNullType()), types.getNullType()));
    }

    /**
     * Verifies that {@link AbstractTypes#asElement(TypeMirror)} throws expected exceptions.
     */
    @Test
    public void asElementInvalidArguments() {
        try {
            types.asElement(null);
            Assert.fail("Exception expected.");
        } catch (NullPointerException ignore) { }
    }

    /**
     * Verifies {@link AbstractTypes#asElement(TypeMirror)}.
     */
    @Test
    public void asElement() {
        assertEquals(types.asElement(type(List.class)), element(List.class));
        assertEquals(types.asElement(type(Integer.class)), element(Integer.class));

        TypeElement listDeclaration = element(List.class);
        TypeVariable simpleTypeVariable = types.createTypeVariable(listDeclaration.getTypeParameters().get(0), null);
        types.setTypeVariableBounds(simpleTypeVariable, type(Number.class), types.getNullType());
        assertEquals(types.asElement(simpleTypeVariable), listDeclaration.getTypeParameters().get(0));

        assertNull(types.asElement(types.getPrimitiveType(TypeKind.INT)));
    }

    /**
     * Verifies {@link javax.lang.model.element.TypeElement#asType()}.
     */
    @Test
    public void asType() {
        DeclaredType typesContractType = (DeclaredType) type(getClass());
        TypeElement outerClassDeclaration = element(OuterClass.class);
        TypeElement innerClassDeclaration = element(OuterClass.InnerClass.class);

        DeclaredType outerClassType= types.getDeclaredType(
            typesContractType, outerClassDeclaration, outerClassDeclaration.getTypeParameters().get(0).asType());
        DeclaredType innerClassType = types.getDeclaredType(
            outerClassType, innerClassDeclaration, innerClassDeclaration.getTypeParameters().get(0).asType());
        assertEquals(innerClassDeclaration.asType(), innerClassType);
    }

    /**
     * Verifies that {@link AbstractTypes#isSameType(TypeMirror, TypeMirror)} throws expected exceptions.
     */
    @Test
    public void testIsSameTypeInvalidArguments() {
        try {
            types.isSameType(null, type(Object.class));
            Assert.fail("Exception expected.");
        } catch (NullPointerException ignore) { }

        try {
            types.isSameType(type(Object.class), null);
            Assert.fail("Exception expected.");
        } catch (NullPointerException ignore) { }

        try {
            types.isSameType(null, null);
            Assert.fail("Exception expected.");
        } catch (NullPointerException ignore) { }
    }

    /**
     * Verifies {@link AbstractTypes#isSameType(TypeMirror, TypeMirror)}.
     */
    @Test
    public void testIsSameType() {
        assertTrue(types.isSameType(type(Object.class), type(Object.class)));
        assertFalse(types.isSameType(type(Object.class), type(Number.class)));

        WildcardType wildcardType = types.getWildcardType(null, null);
        assertFalse(types.isSameType(wildcardType, wildcardType));
    }

    private boolean isSubtypeOfOneOf(TypeMirror subtype, List<? extends TypeMirror> supertypes) {
        for (TypeMirror supertype: supertypes) {
            if (types.isSubtype(subtype, supertype)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifies
     * {@link AbstractTypes#getTypeVariable(javax.lang.model.element.TypeParameterElement, TypeMirror, TypeMirror, WildcardType)}.
     */
    @Test
    public void testGetTypeVariable() {
        TypeElement listDeclaration = element(List.class);
        TypeParameterElement elementTypeParameter = listDeclaration.getTypeParameters().get(0);

        TypeVariable typeVariable
            = types.getTypeVariable(elementTypeParameter, type(Integer.class), types.getNullType(), null);
        assertEquals(typeVariable.asElement(), elementTypeParameter);
        assertTrue(types.isSameType(typeVariable.getUpperBound(), type(Integer.class)));
        assertTrue(types.isSameType(typeVariable.getLowerBound(), types.getNullType()));
        assertNull(types.capturedTypeArgument(typeVariable));
    }

    /**
     * Verifies {@link AbstractTypes#createTypeVariable(TypeParameterElement, WildcardType)}.
     */
    @Test
    public void createTypeVariable() {
        TypeElement listDeclaration = element(List.class);
        TypeParameterElement elementTypeParameter = listDeclaration.getTypeParameters().get(0);

        TypeVariable typeVariable = types.createTypeVariable(elementTypeParameter, null);
        try {
            typeVariable.getUpperBound();
            Assert.fail("Expected exception.");
        } catch (IllegalStateException ignored) { }

        try {
            typeVariable.getLowerBound();
            Assert.fail("Expected exception.");
        } catch (IllegalStateException ignored) { }
    }

    /**
     * Verifies {@link AbstractTypes#setTypeVariableBounds(TypeVariable, TypeMirror, TypeMirror)}.
     */
    @Test
    public void testSetTypeVariableBounds() {
        TypeElement listDeclaration = element(List.class);
        TypeParameterElement elementTypeParameter = listDeclaration.getTypeParameters().get(0);

        TypeVariable typeVariable = types.createTypeVariable(elementTypeParameter, null);
        types.setTypeVariableBounds(typeVariable, type(Integer.class), types.getNullType());
        assertEquals(typeVariable.asElement(), elementTypeParameter);
        assertTrue(types.isSameType(typeVariable.getUpperBound(), type(Integer.class)));
        assertTrue(types.isSameType(typeVariable.getLowerBound(), types.getNullType()));
        assertNull(types.capturedTypeArgument(typeVariable));

        try {
            types.setTypeVariableBounds(typeVariable, type(Integer.class), types.getNullType());
            Assert.fail("Expected exception.");
        } catch (IllegalStateException ignored) { }
    }

    @Test
    public void testGetIntersectionType() {
        IntersectionType intersectionType = types.getIntersectionType(type(Cloneable.class), type(Serializable.class));
        assertTrue(isSubtypeOfOneOf(type(Serializable.class), intersectionType.getBounds()));

        try {
            types.getIntersectionType();
            Assert.fail("Expected exception.");
        } catch (IllegalArgumentException ignored) { }

        try {
            types.getIntersectionType(type(Cloneable.class), null);
            Assert.fail("Expected exception.");
        } catch (NullPointerException ignored) { }

        try {
            types.getIntersectionType(null, type(Cloneable.class));
            Assert.fail("Expected exception.");
        } catch (NullPointerException ignored) { }
    }

    @Test
    public void testToString() {
        // PrimitiveType
        for (TypeKind primitive: Arrays.asList(TypeKind.DOUBLE, TypeKind.FLOAT, TypeKind.LONG, TypeKind.INT,
                TypeKind.SHORT, TypeKind.BYTE, TypeKind.CHAR, TypeKind.BOOLEAN)) {
            assertEquals(types.toString(types.getPrimitiveType(primitive)), primitive.toString().toLowerCase());
        }

        // NullType
        assertEquals(types.toString(types.getNullType()), "null");

        // NoType
        for (TypeKind noType: Arrays.asList(TypeKind.VOID, TypeKind.NONE)) {
            assertEquals(types.toString(types.getNoType(noType)), noType.toString().toLowerCase());
        }

        // DeclaredType
        DeclaredType outerClassType = types.getDeclaredType(element(OuterClass.class), type(Integer.class));
        DeclaredType arrayListOfIntegersType = types.getDeclaredType(element(ArrayList.class), type(Integer.class));
        // innerClassType: OuterClass<Integer>.InnerClass<? extends ArrayList<Integer>>
        DeclaredType innerClassType = types.getDeclaredType(
            outerClassType,
            element(OuterClass.InnerClass.class),
            types.getWildcardType(arrayListOfIntegersType, null)
        );
        assertEquals(types.toString(innerClassType), String.format("%s<%s>.%s<? extends %s<%s>>",
            OuterClass.class.getCanonicalName(), Integer.class.getCanonicalName(),
            OuterClass.InnerClass.class.getSimpleName(),
            ArrayList.class.getCanonicalName(), Integer.class.getCanonicalName()));

        // ArrayType
        assertEquals(types.toString(types.getArrayType(type(Integer.class))), Integer[].class.getCanonicalName());

        // TypeVariable
        TypeVariable typeVariable = (TypeVariable) element(List.class).getTypeParameters().get(0).asType();
        assertEquals(types.toString(typeVariable), List.class.getTypeParameters()[0].getName());
        // listOfNumbersType: List<? extends Number>
        DeclaredType listOfNumbersType
            = types.getDeclaredType(element(List.class), types.getWildcardType(type(Number.class), null));
        TypeVariable capturedTypeVariable
            = (TypeVariable) ((DeclaredType) types.capture(listOfNumbersType)).getTypeArguments().get(0);
        assertEquals(types.toString(capturedTypeVariable),
            String.format("capture<? extends %s>", Number.class.getCanonicalName()));

        // WildcardType
        WildcardType wildcardArgument = types.getWildcardType(null, type(Integer.class));
        assertEquals(types.toString(wildcardArgument), String.format("? super %s", Integer.class.getCanonicalName()));

        // IntersectionType
        IntersectionType intersectionType = types.getIntersectionType(type(Cloneable.class), type(Serializable.class));
        assertEquals(types.toString(intersectionType),
            String.format("%s & %s", Cloneable.class.getCanonicalName(), Serializable.class.getCanonicalName()));
    }

    /**
     * Verifies that {@link AbstractTypes#unboxedType(TypeMirror)} throws expected exceptions.
     */
    @Test
    public void unboxedType() {
        try {
            types.unboxedType(type(List.class));
            Assert.fail("Expected exception.");
        } catch (IllegalArgumentException ignored) { }

        try {
            types.unboxedType(types.getNullType());
            Assert.fail("Expected exception.");
        } catch (IllegalArgumentException ignored) { }
    }

    /**
     * Verifies that {@link AbstractTypes#boxedClass(PrimitiveType)} throws expected exceptions.
     */
    @Test
    public void boxedClass() {
        try {
            types.boxedClass(null);
            Assert.fail("Expected exception.");
        } catch (NullPointerException ignored) { }
    }

    /**
     * Verifies that {@link AbstractTypes#unboxedType(TypeMirror)} and {@link AbstractTypes#boxedClass(PrimitiveType)}
     * are (essentially) inverse methods of each other.
     */
    @Test
    public void boxingAndUnboxing() {
        for (TypeKind primitive: Arrays.asList(TypeKind.DOUBLE, TypeKind.FLOAT, TypeKind.LONG, TypeKind.INT,
                TypeKind.SHORT, TypeKind.BYTE, TypeKind.CHAR, TypeKind.BOOLEAN)) {
            PrimitiveType primitiveType = types.getPrimitiveType(primitive);
            TypeElement boxedClass = types.boxedClass(primitiveType);
            PrimitiveType unboxedType = types.unboxedType(boxedClass.asType());

            assertEquals(unboxedType, primitiveType);
            assertEquals(unboxedType.getKind(), primitive);
        }
    }

    /**
     * Verifies that {@link AbstractTypes#getPrimitiveType(TypeKind)} throws expected exceptions.
     */
    @Test
    public void testGetPrimitiveType() {
        try {
            types.getPrimitiveType(null);
            Assert.fail("Expected exception.");
        } catch (NullPointerException ignored) { }

        try {
            types.getPrimitiveType(TypeKind.ARRAY);
            Assert.fail("Expected exception.");
        } catch (IllegalArgumentException ignored) { }
    }

    @Test
    public void testGetNoType() {
        for (TypeKind noTypeKind: Arrays.asList(TypeKind.VOID, TypeKind.NONE)) {
            assertEquals(types.getNoType(noTypeKind).getKind(), noTypeKind);
        }

        try {
            types.getNoType(null);
            Assert.fail("Expected exception.");
        } catch (NullPointerException ignored) { }

        try {
            types.getNoType(TypeKind.PACKAGE);
            Assert.fail("Expected exception.");
        } catch (IllegalArgumentException ignored) { }
    }

    @Test
    public void testGetNullType() {
        assertEquals(types.getNullType().getKind(), TypeKind.NULL);
    }

    private static void testEqualsAndHashCode(Object first, Object second) {
        assertTrue(first.equals(first));
        assertTrue(second.equals(second));
        assertTrue(first.equals(second));
        assertTrue(second.equals(first));

        assertFalse(first.equals(null));
        assertFalse(first.equals(new Object()));

        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void testEqualsAndHashCode() {
        testEqualsAndHashCode(types.getArrayType(type(Integer.class)), types.getArrayType(type(Integer.class)));
        testEqualsAndHashCode(types.getPrimitiveType(TypeKind.INT), types.getPrimitiveType(TypeKind.INT));
        TypeElement integerDeclaration = element(Integer.class);
        testEqualsAndHashCode(types.getDeclaredType(integerDeclaration), types.getDeclaredType(integerDeclaration));
        testEqualsAndHashCode(
            types.getIntersectionType(type(Serializable.class), type(Cloneable.class)),
            types.getIntersectionType(type(Serializable.class), type(Cloneable.class))
        );
        testEqualsAndHashCode(
            types.getWildcardType(type(Integer.class), null),
            types.getWildcardType(type(Integer.class), null)
        );
        testEqualsAndHashCode(types.getNullType(), types.getNullType());
        testEqualsAndHashCode(types.getNoType(TypeKind.VOID), types.getNoType(TypeKind.VOID));
        TypeElement listDeclaration = element(List.class);
        TypeParameterElement listTypeParameter = listDeclaration.getTypeParameters().get(0);
        TypeVariable listTypeArgument = (TypeVariable) listTypeParameter.asType();
        testEqualsAndHashCode(
            listTypeArgument,
            types.getTypeVariable(listTypeParameter, listTypeArgument.getUpperBound(), listTypeArgument.getLowerBound(),
                null)
        );

        testEqualsAndHashCode(element(Serializable.class), element(Serializable.class));
        testEqualsAndHashCode(listTypeParameter, listTypeParameter);
    }

    @Test
    public void typeElementTest() {
        TypeElement outerClassDeclaration = element(OuterClass.class);
        TypeElement innerClassDeclaration = element(OuterClass.InnerClass.class);
        assertEquals(innerClassDeclaration.getEnclosingElement(), outerClassDeclaration);
        assertTrue(outerClassDeclaration.getEnclosedElements().contains(innerClassDeclaration));
        assertTrue(outerClassDeclaration.getEnclosedElements().containsAll(outerClassDeclaration.getTypeParameters()));

        TypeElement integerDeclaration = element(Integer.class);
        assertEquals(integerDeclaration.getSuperclass(), type(Number.class));
        DeclaredType integerComparableType = types.getDeclaredType(element(Comparable.class), type(Integer.class));
        assertEquals(integerDeclaration.getInterfaces(), Collections.singletonList(integerComparableType));
        assertEquals(integerDeclaration.getQualifiedName().toString(), Integer.class.getName());
        assertEquals(integerDeclaration.getSimpleName().toString(), Integer.class.getSimpleName());
        assertEquals(integerDeclaration.asType(), type(Integer.class));

        // Support for methods not strictly necessary for type-system operations is optional (for instance,
        // getKind() or getNestingKind())
    }

    @Test
    public void typeParameterElementTest() {
        TypeElement outerClassDeclaration = element(OuterClass.class);
        TypeParameterElement outerClassTypeParameter = outerClassDeclaration.getTypeParameters().get(0);
        assertEquals(outerClassTypeParameter.getEnclosingElement(), outerClassDeclaration);
        assertEquals(outerClassTypeParameter.getEnclosedElements(), Collections.emptyList());
        assertEquals(
            outerClassTypeParameter.asType(),
            types.getTypeVariable(outerClassTypeParameter, type(Number.class), types.getNullType(), null)
        );
        assertEquals(outerClassTypeParameter.getBounds(), Collections.singletonList(type(Number.class)));
        assertEquals(
            outerClassTypeParameter.getSimpleName().toString(),
            OuterClass.class.getTypeParameters()[0].getName()
        );
        assertEquals(outerClassTypeParameter.getGenericElement(), outerClassTypeParameter.getEnclosingElement());
    }
}
