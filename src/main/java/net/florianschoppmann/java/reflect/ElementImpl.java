package net.florianschoppmann.java.reflect;

import javax.lang.model.element.Modifier;
import java.util.Set;

abstract class ElementImpl extends AnnotatedConstructImpl implements ReflectionElement {
    private boolean finished = false;

    @Override
    public final Set<Modifier> getModifiers() {
        throw new UnsupportedOperationException(String.format(
            "Modifiers not currently supported by %s.", ReflectionTypes.class
        ));
    }

    final void requireFinished() {
        if (!finished) {
            throw new IllegalStateException(String.format("Instance of %s used before it was ready.", getClass()));
        }
    }

    abstract void finishDerivedFromElement(MirrorContext mirrorContext);

    final void finish(MirrorContext mirrorContext) {
        if (finished) {
            throw new IllegalStateException(String.format(
                "Attempt to finish instance of %s more than once.", getClass()
            ));
        }

        finished = true;
        finishDerivedFromElement(mirrorContext);
    }
}
