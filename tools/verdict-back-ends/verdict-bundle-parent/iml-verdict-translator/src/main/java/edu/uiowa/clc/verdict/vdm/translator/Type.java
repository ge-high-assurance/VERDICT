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

    // New attributes.
    REPLAY_PROTECTION("replayProtection"),
    CAN_RECEIVE_CONFIG_UPDATE("canReceiveConfigUpdate"),
    CAN_RECEIVE_SW_UPDATE("canReceiveSWUpdate"),
    CONTROL_RECEIVED_FROM_UNTRUSTED("controlReceivedFromUntrusted"),
    CONTROL_SENT_TO_UNTRUSTED("controlSentToUntrusted"),
    DATA_RECEIVED_FROM_UNTUSTED("dataReceivedFromUntrusted"),
    DATA_SENT_TO_UNTRUSTED("dataSentToUntrusted"),
    CONFIG_ATTACK("Configuration_Attack"),
    PHY_THEFT_ATTACK("Physical_Theft_Attack"),
    INTERCEPTION_ATTACK("Interception_Attack"),
    HARDWARE_INTEGRITY_ATTACK("Hardware_Integrity_Attack"),
    SUPPLY_CHAIN_ATTACK("Supply_Chain_Attack"),
    BRUTE_FORCE_ATTACK("Brute_Force_Attack"),
    FAULT_INJEC_ATTACK("Fault_Injection_Attack"),
    IDENTITY_SPOOFING_ATTACK("Identity_Spoofing_Attack"),
    EXPRESSIVE_ALLOC_ATTACK("Excessive_Allocation_Attack"),
    SNIFFING_ATTACK("Sniffing_Attack"),
    BUFFER_ATTACK("Buffer_Attack"),
    FLOODING_ATTACK("Flooding_Attack"),

    AUDIT_MESSAGE_RESPONSES("auditMessageResponses"),
    DEVICE_AUTHENTICATION("deviceAuthentication"),
    DOS_PROTECTION("dosProtection"),
    ENCRYPTED_STORAGE("encryptedStorage"),

    INPUT_VALIDATION("inputValidation"),
    LOGGING("logging"),
    MEMORY_PROTECTION("memoryProtection"),
    PHY_ACCESS_CONTROL("physicalAccessControl"),
    REMOVE_IDEN_INFO("removeIdentifyingInformation"),
    RESOURCE_AVAILABILITY("resourceAvailability"),
    RESOURCE_ISOLATION("resourceIsolation"),
    SECURE_BOOT("secureBoot"),
    SESSION_AUTH("sessionAuthenticity"),
    STATIC_CODE_ANALYSIS("staticCodeAnalysis"),
    STRONG_CRYPTO_ALGO("strongCryptoAlgorithms"),
    SUPPLY_CHAIN_SECURITY("supplyChainSecurity"),
    SYS_ACCESS_CONTROL("systemAccessControl"),
    TAMPER_PROTECTION("tamperProtection"),
    USER_AUTH("userAuthentication"),

    AUDIT_MESSAGE_RESPONSE_DAL("auditMessageResponsesDAL"),
    DEVICE_AUTH_DAL("deviceAuthenticationDAL"),
    DOS_PROTECTION_DAL("dosProtectionDAL"),

    ENCRYPTED_STORAGTE_DAL("encryptedStorageDAL"),
    INPUT_VALIDATION_DAL("inputValidationDAL"),
    LOGGING_DAL("loggingDAL"),
    MEMORY_PROTECTION_DAL("memoryProtectionDAL"),
    PHY_ACCESS_CONTROL_DAL("physicalAccessControlDAL"),
    //    REMOVE_IDENTIFYING_INFO_DAL("removeIdentifyingInformationDAL"),
    RESOURCE_AVAIL_DAL("resourceAvailabilityDAL"),
    RESOURCE_ISO_DAL("resourceIsolationDAL"),
    SECURE_BOOT_DAL("secureBootDAL"),
    SESSION_AUTH_DAL("sessionAuthenticityDAL"),
    STATIC_CODE_ANALYSIS_DAL("staticCodeAnalysisDAL"),
    STRONG_PROTECTION_DAL("strongCryptoAlgorithmsDAL"),
    SUPPLY_CHAIN_SECURITY_DAL("supplyChainSecurityDAL"),
    SYSTEM_ACCESS_CONTROL_DAL("systemAccessControlDAL"),
    TAMPER_PROTECTION_DAL("tamperProtectionDAL"),
    USER_AUTH_DAL("userAuthenticationDAL"),
    REMOVE_IDEN_INFO_DAL("removeIdentifyingInformationDAL"),
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
    TRUSTED_CONNECTION("trustedConnection"),
    ENCRYPTED_TRANSMISSION("encryptedTransmission"),
    ENCRYPTED_TRAMISSION_DAL("encryptedTransmissionDAL"),

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
    COMP_KIND("component_type"),

    PEDIGREE("pedigree"),
    PEDIGREE_TYPE("PedigreeType"),

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
    FLOW_TYPE("flow_type"),
    FLOWTYPE("FlowType"),

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
