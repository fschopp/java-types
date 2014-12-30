package net.florianschoppmann.java.reflect;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;
import java.util.Objects;

final class WildcardTypeImpl extends AnnotatedConstructImpl implements ReflectionTypeMirror, WildcardType {
    /**
     * Upper bound of this wildcard. If no upper bound is explicitly declared, this field contains {@code null}.
     */
    private final ReflectionTypeMirror extendsBound;

    /**
     * Lower bound of this wildcard. If no lower bound is explicitly declared, this field contains {@code null}.
     */
    private final ReflectionTypeMirror superBound;

    WildcardTypeImpl(ReflectionTypeMirror extendsBound, ReflectionTypeMirror superBound) {
        this.extendsBound = extendsBound;
        this.superBound = superBound;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        WildcardTypeImpl other = (WildcardTypeImpl) otherObject;
        return Objects.equals(extendsBound, other.extendsBound)
            && Objects.equals(superBound, other.superBound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(extendsBound, superBound);
    }

    @Override
    public String toString() {
        return ReflectionTypes.getInstance().toString(this);
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> visitor, P parameter) {
        return visitor.visitWildcard(this, parameter);
    }

    @Override
    public ReflectionTypeMirror getExtendsBound() {
        return extendsBound;
    }

    @Override
    public ReflectionTypeMirror getSuperBound() {
        return superBound;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.WILDCARD;
    }
}
