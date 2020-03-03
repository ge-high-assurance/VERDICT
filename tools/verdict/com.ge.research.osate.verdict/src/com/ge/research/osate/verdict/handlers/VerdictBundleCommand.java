package com.ge.research.osate.verdict.handlers;

import com.amihaiemil.docker.Container;
import com.amihaiemil.docker.Docker;
import com.amihaiemil.docker.Image;
import com.amihaiemil.docker.Images;
import com.amihaiemil.docker.LocalDocker;
import com.amihaiemil.docker.RemoteDocker;
import com.amihaiemil.docker.UnexpectedResponseException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;

/** Collects all arguments and then runs verdict-bundle via java or docker. */
public class VerdictBundleCommand {

    // Must set dockerImage, args, and binds before running verdict-bundle
    private String dockerImage;
    private List<String> args = new ArrayList<>();
    private List<String> binds = new ArrayList<>();
    private List<String> env = new ArrayList<>();

    /** Tells us whether to run verdict-bundle via java or docker. */
    private boolean isImage() {
        return dockerImage != null && !dockerImage.isEmpty();
    }

    /**
     * Collects verdict-bundle jar or image. Verifies at least one of jar or image was passed but
     * assumes BundlePreferences has checked both really are a jar and/or image.
     *
     * @param bundleJar Filename of verdict-bundle jar
     * @param dockerImage Name of verdict-bundle image
     * @return this object to allow calls to be chained
     */
    public VerdictBundleCommand jarOrImage(String bundleJar, String dockerImage) {
        this.dockerImage = dockerImage;
        if (!isImage()) {
            if (bundleJar != null && !bundleJar.isEmpty()) {
                args.add("java");
                args.add("-jar");
                args.add(bundleJar);
            } else {
                throw new IllegalArgumentException("Missing both jar and image arguments");
            }
        }
        return this;
    }

    /**
     * Collects a verdict-bundle argument.
     *
     * @param arg Argument to be passed to verdict-bundle
     * @return this object to allow calls to be chained
     */
    public VerdictBundleCommand arg(String arg) {
        args.add(arg);
        return this;
    }

    /**
     * Collects a verdict-bundle directory or file argument which can be passed directly to the jar
     * but must be bound to both a host path and a container path when running the image via docker.
     *
     * @param hostPath Directory or file argument on host filesystem
     * @param containerPath Directory or file argument in container filesystem
     * @return this object to allow calls to be chained
     */
    public VerdictBundleCommand argBind(String hostPath, String containerPath) throws IOException {
        // Add the appropriate argument to the command line arguments
        args.add(isImage() ? containerPath : hostPath);

        // We can bind only host and container directories together in
        // Docker, so we need to bind file arguments' parent directories
        if (isImage()) {
            File hostFile = new File(hostPath);
            if (hostFile.isFile() || hostFile.createNewFile()) {
                hostPath = hostFile.getParentFile().getCanonicalPath();
                containerPath = new File(containerPath).getParent();
            }

            // We have to convert any native Windows paths to a quasi-Linux format:
            // 1) Replace any drive letters; 2) replace any backslashes
            hostPath = hostPath.replaceFirst("^([A-Z]):", "/$1").replace("\\", "/");
            containerPath = containerPath.replace("\\", "/");

            // Now we can bind the host and container directories together
            VerdictLogger.info("Binding " + hostPath + " to " + containerPath);
            binds.add(hostPath + ":" + containerPath);
        }

        return this;
    }

    /**
     * Collects a verdict-bundle argument which takes different values for the bundle jar and the
     * docker image.
     *
     * @param jarArg Argument to be passed to bundle jar
     * @param dockerArg Argument to be passed to docker image
     * @return this object to allow calls to be chained
     */
    public VerdictBundleCommand arg2(String jarArg, String dockerArg) {
        args.add(isImage() ? dockerArg : jarArg);
        return this;
    }

    /**
     * Defines an environment variable which should be defined when running the bundle jar.
     * Not used when running the docker image.
     *
     * @param name Name of environment variable to define
     * @param value Value of environment variable to define
     * @return this object to allow calls to be chained
     */
    public VerdictBundleCommand env(String name, String value) {
        env.add(name + "=" + value);
        return this;
    }

    /**
     * Runs verdict-bundle via java or docker. Assumes all arguments have been collected in the
     * correct order already.
     *
     * @return
     */
    public int runJarOrImage() {
        if (isImage()) {
            return runWithDocker();
        } else {
            return runWithJava();
        }
    }

