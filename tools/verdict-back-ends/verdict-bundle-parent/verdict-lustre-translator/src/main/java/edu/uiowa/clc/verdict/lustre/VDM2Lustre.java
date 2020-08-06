/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

   @author: M. Fareed Arif
*/

package edu.uiowa.clc.verdict.lustre;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import verdict.vdm.vdm_data.DataType;
import verdict.vdm.vdm_data.PlainType;
import verdict.vdm.vdm_data.RecordField;
import verdict.vdm.vdm_data.RecordType;
import verdict.vdm.vdm_data.TypeDeclaration;
import verdict.vdm.vdm_lustre.BinaryOperation;
import verdict.vdm.vdm_lustre.ConstantDeclaration;
import verdict.vdm.vdm_lustre.Contract;
import verdict.vdm.vdm_lustre.ContractItem;
import verdict.vdm.vdm_lustre.ContractSpec;
import verdict.vdm.vdm_lustre.Expression;
import verdict.vdm.vdm_lustre.ExpressionList;
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
import verdict.vdm.vdm_lustre.RecordProjection;
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

    private HashMap<String, DataType> eventDeclarations = null;

    private Vector<Port> marked_ports = null;
    private Vector<ComponentType> marked_types = null;

    public VDM2Lustre(Model vdm_model) {

        this.vdm_model = vdm_model;
        this.typeDeclarations = new HashMap<String, TypeDeclaration>();

        this.eventDeclarations = new HashMap<String, DataType>();

        this.marked_ports = new Vector<Port>();
        this.marked_types = new Vector<ComponentType>();
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

        if (lustre_program == null) {
            lustre_program = new LustreProgram();
        }

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

        // Event Ports
        for (ComponentType componentType : vdm_model.getComponentType()) {
            // Collect Node with no output
            Port mPort = markPort(componentType);

            if (mPort == null) {
                // Event Ports to Data Ports;
                for (Port port : componentType.getPort()) {
                    // Update event_ports
                    visit(port, program);
                }
            } else {

                System.out.println("Ignoring Node:" + componentType.getName());
                System.out.println("Ignoring Port:" + mPort.getName());

                this.marked_types.add(componentType);
                this.marked_ports.add(mPort);
            }
        }

        // B) Component Type
        Node cmp_node = null;
        Node impl_node = null;

        String inst_cmp = "(.+)_Inst_.*";
        Pattern inst_pattern = Pattern.compile(inst_cmp);

        for (ComponentType componentType : vdm_model.getComponentType()) {

            boolean is_declared = false;

            if (!this.marked_types.contains(componentType)) {

                for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {

                    if (componentType == componentImpl.getType()) {

                        impl_node = visit(componentType, true);

                        visit(componentImpl, impl_node);
                        is_declared = true;
                        break;
                    }
                }

                if (is_declared) {

                    cmp_node = visit(componentType, false);

                    Matcher m = inst_pattern.matcher(cmp_node.getName());

                    if (m.matches() == false) {
                        program.getNodeDeclaration().add(cmp_node);
                    }

                    program.getNodeDeclaration().add(impl_node);

                } else {
                    cmp_node = visit(componentType, false);
                    program.getNodeDeclaration().add(cmp_node);
                }
            }
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
            identifier += "_dot_Impl";

        } else {
            // Imported Node
            node.setIsImported(true);
            // System.out.println("Imported Nodes:" +identifier );

        }

        node.setName(identifier);

        for (Port port : componentType.getPort()) {

            if (port.isEvent() != null && port.isEvent()) {
                this.eventDeclarations.put(port.getName(), port.getType());
            }

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
                this.eventDeclarations.clear();
            }
        }

        return node;
    }

    // B) Component Implementation Translated into Lustre Node
    public void visit(ComponentImpl componentImpl, Node node) {

        NodeBody nodeBody = new NodeBody();

        // Option 1) Block Implementation
        // retrieve_block(componentImpl);
        BlockImpl blockImpl = componentImpl.getBlockImpl();

        // BlockImpl
        if (blockImpl != null) {

            ComponentType componentType = componentImpl.getType();

            for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {

                componentType = componentInstance.getSpecification();
                ComponentImpl subcomponentImpl = componentInstance.getImplementation();

                // Option Check)

                if (componentType == null && subcomponentImpl == null) {
                    System.out.println(
                            componentInstance.getName()
                                    + " subcomponent is missing both a specification and an implemention.");
                    System.out.println(
                            "Please provide some specification or an implementation to continue.");
                    System.exit(-1);
                }

                // Option 1) Implementation
                if (subcomponentImpl != null) {

                    componentType = subcomponentImpl.getType();
                    if (!this.marked_types.contains(componentType)) {
                        visit(componentType, nodeBody, componentInstance.getId(), true);
                    }
                }

                // Option 2) Specification
                else if (componentType != null) {
                    if (!this.marked_types.contains(componentType)) {
                        visit(componentType, nodeBody, componentInstance.getId(), false);
                    }
                }
            }

            for (Connection connection : blockImpl.getConnection()) {
                if (!ignoreConnection(connection)) {
                    visit(connection, nodeBody);
                }
            }

        } else {
            // Option 2) DataFlow Implementation / NodeBody
            nodeBody = componentImpl.getDataflowImpl();
            // node.setBody(nodeBody);
        }

        node.setBody(nodeBody);
    }

    // Ignore Connection or Marked Ports.
    private boolean ignoreConnection(Connection con) {

        ConnectionEnd srcConnection = con.getSource();
        ComponentType srcType = null;

        ConnectionEnd destConnection = con.getDestination();
        ComponentType destType = null;

        Port srcPort = srcConnection.getComponentPort();

        if (srcPort == null) {
            CompInstancePort compPort = srcConnection.getSubcomponentPort();
            srcPort = compPort.getPort();

            ComponentInstance srcCompInstance = compPort.getSubcomponent();
            srcType = srcCompInstance.getSpecification();
            if (srcType == null) {
                ComponentImpl compImpl = srcCompInstance.getImplementation();
                srcType = compImpl.getType();
            }
        }

        Port destPort = destConnection.getComponentPort();

        if (destPort == null) {
            CompInstancePort compPort = destConnection.getSubcomponentPort();
            destPort = compPort.getPort();

            ComponentInstance destCompInstance = compPort.getSubcomponent();
            destType = destCompInstance.getSpecification();
            if (destType == null) {
                ComponentImpl compImpl = destCompInstance.getImplementation();
                destType = compImpl.getType();
            }
        }

        if (this.marked_ports.contains(srcPort) || this.marked_ports.contains(destPort)) {
            System.out.println("Ignore Port Connection:" + con.getName());
            return true;
        }

        if (this.marked_types.contains(srcType) || this.marked_types.contains(destType)) {
            System.out.println("Ignore Instance Connection:" + con.getName());
            return true;
        }

        return false;
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
            nodeCall.setNodeId(componentType.getName() + "_dot_Impl");
        } else {
            nodeCall.setNodeId(componentType.getName());
        }

        // Port
        for (Port port : componentType.getPort()) {

            // //Event Port Definition
            // for (Port port : componentType.getPort()) {
            // visit(port);
            // }

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

        // Ignore node call that do not have output
        if (eq_lhs.getIdentifier() != null) {

            node.setCall(nodeCall);

            node_eq.setRhs(node);
            node_eq.setLhs(eq_lhs);

            nodeBody.getEquation().add(node_eq);
        }
    }

    // Event Ports
    public void visit(Port port, LustreProgram lustreProgram) {

        // Print Event Ports.
        if (port.isEvent() != null && port.isEvent()) {

            DataType dataType = port.getType();

            DataType eventType = getEventType(dataType, lustreProgram);

            port.setType(eventType);
        }
    }

    // Mark Ports with no output
    protected Port markPort(ComponentType componentType) {

        Port markedPort = null;

        for (Port port : componentType.getPort()) {

            PortMode port_mode = port.getMode();

            if (port_mode == PortMode.OUT) {
                return null;
            } else {
                markedPort = port;
            }
        }

        return markedPort;
    }

    protected DataType getEventType(DataType dataType, LustreProgram lustreProgram) {

        // LustreProgram lustreProgram = vdm_model.getDataflowCode();

        DataType eventType = new DataType();
        TypeDeclaration eventTypeDeclaration = defineEventDeclaration(dataType);

        String eventTypeName = eventTypeDeclaration.getName();
        eventType.setUserDefinedType(eventTypeName);

        if (!typeDeclarations.containsKey(eventTypeName)) {
            typeDeclarations.put(eventTypeName, eventTypeDeclaration);
            lustreProgram.getTypeDeclaration().add(eventTypeDeclaration);
        }

        return eventType;
    }

    protected TypeDeclaration defineEventDeclaration(DataType dataType) {

        TypeDeclaration eventTypeDeclaration = new TypeDeclaration();

        if (dataType != null) {

            String user_defined_type = dataType.getUserDefinedType();

            boolean implemented_type = typeDeclarations.containsKey(user_defined_type);

            if (implemented_type) {

                TypeDeclaration baseType = typeDeclarations.get(user_defined_type);

                user_defined_type = baseType.getName();
                String definedType = user_defined_type.replace("_dot_", "Event_");

                eventTypeDeclaration.setName(definedType);

                DataType eventDefinition = new DataType();
                RecordType eventRecord = getEventrecord(user_defined_type);
                eventDefinition.setRecordType(eventRecord);

                eventTypeDeclaration.setDefinition(eventDefinition);
                eventTypeDeclaration.setName(definedType);
            }

        } else {
            // None DataType
            String definedType = "EventDataType";

            eventTypeDeclaration.setName(definedType);

            DataType eventDefinition = new DataType();
            RecordType eventRecord = getEventrecord(null);
            eventDefinition.setRecordType(eventRecord);

            eventTypeDeclaration.setDefinition(eventDefinition);
            eventTypeDeclaration.setName(definedType);
        }

        return eventTypeDeclaration;
    }

    protected RecordType getEventrecord(String userDefineType) {

        // Define a Record
        RecordType eventRecord = new RecordType();

        // is_present: bool
        RecordField eventField = new RecordField();

        DataType boolType = new DataType();
        boolType.setPlainType(PlainType.BOOL);
        eventField.setName("is_present");
        eventField.setType(boolType);

        eventRecord.getRecordField().add(eventField);

        // value: UserDefined
        if (userDefineType != null) {
            RecordField eventValue = new RecordField();

            DataType valueType = new DataType();
            valueType.setUserDefinedType(userDefineType);

            eventValue.setName("value");
            eventValue.setType(valueType);
            eventRecord.getRecordField().add(eventValue);
        }

        return eventRecord;
    }

    public void visit(Port port, Node node) {

        PortMode port_mode = port.getMode();

        NodeParameter node_parameter = new NodeParameter();

        // Node Name
        String port_name = port.getName();
        node_parameter.setName(port_name);

        // Node DataType
        DataType dataType = port.getType();

        // Change Null DataType to Integer Default Type
        if (dataType == null) {

            dataType = new DataType();
            dataType.setPlainType(PlainType.INT);
            port.setType(dataType);
        }

        node_parameter.setDataType(dataType);

        // Node Input Parameter
        if (port_mode == PortMode.IN) {
            node.getInputParameter().add(node_parameter);
        }
        // Node OutputParameter
        else if (port_mode == PortMode.OUT) {
            node.getOutputParameter().add(node_parameter);

            // if(node.isIsImported() != null) {
            String inst_cmp = "(.+)_Inst_.*";
            Pattern inst_pattern = Pattern.compile(inst_cmp);
            Matcher m = inst_pattern.matcher(node.getName());

            if (m.matches()) {
                node_parameter.setName(node_parameter.getName() + "_instrumented");
            }
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
                // user_defined_type = user_defined_type + "_Impl";

                boolean implemented_type = typeDeclarations.containsKey(user_defined_type);

                if (implemented_type) {
                    data_type.setUserDefinedType(user_defined_type);
                }
            }

            Expression expr = symbol.getDefinition();

            expr = visitExpression(expr);
            symbol.setDefinition(expr);

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

        expr = visitExpression(expr);
        contractItem.setExpression(expr);

        recordLiteral(expr);
    }

    public void recordLiteral(Expression expr) {

        if (expr != null) {

            RecordLiteral recordLiteral = expr.getRecordLiteral();

            if (recordLiteral != null) {

                String identifier = recordLiteral.getRecordType();
                identifier = identifier.replace(".", "_dot_");
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
                    recordLiteral(pre_expr);
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

    protected BinaryOperation binaryOP(BinaryOperation op) {

        BinaryOperation op_exp = new BinaryOperation();

        Expression lhs = op.getLhsOperand();
        lhs = visitExpression(lhs);

        Expression rhs = op.getRhsOperand();
        rhs = visitExpression(rhs);

        op_exp.setLhsOperand(lhs);
        op_exp.setRhsOperand(rhs);

        return op_exp;
    }

    protected Expression visitExpression(Expression expr) {

        Expression u_Expr = new Expression();

        // Binary Operators
        if (expr != null) {

            BinaryOperation op = expr.getEqual();

            if (op != null) {

                u_Expr.setEqual(binaryOP(op));
            }

            op = expr.getNotEqual();

            if (op != null) {

                u_Expr.setNotEqual(binaryOP(op));
            }

            op = expr.getImplies();

            if (op != null) {

                u_Expr.setImplies(binaryOP(op));
            }

            op = expr.getAnd();

            if (op != null) {

                u_Expr.setAnd(binaryOP(op));
            }

            op = expr.getOr();

            if (op != null) {

                u_Expr.setOr(binaryOP(op));
            }

            op = expr.getXor();

            if (op != null) {

                u_Expr.setXor(binaryOP(op));
            }

            op = expr.getArrow();

            if (op != null) {

                u_Expr.setArrow(binaryOP(op));
            }

            op = expr.getLessThanOrEqualTo();

            if (op != null) {

                u_Expr.setLessThanOrEqualTo(binaryOP(op));
            }

            op = expr.getLessThan();

            if (op != null) {

                u_Expr.setLessThan(binaryOP(op));
            }

            op = expr.getGreaterThan();

            if (op != null) {

                u_Expr.setGreaterThan(binaryOP(op));
            }

            op = expr.getGreaterThanOrEqualTo();

            if (op != null) {

                u_Expr.setGreaterThanOrEqualTo(binaryOP(op));
            }

            op = expr.getMinus();

            if (op != null) {

                u_Expr.setMinus(binaryOP(op));
            }

            op = expr.getPlus();

            if (op != null) {

                u_Expr.setPlus(binaryOP(op));
            }

            op = expr.getDiv();

            if (op != null) {

                u_Expr.setDiv(binaryOP(op));
            }

            op = expr.getTimes();

            if (op != null) {

                u_Expr.setTimes(binaryOP(op));
            }

            op = expr.getMod();

            if (op != null) {

                u_Expr.setMod(binaryOP(op));
            }

            op = expr.getCartesianExpression();

            if (op != null) {

                u_Expr.setCartesianExpression(binaryOP(op));
            }

            IfThenElse cond_op = expr.getConditionalExpression();

            if (cond_op != null) {

                Expression cond_expr = cond_op.getCondition();
                cond_expr = visitExpression(cond_expr);

                Expression then_expr = cond_op.getThenBranch();
                then_expr = visitExpression(then_expr);

                Expression else_expr = cond_op.getElseBranch();
                else_expr = visitExpression(else_expr);

                cond_op.setCondition(cond_expr);
                cond_op.setThenBranch(then_expr);
                cond_op.setElseBranch(else_expr);

                u_Expr.setConditionalExpression(cond_op);
            }

            // Record Literal
            RecordLiteral recordLiteral = expr.getRecordLiteral();

            if (recordLiteral != null) {

                Expression fieldExpr;

                for (FieldDefinition fieldDef : recordLiteral.getFieldDefinition()) {
                    fieldExpr = fieldDef.getFieldValue();
                    visitExpression(fieldExpr);
                }

                u_Expr.setRecordLiteral(recordLiteral);
            }

            // Record Project
            RecordProjection recordProj = expr.getRecordProjection();

            if (recordProj != null) {
                Expression recordRef = recordProj.getRecordReference();

                recordRef = visitExpression(recordRef);
                recordProj.setRecordReference(recordRef);

                u_Expr.setRecordProjection(recordProj);
            }

            // Unary Operators
            Expression notExpr = expr.getNot();

            if (notExpr != null) {
                notExpr = visitExpression(notExpr);
                u_Expr.setNot(notExpr);
            }

            Expression negExpr = expr.getNegative();

            if (negExpr != null) {
                negExpr = visitExpression(negExpr);
                u_Expr.setNegative(negExpr);
                ;
            }

            Expression preExpr = expr.getPre();

            if (preExpr != null) {

                preExpr = visitExpression(preExpr);
                u_Expr.setPre(preExpr);
            }

            Expression toIntExpr = expr.getToInt();

            if (toIntExpr != null) {

                toIntExpr = visitExpression(toIntExpr);
                u_Expr.setToInt(toIntExpr);
            }

            Expression toRealExpr = expr.getToReal();

            if (toRealExpr != null) {

                toRealExpr = visitExpression(toRealExpr);
                u_Expr.setToReal(toRealExpr);
            }

            Boolean b = expr.isBoolLiteral();

            if (b != null) {
                u_Expr.setBoolLiteral(b);
            }

            BigInteger int_value = expr.getIntLiteral();

            if (int_value != null) {
                u_Expr.setIntLiteral(int_value);
            }

            BigDecimal real_value = expr.getRealLiteral();

            if (real_value != null) {
                u_Expr.setRealLiteral(real_value);
            }

            // Identifier
            String identifier = expr.getIdentifier();

            if (identifier != null) {
                // Check respective EventType and add value Projection.

                if (eventDeclarations.containsKey(identifier)) {

                    u_Expr = eventExpression(expr, false);

                } else {
                    u_Expr.setIdentifier(identifier);
                }
            }

            // NodeCall
            NodeCall nodeCall = expr.getCall();

            if (nodeCall != null) {
                u_Expr.setCall(nodeCall);

                List<Expression> arguments = new Vector<Expression>();

                for (Expression argExpr : nodeCall.getArgument()) {
                    argExpr = visitExpression(argExpr);
                    arguments.add(argExpr);
                }

                nodeCall.getArgument().clear();
                nodeCall.getArgument().addAll(arguments);

                u_Expr.setCall(nodeCall);
            }

            // Event Expression
            Expression event = expr.getEvent();

            if (event != null) {
                u_Expr = eventExpression(event, true);
                // System.out.println(expr + "^^^ Updated to Event ^^^ " + event);
            }

            ExpressionList expList = expr.getExpressionList();

            if (expList != null) {
                ExpressionList uList = new ExpressionList();
                for (Expression aexpr : expList.getExpression()) {
                    expr = visitExpression(aexpr);
                    uList.getExpression().add(expr);
                }
                expList.getExpression().clear();
                u_Expr.setExpressionList(uList);
            }

            // ExpressionList arrayExpList = expr.getArrayExpression();
            //
            // if(expList != null) {
            // List<Expression> uList = new ArrayList<Expression>();
            // for(Expression aexpr: arrayExpList.getExpression()) {
            // expr = visitExpression(aexpr);
            // uList.add(expr);
            // }
            // arrayExpList.getExpression().clear();
            // arrayExpList.getExpression().addAll(uList);
            // u_Expr.setArrayExpression(arrayExpList);
            // }

        }

        return u_Expr;
    }

    protected Expression eventExpression(Expression expr, boolean event_type) {

        Expression eventExpression = new Expression();

        if (expr != null) {

            RecordProjection recordProject = new RecordProjection();

            if (event_type) {
                recordProject.setFieldId("is_present");
            } else {
                recordProject.setFieldId("value");
            }
            recordProject.setRecordReference(expr);

            eventExpression.setRecordProjection(recordProject);
        }

        return eventExpression;
    }

    public void visit(Node node, LustreProgram program) {

        // Collect Input Event DataTypes
        for (NodeParameter node_param : node.getInputParameter()) {
            visit(node_param);
        }

        // Collect Output Event DataTypes
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

            // String user_defined_type = data_type.getUserDefinedType();

            if (user_defined_type != null) {
                // user_defined_type = user_defined_type + "_Impl";

                boolean implemented_type = typeDeclarations.containsKey(user_defined_type);

                if (implemented_type) {
                    data_type.setUserDefinedType(user_defined_type);
                }
            }
        }

        for (NodeEquation node_equation : nodeBody.getEquation()) {
            visit(node_equation);
        }

        // Update Expression related to Events
        // for (NodeEquation node_equation : nodeBody.getEquation()) {
        // visitNodeEq(node_equation);
        // }
    }

    // protected void visitNodeEq(NodeEquation node_eq) {
    //
    // Expression rhs_expr = node_eq.getRhs();
    // visitExpression(rhs_expr);
    // }

    public void visit(NodeEquation node_equation) {

        Expression expr = node_equation.getRhs();
        recordLiteral(expr);

        // expr = visitExpression(expr);
        // node_equation.setRhs(expr);

    }

    public void visit(NodeParameter node_param) {

        DataType data_type = node_param.getDataType();

        String user_defined_type = null;
        if (data_type != null) {
            user_defined_type = data_type.getUserDefinedType();
        }

        if (user_defined_type != null) {
            // user_defined_type = user_defined_type + "_Impl";

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
                // System.out.println(">>>>>>>>>>>>>Identifiers: " +
                // expr.getIdentifier());

            } else {

                String inst_cmp = "(.+)_instrumented";
                Pattern inst_pattern = Pattern.compile(inst_cmp);
                // System.out.println(src_portID);
                Matcher m = inst_pattern.matcher(src_portID);
                if (m.matches()) {
                    src_portID = m.group(1);
                }

                id_expr = componentInstance.getId() + "_port_" + src_portID;
                expr.setIdentifier(id_expr);
                // System.out.println(id_expr);

                // System.out.println(">>>>>>>>>>>>>Identifiers: " +
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
                called_node_ID = componentType.getName() + "_dot_Impl";
            }
            if (componentType != null && componentImpl != null) {
                componentType = componentImpl.getType();
                called_node_ID = componentType.getName() + "_dot_Impl";

                String inst_cmp = "(.+)_instrumented";

                Pattern inst_pattern = Pattern.compile(inst_cmp);
                // System.out.println(arg_value);
                Matcher m = inst_pattern.matcher(arg_value);
                if (m.matches()) {
                    arg_value = m.group(1);
                }

            } else {
                called_node_ID = componentType.getName();
                arg_value = src_component_port.getName();
                // System.out.println(arg_value);
            }

            for (Port port : componentType.getPort()) {
                // MODE
                PortMode port_mode = port.getMode();

                if (port_mode == PortMode.OUT) {
                    // EQ L.H.S Variables *called Node return values
                    String expr_id = componentInstance.getName() + "_port_" + port.getName();
                    // System.out.print(">>>" + expr_id);
                    NodeEquation n_eq = getNodeEq(expr_id, nodeBody);

                    if (n_eq != null) {
                        Expression eq_rhs = n_eq.getRhs();

                        NodeCall node_called = eq_rhs.getCall();

                        String inst_cmp = "(.+)_Inst_.*";
                        Pattern inst_pattern = Pattern.compile(inst_cmp);

                        // String node_id = "";
                        // if (node_called != null) {
                        // node_id = node_called.getNodeId();
                        // }

                        // System.out.println(" = " + node_id + " (" + arg_value
                        // +
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
                                    if (arg_expr.getIdentifier()
                                            .equals(dest_component_port.getName())) {
                                        arg_expr.setIdentifier(arg_value);
                                    } else if (node_called.getArgument().size() == 1) {
                                        arg_expr.setIdentifier(arg_value);
                                    }
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
                called_node_ID = componentType.getName() + "_dot_Impl";

                String inst_cmp = "(.+)_instrumented";
                Pattern inst_pattern = Pattern.compile(inst_cmp);
                // System.out.print(src_portID + " ==> ");
                Matcher m = inst_pattern.matcher(src_portID);
                if (m.matches()) {
                    old_portID = src_portID;

                    src_portID = m.group(1);
                    arg_expr.setIdentifier(componentInstanceID + "_port_" + src_portID);
                    // System.out.println(old_portID + " -- " + src_portID);
                }
                // System.out.println(arg_expr.getIdentifier() + " <== " +
                // src_portID);

            } else {
                called_node_ID = componentType.getName();
            }

            String node_arg = "(.+)_Inst_.*";
            Pattern arg_pattern = Pattern.compile(node_arg);

            Matcher m_arg = arg_pattern.matcher(called_node_ID);

            if (!m_arg.matches() && old_portID != null) {
                arg_expr.setIdentifier(componentInstanceID + "_port_" + old_portID);
                // System.out.println("Node ID =>" + called_node_ID + "(" +
                // arg_expr.getIdentifier() + ")");
            }

            // System.out.println(called_node_ID);
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
                        // Expression arg = new Expression();
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

        // String identifier = constantDeclaration.getName();
        DataType data_type = constantDeclaration.getDataType();

        String user_defined_type = data_type.getUserDefinedType();
        // Expression expr = constantDeclaration.getDefinition();

        if (user_defined_type != null) {
            // user_defined_type = user_defined_type + "_Impl";

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
        // Renaming dot[.] in Type Declaration Identifier.
        identifier = identifier.replace(".", "_id_");
        typeDeclaration.setName(identifier);

        DataType data_type = typeDeclaration.getDefinition();

        if (data_type != null) {
            RecordType record_type = data_type.getRecordType();

            if (record_type != null) {
                // identifier = identifier + "_Impl";

                List<RecordField> record_fields = record_type.getRecordField();

                for (RecordField record_field : record_fields) {
                    // String identifier = record_field.getName();

                    data_type = record_field.getType();
                    String user_defined_type = data_type.getUserDefinedType();

                    if (user_defined_type != null) {
                        // user_defined_type = user_defined_type + "_Impl";

                        for (TypeDeclaration type_declaration : program.getTypeDeclaration()) {
                            if (user_defined_type.equals(type_declaration.getName())) {
                                data_type.setUserDefinedType(user_defined_type);
                            }
                        }
                    }
                }

                // Updating TypeName_Impl
                typeDeclaration.setName(identifier);
            }
        }

        typeDeclarations.put(identifier, typeDeclaration);
        program.getTypeDeclaration().add(typeDeclaration);
    }

    protected BlockImpl retrieve_block(ComponentImpl compImpl) {

        BlockImpl blockImpl = null;

        String cmpID = compImpl.getType().getId();

        for (ComponentImpl cmpImpl : vdm_model.getComponentImpl()) {
            if (cmpImpl.getBlockImpl() != null) {
                blockImpl = cmpImpl.getBlockImpl();
                for (ComponentInstance cmpInstance : blockImpl.getSubcomponent()) {
                    ComponentImpl impl = cmpInstance.getImplementation();
                    ComponentType enumType = null;

                    if (impl != null) {
                        enumType = impl.getType();
                    } else {
                        enumType = cmpInstance.getSpecification();
                    }

                    if (cmpID.equals(enumType.getId())) {
                        return blockImpl;
                    }
                }
            }
        }

        return blockImpl;
    }
}
