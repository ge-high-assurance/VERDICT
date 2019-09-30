/* See LICENSE in project directory */
package edu.uiowa.clc.verdict.lustre;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import verdict.vdm.vdm_data.DataType;
import verdict.vdm.vdm_data.RecordField;
import verdict.vdm.vdm_data.RecordType;
import verdict.vdm.vdm_data.TypeDeclaration;
import verdict.vdm.vdm_lustre.BinaryOperation;
import verdict.vdm.vdm_lustre.ConstantDeclaration;
import verdict.vdm.vdm_lustre.Contract;
import verdict.vdm.vdm_lustre.ContractItem;
import verdict.vdm.vdm_lustre.ContractSpec;
import verdict.vdm.vdm_lustre.Expression;
import verdict.vdm.vdm_lustre.FieldDefinition;
import verdict.vdm.vdm_lustre.IfThenElse;
import verdict.vdm.vdm_lustre.LustreProgram;
import verdict.vdm.vdm_lustre.Node;
import verdict.vdm.vdm_lustre.NodeBody;
import verdict.vdm.vdm_lustre.NodeCall;
import verdict.vdm.vdm_lustre.NodeEquation;
import verdict.vdm.vdm_lustre.NodeEquationLHS;
import verdict.vdm.vdm_lustre.NodeParameter;
import verdict.vdm.vdm_lustre.RecordLiteral;
import verdict.vdm.vdm_lustre.SymbolDefinition;
import verdict.vdm.vdm_lustre.VariableDeclaration;
import verdict.vdm.vdm_model.BlockImpl;
import verdict.vdm.vdm_model.CompInstancePort;
import verdict.vdm.vdm_model.ComponentImpl;
import verdict.vdm.vdm_model.ComponentInstance;
import verdict.vdm.vdm_model.ComponentType;
import verdict.vdm.vdm_model.Connection;
import verdict.vdm.vdm_model.ConnectionEnd;
import verdict.vdm.vdm_model.Model;
import verdict.vdm.vdm_model.Port;
import verdict.vdm.vdm_model.PortMode;

public class VDM2Lustre {

    private Model vdm_model;
    private HashMap<String, TypeDeclaration> typeDeclarations = null;

    public VDM2Lustre(Model vdm_model) {
        this.vdm_model = vdm_model;
        this.typeDeclarations = new HashMap<String, TypeDeclaration>();
    }

    public Model translate() {

        Model dataFlowModel = visit(vdm_model);

        return dataFlowModel;
    }

    public Model visit(Model vdm_model) {

        Model dataFlowModel = new Model();
        // I) Naming Model *.lus
        String program_name = vdm_model.getName() + ".lus";
        dataFlowModel.setName(program_name);

        // II) Copying exiting DataFlow code
        LustreProgram lustre_program = vdm_model.getDataflowCode();

        visit(vdm_model, lustre_program);

        dataFlowModel.setDataflowCode(lustre_program);

        return dataFlowModel;
    }

    public void visit(Model vdm_model, LustreProgram program) {

        // A) Copying Type Declaration + amend name '_Impl
        for (TypeDeclaration typeDec : vdm_model.getTypeDeclaration()) {
            visit(typeDec, program);
        }

        // Copying over Constant Declarations
        for (ConstantDeclaration constDec : program.getConstantDeclaration()) {
            visit(constDec);
        }

        // B) Component Type
        Node node = null;

        for (ComponentType componentType : vdm_model.getComponentType()) {

            boolean is_implemented = false;

            for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {

                if (componentType == componentImpl.getType()) {
                    node = visit(componentType, true);

                    visit(componentImpl, node);
                    is_implemented = true;
                }
            }
            if (is_implemented == false) {
                node = visit(componentType, false);
            }

            program.getNodeDeclaration().add(node);
        }

        // Copying over Node Declarations.
        for (Node node_dec : program.getNodeDeclaration()) {
            visit(node_dec, program);
        }

        // Copy over Contract Spec.
        for (Contract contract : program.getContractDeclaration()) {
            visit(contract, program);
        }
    }

