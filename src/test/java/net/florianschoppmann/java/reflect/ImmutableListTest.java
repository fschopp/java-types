package net.florianschoppmann.java.reflect;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link ImmutableList}.
 */
public class ImmutableListTest {
    @Test
    public void test() {
        List<Integer> list = Arrays.asList(1, 2);
        ImmutableList<Integer> immutableList = ImmutableList.copyOf(list);
        Assert.assertEquals(immutableList, list);
        Assert.assertSame(ImmutableList.copyOf(immutableList), immutableList);

        Assert.assertEquals(ImmutableList.emptyList(), Collections.emptyList());
        Assert.assertSame(ImmutableList.emptyList(), ImmutableList.emptyList());
    }
}
