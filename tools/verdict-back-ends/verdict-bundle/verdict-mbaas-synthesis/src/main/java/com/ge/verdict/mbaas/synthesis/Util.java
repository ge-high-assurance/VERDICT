package com.ge.verdict.mbaas.synthesis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Various utility functions. */
public class Util {
    /**
     * Given a mapping of keys to lists of values, add a new element to the list corresponding to
     * the given key, adding the binding to the map if necessary.
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param map the map from keys to lists of values
     * @param key the key
     * @param value the new value
     */
    public static <K, V> void putListMap(Map<K, List<V>> map, K key, V value) {
        if (!map.containsKey(key)) {
            map.put(key, new ArrayList<>());
        }
        map.get(key).add(value);
    }

    /**
     * Given a mapping of keys to sets of values, add a new element to the set corresponding to the
     * given key, adding the binding to the map if necessary.
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param map the map from keys to sets of values
     * @param key the key
     * @param value the new value
     */
    public static <K, V> void putSetMap(Map<K, Set<V>> map, K key, V value) {
        if (!map.containsKey(key)) {
            map.put(key, new LinkedHashSet<>());
        }
        map.get(key).add(value);
    }

    /**
     * Given a mapping of keys to lists of values, retrieve the list of values corresponding to the
     * given key, or an empty list if the key is not bound.
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param map the map from keys to lists of values
     * @param key the key
     * @return the list of values bound to the key
     */
    public static <K, V> List<V> guardedGet(Map<K, List<V>> map, K key) {
        if (!map.containsKey(key)) {
            return Collections.emptyList();
        }
        return map.get(key);
    }
}