    // 1) Node signature +/- contract
    // a) Imported Node (Contract, no Implementation)
    // b) Node Impl
    // c) Node Impl + contract
    // d) @TODO: no Contract, no Implementation -- (*@contract gurantee true*)
    public Node visit(ComponentType componentType, boolean is_implemented) {

        Node node = new Node();

        String identifier = componentType.getName();

        if (is_implemented) {
            identifier += "_Impl";

        } else {
            // Imported Node
            node.setIsImported(true);
        }

        node.setName(identifier);

        // Port
        for (Port port : componentType.getPort()) {
            visit(port, node);
        }

        // + Contract (Optional)
        ContractSpec contractSpec = componentType.getContract();

        if (is_implemented == false && contractSpec == null) {
            // Rename output renaming to avoid Duplicate.
            //            List<NodeParameter> node_parameters = node.getOutputParameter();
            //            for (NodeParameter instrumented_param : node_parameters) {
            //                String param_identifier = instrumented_param.getName();
            //                instrumented_param.setName(param_identifier + "_intrumented");
            //            }

            ContractItem true_guarantee_item = new ContractItem();
            //            true_guarantee_item.setName("true");
            Expression true_expr = new Expression();
            Boolean true_lit = new Boolean("true");
            true_expr.setBoolLiteral(true_lit);
            true_guarantee_item.setExpression(true_expr);

            contractSpec = new ContractSpec();
            contractSpec.getGuarantee().add(true_guarantee_item);

            componentType.setContract(contractSpec);
        }

        if (contractSpec != null) {
            visit(contractSpec);

            if (contractSpec.getGuarantee().size() != 0) {
                node.setContract(contractSpec);
            }
        }

        return node;
    }

    // B) Component Implementation Translated into Lustre Node
    public void visit(ComponentImpl componentImpl, Node node) {

        NodeBody nodeBody = new NodeBody();

        // Option 1) Block Implementation
        BlockImpl blockImpl = componentImpl.getBlockImpl();

        // BlockImpl
        if (blockImpl != null) {

            ComponentType componentType = componentImpl.getType();

            for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {

                componentType = componentInstance.getSpecification();
                ComponentImpl subcomponentImpl = componentInstance.getImplementation();

                // Option 1) Implementation
                if (subcomponentImpl != null) {

                    componentType = subcomponentImpl.getType();
                    visit(componentType, nodeBody, componentInstance.getId(), true);
                }

                // Option 2) Specification
                else if (componentType != null) {
                    visit(componentType, nodeBody, componentInstance.getId(), false);
                }
            }

            for (Connection connection : blockImpl.getConnection()) {
                visit(connection, nodeBody);
            }

        } else {
            // Option 2) DataFlow Implementation / NodeBody
            nodeBody = componentImpl.getDataflowImpl();
            // node.setBody(nodeBody);
        }

        node.setBody(nodeBody);
    }

    public void visit(
            ComponentType componentType,
            NodeBody nodeBody,
            String componentInstanceID,
            boolean impl_type) {

        // Node Equation
        NodeEquation node_eq = new NodeEquation();

        // Return variables
        NodeEquationLHS eq_lhs = new NodeEquationLHS();

        // Node Call
        Expression node = new Expression();
        NodeCall nodeCall = new NodeCall();

        if (impl_type) {
            nodeCall.setNodeId(componentType.getName() + "_Impl");
        } else {
            nodeCall.setNodeId(componentType.getName());
        }

        // Port
        for (Port port : componentType.getPort()) {
            // MODE
            PortMode port_mode = port.getMode();

            if (port_mode == PortMode.IN) {
                Expression arg = new Expression();
                arg.setIdentifier(port.getName());

                nodeCall.getArgument().add(arg);
            }

            if (port_mode == PortMode.OUT) {
                VariableDeclaration var = new VariableDeclaration();

                // Node Name
                String output_name = port.getName();
                String var_name = componentInstanceID + "_port_" + output_name;
                var.setName(var_name);
                // Node DataType
                DataType dataType = port.getType();

                var.setDataType(dataType);
                nodeBody.getVariableDeclaration().add(var);

                eq_lhs.getIdentifier().add(var_name);
            }
        }

        node.setCall(nodeCall);

        node_eq.setRhs(node);
        node_eq.setLhs(eq_lhs);

        nodeBody.getEquation().add(node_eq);
    }

