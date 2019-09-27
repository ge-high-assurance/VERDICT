/* See LICENSE in project directory */
package edu.uiowa.clc.verdict.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Reads Kind2 XML output & Generate VerdictProperties.
// PropertyID
// Status
// Source
// Time
// CounterExample ...
// BlameAssignment ...
public class SummaryProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SummaryProcessor.class);

    public static Vector<VerdictProperty> readResults(File resultFile)
            throws FileNotFoundException {

        FileInputStream resultStream = new FileInputStream(resultFile);
        return readResults(resultStream);
    }

    public static Vector<VerdictProperty> readResults(InputStream inputStream)
            throws FileNotFoundException {

        //        File resultFile = new File("eg1_results.xml");

        Vector<VerdictProperty> properties = XMLProcessor.praseXML(inputStream);

        LOGGY.info("================== Properties Summary==================");
        for (VerdictProperty p : properties) {

            LOGGY.info("Property: " + p.getId() + "  " + p.isSAT());

            if (!p.isSAT()
                    && p.getSource().equals("wamax")
                    && p.getAllWeakAssumptions().size() > 0) {

                LOGGY.info("Weak Assumptions: ");
                LOGGY.info("-----------------");
                printwaReport(p.getAllWeakAssumptions());
                LOGGY.info("--------------------------");
            }
        }
        LOGGY.info("=============== Properties Summary=====================");
        return properties;
    }

    private static void printwaReport(Vector<WeakAssumption> wk_assumptions) {

        for (WeakAssumption wk : wk_assumptions) {
            LOGGY.info(wk.getwId() + " " + wk.getStatus());
        }
    }
}
