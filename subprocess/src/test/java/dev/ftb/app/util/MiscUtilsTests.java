package dev.ftb.app.util;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MiscUtilsTests {

    @Test
    public void testSplitCommand() {
        List<String> expected = ImmutableList.of("hello", "world", "i have spaces :D", "woo", "AAAAA", "asd adads sadasdasdasd", "doisgdf");
        String str = expected.stream().map(e -> e.contains(" ") ? "\"" + e + "\"" : e).collect(Collectors.joining(" "));
        assertEquals(expected, MiscUtils.splitCommand(str));

        String cmd = "    a   b   c d e   \"f g\" h  i";
        assertEquals(ImmutableList.of("a", "b", "c", "d", "e", "f g", "h", "i"), MiscUtils.splitCommand(cmd));
    }
}
