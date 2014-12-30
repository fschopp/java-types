package net.florianschoppmann.java.reflect;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class MirrorContext {
    private final ReflectionTypes reflectionTypes;
    private final Map<Class<?>, TypeElementImpl> typeDeclarations;
    private final Map<Class<?>, TypeElementImpl> newTypeDeclarations;

    MirrorContext(ReflectionTypes reflectionTypes, Map<Class<?>, TypeElementImpl> typeDeclarations,
            Map<Class<?>, TypeElementImpl> newTypeDeclarations) {
        this.reflectionTypes = reflectionTypes;
        this.typeDeclarations = typeDeclarations;
        this.newTypeDeclarations = newTypeDeclarations;
    }

    ImmutableList<ReflectionTypeMirror> mirror(Type[] types) {
        List<ReflectionTypeMirror> typeMirrors = new ArrayList<>();
        for (Type type: types) {
            typeMirrors.add(reflectionTypes.mirrorInternal(type, this));
        }
        return ImmutableList.copyOf(typeMirrors);
    }

    /**
     * Returns a type mirror for the given {@link Type} object.
     *
     * <p>This method creates a type mirror within this mirror context. This is similar, but different, to
     * {@link ReflectionTypes#typeMirror(Type)}, which starts a <em>new</em> mirror context.
     *
     * @param type type as represented by Java Reflection API
     * @return type mirror for the given reflection type
     */
    ReflectionTypeMirror mirror(Type type) {
        return reflectionTypes.mirrorInternal(type, this);
    }

    /**
     * Returns a type element for the given {@link Class} object.
     *
     * <p>This methods provides a type element that has been created within this mirror context. Within a mirror
     * context, type elements are always reused. This method is similar, but different, to
     * {@link ReflectionTypes#typeElement(Class)}, which starts a <em>new</em> mirror context.
     *
     * @param clazz class object
     * @return type element for the given class
     */
    TypeElementImpl typeDeclaration(Class<?> clazz) {
        TypeElementImpl typeDeclaration = typeDeclarations.get(clazz);
        if (typeDeclaration != null) {
            return typeDeclaration;
        }

        TypeElementImpl newTypeDeclaration = newTypeDeclarations.get(clazz);
        if (newTypeDeclaration == null) {
            newTypeDeclaration = new TypeElementImpl(clazz);
            newTypeDeclarations.put(clazz, newTypeDeclaration);
        }
        return newTypeDeclaration;
    }
}