    public void visit(Port port, Node node) {

        PortMode port_mode = port.getMode();

        NodeParameter node_parameter = new NodeParameter();

        // Node Name
        String port_name = port.getName();
        node_parameter.setName(port_name);

        // Node DataType
        DataType dataType = port.getType();
        node_parameter.setDataType(dataType);

        // Node Input Parameter
        if (port_mode == PortMode.IN) {
            node.getInputParameter().add(node_parameter);
        }
        // Node OutputParameter
        else if (port_mode == PortMode.OUT) {
            node.getOutputParameter().add(node_parameter);
        }
    }

    public void visit(Contract contract, LustreProgram program) {

        for (ContractSpec contractSpec : contract.getSpecification()) {
            visit(contractSpec);
        }
        // program.getContractDeclaration().add(contract);
    }

    public void visit(ContractSpec contractSpec) {

        for (SymbolDefinition symbol : contractSpec.getSymbol()) {
            DataType data_type = symbol.getDataType();

            String user_defined_type = data_type.getUserDefinedType();

            if (user_defined_type != null) {
                user_defined_type = user_defined_type + "_Impl";

                boolean implemented_type = typeDeclarations.containsKey(user_defined_type);

                if (implemented_type) {
                    data_type.setUserDefinedType(user_defined_type);
                }
            }

            Expression expr = symbol.getDefinition();
            recordLiteral(expr);
        }

        String mbas_fml = "((.+):(.+):(.+))|((.+):Formula$)";
        Pattern fml_pattern = Pattern.compile(mbas_fml);

        List<ContractItem> mbas_guarantee = new ArrayList<ContractItem>();
        // guarantee
        for (ContractItem contractItem : contractSpec.getGuarantee()) {
            if (contractItem.getName() != null) {
                Matcher m = fml_pattern.matcher(contractItem.getName());

                if (m.find()) {
                    mbas_guarantee.add(contractItem);
                } else {
                    // Normal Formula
                    visit(contractItem);
                }
            } else {
                visit(contractItem);
            }
        }
        // remove MBAS guarantee
        contractSpec.getGuarantee().removeAll(mbas_guarantee);
    }

    public void visit(ContractItem contractItem) {

        Expression expr = contractItem.getExpression();
        recordLiteral(expr);
    }

    public void recordLiteral(Expression expr) {

        if (expr != null) {

            RecordLiteral recordLiteral = expr.getRecordLiteral();

            if (recordLiteral != null) {

                String identifier = recordLiteral.getRecordType();
                identifier = identifier.replace(".i", "_I");
                recordLiteral.setRecordType(identifier);

                for (FieldDefinition fieldDef : recordLiteral.getFieldDefinition()) {
                    expr = fieldDef.getFieldValue();
                    recordLiteral(expr);
                }

            } else {

                BinaryOperation op = expr.getEqual();

                if (op == null) {

                    op = expr.getNotEqual();

                    if (op == null) {
                        op = expr.getAnd();

                        if (op == null) {
                            op = expr.getImplies();
                        }
                    }
                }

                if (op != null) {

                    Expression lhs_expr = op.getLhsOperand();
                    recordLiteral(lhs_expr);

                    Expression rhs_expr = op.getRhsOperand();
                    recordLiteral(rhs_expr);
                }

                Expression not_expr = expr.getNot();

                if (not_expr != null) {
                    recordLiteral(not_expr);
                } else {

                    Expression pre_expr = expr.getPre();
                    if (pre_expr != null) {
                        recordLiteral(pre_expr);
                    }
                }

                NodeCall nodeCall = expr.getCall();

                if (nodeCall != null) {

                    for (Expression arg : nodeCall.getArgument()) {
                        recordLiteral(arg);
                    }
                }
            }
        }
    }

    public void visit(Node node, LustreProgram program) {

        for (NodeParameter node_param : node.getInputParameter()) {
            visit(node_param);
        }

        for (NodeParameter node_param : node.getOutputParameter()) {
            visit(node_param);
        }

        NodeBody nodeBody = node.getBody();

        if (nodeBody != null) {
            visit(nodeBody, program);
        }
    }

    public void visit(NodeBody nodeBody, LustreProgram program) {

        for (VariableDeclaration var : nodeBody.getVariableDeclaration()) {
            DataType data_type = var.getDataType();

            String user_defined_type = null;
            if (data_type != null) {
                user_defined_type = data_type.getUserDefinedType();
            }

            //            String user_defined_type = data_type.getUserDefinedType();

            if (user_defined_type != null) {
                user_defined_type = user_defined_type + "_Impl";

                boolean implemented_type = typeDeclarations.containsKey(user_defined_type);

                if (implemented_type) {
                    data_type.setUserDefinedType(user_defined_type);
                }
            }
        }

        for (NodeEquation node_equation : nodeBody.getEquation()) {
            visit(node_equation);
        }
    }

