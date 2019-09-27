/* See LICENSE in project directory */
package edu.uiowa.clc.verdict.vdm.translator;

import java.util.HashMap;
import java.util.Map;

public enum Type {

    // Type
    EOF("EOF"),
    NAME("name"),
    DEFINITION("definition"),
    NODE_TYPE("Node"),
    NODE("node"),
    NODE_CALL("NodeCall"),
    ARGUMENTS("arguments"),
    NODE_EQUATION("NodeEquation"),
    NODE_PROPERTY("NodeProperty"),
    NODE_BODY("NodeBody"),
    NODE_PARAMETER("NodeParameter"),
    INPUT_NODE_PARAMETER("input_parameters"),
    INPUT_PARAMETER("InputParameter"),
    OUTPUT_NODE_PARAMETER("output_parameters"),
    OUTPUT_PARAMETER("OutputParameter"),
    IS_FUNCTION("is_function"),
    IS_MAIN("is_main"),

    IN("In"),
    OUT("Out"),
    //	SYMBOL_DESC("SymbolicDeclaration"),

    LUSTRE_PRG("LustreProgram"),
    BLOCK_IMPL("BlockImpl"),
    Block_IMPL("Block_Impl"),

    DATA_KIND("kind"),

    ENUM("Enum"),
    PLAIN("Plain"),
    SUBRANGE("Subrange"),
    USERDEFINED("UserDefined"),
    RECORD("Record"),

    ENUM_TYPE("EnumType"),
    RECORD_FILED("RecordField"),
    SUB_RANGE_TYPE("SubrangeType"),
    PLAIN_TYPE("PlainType"),
    USER_DEFINED_TYPE("mk_datatype_from_decl"),

    NULL("null"),

    INT("Int"),
    REAL("Real"),
    BOOL("Bool"),

    Int("int"),
    Real("real"),

    // ID
    STRING("String"),

    Float("float"),
    String("string"),
    Boolean("boolean"),
    Char("char"),

    INTEGER_TYPE("IntegerType"),
    REAL_TYPE("RealType"),

    SOME("mk_some"),
    MK_NONE("mk_none"),
    NONE("None"),

    EXPRESSION("Expression"),
    EXPRESSION_KIND("ExpressionKind"),

    IDENTIFIER("Identifier"),
    CONTRACT("Contract"),
    CONTRACT_SPEC("ContractSpec"),
    SYMBOL_DEFINITION("SymbolDefinition"),

    REFERENCE("reference"),
    ASSUMPTIONS("assumptions"),
    THREATS("threats"),

    MISSIONS("missions"),
    MISSION("Mission"),
    CYBER_REQS("reqs"),

    CYBER_REQUIREMENTS("cyber_requirements"),
    CYB_REQ("CyberReq"),

    CYB_REL("CyberRel"),
    INPUTS("inputs"),
    COMMENT("comment"),
    DESCRIPTION("description"),
    PHASES("phases"),
    EXTERN("extern"),

    CIA_PORT("CIAPort"),
    CIA("CIA"),
    CONFIDENTIALITY("Confidentiality"),
    INTEGRITY("Integrity"),
    AVAILABILITY("Availability"),

    SEVERITY("Severity"),

    CYBER_EXP("CyberExpr"),
    CYBER_EXP_KIND("CyberExprKind"),

    PORT("Port"),

    PORT_MODE("PortMode"),
    PORTS("ports"),

    CYBER_RELATIONS("cyber_relations"),

    ARRAY_LIST("ArrayList"),

    DATA_TYPE("DataType"),
    DATA_TYPE_KIND("DataTypeKind"),
    DATA_FLOW_IML("Dataflow_Impl"),

    OPTION("Option"),
    MODEL("Model"),
    DATAFLOW_CODE("dataflow_code"),

    TYPE_DECLARATION("TypeDeclaration"),
    COMPONENT_TYPE("ComponentType"),
    COMPONENT_INSTANCE("ComponentInstance"),
    COMPONENT_INSTANCE_PORT("CompInstPort"),

    COMPONENT_IMPL_TYPE("ComponentImpl"),

    COMPONENT_INSTANCE_KIND("ComponentInstanceKind"),
    SPECIFICATION("Specification"),
    IMPLEMENTATION("Implementation"),
    CONSTANT_DECLARATIONS("constant_declarations"),
    VARIABLE_DECLARATIONS("variable_declarations"),
    ASSERTIONS("assertions"),
    EQUATIONS("equations"),
    PROPERTIES("properties"),

    CONSTANT_DECLARATION("ConstantDeclaration"),
    VARIABLE_DECLARATION("VariableDeclaration"),

    CONTRACT_DECLARATIONS("contract_declarations"),
    NODE_DECLARATIONS("node_declarations"),

    ASSUMES("assumes"),
    GURANTEES("guarantees"),
    MODES("modes"),
    IMPORTS("imports"),

    CONTRACT_ITEM("ContractItem"),
    CONTRACT_IMPORT("ContractImport"),
    INPUT_ARGUMENTS("input_arguments"),
    OUTPUT_ARGUMENTS("output_arguments"),

