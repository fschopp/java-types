package net.florianschoppmann.java.reflect;

import javax.lang.model.element.Parameterizable;
import java.util.List;

interface ReflectionParameterizable extends Parameterizable, ReflectionElement {
    @Override
    List<TypeParameterElementImpl> getTypeParameters();
}
