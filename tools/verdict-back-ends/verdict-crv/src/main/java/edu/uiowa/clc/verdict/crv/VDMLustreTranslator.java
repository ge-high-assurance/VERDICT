/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   Copyright (c) 2019-2020, General Electric Company.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

    @author: John Interrante
    @author: M. Fareed Arif
*/

package edu.uiowa.clc.verdict.crv;

import com.ge.verdict.lustre.VerdictLustreTranslator;
import edu.uiowa.clc.verdict.lustre.VDM2Lustre;
import java.io.File;
import verdict.vdm.vdm_model.Model;

// Accepts VDMDataModel (preferably in memory) and Translates into Lustre code/file.
public class VDMLustreTranslator {

    public static Model getVDM(File inputFile) {

        return VerdictLustreTranslator.unmarshalFromXml(inputFile);
    }

    // Translate to DataFlow
    public static Model getDataFlow(Model verdictDataModel) {

        VDM2Lustre vdm2Lustre = new VDM2Lustre(verdictDataModel);
        verdictDataModel = vdm2Lustre.translate();

        return verdictDataModel;
    }

    public static void dumpLustre(Model verdictDataModel, File outputFile) {
        VerdictLustreTranslator.marshalToLustre(verdictDataModel, outputFile);
    }
}