    public void visit(NodeEquation node_equation) {

        Expression expr = node_equation.getRhs();

        recordLiteral(expr);
    }

    public void visit(NodeParameter node_param) {

        DataType data_type = node_param.getDataType();

        String user_defined_type = null;
        if (data_type != null) {
            user_defined_type = data_type.getUserDefinedType();
        }

        if (user_defined_type != null) {
            user_defined_type = user_defined_type + "_Impl";

            boolean implemented_type = typeDeclarations.containsKey(user_defined_type);

            if (implemented_type) {
                data_type.setUserDefinedType(user_defined_type);
            }
        }
    }

    public void visit(NodeBody nodeBody, Node node) {
        node.setBody(nodeBody);
    }

    // Connect -> NodeCall
    public void visit(Connection connection, NodeBody nodeBody) {

        NodeEquation eq = new NodeEquation();

        // R.H.S
        ConnectionEnd src = connection.getSource();

        // Source Connection
        Port src_component_port = src.getComponentPort();

        // L.H.S.
        ConnectionEnd dest = connection.getDestination();
        // Destination Connection
        Port dest_component_port = dest.getComponentPort();

        if (dest_component_port != null) {
            // Destination = Component (z3)
            // Source = SubComponent (my_b:B _z2)
            NodeEquation neq = new NodeEquation();

            NodeEquationLHS eq_lhs = new NodeEquationLHS();
            eq_lhs.getIdentifier().add(dest_component_port.getName());
            neq.setLhs(eq_lhs);

            CompInstancePort compInstancePort = src.getSubcomponentPort();
            ComponentInstance componentInstance = compInstancePort.getSubcomponent();

            src_component_port = compInstancePort.getPort();

            String src_portID = src_component_port.getName();

            Expression expr = new Expression();

            List<VariableDeclaration> vars = nodeBody.getVariableDeclaration();

            boolean match = false;

            String id_expr = componentInstance.getId() + "_port_" + src_portID;

            for (VariableDeclaration var : vars) {
                if (var.getName().equals(id_expr)) {
                    match = true;
                    break;
                }
            }

            if (match) {
                expr.setIdentifier(id_expr);
                //                System.out.println(">>>>>>>>>>>>>Identifiers: " +
                // expr.getIdentifier());

            } else {

                String inst_cmp = "(.+)_instrumented";
                Pattern inst_pattern = Pattern.compile(inst_cmp);
                //                System.out.println(src_portID);
                Matcher m = inst_pattern.matcher(src_portID);
                if (m.matches()) {
                    src_portID = m.group(1);
                }

                id_expr = componentInstance.getId() + "_port_" + src_portID;
                expr.setIdentifier(id_expr);
                //                System.out.println(id_expr);

                //                System.out.println(">>>>>>>>>>>>>Identifiers: " +
                // expr.getIdentifier());
            }

            neq.setRhs(expr);

            nodeBody.getEquation().add(neq);

        } else if (src_component_port != null) {

            CompInstancePort compInstancePort = dest.getSubcomponentPort();
            // X1
            dest_component_port = compInstancePort.getPort();
            // my_a1 : A
            ComponentInstance componentInstance = compInstancePort.getSubcomponent();

            String arg_value = src_component_port.getName();
            // called node Identifier.
            String called_node_ID = null;
            ComponentType componentType = componentInstance.getSpecification();
            ComponentImpl componentImpl = componentInstance.getImplementation();

            if (componentType == null) {
                componentType = componentImpl.getType();
                called_node_ID = componentType.getName() + "_Impl";
            }
            if (componentType != null && componentImpl != null) {
                componentType = componentImpl.getType();
                called_node_ID = componentType.getName() + "_Impl";

                String inst_cmp = "(.+)_instrumented";

                Pattern inst_pattern = Pattern.compile(inst_cmp);
                //                System.out.println(arg_value);
                Matcher m = inst_pattern.matcher(arg_value);
                if (m.matches()) {
                    arg_value = m.group(1);
                }

            } else {
                called_node_ID = componentType.getName();
                arg_value = src_component_port.getName();
                //                System.out.println(arg_value);
            }

            for (Port port : componentType.getPort()) {
                // MODE
                PortMode port_mode = port.getMode();

                if (port_mode == PortMode.OUT) {
                    // EQ L.H.S Variables *called Node return values
                    String expr_id = componentInstance.getName() + "_port_" + port.getName();
                    //                    System.out.print(">>>" + expr_id);
                    NodeEquation n_eq = getNodeEq(expr_id, nodeBody);

                    Expression eq_rhs = n_eq.getRhs();

                    NodeCall node_called = eq_rhs.getCall();

                    String inst_cmp = "(.+)_Inst_.*";
                    Pattern inst_pattern = Pattern.compile(inst_cmp);

                    String node_id = "";
                    if (node_called != null) {
                        node_id = node_called.getNodeId();
                    }

                    //                    System.out.println(" = " + node_id + " (" + arg_value +
                    // ")");
                    Matcher m = inst_pattern.matcher(node_called.getNodeId());

                    IfThenElse ifelse = new IfThenElse();
                    Expression called_expr = new Expression();
                    // Condition
                    Expression gps_expr = new Expression();

                    if (m.matches()) {
                        // Instrumented component Instance ID
                        String component_id = m.group(1);
                        gps_expr.setIdentifier(component_id);
                    }
                    ifelse.setCondition(gps_expr);
                    // Then
                    ifelse.setThenBranch(called_expr);
                    // Else
                    Expression arg = new Expression();
                    arg.setIdentifier(src_component_port.getName());
                    ifelse.setElseBranch(arg);

                    // NodeCalled Expr
                    called_expr.setCall(node_called);

                    Expression instrumented_expr = new Expression();
                    instrumented_expr.setConditionalExpression(ifelse);

                    if (node_called != null) {
                        if (node_called.getNodeId().equals(called_node_ID)) {
                            for (Expression arg_expr : node_called.getArgument()) {
                                if (arg_expr.getIdentifier() == dest_component_port.getName()) {
                                    arg_expr.setIdentifier(arg_value);
                                } else if (node_called.getArgument().size() == 1) {
                                    arg_expr.setIdentifier(arg_value);
                                }
                            }
                        }
                    }
                }
            }

        } else {

            CompInstancePort compInstancePort = src.getSubcomponentPort();

            ComponentInstance componentInstance = compInstancePort.getSubcomponent();

            src_component_port = compInstancePort.getPort();

            Expression arg_expr = new Expression();
            // my_a1_y1

            String src_portID = src_component_port.getName();

            String componentInstanceID = componentInstance.getId();
            arg_expr.setIdentifier(componentInstanceID + "_port_" + src_portID);

            // called node Identifier.
            String called_node_ID = null;

            compInstancePort = dest.getSubcomponentPort();
            componentInstance = compInstancePort.getSubcomponent();

            dest_component_port = compInstancePort.getPort();

            ComponentType componentType = componentInstance.getSpecification();
            ComponentImpl componentImpl = componentInstance.getImplementation();

            String old_portID = null;

            if (componentType == null) {
                componentType = componentImpl.getType();
                called_node_ID = componentType.getName();
            }
            if (componentType != null && componentImpl != null) {

                componentType = componentImpl.getType();
                called_node_ID = componentType.getName() + "_Impl";

                String inst_cmp = "(.+)_instrumented";
                Pattern inst_pattern = Pattern.compile(inst_cmp);
                //                System.out.print(src_portID + " ==> ");
                Matcher m = inst_pattern.matcher(src_portID);
                if (m.matches()) {
                    old_portID = src_portID;

                    src_portID = m.group(1);
                    arg_expr.setIdentifier(componentInstanceID + "_port_" + src_portID);
                    //                    System.out.println(old_portID + " -- " + src_portID);
                }
                //                System.out.println(arg_expr.getIdentifier() + " <== " +
                // src_portID);

            } else {
                called_node_ID = componentType.getName();
            }

            String node_arg = "(.+)_Inst_.*";
            Pattern arg_pattern = Pattern.compile(node_arg);

            Matcher m_arg = arg_pattern.matcher(called_node_ID);

            if (!m_arg.matches() && old_portID != null) {
                arg_expr.setIdentifier(componentInstanceID + "_port_" + old_portID);
                //                System.out.println("Node ID =>" + called_node_ID + "(" +
                // arg_expr.getIdentifier() + ")");
            }

            //            System.out.println(called_node_ID);
            for (Port port : componentType.getPort()) {
                // MODE
                PortMode port_mode = port.getMode();

                if (port_mode == PortMode.OUT) {
                    // EQ L.H.S Variables *called Node return values
                    String expr_id = componentInstance.getName() + "_port_" + port.getName();
                    NodeEquation n_eq = getNodeEq(expr_id, nodeBody);

                    if (n_eq != null) {
                        Expression eq_rhs = n_eq.getRhs();

                        NodeCall node_called = eq_rhs.getCall();

                        String inst_cmp = "(.+)_Inst_.*";
                        Pattern inst_pattern = Pattern.compile(inst_cmp);
                        String node_id = "";

                        if (node_called != null) {
                            node_id = node_called.getNodeId();
                        }

                        Matcher m = inst_pattern.matcher(node_id);

                        IfThenElse ifelse = new IfThenElse();
                        Expression called_expr = new Expression();
                        // Condition
                        Expression g_expr = new Expression();

                        if (m.matches()) {
                            // Instrumented component Instance ID
                            String component_id = m.group(1);
                            g_expr.setIdentifier(component_id);
                        }

                        ifelse.setCondition(g_expr);
                        // Then
                        ifelse.setThenBranch(called_expr);
                        // Else
                        Expression arg = new Expression();
                        ifelse.setElseBranch(arg_expr);

                        // NodeCalled Expr
                        called_expr.setCall(node_called);

                        Expression instrumented_expr = new Expression();

                        instrumented_expr.setConditionalExpression(ifelse);

                        if (node_called != null) {
                            if (node_called.getNodeId().equals(called_node_ID)) {
                                for (Expression a_expr : node_called.getArgument()) {

                                    if (a_expr.getIdentifier()
                                            .equals(dest_component_port.getName())) {
                                        a_expr.setIdentifier(arg_expr.getIdentifier());
                                    } else if (node_called.getArgument().size() == 1) {
                                        a_expr.setIdentifier(arg_expr.getIdentifier());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private NodeEquation getNodeEq(String eq_id, NodeBody nodeBody) {

        NodeEquation eq = null;

        for (NodeEquation n_eq : nodeBody.getEquation()) {

            NodeEquationLHS eq_lhs = n_eq.getLhs();

            if (eq_lhs.getIdentifier().contains(eq_id)) {
                eq = n_eq;
                break;
            }
        }

        return eq;
    }

    public void visit(ConstantDeclaration constantDeclaration) {

        //        String identifier = constantDeclaration.getName();
        DataType data_type = constantDeclaration.getDataType();

        String user_defined_type = data_type.getUserDefinedType();
        //        Expression expr = constantDeclaration.getDefinition();

        if (user_defined_type != null) {
            user_defined_type = user_defined_type + "_Impl";

            boolean implemented_type = typeDeclarations.containsKey(user_defined_type);

            if (implemented_type) {
                data_type.setUserDefinedType(user_defined_type);
            }

            Expression expr = constantDeclaration.getDefinition();

            recordLiteral(expr);
        }
    }

    // Copying Type Declaration.
    public void visit(TypeDeclaration typeDeclaration, LustreProgram program) {

        String identifier = typeDeclaration.getName();

        DataType data_type = typeDeclaration.getDefinition();

        RecordType record_type = data_type.getRecordType();

        if (record_type != null) {
            identifier = identifier + "_Impl";

            List<RecordField> record_fields = record_type.getRecordField();

            for (RecordField record_field : record_fields) {
                //            String identifier = record_field.getName();

                data_type = record_field.getType();
                String user_defined_type = data_type.getUserDefinedType();

                if (user_defined_type != null) {
                    user_defined_type = user_defined_type + "_Impl";
                    for (TypeDeclaration type_declaration : program.getTypeDeclaration()) {
                        if (user_defined_type.equals(type_declaration.getName())) {
                            data_type.setUserDefinedType(user_defined_type);
                        }
                    }
                }
            }

            // Updating TypeName_Impl
            typeDeclaration.setName(identifier);

            typeDeclarations.put(identifier, typeDeclaration);
        }

        program.getTypeDeclaration().add(typeDeclaration);
    }
}
