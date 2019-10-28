/* See LICENSE in project directory */
package com.ge.verdict.stem;

import com.ge.research.sadl.SADLRuntimeModule;
import com.ge.research.sadl.builder.MessageManager;
import com.ge.research.sadl.ide.handlers.SadlGraphVisualizerHandler;
import com.ge.research.sadl.model.visualizer.GraphVizVisualizer;
import com.ge.research.sadl.model.visualizer.IGraphVisualizer;
import com.ge.research.sadl.reasoner.ResultSet;
import com.ge.research.sadl.utils.SadlConsole;
import com.ge.research.sadl.utils.SadlProjectHelper;
import com.google.inject.ConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Module;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customized STEM runtime module that works around some issues printing console messages and
 * visualizing graphs outside of the Eclipse platform.
 */
public class STEMRuntimeModule extends SADLRuntimeModule implements Module {

    public Class<? extends SadlConsole> bindSadlConsole() {
        return MySadlConsole.class;
    }

    public static class MySadlConsole implements SadlConsole {
        private static final Logger LOGGER = LoggerFactory.getLogger(MySadlConsole.class);

        @Override
        public void print(final MessageManager.MessageType type, final String message) {
            switch (type) {
                case ERROR:
                    LOGGER.error(message);
                    break;
                case WARN:
                    LOGGER.warn(message);
                    break;
                case INFO:
                    LOGGER.info(message);
                    break;
                default:
                    LOGGER.debug(message);
                    break;
            }
        }
    }

    public Class<? extends SadlGraphVisualizerHandler> bindSadlGraphVisualizerHandler() {
        return MySadlGraphVisualizerHandler.class;
    }

    public static class MySadlGraphVisualizerHandler implements SadlGraphVisualizerHandler {
        @Inject private SadlProjectHelper projectHelper;
        private IGraphVisualizer visualizer = new GraphVizVisualizer();

        @Override
        public void resultSetToGraph(
                final Path path,
                final ResultSet resultSet,
                final String description,
                final String baseFileName,
                final IGraphVisualizer.Orientation orientation,
                final Map<String, String> properties)
                throws ConfigurationException, IOException {
            File projectDirectory = new File(projectHelper.getRoot(path.toUri()));
            File graphsDirectory = new File(projectDirectory, "Graphs");
            graphsDirectory.mkdirs();
            visualizer.initialize(
                    graphsDirectory.getAbsolutePath(),
                    baseFileName,
                    baseFileName,
                    null,
                    orientation,
                    description);
            visualizer.graphResultSetData(resultSet);
        }
    }
}
