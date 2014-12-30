package net.florianschoppmann.java.type;

import javax.lang.model.element.TypeElement;
import java.util.Map;

/**
 * Provider for {@link AbstractTypes} instances in contract tests.
 */
public interface AbstractTypesProvider {
    /**
     * Performs pre-contract actions.
     *
     * <p>This method is called from within an {@link org.testng.annotations.BeforeClass} annotated method. It is
     * therefore possible to throw a {@link org.testng.SkipException} in this method.
     */
    void preContract();

    /**
     * Puts {@link javax.lang.model.element.TypeElement} instances into the given map (corresponding to the
     * {@link Class} keys) and returns a {@link AbstractTypes} instance.
     *
     * <p>Implementations of this method must put a {@link javax.lang.model.element.TypeElement} instance for each
     * {@link Class} key into the given map. Implementations must not add or remove entries to/from the given map.
     *
     * @param classTypeElementMap Map with {@link Class} objects as keys. The value for each key is initially undefined
     *     and must be updated with the respective {@link javax.lang.model.element.TypeElement} upon return.
     * @return the {@link AbstractTypes} instance that will be used for the contract test
     */
    AbstractTypes getTypes(Map<Class<?>, TypeElement> classTypeElementMap);
}
