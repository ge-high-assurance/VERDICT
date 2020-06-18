package com.ge.verdict.attackdefensecollector;

import java.util.Map;

/**
 * Represents a deferred reference resolution for references indexed by a name string. Intended for
 * use when loading objects with cyclic dependencies, where eager evaluation would not work.
 *
 * <p>Use get() method to resolve reference.
 *
 * <p>Throws ResolutionException if the specified name could not be resolved.
 *
 * @param <T>
 */
public class NameResolver<T> {
    private String name;
    private Map<String, T> resolutionTable;
    private T resolved;

    /** Thrown if a NameResolver could not be resolved. */
    public static class ResolutionException extends RuntimeException {
        public ResolutionException(String name) {
            super("Resolution failed for name: " + name);
        }
    }

    /**
     * Create a NameResolver, deferring the lookup of name in resolutionTable until get() is called
     * at a later point in execution.
     *
     * @param name the name to resolve
     * @param resolutionTable the table to use for resolution
     */
    public NameResolver(String name, Map<String, T> resolutionTable) {
        this.name = name;
        this.resolutionTable = resolutionTable;
        resolved = null;
    }

    /**
     * Get the name that this NameResolver uses to resolve the reference. Invoking this method does
     * not force a resolution.
     *
     * @return the name to lookup
     */
    public String getName() {
        return name;
    }

    /**
     * Get the resolved reference corresponding to the name specified in the constructor. Invoking
     * this method forces name resolution.
     *
     * <p>Invoking this method while the resolution table is still being populated is an error and
     * could cause ResolutionException even if the name would be defined after the table is fully
     * populated.
     *
     * @return the resolved reference
     */
    public T get() {
        // Only perform resolution once
        // If ResolutionException is caught, this will still run again
        if (resolved == null) {
            if (!resolutionTable.containsKey(name)) {
                throw new ResolutionException(name);
            }

            resolved = resolutionTable.get(name);
        }

        return resolved;
    }

    /*
     * We don't like calling hashCode and equals on NameResolver directly
     * because we lose information about T at runtime. Users are encouraged
     * to instead compare the names using getName().
     */

    @Override
    @Deprecated
    public int hashCode() {
        throw new IllegalStateException("called hashCode on NameResolver");
    }

    @Override
    @Deprecated
    public boolean equals(Object other) {
        throw new IllegalStateException("called equals on NameResolver");
    }
}
