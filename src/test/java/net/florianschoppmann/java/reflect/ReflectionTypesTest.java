package net.florianschoppmann.java.reflect;

import net.florianschoppmann.java.type.AbstractTypes;
import net.florianschoppmann.java.type.AbstractTypesContract;
import net.florianschoppmann.java.type.AbstractTypesProvider;
import net.florianschoppmann.java.type.IntersectionType;
import org.testng.Assert;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.AbstractElementVisitor7;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link ReflectionTypes}.
 */
public final class ReflectionTypesTest {
    private ReflectionTypes types;

    public void setup() {
        types = ReflectionTypes.getInstance();
    }

    @Factory
    public Object[] createTests() {
        setup();

        Provider provider = new Provider();
        return new Object[] {
            new AbstractTypesContract(provider)
        };
    }

    private final class Provider implements AbstractTypesProvider {
        @Override
        public void preContract() { }

        @Override
        public AbstractTypes getTypes(Map<Class<?>, TypeElement> classTypeElementMap) {
            for (Map.Entry<Class<?>, TypeElement> entry: classTypeElementMap.entrySet()) {
                entry.setValue(types.typeElement(entry.getKey()));
            }
            return types;
        }
    }

    enum ThrowingInvocationHandler implements InvocationHandler {
        INSTANCE;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    public void requireValidElement() {
        TypeElement typeElement = (TypeElement) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{ TypeElement.class }, ThrowingInvocationHandler.INSTANCE);
        try {
            types.requireValidElement(typeElement);
            Assert.fail("Expected exception.");
        } catch (IllegalArgumentException exception) {
            Assert.assertTrue(exception.getMessage().contains(ReflectionTypes.class.getName()));
        }
    }

