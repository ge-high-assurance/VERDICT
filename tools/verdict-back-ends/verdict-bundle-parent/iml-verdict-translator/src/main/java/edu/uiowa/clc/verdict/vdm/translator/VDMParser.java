/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   Copyright (c) 2019-2020, General Electric Company.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

    @author: Paul Meng
    @author: M. Fareed Arif
*/

package edu.uiowa.clc.verdict.vdm.translator;

import com.utc.utrc.hermes.iml.iml.NumberLiteral;
import com.utc.utrc.hermes.iml.iml.TruthValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import verdict.vdm.vdm_data.DataType;
import verdict.vdm.vdm_data.EnumType;
import verdict.vdm.vdm_data.GenericAttribute;
import verdict.vdm.vdm_data.PlainType;
import verdict.vdm.vdm_data.RecordField;
import verdict.vdm.vdm_data.RecordType;
import verdict.vdm.vdm_data.TypeDeclaration;
import verdict.vdm.vdm_lustre.BinaryOperation;
import verdict.vdm.vdm_lustre.ConstantDeclaration;
import verdict.vdm.vdm_lustre.Contract;
import verdict.vdm.vdm_lustre.ContractImport;
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
import verdict.vdm.vdm_lustre.NodeProperty;
import verdict.vdm.vdm_lustre.RecordLiteral;
import verdict.vdm.vdm_lustre.RecordProjection;
import verdict.vdm.vdm_lustre.SymbolDefinition;
import verdict.vdm.vdm_lustre.VariableDeclaration;
import verdict.vdm.vdm_model.BlockImpl;
import verdict.vdm.vdm_model.CIA;
import verdict.vdm.vdm_model.CIAPort;
import verdict.vdm.vdm_model.CompInstancePort;
import verdict.vdm.vdm_model.ComponentImpl;
import verdict.vdm.vdm_model.ComponentInstance;
import verdict.vdm.vdm_model.ComponentType;
import verdict.vdm.vdm_model.Connection;
import verdict.vdm.vdm_model.ConnectionEnd;
import verdict.vdm.vdm_model.CyberExpr;
import verdict.vdm.vdm_model.CyberExprKind;
import verdict.vdm.vdm_model.CyberExprList;
import verdict.vdm.vdm_model.CyberRel;
import verdict.vdm.vdm_model.CyberReq;
import verdict.vdm.vdm_model.Event;
import verdict.vdm.vdm_model.EventHappens;
import verdict.vdm.vdm_model.IA;
import verdict.vdm.vdm_model.IAPort;
import verdict.vdm.vdm_model.Mission;
import verdict.vdm.vdm_model.Model;
import verdict.vdm.vdm_model.Port;
import verdict.vdm.vdm_model.PortMode;
import verdict.vdm.vdm_model.SafetyRel;
import verdict.vdm.vdm_model.SafetyRelExpr;
import verdict.vdm.vdm_model.SafetyRelExprKind;
import verdict.vdm.vdm_model.SafetyRelExprList;
import verdict.vdm.vdm_model.SafetyReq;
import verdict.vdm.vdm_model.SafetyReqExpr;
import verdict.vdm.vdm_model.SafetyReqExprKind;
import verdict.vdm.vdm_model.SafetyReqExprList;
import verdict.vdm.vdm_model.Severity;

public class VDMParser extends Parser {

    private Model model;

    private List<TypeDeclaration> typeDeclarations;
    private List<ComponentType> componentTypes;
    private List<ComponentImpl> componentImpls;
    private List<CyberReq> cyberRequirements;
    private List<SafetyReq> safetyRequirements;
    private List<Mission> missions;

    private ComponentImpl componentImpl;
    private BlockImpl blockImpl;

    public VDMParser(ArrayList<Token> tokens) {
        super(tokens);
        model = new Model();
        typeDeclarations = model.getTypeDeclaration();
        componentTypes = model.getComponentType();
        componentImpls = model.getComponentImpl();
        cyberRequirements = model.getCyberReq();
        safetyRequirements = model.getSafetyReq();
        missions = model.getMission();
    }

    private void log(String msg) {
        // System.out.println(msg);
    }

    // Project Rules:

    // IDENTIFIER ::= IDENTIFIER:name String:value
    // Return a String.
    public String Identifier() {

        String identifier_name;
        String identifier_value;

        identifier_name = token.sd.getName();
        consume(Type.IDENTIFIER);

        identifier_value = token.getStringValue();
        consume(Type.String);

        // LOGGER.info(identifier_name + " := " + identifier_value);

        return identifier_value;
    }

    // DATATYPE ::= {DATA_TYPE:t}* {DATA_TYPE_KIND, kind}
    // NULL: Plain | NULL:Enum | NULL:Subrange | NULL:ArrayType | NULL:TupleType |
    // NULL:RecordType
    // | NULL: UserDefinedType
    // DATATYPE ::= {DATA_TYPE:t} NULL:Plain_Type | NULL:Enum |
    // SUB_RANGE_TYPE:subrange | ArrayType
    // | TupleType | RecordType | UserDefinedType
    /*
     * type DataType { kind: DataTypeKind; plain_type: PlainType; subrange_type:
     * SubrangeType; enum_type: EnumType; record_type: RecordType;
     * user_defined_type: TypeDeclaration; };
     */
    public DataType dataType() {

        DataType dataType = new DataType();
        Type type = null;

        while (token.type == Type.DATA_TYPE) {
            consume(Type.DATA_TYPE);

            String type_name = token.sd.getName();
            type = Type.get(type_name);

            // Token token = this.token;
            if (token.type == Type.DATA_TYPE_KIND) {
                type = getDataTypeKind();

                if (type == Type.ENUM) {
                    EnumType enumType = enumType();
                    dataType.setEnumType(enumType);
                } else if (type == Type.RECORD) {
                    RecordType recordType = recordType();
                    dataType.setRecordType(recordType);
                } else if (type == Type.SUBRANGE) {
                    // dataType.setSubrangeType(value);
                } else if (type == Type.PLAIN) {
                    PlainType plainType = plainType();
                    dataType.setPlainType(plainType);
                    break;
                } else if (type == Type.USERDEFINED) {
                    dataType = getUserDefinedType();
                    break;
                    // dataType.setUserDefinedType(type_value);
                }
            }
        }

        return dataType;
    }

    // type DataTypeKind enum { Plain, Subrange, Enum, Record, UserDefined };
    public Type getDataTypeKind() {

        consume(Type.DATA_TYPE_KIND);

        String type_name = token.sd.getName();
        Type type = Type.get(type_name);

        if (type == Type.ENUM) {
            consume(Type.ENUM);
        } else if (type == Type.SUBRANGE) {
            consume(Type.SUBRANGE);
        } else if (type == Type.PLAIN) {
            consume(Type.PLAIN);
        } else if (type == Type.RECORD) {
            consume(Type.RECORD);
        } else if (type == Type.USERDEFINED) {
            consume(Type.USERDEFINED);
        }

        return type;
    }

    /*
     * BoolType: DataType := some(d: DataType) { d.kind = DataTypeKind.Plain &&
     * d.plain_type = PlainType.Bool };
     *
     *
     * RealType: DataType := some(d: DataType) { d.kind = DataTypeKind.Plain &&
     * d.plain_type = PlainType.Real };
     *
     * IntegerType: DataType := some(d: DataType) { d.kind = DataTypeKind.Plain &&
     * d.plain_type = PlainType.Int };
     *
     */

    public DataType recordDataType() {

        DataType recordDataType = new DataType();

        while (token.type == Type.DATA_TYPE) {

            consume(Type.DATA_TYPE);

            String type_value = token.sd.getName();
            Type type = Type.get(type_value);

            if (type == Type.INTEGER_TYPE) {
                consume(Type.INTEGER_TYPE);
                recordDataType = dataType();
            }
            // Token token = this.token;
            if (token.type == Type.DATA_TYPE_KIND) {
                type = getDataTypeKind();
                //
                // if(type == Type.PLAIN) {
                // plainType();
                // } else {
                // System.out.println("Error in IntegerType");
                // }
                if (type == Type.PLAIN) {
                    PlainType plainType = plainType();
                    recordDataType.setPlainType(plainType);
                    // break;
                }
            } else if (token.type == Type.MODEL) {
                TypeDeclaration typeDeclaration = getTypeDeclaration();
                recordDataType.setUserDefinedType(typeDeclaration.getName());
            }
        }

        return recordDataType;
    }

    // type PlainType enum { Int, Real, Bool };
    public PlainType plainType() {

        PlainType plainType = null;

        // while(token.type == Type.DATA_TYPE) {

        consume(Type.DATA_TYPE);

        if (token.type == Type.PLAIN_TYPE) {
            consume(Type.PLAIN_TYPE);

            String type_value = token.sd.getName();
            Type type = Type.get(type_value);
            consume(type);

            if (type == Type.INT) {
                plainType = PlainType.valueOf("INT");
            } else if (type == Type.REAL) {
                plainType = PlainType.valueOf("REAL");
            } else if (type == Type.BOOL) {
                plainType = PlainType.valueOf("BOOL");
            }
        }
        // }

        return plainType;
    }

    // ENUM ::== ENUM*
    // ENUM ::= {DATA_TYPE:t}* ENUM_TYPE:enum_type INT:length Int:value
    // | ({DATATYPE:t} ENUMTYPE:enum_type NULL:element Int:index String:value (Bound
    // by
    // length)
    //

    // type EnumType is ArrayList<Identifier>;
    public EnumType enumType() {

        EnumType enumType = new EnumType();

        int enum_length = 0;

        while (token.type == Type.DATA_TYPE) {

            consume(Type.DATA_TYPE);

            if (token.type == Type.ENUM_TYPE) {
                consume(Type.ENUM_TYPE);

                if (token.type == Type.INT) {
                    // length
                    consume(Type.INT);
                    enum_length = token.getNumberValue();
                    // value
                    consume(Type.Int);

                } else {
                    // enum
                    consume();
                    int enum_index = token.getNumberValue();
                    // index
                    consume(Type.Int);

                    String enum_value = token.getStringValue();
                    consume(Type.String);
                    enumType.getEnumValue().add(enum_index, enum_value);
                }
            }
        }

        return enumType;
    }

    // type RecordType is ArrayList<RecordField>;
    public RecordType recordType() {

        RecordType recordType = new RecordType();

        int enum_length = 0;

        while (token.type == Type.DATA_TYPE) {

            consume(Type.DATA_TYPE);

            if (token.type == Type.RECORDTYPE) {
                consume(Type.RECORDTYPE);

                if (token.type == Type.INT) {
                    // length
                    consume(Type.INT);
                    enum_length = token.getNumberValue();
                    // value
                    consume(Type.Int);

                } else {
                    // enum
                    consume();
                    int rf_index = token.getNumberValue();
                    // index
                    consume(Type.Int);
                    // RecordField.
                    RecordField rf = recordField();
                    recordType.getRecordField().add(rf_index, rf);
                }
            }
        }

        return recordType;
    }

    public DataType getUserDefinedType() {

        DataType dataType = null;

        // while (token.type == Type.DATA_TYPE) {
        consume(Type.DATA_TYPE);

        if (token.type == Type.TYPE_DECLARATION) {
            consume(Type.TYPE_DECLARATION);

            TypeDeclaration typeDeclaration = getTypeDeclaration();
            dataType = new DataType();
            dataType.setUserDefinedType(typeDeclaration.getName());
        }
        // }

        return dataType;
    }

