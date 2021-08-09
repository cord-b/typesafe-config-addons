package dev.viskar.typesafe.config.spring;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import dev.viskar.typesafe.config.spring.internal.ConfigVisitor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class SpringConfigUtils {

    /**
     * Flatten a Config's paths into a flat Map that is compatible with Spring.
     * The returned values are the 'unwrapped' raw values (Lists, Strings, Numbers, Booleans, etc)
     * <p>
     * Individual List elements are also emitted using Spring's indexing syntax, such as foo[0], foo[1], ...
     * <p>
     * {@link ConfigObject} paths are not present in the flat map, only their nested values.
     * This is because Spring's PropertyMapper will error if it finds an object in these locations.
     * <p>
     * Example:
     * <pre>
     *   // application.conf
     *   foo = "abc"
     *
     *   bar {
     *       baz = 123
     *       qux = "def"
     *   }
     *
     *   myList = [
     *      "a",
     *      "b",
     *      "c"
     *   ]
     * </pre>
     * Flattens to:
     * <pre>
     *   {
     *       "foo": "abc",
     *       "bar.baz": 123,
     *       "bar.qux": def",
     *       "myList": [ "a", "b", "c" ]
     *       "myList[0]": "a",
     *       "myList[1]": "b",
     *       "myList[2]": "c"
     *   }
     * </pre>
     */
    public static Map<String, Object> flatten(Config config) {

        TreeMap<String, Object> sortedValues = new TreeMap<>();
        new ConfigVisitor()
                .onList((parent, key, value) -> sortedValues.put(key, value))
                .onValue((parent, key, value) -> sortedValues.put(key, value))
                .visitRoot(config);

        return new LinkedHashMap<>(sortedValues);
    }

}
