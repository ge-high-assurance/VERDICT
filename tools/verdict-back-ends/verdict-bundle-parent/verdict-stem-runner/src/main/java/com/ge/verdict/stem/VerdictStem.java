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
            final Path connCsv = csvDataDir.resolve("ScnConnProps.csv");
            final Path modelsDir = knowledgeBaseDir.resolve("OwlModels");
            final Path templatesDir = knowledgeBaseDir.resolve("Templates");
            final Path archTemplate = templatesDir.resolve("ScnArch.tmpl");
            final Path compTemplate = templatesDir.resolve("ScnCompProps.tmpl");
            final Path connTemplate = templatesDir.resolve("ScnConnProps.tmpl");

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
            srvr.loadCsvData(
                    connCsv.toUri().toString(), csvIncludesHeader, connTemplate.toUri().toString());

            String qry =
                    srvr.prepareQuery(
                            "select distinct ?CompType ?CompInst ?CAPEC ?CAPECDescription "
                                    + "(?ic as ?Confidentiality) (?ii as ?Integrity) (?ia as ?Availability) ?LikelihoodOfSuccess "
                                    + "where {?CompInst <applicableCM> ?x "
                                    + "?x <id> ?id . LET(?CAPEC := concat('CAPEC-',str(?id))) "
                                    + ". ?x <capecDesc> ?CAPECDescription "
                                    + ". ?x <likelihoodOfSuccess> ?LikelihoodOfSuccess "
                                    + ". ?CompInst <type> ?CompType "
                                    + ". FILTER NOT EXISTS {?CompInst <type> ?a1 . ?a1 <rdfs:subClassOf> ?CompType } "
                                    + ". OPTIONAL{?x <ciaIssue> ?ic . FILTER(regex(str(?ic),'Confidentiality'))} "
                                    + ". OPTIONAL{?x <ciaIssue> ?ii . FILTER(regex(str(?ii),'Integrity'))} "
                                    + ". OPTIONAL{?x <ciaIssue> ?ia . FILTER(regex(str(?ia),'Availability'))} "
                                    + "} order by ?CompType ?CompInst ?CAPEC");
            ResultSet rs = srvr.query(qry);
            outputDir.mkdirs();
            Files.write(
                    outputDir.toPath().resolve("CAPEC.csv"),
                    rs.toString().replaceAll(anyNamespace, "").getBytes(StandardCharsets.UTF_8));

            qry =
                    srvr.prepareQuery(
                            "select distinct ?CompType ?CompInst ?CAPEC ?CAPECDescription  "
                                    + "(?ic as ?Confidentiality) (?ii as ?Integrity) (?ia as ?Availability) "
                                    + "?ApplicableDefense1 ?DefenseDescription1 ?ApplicableDefense2 ?DefenseDescription2 "
                                    + "?m1 ?dal1 ?m2 ?dal2 "
                                    + "where {?CompInst <applicableCM> ?x  "
                                    + ". ?x <id> ?id . LET(?CAPEC := concat('CAPEC-',str(?id))) "
                                    + ". ?x <capecDesc> ?CAPECDescription "
                                    + ". ?CompInst <type> ?CompType "
                                    + ". FILTER NOT EXISTS {?CompInst <type> ?a1 . ?a1 <rdfs:subClassOf> ?CompType } "
                                    + ". OPTIONAL{?x <ciaIssue> ?ic . FILTER(regex(str(?ic),'Confidentiality'))} "
                                    + ". OPTIONAL{?x <ciaIssue> ?ii . FILTER(regex(str(?ii),'Integrity'))} "
                                    + ". OPTIONAL{?x <ciaIssue> ?ia . FILTER(regex(str(?ia),'Availability'))} "
                                    + ". ?x <mitigation> ?m1 . LET(?strippedm1 := replace(str(?m1),'http.*#','')) "
                                    + ". LET(?strippedx := replace(str(?x),'http.*#','')) "
                                    + ". LET(?tail := substr(?strippedx,strlen(?strippedx) - strlen(?strippedm1)+1 ,strlen(?strippedm1))) "
                                    + ". FILTER(?tail = ?strippedm1) "
                                    + ". LET(?temp1 := lcase(str(?m1))) "
                                    + ". LET(?q2 := replace(str(?temp1),'http.*#','')) # strip out prefix  "
                                    + ". OPTIONAL{?CompInst ?y2 ?z4 . LET(?temp2 := lcase(str(?y2)))  "
                                    + "           . FILTER(?temp1 = ?temp2) . ?z4 <dal> ?dal1 } "
                                    + ". OPTIONAL{?m1 <dal> ?dal1}  # why do I need this - test by dropping it "
                                    + "{select distinct ?m1 (group_concat(distinct ?nc;separator=';') as ?ApplicableDefense1)  "
                                    + "    (group_concat(distinct ?ncd;separator=';') as ?DefenseDescription1) where "
                                    + "   {?m1 <nistControl> ?yy2  "
                                    + "    . ?yy2 <nistId> ?yy3 . LET(?nc := replace(str(?yy3),'http.*#','')) "
                                    + "    . ?yy2 <nistDesc> ?yy4 . LET(?ncd := replace(str(?yy4),'http.*#','')) "
                                    + "   } group by ?m1 "
                                    + "} "
                                    + ". OPTIONAL {?x <mitigation> ?m2 . FILTER(?m1 != ?m2) # can swap m1 and m2; but not doing that "
                                    + "            . LET(?tmp1 := lcase(str(?m2))) "
                                    + "            . LET(?r2 := replace(str(?tmp1),'http.*#','')) # strip out prefix  "
                                    + "            . ?CompInst ?y3 ?z5 . LET(?tmp2 := lcase(str(?y3)))  "
                                    + "            . FILTER(?tmp1 = ?tmp2) . ?z5 <dal> ?dal2  "
                                    + "            {select distinct ?m2 (group_concat(distinct ?nc;separator=';') as ?ApplicableDefense2)  "
                                    + "                (group_concat(distinct ?ncd;separator=';') as ?DefenseDescription2) where "
                                    + "             {?m2 <nistControl> ?yyy2  "
                                    + "              . ?yyy2 <nistId> ?yyy3 . LET(?nc := replace(str(?yyy3),'http.*#','')) "
                                    + "              . ?yyy2 <nistDesc> ?yyy4 . LET(?ncd := replace(str(?yyy4),'http.*#','')) "
                                    + "             } group by ?m2 "
                                    + "            } "
                                    + "           }  "
                                    + "} order by ?CompInst ?CAPEC ?m1 ");
            rs = srvr.query(qry);
            Files.write(
                    outputDir.toPath().resolve("Defenses.csv"),
                    rs.toString().replaceAll(anyNamespace, "").getBytes(StandardCharsets.UTF_8));

            qry =
                    srvr.prepareQuery(
                            "select distinct ?N1 ?link ?N2 ?N1_style ?N1_fillcolor ?N2_style ?N2_fillcolor (?cplist as ?N1_tooltip) where "
                                    + "{  ?conn <rdf:type> <Connection>  "
                                    + " . ?conn <connectionSource> ?src . ?conn <connectionDestination> ?dest "
                                    + " . ?conn <outPort> ?oport . ?conn <inPort> ?iport "
                                    + " . LET(?N1 := replace(str(?src),'^.*#','')) . LET(?N2 := replace(str(?dest),'^.*#',''))  "
                                    + " . LET(?N1_style := 'filled') . LET(?N2_style := 'filled') "
                                    + " . OPTIONAL{?src <applicableCM> ?acm . LET(?N1_fillcolor := 'red')} "
                                    + " . OPTIONAL{?dest <applicableCM> ?acm2 . LET(?N2_fillcolor := 'red')} "
                                    + " . ?conn <connectionFlow> ?flow  "
                                    + " . LET(?strippedflow := replace(str(?flow),'^.*#','')) "
                                    + " . ?conn <connectionName> ?connname  "
                                    + " . LET(?strippedcname := replace(str(?connname),'^.*#','')) "
                                    + " . LET(?link := ?strippedcname) .  "
                                    + "   OPTIONAL {{select distinct ?src (group_concat(distinct ?capec;separator='; &#10;') as ?capeclist) where  "
                                    + "      {?src <applicableCM> ?longcapec  "
                                    + "       . ?longcapec <id> ?id . ?longcapec <capecDesc> ?desc "
                                    + "       . LET(?capec := concat(concat(concat('CAPEC-',str(?id)),':'),?desc)) "
                                    + "      } group by ?src} "
                                    + "   } "
                                    + " . {select distinct ?src (group_concat(distinct ?c6;separator='; &#10;') as ?plist) where "
                                    + "    { {  ?src ?prop ?z3  "
                                    + "       . ?prop <tooltipProp> ?r2 . ?z3 <val> ?prop_val "
                                    + "      } UNION { OPTIONAL{?src ?prop ?prop_val . ?prop <tooltipProp> ?r2 . FILTER(regex(str(?prop_val),'true') || regex(str(?prop_val),'false'))}} "
                                    + "   . LET(?c3 := concat(str(?prop_val),str(?prop))) "
                                    + "   . LET(?c4 := replace(str(?c3),'http.*#','')) "
                                    + "   . LET(?c5 := replace(str(?c4),'^true','')) "
                                    + "   . LET(?c6 := replace(str(?c5),'^false','NOT_'))  "
                                    + "   } group by ?src} "
                                    + " . LET(?clist     := COALESCE(?capeclist,'')) "
                                    + " . LET(?templist  := concat(concat(?clist,'; &#10;'),?plist)) "
                                    + " . LET(?templist2 := replace(?templist,'^; ',''))   "
                                    + " . LET(?templist3 := replace(?templist2,';','; '))  "
                                    + " . LET(?cplist    := replace(?templist3,'  ',' '))  "
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
