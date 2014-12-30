package net.florianschoppmann.java.reflect;

import javax.lang.model.element.Name;
import java.util.Objects;

final class NameImpl implements Name {
    private final String name;

    NameImpl(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        return name.equals(((NameImpl) otherObject).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean contentEquals(CharSequence charSequence) {
        return name.contentEquals(charSequence);
    }

    @Override
    public int length() {
        return name.length();
    }

    @Override
    public char charAt(int index) {
        return name.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return name.subSequence(start, end);
    }

    @Override
    public String toString() {
        return name;
    }
}
