package net.florianschoppmann.java.reflect;

import java.util.AbstractList;
import java.util.List;

final class ImmutableList<E> extends AbstractList<E> {
    private static final ImmutableList<?> EMPTY_LIST = new ImmutableList<>(new Object[0]);

    private final Object[] array;

    private ImmutableList(Object[] array) {
        assert array != null;

        this.array = array;
    }

    static <E> ImmutableList<E> copyOf(List<E> original) {
        if (original instanceof ImmutableList<?>) {
            return (ImmutableList<E>) original;
        } else {
            return new ImmutableList<>(original.toArray(new Object[original.size()]));
        }
    }

    @SuppressWarnings("unchecked")
    static <E> ImmutableList<E> emptyList() {
        return (ImmutableList<E>) EMPTY_LIST;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E get(int index) {
        return (E) array[index];
    }

    @Override
    public int size() {
        return array.length;
    }
}
