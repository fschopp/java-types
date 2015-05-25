package net.florianschoppmann.java.reflect;

import net.florianschoppmann.java.type.AnnotatedConstruct;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import java.util.List;

interface ReflectionElement extends Element, AnnotatedConstruct {
    @Override
    ReflectionTypeMirror asType();

    @Override
    @Nullable
    ReflectionElement getEnclosingElement();

    @Override
    List<? extends ReflectionElement> getEnclosedElements();
}
