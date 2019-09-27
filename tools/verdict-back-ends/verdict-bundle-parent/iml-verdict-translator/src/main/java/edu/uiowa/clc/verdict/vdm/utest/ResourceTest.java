/* See LICENSE in project directory */
package edu.uiowa.clc.verdict.vdm.utest;

import com.google.inject.Injector;
import com.utc.utrc.hermes.iml.ImlStandaloneSetup;
import com.utc.utrc.hermes.iml.iml.Model;
import edu.uiowa.clc.verdict.vdm.translator.IModelVisitor;
import edu.uiowa.clc.verdict.vdm.translator.Token;
import edu.uiowa.clc.verdict.vdm.translator.Type;
import edu.uiowa.clc.verdict.vdm.translator.VDMParser;
import java.io.IOException;
import java.util.ArrayList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;

public class ResourceTest {

    public static verdict.vdm.vdm_model.Model setup(String input_file) throws IOException {

        Injector injector = ImlStandaloneSetup.getInjector();

        XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);

        resourceSet.addLoadOption(XtextResource.OPTION_RESOLVE_ALL, Boolean.TRUE);
        // Common artifacts
        resourceSet.getResource(URI.createURI("classpath:/iml-common/lang.iml"), true);
        resourceSet.getResource(URI.createURI("classpath:/iml-common/utils.iml"), true);
        // Model artifacts
        resourceSet.getResource(URI.createURI("classpath:/iml-common/vdm_data.iml"), true);
        resourceSet.getResource(URI.createURI("classpath:/iml-common/vdm_lustre.iml"), true);
        resourceSet.getResource(URI.createURI("classpath:/iml-common/vdm_model.iml"), true);

        Resource resource = resourceSet.getResource(URI.createURI(input_file), true);
        // Model model = (Model) resource.getContents().get(0);
        Model iml_model = (Model) resource.getContents().get(0);

        EList<Resource.Diagnostic> model_errors = iml_model.eResource().getErrors();

        if (model_errors.size() > 0) {
            System.err.println("Errors Reported in IML Model!!!");
            for (Resource.Diagnostic dig : model_errors) {
                System.err.println(dig.getLine() + " : " + dig.getMessage());
            }
            System.exit(-1);
        }

        //        ImlValidator model_validator =  new ImlValidator();
        //        org.eclipse.emf.common.util.Diagnostic diagnostic =
        //                Diagnostician.INSTANCE.validate(iml_model);

        //        Diagnostic diagnostic = Diagnostician.INSTANCE.validate(iml_model);
        //        LOGGER.info(diagnostic.toString());

        // TODO: Update ArrayList with Array & Free used Tokens.
        ArrayList<Token> tokens = null;

        IModelVisitor visitor = new IModelVisitor();

        visitor.visit(iml_model);
        tokens = visitor.iml_tokens;
        tokens.add(new Token(null, Type.EOF));

        VDMParser vdm_parser = new VDMParser(visitor.iml_tokens);

        verdict.vdm.vdm_model.Model vdm_model = vdm_parser.model();

        if (vdm_parser.token.type != Type.EOF) {
            System.err.println("Failure!!! -- Tokens remains: " + vdm_parser.token);
        }

        return vdm_model;
    }

    //    public static void main(String[] args) throws IOException {
    //        ResourceTest.setup("hawkeye-UAV/eg/hawkeyeUAV_model_C.iml");
    //    }
}
