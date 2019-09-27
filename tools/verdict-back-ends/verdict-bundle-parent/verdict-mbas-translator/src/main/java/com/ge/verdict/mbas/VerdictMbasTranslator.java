/* See LICENSE in project directory */
package com.ge.verdict.mbas;

import com.ge.verdict.vdm.VdmTranslator;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import verdict.vdm.vdm_lustre.BinaryOperation;
import verdict.vdm.vdm_lustre.ContractItem;
import verdict.vdm.vdm_lustre.Expression;
import verdict.vdm.vdm_model.ComponentImpl;
import verdict.vdm.vdm_model.ComponentInstance;
import verdict.vdm.vdm_model.ComponentType;
import verdict.vdm.vdm_model.Connection;
import verdict.vdm.vdm_model.Model;

/** Translate a Verdict data model to CSV files for MBAS. */
public class VerdictMbasTranslator extends VdmTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerdictMbasTranslator.class);
    private static final String DOT = "\\.";
    private static final String COLON = "\\:";
    private static final String UNDERSCORE = "_";
    private static final String SLASH = "\\/";
    private Map<String, String> connectionMap = new HashMap<>();

    /**
     * Marshal a Verdict data model to Mbas files.
     *
     * @param model Verdict data model to marshal
     * @param inputPath Input path of the Verdict data model
     * @param stemOutputPath Output path where the STEM related csv files be written to
     * @param soteriaOutputPath Output path where the Soteria++ related csv files be written to
     */
    public void marshalToMbasInputs(
            Model model, String inputPath, String stemOutputPath, String soteriaOutputPath) {
        // Pre-process all contracts to get cyber requirements and formulas
        List<ContractItem> cyberReqs = new ArrayList<>();
        List<ContractItem> formulas = new ArrayList<>();
        List<ComponentType> compTypesOfFormulas = new ArrayList<>();

        if (model.getComponentType() != null) {
            for (ComponentType compType : model.getComponentType()) {
                if (compType.getContract() != null
                        && compType.getContract().getGuarantee() != null) {
                    for (ContractItem contract : compType.getContract().getGuarantee()) {
                        // Contract type: 0 = cyber requirement, 1 = formula, 2 = others
                        int contractType = getContractType(contract);
                        if (contractType == 0) {
                            cyberReqs.add(contract);
                        } else if (contractType == 1) {
                            formulas.add(contract);
                            compTypesOfFormulas.add(compType);
                        }
                    }
                }
            }
        }

        // Build connection map for back propagation
        if (model.getComponentImpl() != null) {
            for (ComponentImpl component : model.getComponentImpl()) {
                if (component.getBlockImpl() != null) {
                    if (component.getBlockImpl().getConnection() != null) {
                        for (Connection connection : component.getBlockImpl().getConnection()) {
                            // Mapping between parent component input port and connected child
                            // component input
                            // port
                            if (connection.getSource().getComponentPort() != null
                                    && connection.getDestination().getSubcomponentPort() != null) {
                                connectionMap.put(
                                        connection.getSource().getComponentPort().getName(),
                                        connection
                                                        .getDestination()
                                                        .getSubcomponentPort()
                                                        .getSubcomponent()
                                                        .getName()
                                                + '.'
                                                + connection
                                                        .getDestination()
                                                        .getSubcomponentPort()
                                                        .getPort()
                                                        .getName());
                            }
                            // Mapping between child component output port and connected parent
                            // component output
                            // port
                            if (connection.getSource().getSubcomponentPort() != null
                                    && connection.getDestination().getComponentPort() != null) {
                                connectionMap.put(
                                        connection.getDestination().getComponentPort().getName(),
                                        connection
                                                        .getSource()
                                                        .getSubcomponentPort()
                                                        .getSubcomponent()
                                                        .getName()
                                                + '.'
                                                + connection
                                                        .getSource()
                                                        .getSubcomponentPort()
                                                        .getPort()
                                                        .getName());
                            }
                        }
                    }
                }
            }
        }
        // Build three csv files
        File mission = buildMissionFile(model, inputPath, soteriaOutputPath, cyberReqs);
        File compDep =
                buildCompDepFile(
                        model, inputPath, soteriaOutputPath, formulas, compTypesOfFormulas);
        List<File> scnArch = buildScnArchFile(model, inputPath, stemOutputPath, soteriaOutputPath);
        File scnComp = buildScnCompFile(model, inputPath, soteriaOutputPath);
        File scnCompProps = buildScnCompPropsFile(model, inputPath, stemOutputPath);
        return;
    }

    /**
     * Identify contract type in the componentType: 0 -> cyber requirement, 1 -> formula, and 2 ->
     * others
     *
     * @param contract Contract for identification
     * @return Type value
     */
    private int getContractType(ContractItem contract) {
        if (contract == null
                || contract.getName() == null
                || contract.getName().isEmpty()
                || contract.getExpression() == null) {
            return 2;
        }
        String[] nameArr = contract.getName().split(COLON);
        if (nameArr.length == 0) {
            return 2;
        }
        String lastElement = nameArr.length > 1 ? nameArr[nameArr.length - 2].trim() : "";
        if (lastElement.equals("Confidentiality")
                || lastElement.equals("Integrity")
                || lastElement.equals("Availability")) {
            return 0;
        } else if (nameArr[nameArr.length - 1].trim().equals("Formula")) {
            return 1;
        }
        return 2;
    }

    /**
     * Build Mission.csv (aadl mission requirements) file for MBAS from VDM model.
     *
     * @param model Verdict data model to translate
     * @param inputPath Input path of the Verdict data model
     * @param outputPath Output path where the generated csv files be written to
     * @return file reference of Mission.csv that is written to
     */
    private File buildMissionFile(
            Model model, String inputPath, String outputPath, List<ContractItem> cyberReqs) {
        File outputFile = new File(outputPath + "/Mission.csv");
        if (canWrite(outputFile, LOGGER)) {
            // Open output stream to be written to
            try (OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile))) {

                String[] tags =
                        new String[] {
                            "ModelVersion",
                            "MissionReqId",
                            "MissionReq",
                            "CyberReqId",
                            "CyberReq",
                            "MissionImpactCIA",
                            "Effect",
                            "Severity",
                            "CompInstanceDependency",
                            "CompOutputDependency",
                            "Confidentiality",
                            "Integrity",
                            "Availability"
                        };
                CsvTable table = new CsvTable(tags);
                // ModelVersion name is the AADL package name which is translated to be name of the
                // VDM file.
                String modelVersion = getModelName(inputPath);
                if (modelVersion.length() == 0) {
                    System.err.println(
                            "Input path is not in the correct format. ModelVersion name is empty.");
                }
                for (ContractItem cyberReqItem : cyberReqs) {
                    String missionReqID = "";
                    String missionReq = "";
                    String[] cyberReqArr = cyberReqItem.getName().split(COLON);
                    String cyberReqId = cyberReqArr.length > 0 ? cyberReqArr[0] : "";
                    String cyberReq = "";
                    String missionImpactCIA = cyberReqArr.length > 1 ? cyberReqArr[1] : "";
                    String effect = "";
                    String severity = cyberReqArr.length > 2 ? cyberReqArr[2] : "";
                    List<String> varNameList = getEquationNames(cyberReqItem.getExpression(), true);
                    for (String varName : varNameList) {
                        String[] varNameArr = varName.split(DOT);
                        String compInstanceDependency = varNameArr.length > 0 ? varNameArr[0] : "";
                        String compoutputDependency = varNameArr.length > 1 ? varNameArr[1] : "";
                        String confidentiality =
                                varNameArr.length > 3 && varNameArr[3].equals("C")
                                        ? "Confidentiality"
                                        : "";
                        String integrity =
                                varNameArr.length > 3 && varNameArr[3].equals("I")
                                        ? "Integrity"
                                        : "";
                        String availability =
                                varNameArr.length > 3 && varNameArr[3].equals("A")
                                        ? "Availability"
                                        : "";
                        // Write to table
                        String[] row =
                                new String[] {
                                    modelVersion,
                                    missionReqID,
                                    missionReq,
                                    cyberReqId,
                                    cyberReq,
                                    missionImpactCIA,
                                    effect,
                                    severity,
                                    compInstanceDependency,
                                    compoutputDependency,
                                    confidentiality,
                                    integrity,
                                    availability
                                };
                        table.writeToTable(row);
                    }
                }
                output.write(table.printToCSV().getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                // throw new RuntimeException(e);
                System.err.println(e);
            }
        }

        return outputFile;
    }

    /**
     * Build CompDep.csv (aadl component abstract formulas) file for MBAS from VDM model.
     *
     * @param model Verdict data model to translate
     * @param inputPath Input path of the Verdict data model
     * @param outputPath Output path where the generated csv files be written to
     * @return file reference of CompDep.csv that is written to
     */
    private File buildCompDepFile(
            Model model,
            String inputPath,
            String outputPath,
            List<ContractItem> formulas,
            List<ComponentType> compTypesOfFormulas) {
        File outputFile = new File(outputPath + "/CompDep.csv");
        if (canWrite(outputFile, LOGGER)) {
            // Open output stream to be written to
            try (OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile))) {

                String[] tags =
                        new String[] {
                            "CompType", "InputPort", "InputCIA", "OutputPort", "OutputCIA"
                        };
                CsvTable table = new CsvTable(tags);
                for (int i = 0; i < formulas.size(); i++) {
                    ContractItem formula = formulas.get(i);
                    String compType = compTypesOfFormulas.get(i).getName();
                    String outputVar = getOutputEquationOfImplies(formula.getExpression());
                    String[] outputVarArr = outputVar.split(DOT);
                    String outputPort = outputVarArr.length > 0 ? outputVarArr[0] : "";
                    String outputCIA = "";
                    if (outputVarArr.length > 2 && outputVarArr[2].equals("C")) {
                        outputCIA = "Confidentiality";
                    } else if (outputVarArr.length > 2 && outputVarArr[2].equals("I")) {
                        outputCIA = "Integrity";
                    } else if (outputVarArr.length > 2 && outputVarArr[2].equals("A")) {
                        outputCIA = "Availability";
                    }
                    List<String> varNameList = getEquationNames(formula.getExpression(), false);
                    // if no inputs, then only write the outputs
                    if (varNameList.isEmpty()) {
                        String[] row = new String[] {compType, "", "", outputPort, outputCIA};
                        table.writeToTable(row);
                    }
                    for (String varName : varNameList) {
                        if (varName == outputVar) {
                            continue;
                        }
                        String[] varNameArr = varName.split(DOT);
                        String inputPort = varNameArr.length > 0 ? varNameArr[0] : "";
                        String inputCIA = "";
                        if (varNameArr.length > 2 && varNameArr[2].equals("C")) {
                            inputCIA = "Confidentiality";
                        } else if (varNameArr.length > 2 && varNameArr[2].equals("I")) {
                            inputCIA = "Integrity";
                        } else if (varNameArr.length > 2 && varNameArr[2].equals("A")) {
                            inputCIA = "Availability";
                        }
                        // Write to table
                        String[] row =
                                new String[] {compType, inputPort, inputCIA, outputPort, outputCIA};
                        table.writeToTable(row);
                    }
                }
                output.write(table.printToCSV().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return outputFile;
    }

    /**
     * Build ScnCompProps.csv (aadl architecture) file for MBAS from VDM model.
     *
     * @param model Verdict data model to translate
     * @param inputPath Input path of the Verdict data model
     * @param outputPath Output path where the generated csv files be written to
     * @return file reference of ScnArch.csv that is written to
     */
    private File buildScnCompPropsFile(Model model, String inputPath, String outputPath) {
        File outputFile = new File(outputPath + "/ScnCompProps.csv");
        if (canWrite(outputFile, LOGGER)) {
            // Open output stream to be written to
            try (OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile))) {

                String[] tags =
                        new String[] {
                            "Scenario",
                            "CompType",
                            "CompInstance",
                            "hasSensitiveInfo",
                            "insideTrustedBoundary",
                            "broadcastFromOutsideTB",
                            "wifiFromOutsideTB",
                            "heterogeneity",
                            "encryption",
                            "antiJamming",
                            "antiFlooding",
                            "antiFuzzing"
                        };
                CsvTable table = new CsvTable(tags);
                for (ComponentImpl component : model.getComponentImpl()) {
                    // Scenario name is the AADL package name which is translated to be name of the
                    // VDM file.
                    String scenario = getModelName(inputPath);
                    if (scenario.length() == 0) {
                        System.err.println(
                                "Input path is not in the correct format. Scenario name is empty.");
                    }
                    for (ComponentInstance compInst : component.getBlockImpl().getSubcomponent()) {
                        if (compInst.getSpecification() == null) {
                            continue;
                        }
                        String compType = compInst.getSpecification().getName();
                        String compInstance = compInst.getName();
                        String hasSensitiveInfo =
                                compInst.isHasSensitiveInfo() != null
                                        ? (compInst.isHasSensitiveInfo() ? "1" : "0")
                                        : "";
                        String insideTrustedBoundary =
                                compInst.isInsideTrustedBoundary() != null
                                        ? (compInst.isInsideTrustedBoundary() ? "1" : "0")
                                        : "";
                        ;
                        String broadcastFromOutsideTB =
                                compInst.isBroadcastFromOutsideTB() != null
                                        ? (compInst.isBroadcastFromOutsideTB() ? "1" : "0")
                                        : "";
                        ;
                        String wifiFromOutsideTB =
                                compInst.isWifiFromOutsideTB() != null
                                        ? (compInst.isWifiFromOutsideTB() ? "1" : "0")
                                        : "";
                        ;
                        String heterogeneity = "";
                        if (compInst.isHeterogeneity() != null) {
                            heterogeneity = compInst.isHeterogeneity() ? "1" : "0";
                            heterogeneity +=
                                    compInst.getHeterogeneityDal() == null
                                            ? ""
                                            : "#" + compInst.getHeterogeneityDal().toString();
                        }
                        String encryption = "";
                        if (compInst.isEncryption() != null) {
                            encryption = compInst.isEncryption() ? "1" : "0";
                            encryption +=
                                    compInst.getEncryptionDal() == null
                                            ? ""
                                            : "#" + compInst.getEncryptionDal().toString();
                        }
                        String antiJamming = "";
                        if (compInst.isAntiJamming() != null) {
                            antiJamming = compInst.isAntiJamming() ? "1" : "0";
                            antiJamming +=
                                    compInst.getAntiJammingDal() == null
                                            ? ""
                                            : "#" + compInst.getAntiJammingDal().toString();
                        }
                        String antiFlooding = "";
                        if (compInst.isAntiFlooding() != null) {
                            antiFlooding = compInst.isAntiFlooding() ? "1" : "0";
                            antiFlooding +=
                                    compInst.getAntiFloodingDal() == null
                                            ? ""
                                            : "#" + compInst.getAntiFloodingDal().toString();
                        }
                        String antiFuzzing = "";
                        if (compInst.isAntiFuzzing() != null) {
                            antiFuzzing = compInst.isAntiFuzzing() ? "1" : "0";
                            antiFuzzing +=
                                    compInst.getAntiFuzzingDal() == null
                                            ? ""
                                            : "#" + compInst.getAntiFuzzingDal().toString();
                        }
                        // Write to table
                        String[] row =
                                new String[] {
                                    scenario,
                                    compType,
                                    compInstance,
                                    hasSensitiveInfo,
                                    insideTrustedBoundary,
                                    broadcastFromOutsideTB,
                                    wifiFromOutsideTB,
                                    heterogeneity,
                                    encryption,
                                    antiJamming,
                                    antiFlooding,
                                    antiFuzzing
                                };
                        table.writeToTable(row);
                    }
                }
                output.write(table.printToCSV().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return outputFile;
    }

    /**
     * Build ScnComp.csv (aadl architecture) file for MBAS from VDM model.
     *
     * @param model Verdict data model to translate
     * @param inputPath Input path of the Verdict data model
     * @param outputPath Output path where the generated csv files be written to
     * @return file reference of ScnArch.csv that is written to
     */
    private File buildScnCompFile(Model model, String inputPath, String outputPath) {
        File outputFile = new File(outputPath + "/ScnComp.csv");
        if (canWrite(outputFile, LOGGER)) {
            // Open output stream to be written to
            try (OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile))) {

                Map<String, Integer> compCount = getCompCount(model);
                String[] tags = new String[] {"Scenario", "Parent", "CompType", "Cardinality"};
                CsvTable table = new CsvTable(tags);
                for (ComponentImpl component : model.getComponentImpl()) {
                    // Scenario name is the AADL package name which is translated to be name of the
                    // VDM file.
                    String scenario = getModelName(inputPath);
                    String parent = component.getType().getName();
                    if (scenario.length() == 0) {
                        System.err.println(
                                "Input path is not in the correct format. Scenario name is empty.");
                    }
                    for (ComponentInstance compInst : component.getBlockImpl().getSubcomponent()) {
                        if (compInst.getSpecification() == null) {
                            continue;
                        }
                        String compType = compInst.getSpecification().getName();
                        String cardinality = Integer.toString(compCount.get(compType));
                        // Write to table
                        String[] row = new String[] {scenario, parent, compType, cardinality};
                        table.writeToTable(row);
                    }
                }
                output.write(table.printToCSV().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return outputFile;
    }

    /**
     * Build ScnArch.csv (aadl architecture) file for MBAS from VDM model.
     *
     * @param model Verdict data model to translate
     * @param inputPath Input path of the Verdict data model
     * @param stemOutputPath Output path where the generated csv files be written to
     * @param soteriaOutputPath Output path where the generated csv files be written to
     * @return file references of ScnArch.csv that are written to
     */
    private List<File> buildScnArchFile(
            Model model, String inputPath, String stemOutputPath, String soteriaOutputPath) {
        File stemOutputFile = new File(stemOutputPath + "/ScnArch.csv");
        File soteriaOutputFile = new File(soteriaOutputPath + "/ScnArch.csv");
        if (canWrite(stemOutputFile, LOGGER) && canWrite(soteriaOutputFile, LOGGER)) {
            String[] tags =
                    new String[] {
                        "Scenario",
                        "ConnectionName",
                        "SrcCompType",
                        "SrcCompInstance",
                        "SrcPortName",
                        "DestCompType",
                        "DestCompInstance",
                        "DestPortName",
                        "Flow1",
                        "Flow2",
                        "Flow3"
                    };
            CsvTable table = new CsvTable(tags);
            for (ComponentImpl component : model.getComponentImpl()) {
                // Scenario name is the AADL package name which is translated to be name of the
                // VDM file.
                String scenario = getModelName(inputPath);
                if (scenario.length() == 0) {
                    System.err.println(
                            "Input path is not in the correct format. Scenario name is empty.");
                }
                if (component.getBlockImpl() != null
                        && component.getBlockImpl().getConnection() != null) {
                    for (Connection connection : component.getBlockImpl().getConnection()) {
                        if (connection.getSource().getSubcomponentPort() == null
                                || connection.getDestination().getSubcomponentPort() == null) {
                            continue;
                        }
                        String connectionName = connection.getName();
                        String srcCompType =
                                connection
                                        .getSource()
                                        .getSubcomponentPort()
                                        .getSubcomponent()
                                        .getSpecification()
                                        .getName();
                        String srcCompInstance =
                                connection
                                        .getSource()
                                        .getSubcomponentPort()
                                        .getSubcomponent()
                                        .getName();
                        String srcPortName =
                                connection.getSource().getSubcomponentPort().getPort().getName();
                        String destCompType =
                                connection
                                        .getDestination()
                                        .getSubcomponentPort()
                                        .getSubcomponent()
                                        .getSpecification()
                                        .getName();
                        String destCompInstance =
                                connection
                                        .getDestination()
                                        .getSubcomponentPort()
                                        .getSubcomponent()
                                        .getName();
                        String destPortName =
                                connection
                                        .getDestination()
                                        .getSubcomponentPort()
                                        .getPort()
                                        .getName();
                        String flow1 = connection.getFlow().name().equals("XDATA") ? "Xdata" : "";
                        String flow2 =
                                connection.getFlow().name().equals("CONTROL") ? "Control" : "";
                        String flow3 = connection.getFlow().name().equals("DATA") ? "Data" : "";
                        // Write to table
                        String[] row =
                                new String[] {
                                    scenario,
                                    connectionName,
                                    srcCompType,
                                    srcCompInstance,
                                    srcPortName,
                                    destCompType,
                                    destCompInstance,
                                    destPortName,
                                    flow1,
                                    flow2,
                                    flow3
                                };
                        table.writeToTable(row);
                    }
                }
            }
            String content = table.printToCSV();
            // Open output stream for STEM inputs
            try (OutputStream output =
                    new BufferedOutputStream(new FileOutputStream(stemOutputFile))) {
                output.write(content.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // Open output stream for Soteria++ inputs
            try (OutputStream output =
                    new BufferedOutputStream(new FileOutputStream(soteriaOutputFile))) {
                output.write(content.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        List<File> outputFiles = new ArrayList<>();
        outputFiles.add(stemOutputFile);
        outputFiles.add(soteriaOutputFile);
        return outputFiles;
    }

    /**
     * Get model name from input path
     *
     * @param inputPath Input path of the Verdict data model
     * @return File name of the VDM model
     */
    private String getModelName(String inputPath) {
        String[] dirArray = inputPath.split(SLASH);
        // Get the last non-empty string with .xml
        for (int i = dirArray.length - 1; i >= 0; i--) {
            if (dirArray[i].length() != 0) {
                if (dirArray[i].contains(".xml")) {
                    return dirArray[i].replaceFirst(".xml", "");
                } else {
                    return "";
                }
            }
        }
        return "";
    }

    /**
     * Get variable names from cyber requirement or formula expressions: cyberReq: var1 and var2 and
     * ... varn formula: var1 and var2 and ... varn => varOutput (TODO) Need generalize
     *
     * @param expr Guarantee expression for cyber requirement or formula
     * @return List of variable names in the expression
     */
    private List<String> getVariableNames(Expression expr) {
        if (expr == null) {
            return new ArrayList<>();
        }
        Set<String> visited = new HashSet<>();
        if (expr.getImplies() != null) {
            searchVariablesInAnd(visited, expr.getImplies().getLhsOperand());
        } else {
            searchVariablesInAnd(visited, expr);
        }
        return new ArrayList<>(visited);
    }

    /**
     * Recursively search variables in the And operations:
     *
     * @param visited Added variable names
     * @param expr Expression for search
     */
    private void searchVariablesInAnd(Set<String> visited, Expression expr) {
        if (expr.getAnd() == null) {
            if (expr.getIdentifier() != null) {
                visited.add(expr.getIdentifier());
            }
            return;
        }
        searchVariablesInAnd(visited, expr.getAnd().getLhsOperand());
        searchVariablesInAnd(visited, expr.getAnd().getRhsOperand());
        return;
    }

    /**
     * Get output variable name from formula expression: formula: var1 and var2 and ... varn =>
     * varOutput
     *
     * @param expr Guarantee expression for formula
     * @return Output variable name in the expression
     */
    private String getOutputOfImplies(Expression expr) {
        if (expr == null
                || expr.getImplies() == null
                || expr.getImplies().getRhsOperand() == null
                || expr.getImplies().getRhsOperand().getIdentifier() == null) {
            return "";
        }
        return expr.getImplies().getRhsOperand().getIdentifier();
    }

    /**
     * Get equation names from cyber requirement or formula expressions: cyberReq: equation1 and
     * equation2 and ... equationn formula: equation1 and equation2 and ... equationn =>
     * equation_output (TODO) Need generalize
     *
     * @param expr Guarantee expression for cyber requirement or formula
     * @return List of equation names in the expression
     */
    private List<String> getEquationNames(Expression expr, boolean isCyberReq) {
        if (expr == null) {
            return new ArrayList<>();
        }
        Set<String> visited = new HashSet<>();
        if (expr.getImplies() != null) {
            searchEquationsInExpr(visited, expr.getImplies().getLhsOperand(), isCyberReq);
        } else {
            searchEquationsInExpr(visited, expr, isCyberReq);
        }
        return new ArrayList<>(visited);
    }

    /**
     * Assume the input expr is a (and, or) formula of equations. Recursively search equations in
     * the And operations:
     *
     * @param visited Added equation names
     * @param expr Expression for search
     * @param isCyberReq True if it is a cyber requirement
     */
    private void searchEquationsInExpr(Set<String> visited, Expression expr, boolean isCyberReq) {
        // Base case is an equatioin
        if (expr.getEqual() != null) {
            String equationName = buildEquationName(expr.getEqual(), isCyberReq);
            visited.add(equationName);
            return;
        }
        if (expr.getOr() != null) {
            searchEquationsInExpr(visited, expr.getOr().getLhsOperand(), isCyberReq);
            searchEquationsInExpr(visited, expr.getOr().getRhsOperand(), isCyberReq);
        } else if (expr.getAnd() != null) {
            searchEquationsInExpr(visited, expr.getAnd().getLhsOperand(), isCyberReq);
            searchEquationsInExpr(visited, expr.getAnd().getRhsOperand(), isCyberReq);
        } else {
            // This is expression: NoInputCIA_C(I, A)
            // TODO: Be Careful: if the input variable is a Boolean, I am not sure if we still use
            // equations or just use the variable.
            return;
        }
        //        if (expr.getAnd() == null) {
        //            if (expr.getEqual() != null) {
        //                if (expr.getEqual().getLhsOperand() == null
        //                        || expr.getEqual().getLhsOperand().getIdentifier() == null
        //                        || expr.getEqual().getRhsOperand() == null
        //                        || expr.getEqual().getRhsOperand().getIdentifier() == null) {
        //                    return;
        //                }
        //                String equationName = buildEquationName(expr.getEqual(), isCyberReq);
        //                visited.add(equationName);
        //            }
        //            return;
        //        }
        //        searchEquationsInExpr(visited, expr.getAnd().getLhsOperand(), isCyberReq);
        //        searchEquationsInExpr(visited, expr.getAnd().getRhsOperand(), isCyberReq);
        //        return;
    }

    /**
     * Get output equation name pattern from formula expression: formula: equation1 and equation2
     * and ... equationn => equation_output
     *
     * @param expr Guarantee expression for formula
     * @return Output equation name pattern "portName.cia.<C/I/A>"
     */
    private String getOutputEquationOfImplies(Expression expr) {
        if (expr == null
                || expr.getImplies() == null
                || expr.getImplies().getRhsOperand() == null
                || expr.getImplies().getRhsOperand().getEqual() == null) {
            return "";
        }
        return buildEquationName(expr.getImplies().getRhsOperand().getEqual(), false);
    }

    /**
     * Get output equation name pattern from formula expression: formula: equation1 and equation2
     * and ... equationn => equation_output
     *
     * @param equation Equation at the right side of the implies expression
     * @param isCyberReq True if it is a cyber requirement
     * @return Output equation name pattern "portName.cia.<C/I/A>"
     */
    private String buildEquationName(BinaryOperation equation, boolean isCyberReq) {
        StringBuilder sb = new StringBuilder();
        String leftHandSideString = equation.getLhsOperand().getIdentifier();
        String leftHandSide =
                isCyberReq ? connectionMap.get(leftHandSideString) : leftHandSideString;
        String rightHandSideString = equation.getRhsOperand().getIdentifier();
        String rightHandSide =
                rightHandSideString.length() > 5
                        ? rightHandSideString.substring(
                                rightHandSideString.length() - 5, rightHandSideString.length())
                        : "";
        rightHandSide = rightHandSide.replace(UNDERSCORE, ".");
        if (leftHandSide == null
                || leftHandSide.length() == 0
                || rightHandSide.length() == 0
                || !rightHandSide.contains("cia.")) {
            return "";
        }
        sb.append(leftHandSide).append(".").append(rightHandSide);
        return sb.toString();
    }

    /**
     * Compute the number of instances per component type
     *
     * @param model VDM model
     * @return Output a mapping between component type and the corresponding count
     */
    private Map<String, Integer> getCompCount(Model model) {
        Map<String, Integer> compCount = new HashMap<>();
        if (model.getComponentImpl() != null) {
            for (ComponentImpl component : model.getComponentImpl()) {
                if (component.getBlockImpl() != null) {
                    if (component.getBlockImpl().getSubcomponent() != null) {
                        for (ComponentInstance compInst :
                                component.getBlockImpl().getSubcomponent()) {
                            if (compInst.getSpecification() == null
                                    || compInst.getSpecification().getName() == null) {
                                continue;
                            }
                            String compType = compInst.getSpecification().getName();
                            compCount.put(compType, compCount.getOrDefault(compType, 0) + 1);
                        }
                    }
                }
            }
        }
        return compCount;
    }
}
