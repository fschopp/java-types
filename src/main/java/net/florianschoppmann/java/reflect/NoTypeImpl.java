package net.florianschoppmann.java.reflect;

import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;

final class NoTypeImpl extends AnnotatedConstructImpl implements ReflectionTypeMirror, NoType {
    static final NoTypeImpl VOID = new NoTypeImpl(TypeKind.VOID);
    static final NoTypeImpl NONE = new NoTypeImpl(TypeKind.NONE);

    private final TypeKind typeKind;

    NoTypeImpl(TypeKind typeKind) {
        this.typeKind = typeKind;
    }

    @Override
    public String toString() {
        return ReflectionTypes.getInstance().toString(this);
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> visitor, P parameter) {
        return visitor.visitNoType(this, parameter);
    }

    @Override
    public TypeKind getKind() {
        return typeKind;
    }
}