    /*
     * type RecordField { name: Identifier; dtype: DataType; };
     */
    public RecordField recordField() {

        RecordField recordField = new RecordField();
        DataType dataType = null;

        while (token.type == Type.RECORD_FILED) {

            consume(Type.RECORD_FILED);

            if (token.type == Type.IDENTIFIER) {
                String identifier = Identifier();
                recordField.setName(identifier);
            } else if (token.type == Type.DATA_TYPE) {
                // Peek
                // rf.dtype = IntegerType
                // OR
                // m.type_declarations.element[3]
                dataType = dataType();
                // consume(Type.DATA_TYPE);
                //
                // if (token.type == Type.DATA_TYPE) {
                // consume(Type.DATA_TYPE);
                // dataType = dataType();
                //
                // } else if (token.type == Type.MODEL) {
                // TypeDeclaration typeDeclaration = getTypeDeclaration();
                // dataType = new DataType();
                // dataType.setUserDefinedType(typeDeclaration.getName());
                // }
                recordField.setType(dataType);
            }
        }

        return recordField;
    }

    /*
     * type OptionKind enum { Some, None };
     *
     * type Option<T> { kind: OptionKind; value: T; };
     */
    public Type optionKind() {

        // while(token.type == Type.OPTION) {
        consume(Type.OPTION);

        // kind
        consume(Type.DATA_KIND);
        // Value
        String type_value = token.sd.getName();
        Type type = Type.get(type_value);

        if (type == Type.SOME) {
            consume(Type.SOME);
            // String type_value = peek().sd.getName();
            // Type type = Type.get(type_value);

        } else if (type == Type.NONE) {

            consume(Type.NONE);
        }
        // }
        return type;
    }
    /*
     * <T>mk_some: T -> Option<T> := fun (v:T) { some(o: Option<T>) { o.kind =
     * OptionKind.Some && o.value = v }};
     *
     * <T>mk_none: Option<T> := some(o: Option<T>) { o.kind = OptionKind.None };
     */

    // mk_some ::= mk_some:OPTION {DATA_TYPE:t} DATA_TYPE_KIND:kind DATATYPE
    // Option ::== mk_some or None.
    // Parametric Option<T> ... Option<DataType>
    // Option ::= MK_NONE | MK_SOME
    public Type option() {

        // DataType dataType = null;

        // consume(Type.OPTION);

        String type_value = token.sd.getName();
        Type type = Type.get(type_value);

        if (type == Type.MK_NONE) {
            consume(Type.MK_NONE);
            // Some or NONE
            type = mk_none();
            // return null;
        } else if (type == Type.SOME) {
            consume(Type.SOME);
            // dataType = dataType();
        }

        return type;
        // return dataType;
    }

    public Type mk_none() {

        consume(Type.OPTION);
        return optionKind();
    }

    public Type mk_some() {

        consume(Type.OPTION);
        return optionKind();
    }

    public String optionIdentifier() {

        String identifier = null;

        String type_value = this.token.sd.getName();
        Type type = Type.get(type_value);

        if (type == Type.MK_NONE) {
            consume(Type.MK_NONE);
        } else if (type == Type.SOME) {

            consume(Type.SOME);
            identifier = Identifier();
        }

        return identifier;
    }

    /*
     * type Expression { kind: ExpressionKind; identifier: Identifier; and:
     * BinaryOperation; equal: BinaryOperation; };
     */
    public Expression expression() {

        Expression expression = new Expression();
        Type type = null;

        while (token.type == Type.EXPRESSION) {
            // Kind...
            consume(Type.EXPRESSION);

            if (token.type == Type.EXPRESSION_KIND) {
                type = expressionKind();
            }

            // ID
            if (type == Type.Id) {
                return idExpr();
            } else if (type == Type.INT_Lit) {
                return intExpr();
            } else if (type == Type.REAL_Lit) {
                return realExpr();
            } else if (type == Type.BOOL_Lit) {
                return boolExpr();
            } else if (type == Type.NEG) {
                Expression neg_expr = expression();
                expression.setNegative(neg_expr);
                return expression;
            } else if (type == Type.NOT) {
                Expression not_expr = expression();
                expression.setNot(not_expr);
                return expression;
            } else if (type == Type.PRE) {
                Expression pre_expr = expression();
                expression.setPre(pre_expr);
                return expression;
            } else if (type == Type.EVENT) {
                Expression event_expr = expression();
                expression.setEvent(event_expr);
                return expression;
            } else if (type == Type.TO_REAL) {
                Expression real_expr = expression();
                expression.setToReal(real_expr);
                return expression;
            } else if (type == Type.TO_INT) {
                Expression int_expr = expression();
                expression.setToInt(int_expr);
                return expression;
            } else if (token.type == Type.BINARY_OP) {
                // OP
                // EQ
                // AND
                BinaryOperation op = binaryOP();

                if (type == Type.EQ) {
                    // BinaryOperation eq = binaryExpression();
                    expression.setEqual(op);
                } else if (type == Type.AND) {
                    /// BinaryOperation and = binaryExpression();
                    expression.setAnd(op);
                } else if (type == Type.OR) {
                    // BinaryOperation or = binaryExpression();
                    expression.setOr(op);
                } else if (type == Type.IMPLIES) {
                    // BinaryOperation implies = binaryExpression();
                    expression.setImplies(op);
                } else if (type == Type.PLUS) {
                    expression.setPlus(op);
                } else if (type == Type.MINUS) {
                    expression.setMinus(op);
                } else if (type == Type.DIV) {
                    expression.setDiv(op);
                } else if (type == Type.TIMES) {
                    expression.setTimes(op);
                } else if (type == Type.XOR) {
                    expression.setXor(op);
                } else if (type == Type.MOD) {
                    expression.setMod(op);
                } else if (type == Type.LEQ) {
                    // BinaryOperation leq = binaryExpression();
                    expression.setLessThanOrEqualTo(op);
                } else if (type == Type.LE) {
                    // BinaryOperation leq = binaryExpression();
                    expression.setLessThan(op);
                } else if (type == Type.GEQ) {
                    // BinaryOperation leq = binaryExpression();
                    expression.setGreaterThanOrEqualTo(op);
                } else if (type == Type.GE) {
                    // BinaryOperation leq = binaryExpression();
                    expression.setGreaterThan(op);
                } else if (type == Type.ARROW) {
                    // BinaryOperation arrow = binaryExpression();
                    expression.setArrow(op);
                } else if (type == Type.NEQ) {
                    // BinaryOperation neq = binaryExpression();
                    expression.setNotEqual(op);
                }
            } else if (token.type == Type.ITE) {
                if (type == Type.CND_EXPR) {
                    IfThenElse ite = ITE();
                    expression.setConditionalExpression(ite);
                }
            } else if (token.type == Type.RECORD_PROJECTION) {

                if (type == Type.RECORD_PROJECTION) {
                    RecordProjection record_expr = recordProjection();
                    expression.setRecordProjection(record_expr);
                }
            } else if (token.type == Type.RECORD_LITERAL) {
                if (type == Type.RECORD_LITERAL) {
                    RecordLiteral recordLit = recordLiteral();
                    expression.setRecordLiteral(recordLit);
                }
            } else if (token.type == Type.NODE_CALL) {
                if (type == Type.CALL) {
                    NodeCall nodeCall = nodeCall();
                    expression.setCall(nodeCall);
                }
            }
        }

        return expression;
    }

    /*
     * type ExpressionKind enum { Id, BoolLiteral, IntLiteral, RealLiteral, ToInt,
     * ToReal, RecordProjection, RecordLiteral, Minus, Negative, Plus, Times, Div,
     * IntDiv, Mod, Not, And, Or, Xor, Implies, LessThan, GreaterThan,
     * LessThanOrEqualTo, GreaterThanOrEqualTo, Equal, NotEqual, ConditionalExpr,
     * Arrow, Pre, Call, ExpressionList };
     */
    public Type expressionKind() {

        consume(Type.EXPRESSION_KIND);

        String type_value = token.sd.getName();
        Type type = Type.get(type_value);
        consume(type);

        // if (type == Type.INT) {
        // plainType = ExpressionKind.valueOf("INT");
        // } else if (type == Type.REAL) {
        // plainType = PlainType.valueOf("REAL");
        // } else if (type == Type.BOOL) {
        // plainType = PlainType.valueOf("BOOL");
        // }

        return type;
    }

    /*
     * mk_id_expr: Identifier -> Expression := fun (id: Identifier) {some (e:
     * Expression) { e.kind = ExpressionKind.Id && e.identifier = id }};
     */
    public Expression idExpr() {

        Expression idExpr = new Expression();

        String identifier_name;
        String identifier_value;

        consume(Type.EXPRESSION);

        identifier_name = token.sd.getName();
        consume(Type.IDENTIFIER);

        identifier_value = token.getStringValue();
        consume(Type.String);

        // LOGGER.info(identifier_name + " := " + identifier_value);

        idExpr.setIdentifier(identifier_value);

        return idExpr;
    }

    public Expression intExpr() {

        Expression intExpr = new Expression();

        consume(Type.EXPRESSION);

        consume(Type.INT);
        // System.err.println("+++>" + token);
        int literal_value = token.getNumberValue();
        consume(Type.Int);

        BigInteger bi = BigInteger.valueOf(literal_value);

        intExpr.setIntLiteral(bi);

        return intExpr;
    }

    public Expression realExpr() {

        Expression realExpr = new Expression();

        consume(Type.EXPRESSION);
        consume(Type.REAL);
        // System.out.println("REAL TOKEN:" + token.getFloatingValue());
        // BigDecimal literal_value = BigDecimal.valueOf(token.getFloatingValue());
        BigDecimal literal_value = new BigDecimal(String.format("%f", token.getFloatingValue()));

        consume(Type.Float);

        realExpr.setRealLiteral(literal_value);

        return realExpr;
    }

    public Expression boolExpr() {

        Expression boolExpr = new Expression();

        consume(Type.EXPRESSION);

        consume(Type.BOOL);
        boolean literal_value = token.getTruthValue();
        consume(Type.Boolean);

        boolExpr.setBoolLiteral(literal_value);

        return boolExpr;
    }

    public IfThenElse ITE() {
        IfThenElse ite = new IfThenElse();

        while (token.type == Type.ITE) {
            consume(Type.ITE);
            String type_value = token.sd.getName();
            Type type = Type.get(type_value);

            if (type == Type.CONDITION) {
                Expression condition_expr = expression();
                ite.setCondition(condition_expr);
            } else if (type == Type.THEN) {
                Expression then_expr = expression();
                ite.setThenBranch(then_expr);
            } else if (type == Type.ELSE) {
                Expression else_expr = expression();
                ite.setElseBranch(else_expr);
                break;
            }
        }
        return ite;
    }

    /*
     * type NodeCall { node: Identifier -- Indirect Reference arguments:
     * ArrayList<Expression>; }
     */
    public NodeCall nodeCall() {

        NodeCall nodeCall = new NodeCall();
        int array_length = 0;

        while (token.type == Type.NODE_CALL) {
            consume(Type.NODE_CALL);

            if (token.type == Type.IDENTIFIER) {
                consume(Type.IDENTIFIER);

                String identifier = token.getStringValue();
                consume(Type.String);
                nodeCall.setNodeId(identifier);
            } else if (token.type == Type.ARRAY_LIST) {

                String type_value = token.sd.getName();
                Type type = Type.get(type_value);

                if (peek().type == Type.INT) {

                    array_length = arrayList_length();

                } else {

                    int element_index = arrayList_element();

                    Expression expression = expression();
                    nodeCall.getArgument().add(element_index, expression);
                }
            }
        }

        return nodeCall;
    }