    SOURCE("source"),
    DESTINATION("destination"),

    HAS_SENSITIVE_INFO("has_sensitive_info"),
    INSIDE_TRUSTED_BOUNDRY("inside_trusted_boundary"),
    INTERACT_OUTSIDE_TB("broadcast_from_outside_tb"),
    RECEIVE_OUTSIDE_TB("wifi_from_outside_tb"),
    ENCRYPTION("encryption"),

    HETEROGENEITY("heterogeneity"),
    ANTI_JAMMING("anti_jamming"),
    ANTI_FLOODING("anti_flooding"),
    ANTI_FUZZING("anti_fuzzing"),

    HETEROGENEITY_DAL("heterogeneity_dal"),
    ENCRYPTION_DAL("encryption_dal"),

    ANTI_JAMMING_DAL("anti_jamming_dal"),
    ANTI_FLOODING_DAL("anti_flooding_dal"),
    ANTI_FUZZING_DAL("anti_fuzzing_dal"),

    BINARY_OP("BinaryOperation"),

    CONNECTION("Connection"),

    AUTHENTICATED("authenticated"),
    DATA_ENCRYPTED("data_encrypted"),

    KIND_COMPONENT("KindOfComponent"),
    SOFTWARE("Software"),
    HARDWARE("Hardware"),
    HUMAN("Human"),
    HYBIRD("Hybrid"),

    MANUFACTURER("manufacturer"),
    MANUFACTURER_TYPE("ManufacturerType"),

    THIRD_PARTY("ThirdParty"),
    IN_HOUSE("InHouse"),

    CATEGORY("category"),
    COMP_TYPE("component_type"),

    SITUATED("situated"),
    SITUATED_TYPE("SituatedType"),
    ON_BOARD("OnBoard"),

    ADV_TESTED("adversarially_tested"),

    REMOTE("Remote"),
    LOCAL("Local"),
    CONNECTION_TYPE("ConnectionType"),
    CONN_TYPE("conn_type"),
    CONNECTION_END("ConnectionEnd"),

    CONNECTION_END_KIND("ConnectionEndKind"),

    COMPONENT_CE("ComponentCE"),
    SUBCOMPONENT_CE("SubcomponentCE"),

    FLOW("Flow"),
    FLOW_TYPE("FlowType"),

    TYPE_DECLARATIONS("type_declarations"),
    COMPONENT_TYPES("component_types"),
    COMPONENT_IMPL("component_impl"),
    COMPONENT_IMPL_KIND("ComponentImplKind"),
    AGREE_CODE("agree_code"),

    // Lustre Constructs
    Id("Id"),
    INT_Lit("IntLiteral"),
    REAL_Lit("RealLiteral"),
    BOOL_Lit("BoolLiteral"),

    NOT("Not"),
    PRE("Pre"),
    EQ("Equal"),
    AND("And"),
    OR("Or"),

    LEQ("LessThanOrEqualTo"),
    LE("LessThan"),
    GEQ("GreaterThanOrEqualTo"),
    GE("GreaterThan"),

    ARROW("Arrow"),
    IMPLIES("Implies"),
    PLUS("Plus"),
    MINUS("Minus"),
    TIMES("Times"),
    DIV("Div"),
    MOD("Mod"),
    XOR("Xor"),

    NEQ("NotEqual"),
    LHS("lhs_operand"),
    RHS("rhs_operand"),
    Lhs("lhs"),
    Rhs("rhs"),
    CND_EXPR("ConditionalExpr"),
    ITE("IfThenElse"),
    CALL("Call"),
    CONDITION("condition"),
    THEN("thenBranch"),
    ELSE("elseBranch"),

    RECORD_PROJECTION("RecordProjection"),
    RECORD_REF("record_reference"),
    FIELD_ID("field_id"),
    FIELD_VALUE("field_value"),

    RECORD_LITERAL("RecordLiteral"),
    RECORD_TYPE("record_type"),
    FIELD_DEFINITION("FieldDefinition"),
    FIELD_DEFINITIONS("field_definitions"),
    RECORDTYPE("RecordType");

    private String type_name;
    private static final Map<String, Type> type_lookup = new HashMap<>();

    Type(String dType) {
        this.type_name = dType;
    }

    public String getName() {
        return this.type_name;
    }

    static {
        for (Type type : Type.values()) {
            if (type_lookup.containsKey(type.getName())) {
                System.err.println("Warning: duplicate type name " + type.getName());
            }

            type_lookup.put(type.getName(), type);
        }
    }

    public static Type get(String type_name) {
        return type_lookup.get(type_name);
    }

    // Enumerate over all Types...
    //	for(Type type : Type.values())
    //	{
    //	    System.out.println(type.name() + " :: "+ type.getType());
    //	}

    // Access a single type
    //	Type type = Type.valueOf("IDENTIFIER");
    //	type.getName();

    // Access a single type value
    //		Type type = Type.get("Identifier");
    //

}