    @Test
    public void requireValidType() {
        TypeMirror typeMirror = (TypeMirror) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{ TypeMirror.class }, ThrowingInvocationHandler.INSTANCE);
        try {
            types.requireValidType(typeMirror);
            Assert.fail("Expected exception.");
        } catch (IllegalArgumentException exception) {
            Assert.assertTrue(exception.getMessage().contains(ReflectionTypes.class.getName()));
        }
    }

    private static class ClassElementVisitor extends AbstractElementVisitor7<Class<?>, Void> {
        private static final ClassElementVisitor INSTANCE = new ClassElementVisitor();

        @Override
        public Class<?> visitPackage(PackageElement e, @Nullable Void ignored) {
            return PackageElement.class;
        }

        @Override
        public Class<?> visitType(TypeElement e, @Nullable Void ignored) {
            return TypeElement.class;
        }

        @Override
        public Class<?> visitVariable(VariableElement e, @Nullable Void ignored) {
            return VariableElement.class;
        }

        @Override
        public Class<?> visitExecutable(ExecutableElement e, @Nullable Void ignored) {
            return ExecutableElement.class;
        }

        @Override
        public Class<?> visitTypeParameter(TypeParameterElement e, @Nullable Void ignored) {
            return TypeParameterElement.class;
        }
    }

    @Test
    public void typeElement() {
        TypeElement threadDeclaration = types.typeElement(Thread.class);
        TypeElement threadStateDeclaration = types.typeElement(Thread.State.class);
        Assert.assertEquals(
            threadStateDeclaration.getQualifiedName().toString(),
            Thread.State.class.getCanonicalName()
        );
        Assert.assertEquals(threadStateDeclaration.getKind(), ElementKind.ENUM);
        Assert.assertEquals(threadStateDeclaration.getEnclosingElement(), types.typeElement(Thread.class));
        Assert.assertEquals(threadStateDeclaration.toString(), Thread.State.class.toString());
        Assert.assertEquals(threadStateDeclaration.accept(ClassElementVisitor.INSTANCE, null), TypeElement.class);

        Assert.assertEquals(types.typeElement(Retention.class).getKind(), ElementKind.ANNOTATION_TYPE);

        try {
            threadDeclaration.getEnclosingElement();
            Assert.fail("Expected exception.");
        } catch (UnsupportedOperationException ignored) { }

        try {
            threadDeclaration.getModifiers();
            Assert.fail("Expected exception.");
        } catch (UnsupportedOperationException ignored) { }

        try {
            threadStateDeclaration.getNestingKind();
            Assert.fail("Expected exception.");
        } catch (UnsupportedOperationException ignored) { }

        try {
            types.typeElement(int.class);
            Assert.fail("Expected exception");
        } catch (IllegalArgumentException ignored) { }

        try {
            types.typeElement(Object[].class);
            Assert.fail("Expected exception");
        } catch (IllegalArgumentException ignored) { }
    }

    @Test
    public void typeParameterElement() {
        TypeParameterElement typeParameterElement = types.typeElement(List.class).getTypeParameters().get(0);
        Assert.assertEquals(typeParameterElement.getKind(), ElementKind.TYPE_PARAMETER);
        Assert.assertEquals(
            typeParameterElement.accept(ClassElementVisitor.INSTANCE, null),
            TypeParameterElement.class
        );
        Assert.assertEquals(typeParameterElement.toString(), List.class.getTypeParameters()[0].getName());

        try {
            typeParameterElement.getModifiers();
            Assert.fail("Expected exception.");
        } catch (UnsupportedOperationException ignored) { }
    }

    private static class OuterClass<T extends Serializable> {
        class InnerClass<U extends Cloneable> { }
    }

    private static class TypeToken<T> {
        private final Type javaType;

        TypeToken() {
            ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
            javaType = genericSuperclass.getActualTypeArguments()[0];
        }

        Type getJavaType() {
            return javaType;
        }
    }

    public <T extends Number> void methodWithTypeParameter(T argument) { }

    @Test
    public void typeMirror() throws NoSuchMethodException {
        // Class<?> -> PrimitiveType
        Assert.assertEquals(types.typeMirror(int.class), types.getPrimitiveType(TypeKind.INT));

        // Class<?> -> ArrayType
        Assert.assertEquals(
            types.typeMirror(Serializable[].class),
            types.getArrayType(types.getDeclaredType(types.typeElement(Serializable.class)))
        );

        // Class<?> -> DeclaredType (non-generic)
        Assert.assertEquals(types.typeMirror(Integer.class), types.getDeclaredType(types.typeElement(Integer.class)));

        // Class<?> -> DeclaredType (raw)
        DeclaredType javaTypesTestType = types.getDeclaredType(types.typeElement(ReflectionTypesTest.class));
        DeclaredType rawOuterClassType = types.getDeclaredType(javaTypesTestType, types.typeElement(OuterClass.class));
        Assert.assertEquals(
            types.typeMirror(OuterClass.InnerClass.class),
            types.getDeclaredType(rawOuterClassType, types.typeElement(OuterClass.InnerClass.class))
        );

        // ParameterizedType -> DeclaredType
        DeclaredType actualInnerClassType = (DeclaredType) types.typeMirror(
            new TypeToken<OuterClass<Integer>.InnerClass<byte[]>>() { }.getJavaType()
        );
        DeclaredType outerClassType = types.getDeclaredType(
            javaTypesTestType,
            types.typeElement(OuterClass.class),
            types.typeMirror(Integer.class)
        );
        DeclaredType innerClassType = types.getDeclaredType(
            outerClassType,
            types.typeElement(OuterClass.InnerClass.class),
            types.typeMirror(byte[].class)
        );
        Assert.assertEquals(actualInnerClassType, innerClassType);

        // GenericArrayType -> ArrayType
        DeclaredType listOfStringType
            = types.getDeclaredType(types.typeElement(List.class), types.typeMirror(String.class));
        Assert.assertEquals(
            types.typeMirror(new TypeToken<List<String>[]>() { }.getJavaType()),
            types.getArrayType(listOfStringType)
        );

        // WildcardType -> WildcardType
        WildcardType actualExtendsStringType = (WildcardType) types.typeMirror(
            ((ParameterizedType) new TypeToken<List<? extends String>>() { }.getJavaType()).getActualTypeArguments()[0]
        );
        WildcardType extendsStringType = types.getWildcardType(types.typeMirror(String.class), null);
        Assert.assertEquals(actualExtendsStringType, extendsStringType);

        WildcardType actualSuperStringType = (WildcardType) types.typeMirror(
            ((ParameterizedType) new TypeToken<List<? super String>>() { }.getJavaType()).getActualTypeArguments()[0]
        );
        WildcardType superStringType = types.getWildcardType(null, types.typeMirror(String.class));
        Assert.assertEquals(actualSuperStringType, superStringType);

        WildcardType actualWildcardType = (WildcardType) types.typeMirror(
            ((ParameterizedType) new TypeToken<List<?>>() { }.getJavaType()).getActualTypeArguments()[0]
        );
        WildcardType wildcardType = types.getWildcardType(null, null);
        Assert.assertEquals(actualWildcardType, wildcardType);

        // TypeVariable -> TypeVariable
        TypeVariable actualTypeVariable = (TypeVariable) types.typeMirror(
            ((ParameterizedType) List.class.getGenericInterfaces()[0]).getActualTypeArguments()[0]
        );
        Assert.assertEquals(actualTypeVariable, types.typeElement(List.class).getTypeParameters().get(0).asType());

        try {
            types.typeMirror(getClass().getMethod("methodWithTypeParameter", Number.class).getTypeParameters()[0]);
            Assert.fail("Expected exception.");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testToString() {
        // PrimitiveType
        for (TypeKind primitive: Arrays.asList(TypeKind.DOUBLE, TypeKind.FLOAT, TypeKind.LONG, TypeKind.INT,
                TypeKind.SHORT, TypeKind.BYTE, TypeKind.CHAR, TypeKind.BOOLEAN)) {
            PrimitiveType primitiveType = types.getPrimitiveType(primitive);
            Assert.assertEquals(types.getPrimitiveType(primitive).toString(), types.toString(primitiveType));
        }

        // DeclaredType
        DeclaredType declaredType = (DeclaredType) types.typeMirror(Integer.class);
        Assert.assertEquals(declaredType.toString(), types.toString(declaredType));

        // IntersectionType
        IntersectionType intersectionType
            = types.getIntersectionType(types.typeMirror(Serializable.class), types.typeMirror(Cloneable.class));
        Assert.assertEquals(intersectionType.toString(), types.toString(intersectionType));

        // WildcardType
        WildcardType wildcardType = types.getWildcardType(types.typeMirror(Integer.class), null);
        Assert.assertEquals(wildcardType.toString(), types.toString(wildcardType));

        // NullType
        NullType nullType = types.getNullType();
        Assert.assertEquals(nullType.toString(), types.toString(nullType));

        // NoType
        NoType noType = types.getNoType(TypeKind.VOID);
        Assert.assertEquals(noType.toString(), types.toString(noType));

        // TypeVariable
        TypeVariable typeVariable = (TypeVariable) types.typeElement(List.class).getTypeParameters().get(0).asType();
        Assert.assertEquals(typeVariable.toString(), types.toString(typeVariable));
    }

    @Test
    public void requireCondition() {
        ReflectionTypes.requireCondition(true, "Message: %s", 24);
        try {
            ReflectionTypes.requireCondition(false, "Message: %s", 24);
            Assert.fail("Expected exception.");
        } catch (IllegalStateException exception) {
            Assert.assertEquals(exception.getMessage(), "Message: 24");
        }
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testIsAssignable() {
        types.isAssignable(types.getPrimitiveType(TypeKind.INT), types.getPrimitiveType(TypeKind.LONG));
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void directSupertypes() {
        types.directSupertypes(types.getPrimitiveType(TypeKind.INT));
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void asMemberOf() {
        TypeElement listDeclaration = types.typeElement(List.class);
        types.asMemberOf(
            types.getDeclaredType(listDeclaration, types.typeMirror(String.class)),
            listDeclaration.getTypeParameters().get(0)
        );
    }
}