    /*
     * lhs_operand: Expression; rhs_operand: Expression;
     *
     */
    public BinaryOperation binaryOP() {

        BinaryOperation binaryOperation = new BinaryOperation();

        while (token.type == Type.BINARY_OP) {
            consume(Type.BINARY_OP);

            String type_value = token.sd.getName();
            Type type = Type.get(type_value);

            if (type == Type.LHS) {
                Expression lhr_expr = expression();
                binaryOperation.setLhsOperand(lhr_expr);
            } else if (type == Type.RHS) {
                Expression rhs_expresion = expression();
                binaryOperation.setRhsOperand(rhs_expresion);
                break;
            }
        }

        return binaryOperation;
    }

    /*
     * type RecordProjection { record_reference: Expression; field_id: Identifier;
     * --- Indirect reference }
     */

    public RecordProjection recordProjection() {

        RecordProjection recordProjection = new RecordProjection();

        while (token.type == Type.RECORD_PROJECTION) {
            consume(Type.RECORD_PROJECTION);

            String type_value = token.sd.getName();
            Type type = Type.get(type_value);

            if (type == Type.RECORD_REF) {
                Expression expression = expression();
                recordProjection.setRecordReference(expression);
            } else if (type == Type.FIELD_ID) {
                String identifier = Identifier();
                // consume(Type.IDENTIFIER);
                // String identifier = token.getStringValue();
                // consume(Type.String);
                recordProjection.setFieldId(identifier);
                break;
            }
        }

        return recordProjection;
    }

    /*
     * type FieldDefinition { field_id: Identifier; -- Indirect reference
     * field_value: Expression; };
     */
    public FieldDefinition fieldDefinition() {
        FieldDefinition fieldDefinition = new FieldDefinition();

        while (token.type == Type.FIELD_DEFINITION) {
            consume(Type.FIELD_DEFINITION);

            String type_value = token.sd.getName();
            Type type = Type.get(type_value);

            if (type == Type.FIELD_ID) {
                consume(Type.IDENTIFIER);
                String identifier = token.getStringValue();
                consume(Type.String);
                fieldDefinition.setFieldIdentifier(identifier);
            } else if (type == Type.FIELD_VALUE) {
                Expression expression = expression();
                fieldDefinition.setFieldValue(expression);
            }
        }

        return fieldDefinition;
    }

    /*
     * type RecordLiteral { record_type: Identifier; -- Indirect reference
     * field_definitions: ArrayList<FieldDefinition>; };
     */

    public RecordLiteral recordLiteral() {

        RecordLiteral recordLiteral = new RecordLiteral();
        int array_length = 0;

        while (token.type == Type.RECORD_LITERAL) {
            consume(Type.RECORD_LITERAL);

            if (token.type == Type.IDENTIFIER) {
                consume(Type.IDENTIFIER);

                String identifier = token.getStringValue();
                consume(Type.String);
                recordLiteral.setRecordType(identifier);

            } else if (token.type == Type.ARRAY_LIST) {

                String type_value = token.sd.getName();
                Type type = Type.get(type_value);

                if (peek().type == Type.INT) {

                    array_length = arrayList_length();

                } else {

                    int element_index = arrayList_element();

                    // if(type.FIELD_DEFINITION == Type.FIELD_DEFINITION) {
                    FieldDefinition fieldDefinition = fieldDefinition();
                    recordLiteral.getFieldDefinition().add(element_index, fieldDefinition);
                    // }
                    if (element_index == array_length - 1) {
                        break;
                    }
                }
            }
        }
        return recordLiteral;
    }

    public ArrayList<Expression> arrayList_Expression() {

        ArrayList<Expression> expressionList = new ArrayList<Expression>();

        int list_length = 0;

        Type type = this.token.type;

        while (type == Type.ARRAY_LIST) {

            consume(Type.ARRAY_LIST);

            // length
            if (this.token.type == Type.INT) {

                Token token = this.token;
                NumberLiteral enumLength_literal = (NumberLiteral) token.value;
                list_length = enumLength_literal.getValue();
                consume(Type.INT);

                while (list_length > 0) {

                    // NULL:element
                    consume(Type.NULL);
                    list_length--;
                    // element Index
                    token = this.token;
                    NumberLiteral element_index = (NumberLiteral) token.value;
                    consume(Type.INT);

                    // //element Value : Expression
                    Expression expression = expression();
                    expressionList.add(element_index.getValue(), expression);
                }
            }
        }

        return expressionList;
    }

    // o.kind = OptionKind.Some &&
    // o.value = v

    // TypeDeclaration ::= TYPE_DECLARATION:Name TYPE_DECLARATION:d IDENTIFIER:Name
    // TYPE_DECLARATION:d
    // OPTION:Definition OPTION:mk_some DATATYPE:t DATA_TYPE_KIND:kind
    // null:Enum

    // Type Declaration ::= Type_Declaration:d Identifier:name | Type_Declaration:d
    // definition:
    // Option<DataType>
    public TypeDeclaration typeDeclaration() {

        TypeDeclaration typeDeclaration = new TypeDeclaration();
        String type_id = null;
        // String ID = this.token.sd.getName();

        while (token.type == Type.TYPE_DECLARATION) {
            consume(Type.TYPE_DECLARATION);

            if (token.type == Type.IDENTIFIER) {

                type_id = Identifier();
                // Renaming dot[.] in Type Declaration Identifier.
                String identifier = type_id.replace(".", "_dot_");
                typeDeclaration.setName(identifier);

            } else if (token.type == Type.OPTION) {

                consume(Type.OPTION);

                Type type = option();

                if (type == Type.SOME) {
                    // Option<DataType>
                    if (token.type == Type.DATA_TYPE) {
                        consume(Type.DATA_TYPE);
                        try {

                            DataType dataType = dataType();
                            typeDeclaration.setDefinition(dataType);
                        } catch (IndexOutOfBoundsException exp) {

                            System.out.println(
                                    "Forward references in datatype declaration is not supported.");

                            if (type_id != null) {
                                System.out.println("Please fix " + type_id);
                                System.out.println("Error message: " + exp.getMessage());
                                exp.printStackTrace();
                            }

                            System.exit(-1);
                        }
                    }
                } else {
                    // System.out.println("Found Data Type with No
                    // Definition!!!!!!!!!!!");
                }
            }
        }

        return typeDeclaration;
    }

    public TypeDeclaration getTypeDeclaration() {

        TypeDeclaration typeDeclaration = null;
        // consume(Type.COMPONENT_TYPE);
        consume(Type.MODEL);

        int typeDeclaration_index = arrayList_element();

        //		try {
        typeDeclaration = typeDeclarations.get(typeDeclaration_index);
        //		} catch (IndexOutOfBoundsException exp) {
        //			System.out.println("Forward reference in datatype declaration is not supported!");
        //			System.exit(-1);
        //		}

        return typeDeclaration;
    }

    /*
     * type ArrayList<T> { length: Int; element: T[length]; };
     */
    public int arrayList_length() {

        int list_length = 0;
        Type type = this.token.type;

        if (type == Type.ARRAY_LIST) {
            consume(Type.ARRAY_LIST);
            // length
            if (this.token.type == Type.INT) {
                consume(Type.INT);

                Token token = this.token;
                NumberLiteral enumLength_literal = (NumberLiteral) token.value;
                list_length = enumLength_literal.getValue();
                consume(Type.Int);
            }
        }

        return list_length;
    }

    // name: Identifier;
    // dtype: DataType;
    // definition: Option<Expression>;

    public ConstantDeclaration constantDeclaration() {

        ConstantDeclaration constantDeclaration = new ConstantDeclaration();
        DataType dataType = null;
        // String ID = this.token.sd.getName();
        // constantDeclaration.setId(ID);

        while (token.type == Type.CONSTANT_DECLARATION) {

            consume(Type.CONSTANT_DECLARATION);

            if (token.type == Type.IDENTIFIER) {
                String identifier = Identifier();
                constantDeclaration.setName(identifier);
            } else if (token.type == Type.DATA_TYPE) {

                consume(Type.DATA_TYPE);

                if (token.type == Type.DATA_TYPE) {
                    // consume(Type.DATA_TYPE);
                    dataType = dataType();

                } else if (token.type == Type.MODEL) {
                    TypeDeclaration typeDeclaration = getTypeDeclaration();
                    dataType = new DataType();
                    dataType.setUserDefinedType(typeDeclaration.getName());
                }

                // DataType dataType_value = dataType();
                constantDeclaration.setDataType(dataType);
            } else if (token.type == Type.OPTION) {
                consume(Type.OPTION);

                Type type = option();

                if (type == Type.SOME) {
                    Expression expression = expression();
                    constantDeclaration.setDefinition(expression);
                }
                // skip None
            }
        }

        return constantDeclaration;
    }

    /*
     * type VariableDeclaration { name: Identifier; dtype: DataType; };
     */
    public VariableDeclaration variableDeclaration() {

        VariableDeclaration variableDeclaration = new VariableDeclaration();
        DataType dataType = null;

        while (token.type == Type.VARIABLE_DECLARATION) {

            consume(Type.VARIABLE_DECLARATION);

            if (token.type == Type.IDENTIFIER) {
                String identifier = Identifier();
                variableDeclaration.setName(identifier);
            } else if (token.type == Type.DATA_TYPE) {

                consume(Type.DATA_TYPE);

                if (token.type == Type.DATA_TYPE) {
                    // consume(Type.DATA_TYPE);
                    dataType = dataType();

                } else if (token.type == Type.MODEL) {
                    TypeDeclaration typeDeclaration = getTypeDeclaration();
                    dataType = new DataType();
                    dataType.setUserDefinedType(typeDeclaration.getName());
                }

                // DataType dataType_value = dataType();
                variableDeclaration.setDataType(dataType);
            }
        }

        return variableDeclaration;
    }

    // type Port {
    // name: Identifier;
    // mode: PortMode;
    // is_event: Bool;
    // ptype: Option<DataType>;
    // probe: Bool;
    // };

    public Port port(String componentID) {

        Port port = new Port();

        // String ID = this.token.sd.getName();
        // port.setId(ID);

        while (token.type == Type.PORT) {

            consume(Type.PORT);

            if (token.type == Type.IDENTIFIER) {

                String identifier = Identifier();
                port.setName(identifier);
                port.setId(identifier);
                //                port.setId(componentID + "." + identifier);

            } else if (token.type == Type.PORT_MODE) {
                PortMode portMode = portMode();
                port.setMode(portMode);
            } else if (token.type == Type.BOOL) {

                // consume(Type.BOOL);

                String type_value = this.token.sd.getName();
                Type type = Type.get(type_value);

                if (type == Type.PROBE) {
                    consume(Type.PROBE);

                    boolean is_probe = token.getTruthValue();

                    port.setProbe(is_probe);
                    consume(Type.Boolean);

                } else if (type == Type.IS_EVENT) {
                    consume(Type.IS_EVENT);

                    boolean is_event = token.getTruthValue();

                    port.setEvent(is_event);
                    consume(Type.Boolean);
                }

            } else if (token.type == Type.OPTION) {
                consume(Type.OPTION);

                Type type = option();
                // SOME
                if (type == Type.SOME) {
                    DataType dataType = null;

                    if (token.type == Type.MODEL) {
                        TypeDeclaration typeDeclaration = getTypeDeclaration();
                        dataType = new DataType();
                        dataType.setUserDefinedType(typeDeclaration.getName());
                    } else if (token.type == Type.DATA_TYPE) {
                        dataType = dataType();
                    }
                    port.setType(dataType);
                }
                // Skip None
            }
        }

        return port;
    }

