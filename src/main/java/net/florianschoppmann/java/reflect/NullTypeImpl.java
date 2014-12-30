package net.florianschoppmann.java.reflect;

import javax.lang.model.type.NullType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

final class NullTypeImpl extends AnnotatedConstructImpl implements ReflectionTypeMirror, NullType {
    static final NullTypeImpl INSTANCE = new NullTypeImpl();

    @Override
    public String toString() {
        return ReflectionTypes.getInstance().toString(this);
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> visitor, P parameter) {
        return visitor.visitNull(this, parameter);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.NULL;
    }
}
