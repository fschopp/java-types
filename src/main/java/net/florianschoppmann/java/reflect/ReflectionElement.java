package net.florianschoppmann.java.reflect;

import net.florianschoppmann.java.type.AnnotatedConstruct;

import javax.lang.model.element.Element;
import java.util.List;

interface ReflectionElement extends Element, AnnotatedConstruct {
    @Override
    ReflectionTypeMirror asType();

    @Override
    ReflectionElement getEnclosingElement();

    @Override
    List<? extends ReflectionElement> getEnclosedElements();
}
