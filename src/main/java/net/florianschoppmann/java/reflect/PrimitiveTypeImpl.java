package net.florianschoppmann.java.reflect;

import javax.annotation.Nullable;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

final class PrimitiveTypeImpl extends AnnotatedConstructImpl implements ReflectionTypeMirror, PrimitiveType {
    static final PrimitiveTypeImpl DOUBLE = new PrimitiveTypeImpl(TypeKind.DOUBLE);
    static final PrimitiveTypeImpl FLOAT = new PrimitiveTypeImpl(TypeKind.FLOAT);
    static final PrimitiveTypeImpl LONG = new PrimitiveTypeImpl(TypeKind.LONG);
    static final PrimitiveTypeImpl INT = new PrimitiveTypeImpl(TypeKind.INT);
    static final PrimitiveTypeImpl SHORT = new PrimitiveTypeImpl(TypeKind.SHORT);
    static final PrimitiveTypeImpl BYTE = new PrimitiveTypeImpl(TypeKind.BYTE);
    static final PrimitiveTypeImpl CHAR = new PrimitiveTypeImpl(TypeKind.CHAR);
    static final PrimitiveTypeImpl BOOLEAN = new PrimitiveTypeImpl(TypeKind.BOOLEAN);

    private final TypeKind typeKind;

    PrimitiveTypeImpl(TypeKind typeKind) {
        this.typeKind = typeKind;
    }

    @Override
    public String toString() {
        return ReflectionTypes.getInstance().toString(this);
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> visitor, @Nullable P parameter) {
        return visitor.visitPrimitive(this, parameter);
    }

    @Override
    public TypeKind getKind() {
        return typeKind;
    }
}
