/* See LICENSE in project directory */
package com.ge.verdict.mbas;

import com.ge.verdict.vdm.VdmTranslator;

import verdict.vdm.vdm_model.Model;

import java.io.File;

/** Convert parsed VDM XML to CSV files for input to MBAS (STEM and Soteria++). */
public class VDM2CSV extends VdmTranslator {

    /**
     * Marshal a Verdict data model to Mbas files.
     *
     * <p>Produces the following files:
     *
     * <ul>
     *   <li>ScnArch.csv (STEM and Soteria++)
     *   <li>ScnCompProps.csv (STEM)
     *   <li>ScnConnectionProps.csv (STEM)
     *   <li>CompSaf.csv (Soteria++)
     *   <li>Events.csv (Soteria++)
     *   <li>ScnComp.csv (Soteria++)
     *   <li>CompDep.csv (Soteria++)
     *   <li>Mission.csv (Soteria++)
     * </ul>
     *
     * @param model Verdict data model to marshal
     * @param inputPath input path of the Verdict data model
     * @param stemOutputPath output path where the STEM related CSV files be written to
     * @param soteriaOutputPath output path where the Soteria++ related CSV files be written to
     */
    public static void marshalToMbasInputs(
            Model model, String inputPath, String stemOutputPath, String soteriaOutputPath) {

        String scenario = (new File(inputPath)).getName().replace(".xml", "");

        if (scenario.length() == 0) {
            System.err.println(
                    "Error: Input path is not in the correct format. Scenario name is empty.");
        }
    }
}
