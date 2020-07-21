import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Prepends this capsule's cache directory to the appropriate dynamic loader library path
 * environment variable (PATH, LD_LIBRARY_PATH, or DYLD_LIBRARY_PATH) for whichever operating system
 * we're running on. Works around "can't find dependent library" exception thrown when a native lib
 * can't find another native lib. Note that adding the cache directory to "java.library.path" isn't
 * sufficient to let a native lib load another native lib; we must add the cache directory to the
 * OS's dynamic loader library path too.
 */
public final class SharedLibraryPathCapsule extends Capsule {

    /**
     * Constructs a capsule.
     *
     * <p>This constructor is used by a caplet that will be listed in the manifest's {@code
     * Main-Class} attribute. <b>Caplets are encouraged to "override" the {@link #Capsule(Capsule)
     * other constructor} so that they may be listed in the {@code Caplets} attribute.</b>
     *
     * <p>This constructor or that of a subclass must not make use of any registered capsule
     * options, as they may not have been properly pre-processed yet.
     *
     * @param jarFile the path to the JAR file
     */
    protected SharedLibraryPathCapsule(Path jarFile) {
        super(jarFile);
    }

    /**
     * Caplets are required to have this constructor and pass their argument up to the super class
     * constructor with the same signature as this constructor.
     *
     * @param pred The capsule preceding this one in the chain (caplets must not access the passed
     *     capsule in their constructor)
     */
    protected SharedLibraryPathCapsule(Capsule pred) {
        super(pred);
    }

    /**
     * Returns a map of environment variables (property-value pairs) with the appropriate shared
     * library path environment variable modified by prepending this capsule's cache directory to
     * any pre-existing path. We want the cache directory to be the first entry in the path since we
     * don't want to load an already installed library that has a different version than our own
     * native dependency.
     *
     * @param env the current environment
     */
    @Override
    protected Map<String, String> buildEnvironmentVariables(Map<String, String> env) {
        // On OSX, we must use DYLD_LIBRARY_PATH while on Windows, we must use PATH.
        final String PROP_OS_NAME = "os.name";
        String os = getProperty(PROP_OS_NAME).toLowerCase(Locale.ROOT);
        String sharedLibraryPath = "LD_LIBRARY_PATH";
        if (os.startsWith("mac")) {
            sharedLibraryPath = "DYLD_LIBRARY_PATH";
        } else if (os.startsWith("windows")) {
            sharedLibraryPath = "PATH";
        }

        // Prepend the cache directory to the path or simply set the path to the
        // directory if the path doesn't exist yet.
        Path appDir = getAppDir();
        if (appDir != null) {
            final String PROP_PATH_SEPARATOR = "path.separator";
            String separator = getProperty(PROP_PATH_SEPARATOR);
            env.merge(sharedLibraryPath, appDir.toString(), (path, dir) -> dir + separator + path);
        }

        return env;
    }

    /**
     * Returns the value of the given capsule attribute with consideration to the capsule's mode.
     * This method must not be called directly except as {@code super.attribute(attr)} in the
     * caplet's implementation of this method.
     *
     * <p>This implementation ensures that the Library-Path-P attribute contains this capsule's
     * cache directory so that it comes first, not last, in "java.library.path" (as we noted with
     * the shared library path environment variable above, we don't want to load an already
     * installed library that has a different version than our own native dependency).
     *
     * @param attr the attribute
     * @return the value of the attribute.
     * @see #getAttribute(Map.Entry)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected <T> T attribute(Entry<String, T> attr) {
        T value = super.attribute(attr);
        if (attr == ATTR_LIBRARY_PATH_P) {
            List<Object> oldPath = (List<Object>) value;
            List<Object> newPath = new ArrayList<>();
            Path appDir = getAppDir();
            newPath.add(appDir);
            newPath.addAll(oldPath);
            return (T) newPath;
        }
        return value;
    }
}