    // type GenericAttribute {
    // name: String;
    // atype: AttributeType;
    // value: String;
    // };

    public GenericAttribute genericAttribute() {

        GenericAttribute gAttribute = new GenericAttribute();

        // String ID = this.token.sd.getName();
        // port.setId(ID);

        while (token.type == Type.GENERIC_ATTRIBUTE) {

            consume(Type.GENERIC_ATTRIBUTE);

            if (token.type == Type.STRING) {

                String type_value = this.token.sd.getName();
                Type type = Type.get(type_value);

                if (type == Type.NAME) {
                    consume(Type.NAME);

                    String identifier = token.getStringValue();
                    gAttribute.setName(identifier);

                    consume(Type.String);

                } else if (type == Type.VALUE) {
                    consume(Type.VALUE);

                    String value = token.getStringValue();
                    gAttribute.setValue(value);

                    consume(Type.String);
                }

            } else if (token.type == Type.ATTRIBUTE_TYPE) {
                QName attributeType = attributeType();
                gAttribute.setType(attributeType);
            }
        }

        return gAttribute;
    }

    // Array_List ::= ARRAY_LIST:<T> INT:length Int:value |
    // ARRAY_LIST:<T> NULL:element Int:index T:value

    public int arrayList_element() {

        int e_index = -1;

        // ComponentTypes
        consume(Type.ARRAY_LIST);

        // NULL:element
        consume();

        // element Index
        // token = this.token;
        NumberLiteral element_index = (NumberLiteral) token.value;
        e_index = element_index.getValue();
        consume(Type.Int);

        // Port port = port(component_ID);
        // portList.add(, port);

        return e_index;
    }

    /** type PortMode enum { In, Out }; */
    public PortMode portMode() {

        PortMode portMode = null;

        Type type = token.type;
        consume(Type.PORT_MODE);
        // Enum
        String type_value = this.token.sd.getName();
        type = Type.get(type_value);

        if (type == Type.IN) {
            portMode = PortMode.valueOf("IN");
            consume();

        } else if (type == Type.OUT) {

            portMode = PortMode.valueOf("OUT");
            consume();
        }

        return portMode;
    }

    /** type AttributeType enum {Int, Real, Bool, String}; */
    public QName attributeType() {

        QName attributeType = null;

        Type type = token.type;
        consume(Type.ATTRIBUTE_TYPE);
        // Enum
        // String type_value = this.token.type;
        type = this.token.type;

        if (type == Type.INT) {
            attributeType = new QName("Int");
            consume();
        } else if (type == Type.BOOL) {
            attributeType = new QName("Bool");
            consume();
        } else if (type == Type.REAL) {
            attributeType = new QName("Real");
            consume();
        } else if (type == Type.STRING) {
            attributeType = new QName("String");
            consume();
        }

        return attributeType;
    }

    /*
     *
     * type ContractSpec { constant_declarations: ArrayList<SymbolDefinition>;
     * variable_declarations: ArrayList<SymbolDefinition>; assumes:
     * ArrayList<ContractItem>; guarantees: ArrayList<ContractItem>; modes:
     * ArrayList<ContractMode>; imports: ArrayList<ContractImport>; };
     */
    public ContractSpec contractSpec() {

        ContractSpec contractSpec = new ContractSpec();
        int array_length = 0;
        // String ID = this.token.sd.getName();
        // contractSpec.setId(ID);

        while (this.token.type == Type.CONTRACT_SPEC) {

            consume(Type.CONTRACT_SPEC);

            if (token.type == Type.ARRAY_LIST) {

                String type_value = this.token.sd.getName();
                Type type = Type.get(type_value);

                if (peek().type == Type.INT) {
                    array_length = arrayList_length();
                } else {

                    if (type == Type.CONSTANT_DECLARATIONS) {
                        int constantDeclaration_index = arrayList_element();

                        SymbolDefinition symbolDefinition = symbolDefinition();
                        contractSpec.getSymbol().add(constantDeclaration_index, symbolDefinition);
                    } else if (type == Type.VARIABLE_DECLARATIONS) {
                        int variableDeclaration_index = arrayList_element();

                        SymbolDefinition symbolDefinition = symbolDefinition();
                        contractSpec.getSymbol().add(variableDeclaration_index, symbolDefinition);

                    } else if (type == Type.ASSUMES) {
                        int assume_index = arrayList_element();
                        ContractItem contractItem = contractItem();
                        contractSpec.getAssume().add(assume_index, contractItem);
                    } else if (type == Type.GURANTEES) {
                        int gurantee_index = arrayList_element();
                        ContractItem contractItem = contractItem();
                        // LOGGER.info(
                        // "guarantees - ["
                        // + gurantee_index
                        // + "] = "
                        // + contractItem.getName());
                        contractSpec.getGuarantee().add(gurantee_index, contractItem);

                    } else if (type == Type.MODES) {
                        int mode_index = arrayList_element();

                        // ContractMode contractMode = contractMode();
                        // contractSpec.getMode().add(mode_index,
                        // contractMode);

                    } else if (type == Type.IMPORTS) {
                        int import_index = arrayList_element();

                        ContractImport contractImport = contractImport();
                        contractSpec.getImport().add(import_index, contractImport);
                    }
                }
            }
        }

        return contractSpec;
    }

    /*
     * type ContractImport { contract: Contract; input_arguments:
     * ArrayList<Expression>; input_arguments: ArrayList<Expression>; }
     */
    public ContractImport contractImport() {

        ContractImport contractImport = new ContractImport();

        // String ID = this.token.sd.getName();
        // contractImport.setId(ID);

        Type type = this.token.type;

        while (type == Type.CONTRACT_IMPORT) {
            consume(Type.CONTRACT_IMPORT);

            String type_value = this.token.sd.getName();
            type = Type.get(type_value);

            if (type == Type.CONTRACT) {
                Contract contract = contract();
                // contractImport.setContractId(contract);
            } else if (type == Type.INPUT_ARGUMENTS) {
                ArrayList<Expression> input_arguments = arrayList_Expression();

            } else if (type == Type.OUTPUT_ARGUMENTS) {
                ArrayList<Expression> output_arguments = arrayList_Expression();
            }
        }

        return contractImport;
    }

    /*
     * type Contract { name: Identifier; input_paramenters:
     * ArrayList<NodeParameter>; output_paramenters: ArrayList<NodeParameter>;
     * specification: ContractSpec; };
     */
    public Contract contract() {

        Contract contract = new Contract();

        String ID = this.token.sd.getName();
        // contract.setId(ID);

        while (this.token.type == Type.CONTRACT) {

            consume(Type.CONTRACT);

            String type_value = this.token.sd.getName();
            Type type = Type.get(type_value);

            if (type == Type.IDENTIFIER) {
                String identifier = Identifier();
                contract.setName(identifier);
            } else if (type == Type.INPUT_NODE_PARAMETER) {
                // ArrayList<NodeParameter> input_parameters = nodeParameter();
                // contract.getInputParamater().addAll(input_parameters);
            } else if (type == Type.OUTPUT_NODE_PARAMETER) {
                // ArrayList<NodeParameter> output_parameters = nodeParameter();
                // contract.getInputParamater().addAll(output_parameters);
            } else if (type == Type.CONTRACT_SPEC) {
                ContractSpec specification = contractSpec();
                contract.getSpecification().add(specification);
            }
        }

        return contract;
    }

    /*
     * type ContractItem { name: Option<Identifier>; expression: Expression; }
     */
    public ContractItem contractItem() {

        ContractItem contractItem = new ContractItem();

        // String ID = this.token.sd.getName();

        while (this.token.type == Type.CONTRACT_ITEM) {
            consume(Type.CONTRACT_ITEM);

            if (this.token.type == Type.OPTION) {
                consume(Type.OPTION);

                Type type = option();

                if (type == Type.SOME) {
                    String identifier = token.getStringValue();
                    contractItem.setName(identifier);
                    consume(Type.String);
                    // LOGGER.info("Contract ITEM: " + identifier);
                }
            } else if (this.token.type == Type.EXPRESSION) {
                Expression expression = expression();
                contractItem.setExpression(expression);
            }
        }

        return contractItem;
    }

    /*
     * type SymbolDefinition { name: Identifier; is_constant: Bool; dtype: DataType;
     * definition: Expression; };
     */
    public SymbolDefinition symbolDefinition() {

        SymbolDefinition symbolDefinition = new SymbolDefinition();
        DataType dataType = null;

        String ID = this.token.sd.getName();
        // symbolDefinition.setId(ID);

        while (this.token.type == Type.SYMBOL_DEFINITION) {

            consume(Type.SYMBOL_DEFINITION);

            if (token.type == Type.IDENTIFIER) {
                String identifier = Identifier();
                symbolDefinition.setName(identifier);
            } else if (token.type == Type.DATA_TYPE) {

                consume(Type.DATA_TYPE);

                if (token.type == Type.DATA_TYPE) {
                    // consume(Type.DATA_TYPE);
                    dataType = dataType();
                    symbolDefinition.setDataType(dataType);

                } else if (token.type == Type.MODEL) {
                    TypeDeclaration typeDeclaration = getTypeDeclaration();
                    dataType = new DataType();
                    dataType.setUserDefinedType(typeDeclaration.getName());
                }

                // DataType dataType_value = dataType();
                symbolDefinition.setDataType(dataType);
            } else if (token.type == Type.BOOL) {
                consume(Type.BOOL);

                boolean value = token.getTruthValue();
                consume(Type.Boolean);
                // DATA this field into VDM Lustre in NodeParameter.
                symbolDefinition.setIsConstant(truthValue());
            } else if (token.type == Type.EXPRESSION) {
                Expression expression = expression();
                symbolDefinition.setDefinition(expression);
            }
        }

        return symbolDefinition;
    }

    /** True or False */
    public boolean truthValue() {

        boolean flag = false;

        TruthValue truthValue = (TruthValue) this.token.value;
        consume(Type.Boolean);

        if (truthValue.isTRUE()) {
            flag = true;
        }

        return flag;
    }

