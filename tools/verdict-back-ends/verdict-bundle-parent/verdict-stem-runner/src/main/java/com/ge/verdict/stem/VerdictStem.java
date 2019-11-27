/* See LICENSE in project directory */
package com.ge.verdict.stem;

import com.ge.research.sadl.importer.TemplateException;
import com.ge.research.sadl.model.visualizer.GraphVizVisualizer;
import com.ge.research.sadl.model.visualizer.IGraphVisualizer.Orientation;
import com.ge.research.sadl.reasoner.ConfigurationException;
import com.ge.research.sadl.reasoner.InvalidNameException;
import com.ge.research.sadl.reasoner.QueryCancelledException;
import com.ge.research.sadl.reasoner.QueryParseException;
import com.ge.research.sadl.reasoner.ReasonerNotFoundException;
import com.ge.research.sadl.reasoner.ResultSet;
import com.ge.research.sadl.server.ISadlServer;
import com.ge.research.sadl.server.SessionNotFoundException;
import com.ge.research.sadl.server.server.SadlServerImpl;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Runs SADL on a Verdict STEM project. */
public class VerdictStem {
    private static final Logger LOGGER = LoggerFactory.getLogger(VerdictStem.class);

    /**
     * Runs SADL on a Verdict STEM project.
     *
     * @param projectDir Path to Verdict STEM project
     * @param outputDir Output directory to write to
     * @param graphsDir Graphs directory to write to
     */
    public void runStem(File projectDir, File outputDir, File graphsDir) {
        try {
            final Path knowledgeBaseDir = projectDir.toPath();
            final Path csvDataDir = knowledgeBaseDir.resolve("CSVData");
            final boolean csvIncludesHeader = true;
            final Path archCsv = csvDataDir.resolve("ScnArch.csv");
            final Path compCsv = csvDataDir.resolve("ScnCompProps.csv");
            final Path connCsv = csvDataDir.resolve("ScnConnectionProps.csv");
            final Path capecCsv = outputDir.toPath().resolve("CAPEC.csv");
            final Path defensesCsv = outputDir.toPath().resolve("Defenses.csv");
            final Path defenses2NistCsv = outputDir.toPath().resolve("Defenses2NIST.csv");
            final String graphName = "Run_sadl11";
            final Path modelsDir = knowledgeBaseDir.resolve("OwlModels");
            final Path templatesDir = knowledgeBaseDir.resolve("Templates");
            final Path archTemplate = templatesDir.resolve("ScnArch.tmpl");
            final Path compTemplate = templatesDir.resolve("ScnCompProps.tmpl");
            final Path connTemplate = templatesDir.resolve("ScnConnectionProps.tmpl");

            final String modelName = "http://sadl.org/STEM/Run";
            final String instanceDataNamespace = "http://sadl.org/STEM/Scenario#";
            final String anyNamespace = "http[^#]*#";

            ISadlServer srvr = new SadlServerImpl(knowledgeBaseDir.toString());
            srvr.selectServiceModel(modelsDir.toString(), modelName);
            srvr.setInstanceDataNamespace(instanceDataNamespace);

            srvr.loadCsvData(
                    archCsv.toUri().toString(), csvIncludesHeader, archTemplate.toUri().toString());
            srvr.loadCsvData(
                    compCsv.toUri().toString(), csvIncludesHeader, compTemplate.toUri().toString());
            srvr.loadCsvData(
                    connCsv.toUri().toString(), csvIncludesHeader, connTemplate.toUri().toString());

            ResultSet rs = srvr.query("http://sadl.org/STEM/Queries#Defenses2NIST");
            outputDir.mkdirs();
            Files.write(
                    defenses2NistCsv,
                    rs.toString().replaceAll(anyNamespace, "").getBytes(StandardCharsets.UTF_8));

            rs = srvr.query("http://sadl.org/STEM/Queries#CAPEC");
            outputDir.mkdirs();
            Files.write(
                    capecCsv,
                    rs.toString().replaceAll(anyNamespace, "").getBytes(StandardCharsets.UTF_8));

            rs = srvr.query("http://sadl.org/STEM/Queries#Defenses");
            outputDir.mkdirs();
            Files.write(
                    defensesCsv,
                    rs.toString().replaceAll(anyNamespace, "").getBytes(StandardCharsets.UTF_8));

            rs = srvr.query("http://sadl.org/STEM/Queries#STEMgraph");
            graphsDir.mkdirs();
            GraphVizVisualizer visualizer = new GraphVizVisualizer();
            visualizer.initialize(
                    graphsDir.getPath(),
                    graphName,
                    graphName,
                    null,
                    Orientation.TD,
                    "Cmd 13  (Graph)");
            visualizer.graphResultSetData(rs);
            LOGGER.info("Run finished");
        } catch (IOException
                | InvalidNameException
                | SessionNotFoundException
                | QueryCancelledException
                | QueryParseException
                | ReasonerNotFoundException
                | ConfigurationException
                | URISyntaxException
                | TemplateException e) {
            throw new RuntimeException(e);
        }
    }
}
