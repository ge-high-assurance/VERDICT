/* See LICENSE in project directory */
package edu.uiowa.clc.verdict.crv;

import com.ge.verdict.lustre.VerdictLustreTranslator;
import edu.uiowa.clc.verdict.lustre.VDM2Lustre;
import java.io.File;
import verdict.vdm.vdm_model.Model;

// Accepts VDMDataModel (preferably in memory) and Translates into Lustre code/file.
public class VDMLustreTranslator {

    private VerdictLustreTranslator translator = new VerdictLustreTranslator();

    public Model getVDM(File inputFile) {

        return translator.unmarshalFromXml(inputFile);
    }

    // Translate to DataFlow
    public Model getDataFlow(Model verdictDataModel) {

        VDM2Lustre vdm2Lustre = new VDM2Lustre(verdictDataModel);
        verdictDataModel = vdm2Lustre.translate();

        return verdictDataModel;
    }

    public void dumpLustre(Model verdictDataModel, File outputFile) {
        translator.marshalToLustre(verdictDataModel, outputFile);
    }
}