    /*
     * name: Identifier; ports: ArrayList<Port>; contract: Option<ContractSpec>;
     * cyber_relations: ArrayList<CyberRel>;
     *
     */
    // type ComponentType {
    // name: Identifier;
    // ports: ArrayList<Port>;
    // compCateg: Option<String>;
    // contract: Option<ContractSpec>;
    // cyber_relations: ArrayList<CyberRel>;
    // safety_relations: ArrayList<SafetyRel>;
    // safety_events: ArrayList<SafetyEvent>;
    // };
    public ComponentType componentType() {

        ComponentType componentType = new ComponentType();
        List<Port> ports = componentType.getPort();
        List<CyberRel> cyber_relations = componentType.getCyberRel();
        List<SafetyRel> safety_relations = componentType.getSafetyRel();
        List<Event> events = componentType.getEvent();

        int list_size = 0;
        Type type;

        // @TODO: Check ID.
        // String ID = token.sd.getName();
        //

        while (token.type == Type.COMPONENT_TYPE) {

            consume(Type.COMPONENT_TYPE);

            if (token.type == Type.IDENTIFIER) {
                String identifier = Identifier();
                componentType.setName(identifier);
                componentType.setId(identifier);
                // LOGGER.info("COMPONENT TYPE: >>>>>>>>>>>" + identifier);
            }
            // PORT ARRAY LIST.
            else if (token.type == Type.ARRAY_LIST) {

                String type_name = this.token.sd.getName();
                type = Type.get(type_name);

                if (peek().type == Type.INT) {

                    list_size = arrayList_length();

                } else {

                    if (type == Type.PORTS) {
                        int port_index = arrayList_element();
                        Port port = port(componentType.getId());
                        ports.add(port_index, port);
                        // LOGGER.info("["+ port_index + "," + port.getName()+"]");
                    } else if (type == Type.CYBER_RELATIONS) {
                        log("cyber_rel" + token);
                        int rel_index = arrayList_element();

                        CyberRel cyberRel = cyber_rel();
                        cyber_relations.add(rel_index, cyberRel);
                    } else if (type == Type.SAFETY_RELATIONS) {
                        log("safety_rel: " + token);
                        int rel_index = arrayList_element();

                        SafetyRel safetyRel = safety_rel();
                        safety_relations.add(rel_index, safetyRel);
                    } else if (type == Type.SAFETY_EVENTS) {
                        log("safety event: " + token);
                        int event_index = arrayList_element();

                        Event event = safetyevent();
                        events.add(event_index, event);
                    }
                }
                // Confirm length of ports match with elements.
            } else if (token.type == Type.OPTION) {

                consume(Type.OPTION);

                type = option();
                // Some
                if (type == Type.SOME) {

                    if (token.type == Type.CONTRACT_SPEC) {
                        ContractSpec contract = contractSpec();
                        componentType.setContract(contract);
                    } else if (token.type == Type.String) {
                        String categ = token.getStringValue();
                        componentType.setCompCateg(categ);
                        consume(Type.String);
                    }
                }
                // Skip None
            }
        }

        return componentType;
    }

    /*
     * type CyberExpr { kind: CyberExprKind; port: CIAPort; and:
     * ArrayList<CyberExpr>; or: ArrayList<CyberExpr>; not: CyberExpr; };
     */

    public CyberExpr cyberExpr() {

        CyberExpr cyberExpr = new CyberExpr();
        CyberExprKind kind = null;

        while (this.token.type == Type.CYBER_EXP) {

            consume(Type.CYBER_EXP);

            if (token.type == Type.CIA_PORT) {

                CIAPort ciaPort = cia_port();
                cyberExpr.setPort(ciaPort);
            }
            if (token.type == Type.CYBER_EXP_KIND) {
                kind = cyberExprKind();
            }

            if (kind == CyberExprKind.NOT) {
                CyberExpr not_expr = new CyberExpr();
                not_expr.setNot(not_expr);
                return not_expr;
            }

            if (kind == CyberExprKind.OR) {

                CyberExprList or_exprs = cyberExprList();
                cyberExpr.setOr(or_exprs);
            }
            if (kind == CyberExprKind.AND) {

                CyberExprList and_exprs = cyberExprList();
                cyberExpr.setAnd(and_exprs);
            }
            if (kind == CyberExprKind.PORT) {
                return cyberExpr();
                // CIAPort ciaPort = cia_port();
                // cyberExpr.setPort(ciaPort);
            }

            //
            // return cyberExprList;

        }

        return cyberExpr;
    }

    public CyberExprList cyberExprList() {

        CyberExprList cyberExprList = new CyberExprList();

        int array_length = 0;

        while (this.token.type == Type.CYBER_EXP) {

            consume(Type.CYBER_EXP);
            if (token.type == Type.ARRAY_LIST) {

                // String type_value = token.sd.getName();
                // Type type = Type.get(type_value);

                if (peek().type == Type.INT) {

                    array_length = arrayList_length();

                } else {

                    while (array_length > 0) {

                        array_length--;

                        int element_index = arrayList_element();

                        CyberExpr expr = cyberExpr();
                        cyberExprList.getExpr().add(element_index, expr);
                    }
                }
            }
        }

        return cyberExprList;
    }

    /** Handle safety requirement expressions */
    public SafetyReqExpr safetyReqExpr() {

        SafetyReqExpr safetyReqExpr = new SafetyReqExpr();
        SafetyReqExprKind kind = null;

        while (this.token.type == Type.SAFETY_EXP) {

            consume(Type.SAFETY_EXP);

            if (token.type == Type.IA_PORT) {

                IAPort iaPort = ia_port();
                safetyReqExpr.setPort(iaPort);
            }
            if (token.type == Type.SAFETY_EXP_KIND) {
                kind = safetyReqExprKind();
            }
            if (kind == SafetyReqExprKind.NOT) {
                SafetyReqExpr not_expr = new SafetyReqExpr();
                not_expr.setNot(not_expr);
                return not_expr;
            }
            if (kind == SafetyReqExprKind.OR) {

                SafetyReqExprList or_exprs = safetyReqExprList();
                safetyReqExpr.setOr(or_exprs);
            }
            if (kind == SafetyReqExprKind.AND) {

                SafetyReqExprList and_exprs = safetyReqExprList();
                safetyReqExpr.setAnd(and_exprs);
            }
            if (kind == SafetyReqExprKind.PORT) {
                return safetyReqExpr();
            }
        }

        return safetyReqExpr;
    }

    public SafetyReqExprList safetyReqExprList() {

        SafetyReqExprList safetyReqExprList = new SafetyReqExprList();

        int array_length = 0;

        while (this.token.type == Type.SAFETY_EXP) {

            consume(Type.SAFETY_EXP);
            if (token.type == Type.ARRAY_LIST) {
                if (peek().type == Type.INT) {
                    array_length = arrayList_length();
                } else {

                    while (array_length > 0) {

                        array_length--;

                        int element_index = arrayList_element();
                        SafetyReqExpr reqExpr = safetyReqExpr();
                        safetyReqExprList.getExpr().add(element_index, reqExpr);
                    }
                }
            }
        }

        return safetyReqExprList;
    }

    // type SafetyRel {
    // id: String;
    // output: IAPort;
    // faultSrc: Option<SafetyExpr>;
    // comment: Option<String>;
    // description : Option<String>;
    // };

    /** Handle safety relations */
    public SafetyRel safety_rel() {

        SafetyRel safetyRel = new SafetyRel();

        while (this.token.type == Type.SAFETY_REL) {

            consume(Type.SAFETY_REL);

            if (token.type == Type.STRING) {

                String identifier = id_value();
                log("safety_rel id = " + identifier);
                safetyRel.setId(identifier);

            } else if (token.type == Type.IA_PORT) {

                IAPort iaPort = ia_port();
                safetyRel.setOutput(iaPort);
            } else if (token.type == Type.OPTION) {

                String type_value = token.sd.getName();
                Type type = Type.get(type_value);
                consume(Type.OPTION);

                log("safety rel type_value = " + type_value);

                if (type == Type.FAULTSRC) {
                    SafetyRelExpr expr_value = safetyRelExpr();
                    safetyRel.setFaultSrc(expr_value);

                } else if (type == Type.COMMENT) {

                    String comment = str_value();
                    log("safety rel comment = " + comment);
                    safetyRel.setComment(comment);

                } else if (type == Type.DESCRIPTION) {

                    String description = str_value();
                    safetyRel.setDescription(description);

                } else if (type == Type.PHASES) {

                    String phases = str_value();
                    safetyRel.setPhases(phases);

                } else if (type == Type.EXTERN) {

                    String extern = str_value();
                    safetyRel.setExtern(extern);
                }
            }
        }

        return safetyRel;
    }

    // type SafetyExpr {
    // kind: SafetyExprKind;
    // port: IAPort;
    // fault: String;
    // and: ArrayList<SafetyExpr>;
    // or: ArrayList<SafetyExpr>;
    // not: SafetyExpr;
    // };
    public SafetyRelExpr safetyRelExpr() {

        SafetyRelExpr safetyRelExpr = new SafetyRelExpr();
        SafetyRelExprKind kind = null;

        // Check length bound and terminate.
        int array_length = 0;

        while (this.token.type == Type.SAFETY_EXP) {

            consume(Type.SAFETY_EXP);

            if (token.type == Type.IA_PORT) {

                IAPort iaPort = ia_port();
                safetyRelExpr.setPort(iaPort);
            }
            if (token.type == Type.SAFETY_EXP_KIND) {
                kind = safetyRelExprKind();
            }
            if (kind == SafetyRelExprKind.NOT) {
                SafetyRelExpr not_expr = new SafetyRelExpr();
                not_expr.setNot(not_expr);
                return not_expr;
            }
            if (kind == SafetyRelExprKind.OR) {

                SafetyRelExprList or_exprs = safetyRelExprList();
                safetyRelExpr.setOr(or_exprs);
            }
            if (kind == SafetyRelExprKind.AND) {

                SafetyRelExprList and_exprs = safetyRelExprList();
                safetyRelExpr.setAnd(and_exprs);
            }
            if (kind == SafetyRelExprKind.PORT) {
                return safetyRelExpr();
            }
            if (kind == SafetyRelExprKind.FAULT) {

                EventHappens event = eventHappens();
                safetyRelExpr.setFault(event);
                safetyRelExpr();
            }
        }

        return safetyRelExpr;
    }

    public SafetyRelExprList safetyRelExprList() {

        SafetyRelExprList safetyRelExprList = new SafetyRelExprList();

        int array_length = 0;

        while (this.token.type == Type.SAFETY_EXP) {

            consume(Type.SAFETY_EXP);
            if (token.type == Type.ARRAY_LIST) {
                if (peek().type == Type.INT) {
                    array_length = arrayList_length();
                } else {

                    while (array_length > 0) {

                        array_length--;

                        int element_index = arrayList_element();
                        SafetyRelExpr relExpr = safetyRelExpr();
                        safetyRelExprList.getExpr().add(element_index, relExpr);
                    }
                }
            }
        }

        return safetyRelExprList;
    }

    /*
     * type CyberRel { id : String; output : CIAPort; inputs : Option<CyberExpr>;
     * comment: Option<String>; description : Option<String>; phases :
     * Option<String>; extern : Option<String>; };
     */
    public CyberRel cyber_rel() {

        CyberRel cyberRel = new CyberRel();

        while (this.token.type == Type.CYB_REL) {

            consume(Type.CYB_REL);

            if (token.type == Type.STRING) {

                String identifier = id_value();
                cyberRel.setId(identifier);

            } else if (token.type == Type.CIA_PORT) {

                CIAPort ciaPort = cia_port();
                cyberRel.setOutput(ciaPort);

            } else if (token.type == Type.OPTION) {

                String type_value = token.sd.getName();
                Type type = Type.get(type_value);
                consume(Type.OPTION);

                if (type == Type.INPUTS) {
                    CyberExpr expr_value = cyberExpr();
                    cyberRel.setInputs(expr_value);

                } else if (type == Type.COMMENT) {

                    String comment = str_value();
                    cyberRel.setComment(comment);

                } else if (type == Type.DESCRIPTION) {

                    String description = str_value();
                    cyberRel.setDescription(description);

                } else if (type == Type.PHASES) {

                    String phases = str_value();
                    cyberRel.setPhases(phases);

                } else if (type == Type.EXTERN) {

                    String extern = str_value();
                    cyberRel.setExtern(extern);
                }
            }
        }

        return cyberRel;
    }

    // IDENTIFIER ::= IDENTIFIER:name String:value
    // Return a String.
    public String id_value() {

        String identifier_name;
        String identifier_value;

        identifier_name = token.sd.getName();
        consume(Type.STRING);

        identifier_value = token.getStringValue();
        consume(Type.String);

        // LOGGER.info(identifier_name + " := " + identifier_value);

        return identifier_value;
    }

    public String str_value() {
        String value = token.getStringValue();
        consume();
        return value;
    }