    /**
     * Runs verdict-bundle image with docker using the docker-client Java API.
     *
     * @return Exit code from verdict-bundle
     */
    private int runWithDocker() {
        try {
            // Connect to a Unix or TCP socket, although we must change tcp to http
            String unixSocket = "/var/run/docker.sock";
            String tcpSocket = System.getenv("DOCKER_HOST");
            tcpSocket = (tcpSocket != null) ? tcpSocket.replace("tcp:", "http:") : null;
            Docker docker =
                    (tcpSocket != null)
                            ? new RemoteDocker(URI.create(tcpSocket))
                            : new LocalDocker(new File(unixSocket));
            URI baseUri =
                    (tcpSocket != null)
                            ? URI.create(URI.create(tcpSocket) + "/v1.35")
                            : URI.create("unix://localhost:80/v1.35");

            // Make sure we can connect to the docker server
            if (!docker.ping()) {
                VerdictLogger.severe("Unable to connect to docker " + docker);
                return -1;
            }

            // Make sure we can find, or pull, the verdict image
            Map<String, Iterable<String>> filters = new HashMap<>();
            filters.put("reference", Arrays.asList(dockerImage));
            Images filtered = docker.images().filter(filters);
            Image image = null;
            for (Image img : filtered) {
                image = img;
                VerdictLogger.info("Found image " + image.getJsonArray("RepoTags"));
                break;
            }
            if (image == null) {
                VerdictLogger.info("Pulling image " + dockerImage + ":latest");
                image = docker.images().pull(dockerImage, "latest");
            }

            // Create a container using the specified image and
            // binding the given directories
            JsonObject containerConfig =
                    Json.createObjectBuilder()
                            .add(
                                    "HostConfig",
                                    Json.createObjectBuilder()
                                            .add("Binds", Json.createArrayBuilder(binds))
                                            .build())
                            .add("Image", dockerImage)
                            .add("Cmd", Json.createArrayBuilder(args))
                            .build();
            Container container = docker.containers().create(containerConfig);
            String containerId = container.containerId();

            // Start the container and wait for it to exit
            VerdictLogger.info("Running image: " + dockerImage + " " + String.join(" ", args));
            try {
                container.start();
                // Container doesn't have wait() method yet, so we will have to make the http call
                // ourselves
                HttpPost httpPost =
                        new HttpPost(
                                URI.create(
                                        baseUri.toString()
                                                + "/containers/"
                                                + containerId
                                                + "/wait"));
                try {
                    VerdictLogger.info("Started, now waiting for container to finish: " + containerId);
                    HttpResponse httpResponse = docker.httpClient().execute(httpPost);
                    JsonObject waitResult =
                            Json.createReader(httpResponse.getEntity().getContent()).readObject();
                    int httpStatus = httpResponse.getStatusLine().getStatusCode();
                    if (HttpStatus.SC_OK == httpStatus) {
                        int statusCode = waitResult.getInt("StatusCode");

                        // Read and relay all output printed by verdict-bundle to our own stdout
                        String logs = container.logs().fetch();
                        System.out.print(logs);

                        // Return verdict-bundle's exit code
                        return statusCode;
                    } else {
                        throw new UnexpectedResponseException(
                                httpPost.getURI().toString(),
                                httpStatus,
                                HttpStatus.SC_OK,
                                waitResult);
                    }
                } finally {
                    httpPost.releaseConnection();
                }
            } finally {
                // Always remove the container before we return
                container.remove();
            }
        } catch (IOException | UnexpectedResponseException e) {
            VerdictLogger.severe("Unable to run command: " + e);
            return -1;
        }
    }

    /**
     * Runs verdict-bundle jar with java using the Ant Execute task API.
     *
     * @return Exit code from verdict-bundle
     */
    private int runWithJava() {
        if (OS.equals("osx")) {
            env("PATH", MAC_PATH);
        } else if (OS.equals("win") || OS.equals("unknown")) {
            VerdictLogger.severe("We don't support OS " + OS + " yet!");
            return 1;
        }

        Execute executor = new Execute(new PumpStreamHandler(System.out, System.err));
        executor.setCommandline(args.toArray(new String[args.size()]));
        if (!env.isEmpty()) {
            executor.setEnvironment(env.toArray(new String[env.size()]));
        }

        try {
            VerdictLogger.info("Running command: " + String.join(" ", args));
            return executor.execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // OS variables
    static final String MACHINEOS = System.getProperty("os.name").toLowerCase();
    static final String OS =
            MACHINEOS.startsWith("mac")
                    ? "osx"
                    : (MACHINEOS.startsWith("win")
                            ? "win"
                            : ((MACHINEOS.startsWith("linux") ? "glnx" : "unknown")));
    // Environment variables
    static final String MAC_PATH = "/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin";
}
