package io.gatehill.imposter.util;

import org.hamcrest.Matcher;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assume.assumeThat;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class TestEnvironmentUtil {
    private static final File NULL_FILE = new File(
            (System.getProperty("os.name").startsWith("Windows") ? "NUL" : "/dev/null")
    );

    /**
     * Skips a JUnit test if running in CircleCI.
     */
    public static void assumeNotInCircleCi() {
        assumeThat("Not running in CircleCI", System.getenv("CIRCLECI"), not("true"));
    }

    /**
     * Skips a JUnit test if Docker is not accessible.
     */
    public static void assumeDockerAccessible() throws Exception {
        assumeThat("Docker is running", TestEnvironmentUtil.testDockerRunning(), is(0));
    }

    /**
     * Tests if the Docker process can be started successfully.
     * <p>
     * Typically used with a {@link org.junit.Assume#assumeThat(Object, Matcher)} statement in a JUnit test,
     * for example:
     *
     * <pre>
     *     assumeThat("Docker is running", TestEnvironmentUtil.testDockerRunning(), is(0));
     * </pre>
     *
     * @return the exit code of running the 'docker ps' command
     */
    private static int testDockerRunning() throws Exception {
        return new ProcessBuilder("docker", "ps")
                .redirectErrorStream(true)
                .redirectOutput(NULL_FILE)
                .start().waitFor();
    }
}