    /*
     * type CyberReq { id: String; cia: CIA; severity: Severity; condition:
     * CyberExpr; comment: Option<String>; description : Option<String>; phases :
     * Option<String>; extern : Option<String>; };
     *
     */
    public CyberReq cyber_req() {

        CyberReq cyberReq = new CyberReq();

        while (this.token.type == Type.CYB_REQ) {
            consume(Type.CYB_REQ);

            if (token.type == Type.STRING) {

                String identifier = id_value();
                cyberReq.setId(identifier);

            } else if (token.type == Type.CIA) {

                CIA cia = cia();
                cyberReq.setCia(cia);

            } else if (token.type == Type.SEVERITY) {

                Severity severity = severity();
                cyberReq.setSeverity(severity);

            } else if (token.type == Type.CYBER_EXP) {

                CyberExpr expr = cyberExpr();
                cyberReq.setCondition(expr);

            } else if (token.type == Type.OPTION) {

                String type_value = token.sd.getName();
                Type type = Type.get(type_value);
                // consume(Type.BOOL);
                consume(Type.OPTION);

                if (type == Type.COMMENT) {

                    String comment = str_value();
                    cyberReq.setComment(comment);

                } else if (type == Type.DESCRIPTION) {

                    String description = str_value();
                    cyberReq.setDescription(description);

                } else if (type == Type.PHASES) {

                    String phases = str_value();
                    cyberReq.setPhases(phases);

                } else if (type == Type.EXTERN) {

                    String extern = str_value();
                    cyberReq.setExtern(extern);
                }
            }
        }

        return cyberReq;
    }

    public SafetyReq safety_req() {

        SafetyReq safetyReq = new SafetyReq();

        while (this.token.type == Type.SAFETY_REQ) {
            consume(Type.SAFETY_REQ);

            if (token.type == Type.STRING) {

                String identifier = id_value();
                safetyReq.setId(identifier);

            } else if (token.type == Type.SAFETY_EXP) {

                SafetyReqExpr safetyExpr = safetyReqExpr();
                safetyReq.setCondition(safetyExpr);

            } else if (token.type == Type.OPTION) {

                String type_value = token.sd.getName();
                Type type = Type.get(type_value);
                consume(Type.OPTION);

                if (type == Type.COMMENT) {

                    String comment = str_value();
                    safetyReq.setComment(comment);

                } else if (type == Type.DESCRIPTION) {

                    String description = str_value();
                    safetyReq.setDescription(description);

                } else if (type == Type.PHASES) {

                    String phases = str_value();
                    safetyReq.setPhases(phases);

                } else if (type == Type.EXTERN) {

                    String extern = str_value();
                    safetyReq.setExtern(extern);

                } else if (type == Type.TARGET_PROBABILITY) {

                    String targetProbability = str_value();
                    safetyReq.setTargetProbability(targetProbability);
                }
            }
        }

        return safetyReq;
    }

    public Mission mission() {
        Mission mission = new Mission();

        while (this.token.type == Type.MISSION) {
            consume(Type.MISSION);

            if (token.type == Type.STRING) {

                String identifier = id_value();
                mission.setId(identifier);

            } else if (token.type == Type.ARRAY_LIST) {

                int array_length = 0;

                String type_name = this.token.sd.getName();
                Type type = Type.get(type_name);

                if (peek().type == Type.INT) {
                    array_length = arrayList_length();
                } else {
                    if (type == Type.CYBER_REQS) {
                        int cyberReq_index = arrayList_element();

                        String req = str_value();
                        mission.getCyberReqs().add(req);
                    }
                }

            } else if (token.type == Type.OPTION) {

                Type type = Type.get(token.sd.getName());
                consume(Type.OPTION);
                Type optType = option();

                if (optType == Type.SOME) {
                    // Only consume if this is Some, not None
                    if (type == Type.DESCRIPTION) {

                        String description = str_value();
                        mission.setDescription(description);

                    } else if (type == Type.COMMENT) {

                        String comment = str_value();
                        mission.setComment(comment);
                    }
                }
            }
        }

        return mission;
    }

    // type SafetyEvent {
    // id: String;
    // probability: String;
    // comment: Option<String>;
    // description : Option<String>;
    // };
    public Event safetyevent() {

        Event event = new Event();

        while (this.token.type == Type.SAFETY_EVENT) {

            consume(Type.SAFETY_EVENT);

            if (token.type == Type.STRING) {

                String str_type_value = token.sd.getName();
                Type str_type = Type.get(str_type_value);

                if (str_type == Type.EVENT_ID) {
                    String identifier = id_value();
                    event.setId(identifier);
                } else if (str_type == Type.PROBABILITY) {
                    String prob = id_value();
                    event.setProbability(prob);
                }

            } else if (token.type == Type.OPTION) {

                String type_value = token.sd.getName();
                Type type = Type.get(type_value);
                consume(Type.OPTION);

                if (type == Type.COMMENT) {

                    String comment = str_value();
                    event.setComment(comment);

                } else if (type == Type.DESCRIPTION) {

                    String description = str_value();
                    event.setDescription(description);
                }
            }
        }

        return event;
    }

    /*
     * type CIAPort { name: String; cia: CIA; }
     */
    public CIAPort cia_port() {
        CIAPort ciaPort = new CIAPort();

        while (this.token.type == Type.CIA_PORT) {
            consume(Type.CIA_PORT);

            if (token.type == Type.STRING) {

                String identifier = id_value();
                ciaPort.setName(identifier);

            } else if (token.type == Type.CIA) {
                CIA cia = cia();
                ciaPort.setCia(cia);
            }
        }

        return ciaPort;
    }

    // SAFETY
    public IAPort ia_port() {
        IAPort iaPort = new IAPort();

        while (this.token.type == Type.IA_PORT) {
            consume(Type.IA_PORT);

            if (token.type == Type.STRING) {

                String identifier = id_value();
                iaPort.setName(identifier);

            } else if (token.type == Type.IA) {
                IA ia = ia();
                iaPort.setIa(ia);
            }
        }

        return iaPort;
    }

    public EventHappens eventHappens() {
        EventHappens event = new EventHappens();

        // while (this.token.type == Type.HAPPENS) {
        // consume(Type.HAPPENS);
        consume();
        if (token.type == Type.STRING) {
            String identifier = id_value();
            event.setEventName(identifier);
            // } else {
            // System.out.println("Event happens: toeken " + token);
            // }
        }
        // consume();

        return event;
    }

    /*
     * type CIA enum {Confidentiality, Integrity, Availability};
     */
    public CIA cia() {

        consume(Type.CIA);

        String type_name = token.sd.getName();

        CIA cia_type = CIA.fromValue(type_name);

        consume();

        return cia_type;
    }

    public IA ia() {

        consume(Type.IA);

        String type_name = token.sd.getName();

        IA ia_type = IA.fromValue(type_name);

        consume();

        return ia_type;
    }

    /*
     * type Severity enum {None, Minor, Major, Hazardous, Catastrophic};
     */
    public Severity severity() {

        consume(Type.SEVERITY);

        String type_name = token.sd.getName();

        Severity severity_type = Severity.fromValue(type_name);

        consume();

        return severity_type;
    }

    /*
     * type CyberExprKind enum {Port, And, Or, Not};
     */

    public CyberExprKind cyberExprKind() {

        consume(Type.CYBER_EXP_KIND);

        String type_name = token.sd.getName();

        CyberExprKind cyberExprKind = CyberExprKind.fromValue(type_name);

        consume();

        return cyberExprKind;
    }

    public SafetyReqExprKind safetyReqExprKind() {

        consume(Type.SAFETY_EXP_KIND);

        String type_name = token.sd.getName();

        SafetyReqExprKind safetyReqExprKind = SafetyReqExprKind.fromValue(type_name);

        consume();

        return safetyReqExprKind;
    }

    public SafetyRelExprKind safetyRelExprKind() {

        consume(Type.SAFETY_EXP_KIND);

        String type_name = token.sd.getName();

        SafetyRelExprKind safetyRelExprKind = SafetyRelExprKind.fromValue(type_name);

        consume();

        return safetyRelExprKind;
    }

    // type Connection {
    // name: Identifier;
    // attributes: ArrayList<GenericAttribute>;
    // source: ConnectionEnd;
    // destination: ConnectionEnd;
    // };

    public Connection connection() {

        Connection connection = new Connection();
        List<GenericAttribute> connectionAttributes = connection.getAttribute();

        int list_size = 0;
        Type type;

        while (this.token.type == Type.CONNECTION) {

            consume(Type.CONNECTION);

            if (token.type == Type.IDENTIFIER) {

                String identifier = Identifier();
                connection.setName(identifier);

            } else if (token.type == Type.ARRAY_LIST) {

                String type_name = this.token.sd.getName();
                type = Type.get(type_name);

                if (peek().type == Type.INT) {
                    list_size = arrayList_length();

                } else {
                    // while (list_size > 0) {
                    // list_size--;
                    // if (type == Type.GENERIC_ATTRIBUTE) {
                    int attr_index = arrayList_element();

                    GenericAttribute gattribute = genericAttribute();
                    connectionAttributes.add(attr_index, gattribute);
                    // }
                    // }
                }

            } else if (token.type == Type.CONNECTION_END) {

                String type_value = token.sd.getName();
                type = Type.get(type_value);

                if (type == Type.SOURCE) {
                    consume(Type.SOURCE);
                    ConnectionEnd source = connectionEnd();
                    connection.setSource(source);
                } else if (type == Type.DESTINATION) {
                    consume(Type.DESTINATION);
                    ConnectionEnd destination = connectionEnd();
                    connection.setDestination(destination);
                }
            }
        }

        return connection;
    }

    // ConnectionEnd
    /*
     * type ConnectionEnd { kind: ConnectionEndKind; component_port: Port;
     * subcomponent_port: CompInstPort; };
     */
    public ConnectionEnd connectionEnd() {

        ConnectionEnd connectionEnd = new ConnectionEnd();

        while (token.type == Type.CONNECTION_END) {
            consume(Type.CONNECTION_END);

            if (token.type == Type.CONNECTION_END_KIND) {
                // ComponentCE OR SubcomponentCE
                connectionEndKind();
            }
            if (token.type == Type.PORT) {
                consume(Type.PORT);

                ComponentType type = getComponentType();
                int port_index = arrayList_element();
                Port port = type.getPort().get(port_index);
                connectionEnd.setComponentPort(port);
                // Port component_port = port();
                // connectionEnd.setComponentPort(component_port);
            } else if (token.type == Type.COMPONENT_INSTANCE_PORT) {
                CompInstancePort subcomponent_port = compInstancePort();
                connectionEnd.setSubcomponentPort(subcomponent_port);
            }
        }

        return connectionEnd;
    }

    // public ComponentType getComponentType() {
    //
    // ComponentType componentType;
    // consume(Type.COMPONENT_TYPE);
    // consume(Type.MODEL);
    //
    // int componet_type_index = arrayList_element();
    // componentType = model.getComponentType().get(componet_type_index);
    //
    // return componentType;
    //
    // }

    // type ConnectionEndKind enum { ComponentCE, SubcomponentCE };
    public Type connectionEndKind() {

        consume(Type.CONNECTION_END_KIND);

        String type_name = token.sd.getName();
        Type type = Type.get(type_name);

        if (type == Type.COMPONENT_CE) {
            consume(Type.COMPONENT_CE);
        } else if (type == Type.SUBCOMPONENT_CE) {
            consume(Type.SUBCOMPONENT_CE);
        }

        return type;
    }

