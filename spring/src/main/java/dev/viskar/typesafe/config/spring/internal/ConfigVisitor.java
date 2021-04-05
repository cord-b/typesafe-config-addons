package dev.viskar.typesafe.config.spring.internal;

import com.typesafe.config.Config;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Helper class to walk through a Config's structure and emit Spring-style paths for each key/value.
 */
class ConfigVisitor {

    private Listener<Map<String, Object>> onMap = Listener.nop();
    private Listener<List<Object>> onList = Listener.nop();
    private Listener<Object> onValue = Listener.nop();
    private Listener<Object> onAny = Listener.nop();

    // ************************************************************************
    // Configure Listeners
    // ************************************************************************

    ConfigVisitor onMap(Listener<Map<String, Object>> callback) {
        this.onMap = callback;
        return this;
    }

    ConfigVisitor onList(Listener<List<Object>> callback) {
        this.onList = callback;
        return this;
    }

    ConfigVisitor onValue(Listener<Object> callback) {
        this.onValue = callback;
        return this;
    }

    ConfigVisitor onAny(Listener<Object> callback) {
        this.onAny = callback;
        return this;
    }

    // ************************************************************************
    // Visitor
    // ************************************************************************

    void visitRoot(Config config) {
        for (Entry<String, Object> e : config.root().unwrapped().entrySet()) {
            visit("", e.getKey(), e.getValue());
        }
    }

    void visit(String parentPosition, String position, Object any) {
        onAny.accept(parentPosition, position, any);
        if (any instanceof Map) {
            visitMap(parentPosition, position, (Map) any);
        } else if (any instanceof List) {
            visitList(parentPosition, position, (List) any);
        } else {
            visitValue(parentPosition, position, any);
        }
    }

    void visitMap(String parentPosition, String position, Map<String, Object> map) {
        onMap.accept(parentPosition, position, map);
        for (Entry<String, Object> e : map.entrySet()) {
            visit(position, position + "." + e.getKey(), e.getValue());
        }
    }

    void visitList(String parentPosition, String position, List<Object> list) {
        onList.accept(parentPosition, position, list);
        int i = 0;
        for (Object value : list) {
            visit(position, position + "[" + i + "]", value);
            i++;
        }
    }

    void visitValue(String parentPosition, String position, Object value) {
        onValue.accept(parentPosition, position, value);
    }

    // ************************************************************************
    // Helpers
    // ************************************************************************

    interface Listener<T> {

        void accept(String parentPosition, String position, T value);

        static <T> Listener<T> nop() {
            return (parent, key, value) -> {
                // no-op
            };
        }
    }

}