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
            final Path modelsDir = knowledgeBaseDir.resolve("OwlModels");
            final Path templatesDir = knowledgeBaseDir.resolve("Templates");
            final Path archTemplate = templatesDir.resolve("ScnArch.tmpl");
            final Path compTemplate = templatesDir.resolve("ScnCompProps.tmpl");

            final String modelName = "http://sadl.org/STEM/Run";
            final String instanceDataNamespace = "http://sadl.org/STEM/Scenario#";
            final String anyNamespace = "http[^#]*#";
            final String graphName = "Run_sadl12";

            ISadlServer srvr = new SadlServerImpl(knowledgeBaseDir.toString());
            srvr.selectServiceModel(modelsDir.toString(), modelName);
            srvr.setInstanceDataNamespace(instanceDataNamespace);

            srvr.loadCsvData(
                    archCsv.toUri().toString(), csvIncludesHeader, archTemplate.toUri().toString());
            srvr.loadCsvData(
                    compCsv.toUri().toString(), csvIncludesHeader, compTemplate.toUri().toString());

            String qry =
                    srvr.prepareQuery(
                            "select distinct (?z5 as ?CompType) (?z2 as ?CompInst) ?CAPEC ?CAPECDescription "
                                    + "(?ic as ?Confidentiality) (?ii as ?Integrity) (?ia as ?Availability) ?LikelihoodOfSuccess "
                                    + "where {?x <affectedComponent> ?z2 "
                                    + ". OPTIONAL{?x <ciaIssue> ?ic . FILTER(regex(str(?ic),'Confidentiality'))} "
                                    + ". OPTIONAL{?x <ciaIssue> ?ii . FILTER(regex(str(?ii),'Integrity'))} "
                                    + ". OPTIONAL{?x <ciaIssue> ?ia . FILTER(regex(str(?ia),'Availability'))} "
                                    + ". ?x <capec> ?CAPEC . ?z2 <type> ?z5 . ?x <capecDescription> ?CAPECDescription "
                                    + ". ?x <likelihoodOfSuccess> ?LikelihoodOfSuccess "
                                    + ". FILTER NOT EXISTS {?z2 <type> ?z6 . ?z6 <rdfs:subClassOf> ?z5 }} order by ?z5 ?z2 ?CAPEC");
            ResultSet rs = srvr.query(qry);
            outputDir.mkdirs();
            Files.write(
                    outputDir.toPath().resolve("CAPEC.csv"),
                    rs.toString().replaceAll(anyNamespace, "").getBytes(StandardCharsets.UTF_8));

            qry =
                    srvr.prepareQuery(
                            "select distinct  (?z6 as ?CompType) (?z2 as ?CompInst) (?z8 as ?CAPEC) "
                                    + "(?z10 as ?CAPECDescription) "
                                    + "(?ic as ?Confidentiality) (?ii as ?Integrity) (?ia as ?Availability) "
                                    + "(?z9 as ?ApplicableDefense) (?z7 as ?DefenseDescription) ?ImplProperty ?DAL "
                                    + "where {?x <defense> ?z5 . ?x <affectedComponent> ?z2 "
                                    + ". OPTIONAL{?x <ciaIssue> ?ic . FILTER(regex(str(?ic),'Confidentiality'))} "
                                    + ". OPTIONAL{?x <ciaIssue> ?ii . FILTER(regex(str(?ii),'Integrity'))} "
                                    + ". OPTIONAL{?x <ciaIssue> ?ia . FILTER(regex(str(?ia),'Availability'))} "
                                    + ". ?z2 <type> ?z6 "
                                    + ". FILTER NOT EXISTS {?z2 <type> ?a1 . ?a1 <rdfs:subClassOf> ?z6 } "
                                    + ". ?x <protectionDescription> ?z7 . ?x <capecMitigated> ?z8 . ?x <defense> ?z9 "
                                    + ". ?x <capecDescription> ?z10 "
                                    + ". OPTIONAL{?x <implProperty> ?ImplProperty} "
                                    + ". OPTIONAL{?x <dal> ?DAL}} order by ?z6 ?z2 ?CAPEC");
            rs = srvr.query(qry);
            Files.write(
                    outputDir.toPath().resolve("Defenses.csv"),
                    rs.toString().replaceAll(anyNamespace, "").getBytes(StandardCharsets.UTF_8));

            qry =
                    srvr.prepareQuery(
                            "select distinct ?N1 ?link ?N2 ?N1_style ?N1_fillcolor ?N2_style ?N2_fillcolor (?cplist as ?N1_tooltip) where "
                                    + "{  ?x <rdf:type> ?z . FILTER(regex(str(?z),'Connection')) "
                                    + " . ?x <connectionSource> ?src . ?x <connectionDestination> ?dest "
                                    + " . ?x <outPort> ?oport . ?x <inPort> ?iport "
                                    + " . LET(?N1 := replace(str(?src),'^.*#','')) . LET(?N2 := replace(str(?dest),'^.*#','')) "
                                    + " . LET(?N1_style := 'filled') . LET(?N2_style := 'filled') "
                                    + " . OPTIONAL{  ?u <affectedComponent> ?src . ?u <addressed> ?c1 . FILTER(regex(str(?c1), 'true')) "
                                    + "            . ?src <capecString> ?str . LET(?N1_fillcolor := 'yellow')} "
                                    + " . OPTIONAL{  ?u2 <affectedComponent> ?dest . ?u2 <addressed> ?c1 . FILTER(regex(str(?c1), 'true')) "
                                    + "            . ?dest <capecString> ?str . LET(?N2_fillcolor := 'yellow')} "
                                    + " . OPTIONAL{?u <affectedComponent> ?src . ?src <capecString> ?str . LET(?N1_fillcolor := 'red')} "
                                    + " . OPTIONAL{?u2 <affectedComponent> ?dest . ?dest <capecString> ?str . LET(?N2_fillcolor := 'red')} "
                                    + " . ?x <connectionFlow> ?cf0 "
                                    + " . LET(?cf := replace(str(?cf0),'^.*#','')) "
                                    + " . LET(?link := ?cf) . "
                                    + "   {select distinct ?src (group_concat(distinct ?capec;separator='; &#10;') as ?capeclist) where "
                                    + "      {?x <rdf:type> <Connection> . ?x <connectionSource> ?src . OPTIONAL{?src <capecString> ?capec} "
                                    + "      } group by ?src "
                                    + "   } "
                                    + " . {select distinct ?src (group_concat(distinct ?c6;separator='; &#10;') as ?plist) where "
                                    + "    { { "
                                    + "         ?src ?prop ?z3 "
                                    + "       . ?prop <tooltipProp> ?r2 . ?z3 <val> ?prop_val "
                                    + "      } "
                                    + "    UNION "
                                    + "    {?x <rdf:type> ?z . FILTER(regex(str(?z),'Connection')) . ?x <connectionSource> ?src "
                                    + "     . OPTIONAL{?src ?prop ?prop_val . ?prop <tooltipProp> ?r2 . FILTER(regex(str(?prop_val),'true') || regex(str(?prop_val),'false'))} "
                                    + "    } "
                                    + "   . LET(?c3 := concat(str(?prop_val),str(?prop))) "
                                    + "   . LET(?c4 := replace(str(?c3),'http.*#','')) "
                                    + "   . LET(?c5 := replace(str(?c4),'^true','')) "
                                    + "   . LET(?c6 := replace(str(?c5),'^false','NOT_')) "
                                    + "   } group by ?src} "
                                    + " . LET(?clist     := COALESCE(?capeclist,'')) "
                                    + " . LET(?templist  := concat(concat(?clist,'; &#10;'),?plist)) "
                                    + " . LET(?templist2 := replace(?templist,'^; ','')) "
                                    + " . LET(?templist3 := replace(?templist2,';','; ')) "
                                    + " . LET(?cplist    := replace(?templist3,'  ',' ')) "
                                    + "}");
            rs = srvr.query(qry);
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