    /*
     * type CompInstPort { subcomponent: ComponentInstance; port: Port; };
     */
    public CompInstancePort compInstancePort() {

        CompInstancePort compInstancePort = new CompInstancePort();

        while (this.token.type == Type.COMPONENT_INSTANCE_PORT) {
            consume(Type.COMPONENT_INSTANCE_PORT);

            if (token.type == Type.COMPONENT_INSTANCE) {
                consume(Type.COMPONENT_INSTANCE);

                ComponentInstance subcomponent = getComponentInstance();
                compInstancePort.setSubcomponent(subcomponent);
            } else if (token.type == Type.PORT) {
                consume(Type.PORT);

                ComponentType componentType = getComponentType();
                int port_index = arrayList_element();
                Port port = componentType.getPort().get(port_index);

                compInstancePort.setPort(port);
            }
        }

        return compInstancePort;
    }

    /*
     * type ComponentInstance { name: Identifier;
     *
     * kind: ComponentInstanceKind;
     *
     * specification: ComponentType; implementation: ComponentImpl;
     *
     * has_sensitive_info: Option<Bool>; inside_trusted_boundary: Option<Bool>;
     * broadcast_from_outside_tb: Option<Bool>; unenc_wifi_from_outside_tb:
     * Option<Bool>; heterogeneity: Option<Bool>; encryption: Option<Bool>;
     * anti_jamming: Option<Bool>; anti_flooding: Option<Bool>; anti_fuzzing:
     * Option<Bool>;
     *
     * heterogeneity_dal: Option<Int>; encryption_dal: Option<Int>;
     * anti_jamming_dal: Option<Int>; anti_flooding_dal: Option<Int>;
     * anti_fuzzing_dal: Option<Int>; };
     */
    // type ComponentInstance {
    // name: Identifier;
    // kind: ComponentInstanceKind;
    // specification: ComponentType;
    // implementation: ComponentImpl;
    // attributes: ArrayList<GenericAttribute>;
    // };

    public ComponentInstance componentInstance(String block_compImpl_Id) {

        ComponentInstance componentInstance = new ComponentInstance();
        List<GenericAttribute> componentAttributes = componentInstance.getAttribute();

        int list_size = 0;
        Type type;

        while (this.token.type == Type.COMPONENT_INSTANCE) {
            consume(Type.COMPONENT_INSTANCE);

            if (token.type == Type.IDENTIFIER) {

                String identifier = Identifier();
                // Unique Block Implementation ID
                block_compImpl_Id = block_compImpl_Id.replace(".", "_dot_");

                componentInstance.setName(block_compImpl_Id + "_" + identifier);
                componentInstance.setId(block_compImpl_Id + "_" + identifier);

            } else if (token.type == Type.COMPONENT_INSTANCE_KIND) {
                // Specification or Implementation
                componentInstanceKind();
            } else if (token.type == Type.COMPONENT_TYPE) {
                consume(Type.COMPONENT_TYPE);
                ComponentType specification = getComponentType();
                componentInstance.setSpecification(specification);
            } else if (token.type == Type.COMPONENT_IMPL_TYPE) {
                consume(Type.COMPONENT_IMPL_TYPE);
                ComponentImpl componentImpl = getComponentImpl();
                componentInstance.setImplementation(componentImpl);
            } else if (token.type == Type.ARRAY_LIST) {

                String type_name = this.token.sd.getName();
                type = Type.get(type_name);

                if (peek().type == Type.INT) {
                    list_size = arrayList_length();

                } else {

                    // while (list_size > 0) {
                    // list_size--;

                    // if (type == Type.GENERIC_ATTRIBUTE) {
                    int attr_index = arrayList_element();

                    GenericAttribute gattribute = genericAttribute();
                    componentAttributes.add(attr_index, gattribute);
                    // }
                }
            }
        }

        return componentInstance;
    }

    // type ComponentInstanceKind enum { Specification, Implementation };
    public Type componentInstanceKind() {

        consume(Type.COMPONENT_INSTANCE_KIND);

        String type_name = token.sd.getName();
        Type type = Type.get(type_name);

        if (type == Type.SPECIFICATION) {
            consume(Type.SPECIFICATION);
        } else if (type == Type.IMPLEMENTATION) {
            consume(Type.IMPLEMENTATION);
        }

        return type;
    }

    public String category() {

        String category_value = token.getStringValue();
        consume(Type.String);

        return category_value;
    }

    public int integerValue() {

        int int_value;
        Token token = this.token;
        NumberLiteral value = (NumberLiteral) token.value;
        int_value = value.getValue();
        consume(Type.INT);

        return int_value;
    }
    // type ConnectionEndKind enum { ComponentCE, SubcomponentCE };
    // public ConnectionEnd

    /*
     * type ComponentImpl { name: Identifier; ctype: ComponentType; kind:
     * ComponentImplKind; block_impl: BlockImpl; dataflow_impl: NodeBody; };
     */
    public ComponentImpl componentImpl() {

        String identifier = null;
        componentImpl = new ComponentImpl();

        // @TODO: Check ID.
        /// String ID = this.token.sd.getName();

        while (token.type == Type.COMPONENT_IMPL_TYPE) {
            consume(Type.COMPONENT_IMPL_TYPE);

            if (token.type == Type.IDENTIFIER) {
                identifier = Identifier();
                componentImpl.setName(identifier);
                componentImpl.setId(identifier);
            } else if (token.type == Type.COMPONENT_TYPE) {
                consume(Type.COMPONENT_TYPE);
                ComponentType componentType = getComponentType();
                componentImpl.setType(componentType);

            } else if (token.type == Type.COMPONENT_IMPL_KIND) {
                // Not Restricting Type Yet [DataFlow or BlockType]
                componentImplKind();
            } else if (token.type == Type.BLOCK_IMPL) {
                blockImpl = blockImpl(identifier);
                componentImpl.setBlockImpl(blockImpl);
            } else if (token.type == Type.NODE_BODY) {
                // consume(Type.COMPONENT_IMPL_TYPE);

                NodeBody nodeBody = nodeBody();
                componentImpl.setDataflowImpl(nodeBody);
            }
        }

        return componentImpl;
    }

    // type ComponentImplKind enum { Block_Impl, Dataflow_Impl };
    public Type componentImplKind() {

        consume(Type.COMPONENT_IMPL_KIND);
        String type_name = token.sd.getName();
        Type type = Type.get(type_name);

        if (type == Type.Block_IMPL) {
            consume(Type.Block_IMPL);
        }
        if (type == Type.DATA_FLOW_IML) {
            consume(Type.DATA_FLOW_IML);
        }

        return type;
    }

    public ComponentType getComponentType() {

        ComponentType componentType;
        // consume(Type.COMPONENT_TYPE);
        consume(Type.MODEL);

        int componet_type_index = arrayList_element();
        componentType = model.getComponentType().get(componet_type_index);

        return componentType;
    }

    public ComponentImpl getComponentImpl() {

        ComponentImpl componentImpl;

        consume(Type.MODEL);

        int componet_type_index = arrayList_element();
        componentImpl = model.getComponentImpl().get(componet_type_index);

        return componentImpl;
    }

    public ComponentInstance getComponentInstance() {

        ComponentInstance componentInstance;
        consume(Type.BLOCK_IMPL);

        int component_instance_index = arrayList_element();

        componentInstance = blockImpl.getSubcomponent().get(component_instance_index);

        return componentInstance;
    }

    /*
     * type BlockImpl { subcomponents: ArrayList<ComponentInstance>; connections:
     * ArrayList<Connection>; };
     */
    public BlockImpl blockImpl(String block_compImpl_Id) {

        blockImpl = new BlockImpl();
        List<ComponentInstance> subcomponents = blockImpl.getSubcomponent();
        List<Connection> connections = blockImpl.getConnection();

        int array_length = -1;

        while (this.token.type == Type.BLOCK_IMPL) {
            consume(Type.BLOCK_IMPL);

            if (token.type == Type.ARRAY_LIST) {

                String type_name = this.token.sd.getName();
                Type type = Type.get(type_name);

                if (peek().type == Type.INT) {

                    array_length = arrayList_length();

                } else {

                    int element_index = arrayList_element();

                    if (token.type == Type.COMPONENT_INSTANCE) {

                        ComponentInstance componentInstance = componentInstance(block_compImpl_Id);
                        subcomponents.add(element_index, componentInstance);
                    } else if (token.type == Type.CONNECTION) {

                        Connection connection = connection();
                        connections.add(element_index, connection);
                    }
                }
            }
        }

        return blockImpl;
    }

    /*
     * type NodeBody { constant_declarations: ArrayList<ConstantDeclaration>;
     * variable_declarations: ArrayList<VariableDeclaration>; assertions:
     * ArrayList<Expression>; equations: ArrayList<NodeEquation>; properties:
     * ArrayList<NodeProperty>; };
     */
    public NodeBody nodeBody() {

        NodeBody nodeBody = new NodeBody();
        int array_length = 0;

        while (token.type == Type.NODE_BODY) {

            consume(Type.NODE_BODY);

            if (token.type == Type.ARRAY_LIST) {

                String type_value = this.token.sd.getName();
                Type type = Type.get(type_value);

                if (peek().type == Type.INT) {
                    array_length = arrayList_length();
                } else {

                    if (type == Type.CONSTANT_DECLARATIONS) {
                        int constantDeclaration_index = arrayList_element();

                        ConstantDeclaration constantDeclaration = constantDeclaration();
                        nodeBody.getConstantDeclaration()
                                .add(constantDeclaration_index, constantDeclaration);
                    } else if (type == Type.VARIABLE_DECLARATIONS) {
                        int variableDeclaration_index = arrayList_element();

                        VariableDeclaration variableDeclaration = variableDeclaration();
                        nodeBody.getVariableDeclaration()
                                .add(variableDeclaration_index, variableDeclaration);

                    } else if (type == Type.ASSERTIONS) {
                        int expression_index = arrayList_element();
                        Expression expression = expression();
                        nodeBody.getAssertion().add(expression_index, expression);
                    } else if (type == Type.EQUATIONS) {
                        int nodeEquation_index = arrayList_element();
                        NodeEquation nodeEquation = nodeEquation();
                        // LOGGER.info("NODE EQUATION: [" + nodeEquation_index +"] = "+
                        // nodeEquation.getName());
                        nodeBody.getEquation().add(nodeEquation_index, nodeEquation);

                    } else if (type == Type.PROPERTIES) {
                        int property_index = arrayList_element();
                        NodeProperty nodeProperty = nodeProperty();
                        nodeBody.getProperty().add(property_index, nodeProperty);
                    }
                }
            }
        }

        return nodeBody;
    }
    /*
     * type NodeProperty { name: Option<Identifier>; expression: Expression; };
     */

    public NodeProperty nodeProperty() {
        NodeProperty nodeProperty = new NodeProperty();

        while (this.token.type == Type.NODE_PROPERTY) {
            consume(Type.NODE_PROPERTY);

            if (peek().type == Type.OPTION) {
                String identifier = optionIdentifier();
                nodeProperty.setName(identifier);
            } else if (peek().type == Type.EXPRESSION) {
                Expression expression = expression();
                nodeProperty.setExpression(expression);
            }
        }
        return nodeProperty;
    }

