/* See LICENSE in project directory */
package com.ge.verdict.bundle;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;

/** Unpack and invoke binaries from the resources directory. Used for aadl2iml, kind2, etc. */
public class Binary {
    /** Thrown upon errors during unpacking/invocation of a binary. */
    public static class ExecutionException extends Exception {
        private Optional<Integer> code;

        public ExecutionException(Exception child) {
            super(child);
            code = Optional.empty();
        }

        public ExecutionException(String msg) {
            super(msg);
            code = Optional.empty();
        }

        public ExecutionException(int code) {
            super("Process failed with error code: " + code);
            this.code = Optional.of(code);
        }

        /**
         * If the invocation of the binary fails with an error code, then this will return that
         * error code. Otherwise, empty.
         *
         * @return the error code
         */
        public Optional<Integer> getCode() {
            return code;
        }
    }

    private String name;

    public Binary(String name) {
        this.name = name;
    }

    /** @return the directory where unpacked binaries are stored */
    private static File getBinaryDir() {
        // /tmp/ on Unix
        return new File(System.getProperty("java.io.tmpdir"));
    }

    /**
     * Get the extension for the current operating system, used for unpacking the correct binary.
     *
     * <p>One of: "nix", "mac", "win"
     *
     * @return the extension corresponding to the current OS
     */
    private static String getOsExtension() {
        String os = System.getProperty("os.name", "generic").toLowerCase(Locale.US);
        System.out.flush();
        if (os.indexOf("mac") != -1 || os.indexOf("darwin") != -1) {
            // macOS
            return "mac";
        } else if (os.indexOf("win") != -1) {
            // Windows
            return "win";
        } else if (os.indexOf("nux") != -1) {
            // Linux
            return "nix";
        } else {
            System.err.println("Unable to detect OS: " + os + " , defaulting to *nix");
            return "nix";
        }
    }

    /** @return the file that the binary is unpacked to */
    private File getFile() {
        return new File(getBinaryDir(), name);
    }

    /**
     * Extract a resource file.
     *
     * @param resPath path in resources
     * @param dest destination file
     * @throws ExecutionException
     */
    public static void copyResource(String resPath, File dest, boolean executable)
            throws ExecutionException {
        URL url = null;

        /*
         * Search for the file in the classpath.
         *
         * The capsule maven plugin makes classpaths really screwy,
         * but this approach should be resilient to that screwiness.
         */
        try {
            Enumeration<URL> urls = Binary.class.getClassLoader().getResources(resPath);

            if (urls.hasMoreElements()) {
                url = urls.nextElement();
            } else {
                throw new ExecutionException("Could not find " + resPath + " on classpath");
            }
        } catch (IOException e) {
            throw new ExecutionException(e);
        }

        /*
         * Copy the file to the destination.
         */
        try (BufferedInputStream in = new BufferedInputStream(url.openStream());
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest))) {

            byte[] buffer = new byte[1024];
            int read;
            while (in.available() > 0) {
                read = in.read(buffer);
                out.write(buffer, 0, read);
            }

            if (executable) {
                dest.setExecutable(true);
            }
        } catch (IOException e) {
            throw new ExecutionException(e);
        }
    }

    /**
     * Unpack the binary from resources to dest.
     *
     * @param dest
     * @throws ExecutionException
     */
    private void unpack(File dest) throws ExecutionException {
        String resName = getOsExtension() + "/" + name;
        copyResource(resName, dest, true);
    }

    /**
     * Invoke the binary with the given arguments, first unpacking from resources if necessary.
     *
     * <p>Waits for the spawned process to terminate.
     *
     * @param wd working directory, may be null
     * @param streamHandler stream redirect targets (Apache)
     * @param args
     * @throws ExecutionException
     */
    public void invoke(String wd, ExecuteStreamHandler streamHandler, String... args)
            throws ExecutionException {
        File file = getFile();

        // We can just unpack every time
        // This way we don't have to worry if we update the binary
        unpack(file);

        List<String> argsList = new ArrayList<>();
        argsList.add(file.getAbsolutePath());
        argsList.addAll(Arrays.asList(args));

        String[] env;

        switch (getOsExtension()) {
            case "nix":
                env = new String[] {};
                break;
            case "mac":
                env = new String[] {"PATH=/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin"};
                break;
            case "win":
                throw new RuntimeException("Windows not yet supported");
            default:
                throw new RuntimeException("Invalid OS: " + getOsExtension());
        }

        Execute executor = new Execute(streamHandler);
        if (wd != null) {
            executor.setWorkingDirectory(new File(wd));
        }
        executor.setCommandline(argsList.toArray(new String[argsList.size()]));
        executor.setEnvironment(env);

        try {
            int resultCode = executor.execute();

            if (resultCode != 0) {
                throw new ExecutionException(resultCode);
            }
        } catch (IOException e) {
            throw new ExecutionException(e);
        }
    }

    /**
     * Invoke the binary with the given arguments, first unpacking from resources if necessary.
     *
     * <p>Waits for the spawned process to terminate.
     *
     * @param args
     * @throws ExecutionException
     */
    public void invoke(String... args) throws ExecutionException {
        invoke(null, new PumpStreamHandler(), args);
    }
}