    public ArrayList<NodeProperty> arrayList_NodeProperty() {

        ArrayList<NodeProperty> nodePropertyList = new ArrayList<NodeProperty>();

        int list_length = 0;

        Type type = this.token.type;

        while (type == Type.ARRAY_LIST) {

            consume(Type.ARRAY_LIST);

            // length
            if (this.token.type == Type.INT) {

                Token token = this.token;
                NumberLiteral enumLength_literal = (NumberLiteral) token.value;
                list_length = enumLength_literal.getValue();
                consume(Type.INT);

                while (list_length > 0) {

                    // NULL:element
                    consume(Type.NULL);
                    list_length--;
                    // element Index
                    token = this.token;
                    NumberLiteral element_index = (NumberLiteral) token.value;
                    consume(Type.INT);

                    // //element Value : PORT
                    // token = this.token;
                    NodeProperty nodeProperty = nodeProperty();
                    // consume(Type.String);
                    nodePropertyList.add(element_index.getValue(), nodeProperty);
                }
            }
        }

        return nodePropertyList;
    }

    /*
     * type NodeEquation { lhs: ArrayList<Identifier>; -- Indirect Reference rhs:
     * Expression; };
     */
    public NodeEquation nodeEquation() {

        NodeEquation nodeEquation = new NodeEquation();
        NodeEquationLHS nodeEquationLHS = new NodeEquationLHS();
        int length = 0;

        while (token.type == Type.NODE_EQUATION) {
            consume(Type.NODE_EQUATION);

            // String type_value = token.sd.getName();
            // Type type = Type.get(type_value);

            if (token.type == Type.ARRAY_LIST) {
                consume(Type.ARRAY_LIST);

                // if(type == Type.Lhs) {

                // String type_name = this.token.sd.getName();
                // Type type = Type.get(type_name);

                if (token.type == Type.INT) {
                    // length
                    consume(Type.INT);
                    length = token.getNumberValue();
                    // value
                    consume(Type.Int);

                } else {
                    // enum
                    consume();
                    int id_index = token.getNumberValue();
                    // index
                    consume(Type.Int);

                    String identifier = token.getStringValue();
                    consume(Type.String);
                    nodeEquationLHS.getIdentifier().add(id_index, identifier);

                    if (length == id_index + 1) {
                        nodeEquation.setLhs(nodeEquationLHS);
                    }
                }

                // }
            } else if (token.type == Type.EXPRESSION) {
                // if(type == Type.Rhs) {
                consume(Type.EXPRESSION);
                Expression expression = expression();
                nodeEquation.setRhs(expression);
                // }
            }
        }

        return nodeEquation;
    }

    // public NodeEquationLHS nodeEquationLHS() {
    //
    // NodeEquationLHS nodeEquationLHS = new NodeEquationLHS();
    // int length = 0;
    //
    // while (token.type == Type.NODE_EQUATION) {
    // consume(Type.NODE_EQUATION);
    //
    //
    // }
    //
    // return nodeEquationLHS;
    //
    // }

    /*
     * type Model { name: Identifier; type_declarations: ArrayList<TypeDeclaration>;
     * component_types: ArrayList<ComponentType>; component_impl:
     * ArrayList<ComponentImpl>; dataflow_code: Option<LustreProgram>;
     * cyber_requirements: ArrayList<CyberReq>; };
     */

    public Model model() {

        int array_length = 0;

        while (token.type == Type.MODEL) {

            consume(Type.MODEL);

            if (token.type == Type.IDENTIFIER) {
                String identifier = Identifier();
                model.setName(identifier);
            } else if (token.type == Type.ARRAY_LIST) {

                String type_name = this.token.sd.getName();
                Type type = Type.get(type_name);

                if (peek().type == Type.INT) {

                    array_length = arrayList_length();

                } else {

                    if (type == Type.TYPE_DECLARATIONS) {

                        int typeDeclaration_index = arrayList_element();

                        TypeDeclaration typeDeclaration = typeDeclaration();
                        typeDeclarations.add(typeDeclaration_index, typeDeclaration);

                    } else if (type == Type.COMPONENT_TYPES) {

                        int component_index = arrayList_element();

                        ComponentType componentType = componentType();
                        componentTypes.add(component_index, componentType);

                    } else if (type == Type.COMPONENT_IMPL) {

                        int componet_impl_index = arrayList_element();

                        componentImpl = componentImpl();
                        componentImpls.add(componet_impl_index, componentImpl);

                    } else if (type == Type.CYBER_REQUIREMENTS) {

                        int cyberReq_index = arrayList_element();

                        CyberReq cyberReq = cyber_req();
                        cyberRequirements.add(cyberReq_index, cyberReq);

                    } else if (type == Type.SAFETY_REQUIREMENTS) {
                        log("safety_req: " + token);
                        int safetyReq_index = arrayList_element();

                        SafetyReq safetyReq = safety_req();
                        safetyRequirements.add(safetyReq_index, safetyReq);

                    } else if (type == Type.MISSIONS) {

                        int mission_index = arrayList_element();

                        Mission mission = mission();
                        missions.add(mission_index, mission);
                    }
                }

            } else if (token.type == Type.OPTION) {

                consume(Type.OPTION);

                Type type = option();

                if (type == Type.SOME) {
                    if (token.type == Type.LUSTRE_PRG) {
                        LustreProgram lustreProgram = lustreProgram();
                        model.setDataflowCode(lustreProgram);
                    }
                }
                // skip None
            }
        }

        return model;
    }

    /*
     * type LustreProgram { type_declarations: ArrayList<TypeDeclaration>;
     * constant_declarations: ArrayList<ConstantDeclaration>; contract_declarations:
     * ArrayList<Contract>; node_declarations: ArrayList<Node>; };
     */
    public LustreProgram lustreProgram() {

        LustreProgram lustreProgram = new LustreProgram();
        int array_length = 0;

        while (token.type == Type.LUSTRE_PRG) {

            consume(Type.LUSTRE_PRG);

            if (token.type == Type.ARRAY_LIST) {

                String type_name = this.token.sd.getName();
                Type type = Type.get(type_name);

                if (peek().type == Type.INT) {

                    array_length = arrayList_length();

                } else {

                    if (type == Type.TYPE_DECLARATIONS) {

                        int typeDeclaration_index = arrayList_element();

                        TypeDeclaration typeDeclaration = typeDeclaration();
                        lustreProgram
                                .getTypeDeclaration()
                                .add(typeDeclaration_index, typeDeclaration);
                    } else if (type == Type.CONSTANT_DECLARATIONS) {
                        int constantDeclaration_index = arrayList_element();

                        ConstantDeclaration constantDeclaration = constantDeclaration();
                        // LOGGER.info(
                        // "CONSTANT DECLARATION: ["
                        // + constantDeclaration_index
                        // + "] = "
                        // + constantDeclaration.getName());
                        // SymbolDefinition symbolDefinition =
                        // symbolDefinition();
                        lustreProgram
                                .getConstantDeclaration()
                                .add(constantDeclaration_index, constantDeclaration);
                    } else if (type == Type.CONTRACT_DECLARATIONS) {

                        int contract_index = arrayList_element();

                        Contract contract = contract();
                        lustreProgram.getContractDeclaration().add(contract_index, contract);

                    } else if (type == Type.NODE_DECLARATIONS) {

                        int node_index = arrayList_element();

                        Node node = node();
                        lustreProgram.getNodeDeclaration().add(node_index, node);
                    }
                }
            }
        }

        return lustreProgram;
    }

    /*
     * type Node { name: Identifier; is_function: Bool; is_main: Bool;
     * input_parameters: ArrayList<InputParameter>; output_parameters:
     * ArrayList<OutputParameter>; contract: Option<ContractSpec>; body:
     * Option<NodeBody>; };
     */
    public Node node() {

        Node node = new Node();

        int array_length = 0;

        while (token.type == Type.NODE_TYPE) {

            consume(Type.NODE_TYPE);

            if (token.type == Type.IDENTIFIER) {
                String identifier = Identifier();
                node.setName(identifier);
            } else if (token.type == Type.BOOL) {

                String type_name = this.token.sd.getName();
                Type type = Type.get(type_name);

                consume(Type.BOOL);
                boolean value = token.getTruthValue();
                consume(Type.Boolean);

                if (type == Type.IS_FUNCTION) {
                    node.setIsFunction(value);
                } else if (type == Type.IS_MAIN) {
                    // TODO node.setIsMain(value);
                }
            } else if (token.type == Type.ARRAY_LIST) {

                String type_name = this.token.sd.getName();
                Type type = Type.get(type_name);

                if (peek().type == Type.INT) {

                    array_length = arrayList_length();

                } else {

                    if (type == Type.INPUT_NODE_PARAMETER) {

                        int parameter_index = arrayList_element();
                        NodeParameter nodeParametre = inputParameter();
                        node.getInputParameter().add(parameter_index, nodeParametre);
                    } else if (type == Type.OUTPUT_NODE_PARAMETER) {

                        int parameter_index = arrayList_element();
                        NodeParameter nodeParametre = outputParameter();
                        node.getOutputParameter().add(parameter_index, nodeParametre);
                    }
                }

            } else if (token.type == Type.OPTION) {

                consume(Type.OPTION);

                Type type = option();

                if (type == Type.SOME) {
                    if (token.type == Type.CONTRACT_SPEC) {
                        ContractSpec contractSpec = contractSpec();
                        node.setContract(contractSpec);
                    }
                    if (token.type == Type.NODE_BODY) {

                        NodeBody nodeBody = nodeBody();
                        node.setBody(nodeBody);
                    }
                }
                // skip None
            }
        }

        return node;
    }

    /*
     * type NodeParameter { name: Identifier; dtype: DataType; }
     */

    /*
     * type InputParameter { name: Identifier; dtype: DataType; is_constant: Bool;
     * };
     */
    public NodeParameter inputParameter() {

        NodeParameter nodeParameter = new NodeParameter();
        DataType dataType = null;

        while (token.type == Type.INPUT_PARAMETER) {
            consume(Type.INPUT_PARAMETER);

            if (token.type == Type.IDENTIFIER) {
                String identifier = Identifier();

                nodeParameter.setName(identifier);

            } else if (token.type == Type.DATA_TYPE) {

                consume(Type.DATA_TYPE);

                if (token.type == Type.DATA_TYPE) {
                    // consume(Type.DATA_TYPE);
                    dataType = dataType();

                } else if (token.type == Type.MODEL) {
                    TypeDeclaration typeDeclaration = getTypeDeclaration();
                    dataType = new DataType();
                    dataType.setUserDefinedType(typeDeclaration.getName());
                }

                // DataType dataType_value = dataType();
                nodeParameter.setDataType(dataType);
            } else if (token.type == Type.BOOL) {
                consume(Type.BOOL);

                boolean value = token.getTruthValue();
                consume(Type.Boolean);
                // DATA this field into VDM Lustre in NodeParameter.
                nodeParameter.setIsConstant(value);
            }
        }

        return nodeParameter;
    }

    /*
     * type OutputParameter { name: Identifier; dtype: DataType; };
     */

    public NodeParameter outputParameter() {

        NodeParameter nodeParameter = new NodeParameter();
        DataType dataType = null;

        while (token.type == Type.OUTPUT_PARAMETER) {
            consume(Type.OUTPUT_PARAMETER);

            if (token.type == Type.IDENTIFIER) {
                String identifier = Identifier();

                nodeParameter.setName(identifier);

            } else if (token.type == Type.DATA_TYPE) {

                consume(Type.DATA_TYPE);

                if (token.type == Type.DATA_TYPE) {
                    // consume(Type.DATA_TYPE);
                    dataType = dataType();

                } else if (token.type == Type.MODEL) {
                    TypeDeclaration typeDeclaration = getTypeDeclaration();
                    dataType = new DataType();
                    dataType.setUserDefinedType(typeDeclaration.getName());
                }

                // DataType dataType_value = dataType();
                nodeParameter.setDataType(dataType);
            }
        }

        return nodeParameter;
    }
}
