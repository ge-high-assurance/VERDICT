<?xml version="1.0" encoding="UTF-8"?>
<structure version="21" xsltversion="3" enforce-relative-filepaths="1" html-doctype="HTML5" compatibility-view="IE9" html-outputextent="Complete" relativeto="*SPS" encodinghtml="UTF-8" encodingrtf="ISO-8859-1" encodingpdf="UTF-8" useimportschema="1" embed-images="1" enable-authentic-scripts="1" authentic-scripts-in-debug-mode-external="0" generated-file-location="DEFAULT" ixbrl-version="1.0">
	<parameters/>
	<schemasources>
		<namespaces>
			<nspair prefix="jaxb" uri="http://java.sun.com/xml/ns/jaxb"/>
			<nspair prefix="vdm_data" uri="verdict::vdm::vdm_data"/>
			<nspair prefix="vdm_lustre" uri="verdict::vdm::vdm_lustre"/>
			<nspair prefix="vdm_model" uri="verdict::vdm::vdm_model"/>
		</namespaces>
		<schemasources>
			<xsdschemasource name="XML" main="1" schemafile="..\..\..\..\verdict-data-model\src\main\resources\vdm_model.xsd" workingxmlfile="..\..\..\..\verdict-data-model\src\test\resources\vdm-model.xml"/>
		</schemasources>
	</schemasources>
	<modules/>
	<flags>
		<scripts/>
		<mainparts/>
		<globalparts/>
		<designfragments/>
		<pagelayouts/>
		<xpath-functions/>
	</flags>
	<scripts>
		<script language="javascript"/>
	</scripts>
	<script-project>
		<Project version="4" app="AuthenticView"/>
	</script-project>
	<importedxslt/>
	<globalstyles/>
	<mainparts>
		<children>
			<globaltemplate subtype="main" match="/">
				<document-properties/>
				<styles font-family="Courier" font-size="10pt" font-weight="normal" line-height="10pt" text-align="left" white-space="nowrap"/>
				<children>
					<documentsection>
						<properties columncount="1" columngap="0.50in" headerfooterheight="fixed" pagemultiplepages="0" pagenumberingformat="1" pagenumberingstartat="auto" pagestart="next" paperheight="11.000in" papermarginbottom="1.000in" papermarginfooter="0.500in" papermarginheader="0.500in" papermarginleft="1.000in" papermarginright="1.000in" papermargintop="1.000in" paperwidth="8.500in"/>
						<watermark>
							<image transparency="50" fill-page="1" center-if-not-fill="1"/>
							<text transparency="50"/>
						</watermark>
					</documentsection>
					<template subtype="source" match="XML">
						<children>
							<template subtype="element" match="vdm_model:model">
								<children>
									<template subtype="element" match="dataflowCode">
										<children>
											<calltemplate subtype="named" match="TypeDeclarations">
												<parameters/>
											</calltemplate>
											<newline/>
											<calltemplate subtype="named" match="ConstantDeclarations">
												<parameters/>
											</calltemplate>
											<newline/>
											<calltemplate subtype="named" match="ContractDeclarations">
												<parameters/>
											</calltemplate>
											<newline/>
											<calltemplate subtype="named" match="NodeDeclarations">
												<parameters/>
											</calltemplate>
										</children>
										<variables/>
									</template>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
		</children>
	</mainparts>
	<globalparts/>
	<designfragments>
		<children>
			<globaltemplate subtype="named" match="InputParameters">
				<parameters/>
				<children>
					<condition>
						<children>
							<conditionbranch xpath="boolean(vdm_lustre:inputParameter/node())">
								<children>
									<template subtype="element" match="vdm_lustre:inputParameter">
										<children>
											<newline/>
											<text fixtext="  "/>
											<condition>
												<children>
													<conditionbranch xpath="position() = 1">
														<children>
															<text fixtext="("/>
														</children>
													</conditionbranch>
												</children>
											</condition>
											<condition>
												<children>
													<conditionbranch xpath="string(@vdm_lustre:isConstant) = (&apos;true&apos;, &apos;1&apos;)">
														<children>
															<text fixtext="const "/>
														</children>
													</conditionbranch>
												</children>
											</condition>
											<template subtype="element" match="vdm_lustre:name">
												<children>
													<content subtype="regular"/>
												</children>
												<variables/>
											</template>
											<template subtype="element" match="vdm_lustre:dataType">
												<children>
													<text fixtext=" : "/>
													<calltemplate subtype="named" match="DataType">
														<parameters/>
													</calltemplate>
												</children>
												<variables/>
											</template>
											<condition>
												<children>
													<conditionbranch xpath="position() = last()">
														<children>
															<text fixtext=")"/>
														</children>
													</conditionbranch>
													<conditionbranch>
														<children>
															<text fixtext=";"/>
														</children>
													</conditionbranch>
												</children>
											</condition>
										</children>
										<variables/>
									</template>
								</children>
							</conditionbranch>
							<conditionbranch>
								<children>
									<newline/>
									<text fixtext="  ()"/>
								</children>
							</conditionbranch>
						</children>
					</condition>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="EnumValues">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_data:enumValue">
						<children>
							<content subtype="regular"/>
							<condition>
								<children>
									<conditionbranch xpath="position() = last()"/>
									<conditionbranch>
										<children>
											<text fixtext=", "/>
										</children>
									</conditionbranch>
								</children>
							</condition>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="DataType">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_data:plainType">
						<children>
							<content subtype="regular"/>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_data:subrangeType">
						<children>
							<text fixtext="subrange ["/>
							<template subtype="element" match="vdm_data:lowerBound">
								<children>
									<content subtype="regular">
										<format basic-type="xsd" datatype="integer"/>
									</content>
								</children>
								<variables/>
							</template>
							<text fixtext=", "/>
							<template subtype="element" match="vdm_data:upperBound">
								<children>
									<content subtype="regular">
										<format basic-type="xsd" datatype="integer"/>
									</content>
								</children>
								<variables/>
							</template>
							<text fixtext="] of "/>
							<template subtype="attribute" match="vdm_data:type">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_data:arrayType">
						<children>
							<template subtype="element" match="vdm_data:dataType">
								<children>
									<calltemplate subtype="named" match="DataType">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext="^"/>
							<template subtype="element" match="vdm_data:dimension">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_data:tupleType">
						<children>
							<text fixtext="["/>
							<template subtype="element" match="vdm_data:dataType">
								<children>
									<calltemplate subtype="named" match="DataType">
										<parameters/>
									</calltemplate>
									<condition>
										<children>
											<conditionbranch xpath="position() = last()"/>
											<conditionbranch>
												<children>
													<text fixtext=", "/>
												</children>
											</conditionbranch>
										</children>
									</condition>
								</children>
								<variables/>
							</template>
							<text fixtext="]"/>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_data:userDefinedType">
						<children>
							<content subtype="regular"/>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="RecordFields">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_data:recordField">
						<children>
							<template subtype="element" match="vdm_data:name">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
							<template subtype="element" match="vdm_data:type">
								<children>
									<text fixtext=" : "/>
									<calltemplate subtype="named" match="DataType">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<condition>
								<children>
									<conditionbranch xpath="position() = last()"/>
									<conditionbranch>
										<children>
											<text fixtext="; "/>
										</children>
									</conditionbranch>
								</children>
							</condition>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="OutputParameters">
				<parameters/>
				<children>
					<condition>
						<children>
							<conditionbranch xpath="boolean(vdm_lustre:outputParameter/node())">
								<children>
									<template subtype="element" match="vdm_lustre:outputParameter">
										<children>
											<newline/>
											<text fixtext="  "/>
											<condition>
												<children>
													<conditionbranch xpath="position() = 1">
														<children>
															<text fixtext="("/>
														</children>
													</conditionbranch>
												</children>
											</condition>
											<template subtype="element" match="vdm_lustre:name">
												<children>
													<content subtype="regular"/>
												</children>
												<variables/>
											</template>
											<template subtype="element" match="vdm_lustre:dataType">
												<children>
													<text fixtext=" : "/>
													<calltemplate subtype="named" match="DataType">
														<parameters/>
													</calltemplate>
												</children>
												<variables/>
											</template>
											<condition>
												<children>
													<conditionbranch xpath="position() = last()">
														<children>
															<text fixtext=");"/>
														</children>
													</conditionbranch>
													<conditionbranch>
														<children>
															<text fixtext=";"/>
														</children>
													</conditionbranch>
												</children>
											</condition>
										</children>
										<variables/>
									</template>
								</children>
							</conditionbranch>
							<conditionbranch>
								<children>
									<newline/>
									<text fixtext="  ();"/>
								</children>
							</conditionbranch>
						</children>
					</condition>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="LocalVariables">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_lustre:variableDeclaration">
						<children>
							<condition>
								<children>
									<conditionbranch xpath="position() = 1">
										<children>
											<text fixtext="var"/>
											<newline/>
										</children>
									</conditionbranch>
								</children>
							</condition>
							<text fixtext="  "/>
							<template subtype="element" match="vdm_lustre:name">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
							<template subtype="element" match="vdm_lustre:dataType">
								<children>
									<text fixtext=" : "/>
									<calltemplate subtype="named" match="DataType">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=";"/>
							<newline/>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="Expression">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_lustre:identifier">
						<children>
							<content subtype="regular"/>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:boolLiteral">
						<children>
							<content subtype="regular"/>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:intLiteral">
						<children>
							<content subtype="regular">
								<format basic-type="xsd" datatype="integer"/>
							</content>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:realLiteral">
						<children>
							<content subtype="regular">
								<format basic-type="xsd" string="?*#0.0#*" datatype="decimal"/>
							</content>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:expressionList">
						<children>
							<text fixtext="("/>
							<calltemplate subtype="named" match="Expressions">
								<parameters/>
							</calltemplate>
							<text fixtext=")"/>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:recordLiteral">
						<children>
							<template subtype="attribute" match="vdm_lustre:recordType">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
							<text fixtext=" { "/>
							<calltemplate subtype="named" match="RecordFieldDefinitions">
								<parameters/>
							</calltemplate>
							<text fixtext=" }"/>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:arrayExpression">
						<children>
							<text fixtext="["/>
							<calltemplate subtype="named" match="Expressions">
								<parameters/>
							</calltemplate>
							<text fixtext="]"/>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:cartesianExpression">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext="^"/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:tupleExpression">
						<children>
							<text fixtext="{"/>
							<calltemplate subtype="named" match="Expressions">
								<parameters/>
							</calltemplate>
							<text fixtext="}"/>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:negative">
						<children>
							<text fixtext="-"/>
							<calltemplate subtype="named" match="Expression">
								<parameters/>
							</calltemplate>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:pre">
						<children>
							<text fixtext="pre "/>
							<calltemplate subtype="named" match="Expression">
								<parameters/>
							</calltemplate>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:current">
						<children>
							<text fixtext="current "/>
							<calltemplate subtype="named" match="Expression">
								<parameters/>
							</calltemplate>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:toInt">
						<children>
							<text fixtext="int "/>
							<calltemplate subtype="named" match="Expression">
								<parameters/>
							</calltemplate>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:toReal">
						<children>
							<text fixtext="real "/>
							<calltemplate subtype="named" match="Expression">
								<parameters/>
							</calltemplate>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:when">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" when "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:times">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" * "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:div">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" / "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:mod">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" mod "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:intDiv">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" div "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:plus">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" + "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:minus">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" - "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:not">
						<children>
							<text fixtext="not "/>
							<calltemplate subtype="named" match="Expression">
								<parameters/>
							</calltemplate>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:lessThan">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" &lt; "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:lessThanOrEqualTo">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" &lt;= "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:equal">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" = "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:greaterThanOrEqualTo">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" &gt;= "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:greaterThan">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" &gt; "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:notEqual">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" &lt;&gt; "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:and">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" and "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:or">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" or "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:xor">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" xor "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:implies">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" =&gt; "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:arrow">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" -&gt; "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:concat">
						<children>
							<template subtype="element" match="vdm_lustre:lhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" | "/>
							<template subtype="element" match="vdm_lustre:rhsOperand">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:diese">
						<children>
							<text fixtext="#("/>
							<calltemplate subtype="named" match="Expressions">
								<parameters/>
							</calltemplate>
							<text fixtext=")"/>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:nor">
						<children>
							<text fixtext="nor("/>
							<calltemplate subtype="named" match="Expressions">
								<parameters/>
							</calltemplate>
							<text fixtext=")"/>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:conditionalExpression">
						<children>
							<text fixtext="if "/>
							<template subtype="element" match="vdm_lustre:condition">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" then "/>
							<template subtype="element" match="vdm_lustre:thenBranch">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=" else "/>
							<template subtype="element" match="vdm_lustre:elseBranch">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:call">
						<children>
							<template subtype="attribute" match="vdm_lustre:nodeId">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
							<text fixtext="("/>
							<calltemplate subtype="named" match="Arguments">
								<parameters/>
							</calltemplate>
							<text fixtext=")"/>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:recordProjection">
						<children>
							<template subtype="element" match="vdm_lustre:recordReference">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext="."/>
							<template subtype="element" match="vdm_lustre:fieldId">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:arraySelection">
						<children>
							<template subtype="element" match="vdm_lustre:array">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext="["/>
							<template subtype="element" match="vdm_lustre:selector">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<template subtype="element" match="vdm_lustre:trancheEnd">
								<children>
									<text fixtext=".."/>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<template subtype="element" match="vdm_lustre:sliceStep">
								<children>
									<text fixtext=" step "/>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext="]"/>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_lustre:merge">
						<children>
							<text fixtext="merge "/>
							<template subtype="element" match="vdm_lustre:clock">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
							<text fixtext=" "/>
							<template subtype="element" match="vdm_lustre:mergeCase">
								<children>
									<text fixtext="("/>
									<template subtype="element" match="vdm_lustre:case">
										<children>
											<content subtype="regular"/>
										</children>
										<variables/>
									</template>
									<text fixtext=" -&gt; "/>
									<template subtype="element" match="vdm_lustre:expr">
										<children>
											<calltemplate subtype="named" match="Expression">
												<parameters/>
											</calltemplate>
										</children>
										<variables/>
									</template>
									<text fixtext=")"/>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="Arguments">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_lustre:argument">
						<children>
							<calltemplate subtype="named" match="Expression">
								<parameters/>
							</calltemplate>
							<condition>
								<children>
									<conditionbranch xpath="position() = last()"/>
									<conditionbranch>
										<children>
											<text fixtext=", "/>
										</children>
									</conditionbranch>
								</children>
							</condition>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="RecordFieldDefinitions">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_lustre:fieldDefinition">
						<children>
							<template subtype="element" match="vdm_lustre:fieldIdentifier">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
							<template subtype="element" match="vdm_lustre:fieldValue">
								<children>
									<text fixtext=" = "/>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<condition>
								<children>
									<conditionbranch xpath="position() = last()"/>
									<conditionbranch>
										<children>
											<text fixtext="; "/>
										</children>
									</conditionbranch>
								</children>
							</condition>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="Expressions">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_lustre:expression">
						<children>
							<calltemplate subtype="named" match="Expression">
								<parameters/>
							</calltemplate>
							<condition>
								<children>
									<conditionbranch xpath="position() = last()"/>
									<conditionbranch>
										<children>
											<text fixtext=", "/>
										</children>
									</conditionbranch>
								</children>
							</condition>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="Assumes">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_lustre:assume">
						<children>
							<text fixtext="  assume "/>
							<template subtype="element" match="vdm_lustre:expression">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=";"/>
							<newline/>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="Guarantees">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_lustre:guarantee">
						<children>
							<text fixtext="  guarantee "/>
							<template subtype="element" match="vdm_lustre:name">
								<children>
									<content subtype="regular"/>
									<text fixtext=" "/>
								</children>
								<variables/>
							</template>
							<template subtype="element" match="vdm_lustre:expression">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=";"/>
							<newline/>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="Imports">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_lustre:import">
						<children>
							<text fixtext="  import "/>
							<template subtype="attribute" match="vdm_lustre:contractId">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
							<text fixtext=" ("/>
							<template subtype="element" match="vdm_lustre:inputArgument">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
									<condition>
										<children>
											<conditionbranch xpath="position() = last()"/>
											<conditionbranch>
												<children>
													<text fixtext=", "/>
												</children>
											</conditionbranch>
										</children>
									</condition>
								</children>
								<variables/>
							</template>
							<text fixtext=") returns ("/>
							<template subtype="element" match="vdm_lustre:outputArgument">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
									<condition>
										<children>
											<conditionbranch xpath="position() = last()"/>
											<conditionbranch>
												<children>
													<text fixtext=", "/>
												</children>
											</conditionbranch>
										</children>
									</condition>
								</children>
								<variables/>
							</template>
							<text fixtext=");"/>
							<newline/>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="Equations">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_lustre:equation">
						<children>
							<text fixtext="  "/>
							<template subtype="element" match="vdm_lustre:lhs">
								<children>
									<condition>
										<children>
											<conditionbranch xpath="boolean(vdm_lustre:identifier/node())">
												<children>
													<template subtype="element" match="vdm_lustre:identifier">
														<children>
															<content subtype="regular"/>
															<condition>
																<children>
																	<conditionbranch xpath="position() = last()"/>
																	<conditionbranch>
																		<children>
																			<text fixtext=", "/>
																		</children>
																	</conditionbranch>
																</children>
															</condition>
														</children>
														<variables/>
													</template>
												</children>
											</conditionbranch>
											<conditionbranch>
												<children>
													<text fixtext="()"/>
												</children>
											</conditionbranch>
										</children>
									</condition>
								</children>
								<variables/>
							</template>
							<text fixtext=" = "/>
							<template subtype="element" match="vdm_lustre:rhs">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=";"/>
							<newline/>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="TypeDeclarations">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_lustre:typeDeclaration">
						<children>
							<text fixtext="type "/>
							<template subtype="element" match="vdm_data:name">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
							<template subtype="element" match="vdm_data:definition">
								<children>
									<text fixtext=" = "/>
									<calltemplate subtype="named" match="TypeDefinition">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=";"/>
							<newline/>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="ContractDeclarations">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_lustre:contractDeclaration">
						<children>
							<text fixtext="contract "/>
							<template subtype="element" match="vdm_lustre:name">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
							<calltemplate subtype="named" match="InputParameters">
								<parameters/>
							</calltemplate>
							<newline/>
							<text fixtext="returns"/>
							<calltemplate subtype="named" match="OutputParameters">
								<parameters/>
							</calltemplate>
							<newline/>
							<template subtype="element" match="vdm_lustre:specification">
								<children>
									<text fixtext="let"/>
									<newline/>
									<calltemplate subtype="named" match="Symbols">
										<parameters/>
									</calltemplate>
									<calltemplate subtype="named" match="Assumes">
										<parameters/>
									</calltemplate>
									<calltemplate subtype="named" match="Guarantees">
										<parameters/>
									</calltemplate>
									<calltemplate subtype="named" match="Modes">
										<parameters/>
									</calltemplate>
									<calltemplate subtype="named" match="Imports">
										<parameters/>
									</calltemplate>
									<text fixtext="tel"/>
									<newline/>
								</children>
								<variables/>
							</template>
							<newline/>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="NodeDeclarations">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_lustre:nodeDeclaration">
						<children>
							<condition>
								<children>
									<conditionbranch xpath="string(@vdm_lustre:isFunction) = (&apos;true&apos;, &apos;1&apos;)">
										<children>
											<text fixtext="function"/>
										</children>
									</conditionbranch>
									<conditionbranch>
										<children>
											<text fixtext="node"/>
										</children>
									</conditionbranch>
								</children>
							</condition>
							<condition>
								<children>
									<conditionbranch xpath="string(@vdm_lustre:isImported) = (&apos;true&apos;, &apos;1&apos;)">
										<children>
											<text fixtext=" imported"/>
										</children>
									</conditionbranch>
								</children>
							</condition>
							<text fixtext=" "/>
							<template subtype="element" match="vdm_lustre:name">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
							<calltemplate subtype="named" match="InputParameters">
								<parameters/>
							</calltemplate>
							<newline/>
							<text fixtext="returns"/>
							<calltemplate subtype="named" match="OutputParameters">
								<parameters/>
							</calltemplate>
							<newline/>
							<template subtype="element" match="vdm_lustre:contract">
								<children>
									<text fixtext="(*@contract"/>
									<newline/>
									<calltemplate subtype="named" match="Symbols">
										<parameters/>
									</calltemplate>
									<calltemplate subtype="named" match="Assumes">
										<parameters/>
									</calltemplate>
									<calltemplate subtype="named" match="Guarantees">
										<parameters/>
									</calltemplate>
									<calltemplate subtype="named" match="Modes">
										<parameters/>
									</calltemplate>
									<calltemplate subtype="named" match="Imports">
										<parameters/>
									</calltemplate>
									<text fixtext="*)"/>
									<newline/>
								</children>
								<variables/>
							</template>
							<template subtype="element" match="vdm_lustre:body">
								<children>
									<calltemplate subtype="named" match="LocalConstants">
										<parameters/>
									</calltemplate>
									<calltemplate subtype="named" match="LocalVariables">
										<parameters/>
									</calltemplate>
									<text fixtext="let"/>
									<newline/>
									<calltemplate subtype="named" match="Assertions">
										<parameters/>
									</calltemplate>
									<calltemplate subtype="named" match="Equations">
										<parameters/>
									</calltemplate>
									<calltemplate subtype="named" match="Main">
										<parameters/>
									</calltemplate>
									<calltemplate subtype="named" match="Properties">
										<parameters/>
									</calltemplate>
									<text fixtext="tel"/>
									<newline/>
								</children>
								<variables/>
							</template>
							<newline/>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="ConstantDeclarations">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_lustre:constantDeclaration">
						<children>
							<text fixtext="const "/>
							<template subtype="element" match="vdm_lustre:name">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
							<template subtype="element" match="vdm_lustre:dataType">
								<children>
									<text fixtext=" : "/>
									<calltemplate subtype="named" match="DataType">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<template subtype="element" match="vdm_lustre:definition">
								<children>
									<text fixtext=" = "/>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=";"/>
							<newline/>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="TypeDefinition">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_data:plainType">
						<children>
							<content subtype="regular"/>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_data:subrangeType">
						<children>
							<text fixtext="subrange ["/>
							<template subtype="element" match="vdm_data:lowerBound">
								<children>
									<content subtype="regular">
										<format basic-type="xsd" datatype="integer"/>
									</content>
								</children>
								<variables/>
							</template>
							<text fixtext=", "/>
							<template subtype="element" match="vdm_data:upperBound">
								<children>
									<content subtype="regular">
										<format basic-type="xsd" datatype="integer"/>
									</content>
								</children>
								<variables/>
							</template>
							<text fixtext="] of "/>
							<template subtype="attribute" match="vdm_data:type">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_data:arrayType">
						<children>
							<template subtype="element" match="vdm_data:dataType">
								<children>
									<calltemplate subtype="named" match="DataType">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext="^"/>
							<template subtype="element" match="vdm_data:dimension">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_data:tupleType">
						<children>
							<text fixtext="["/>
							<template subtype="element" match="vdm_data:dataType">
								<children>
									<calltemplate subtype="named" match="DataType">
										<parameters/>
									</calltemplate>
									<condition>
										<children>
											<conditionbranch xpath="position() = last()"/>
											<conditionbranch>
												<children>
													<text fixtext=", "/>
												</children>
											</conditionbranch>
										</children>
									</condition>
								</children>
								<variables/>
							</template>
							<text fixtext="]"/>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_data:enumType">
						<children>
							<text fixtext="enum { "/>
							<calltemplate subtype="named" match="EnumValues">
								<parameters/>
							</calltemplate>
							<text fixtext=" }"/>
						</children>
						<variables/>
					</template>
					<template subtype="element" match="vdm_data:recordType">
						<children>
							<text fixtext="struct { "/>
							<calltemplate subtype="named" match="RecordFields">
								<parameters/>
							</calltemplate>
							<text fixtext=" }"/>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="LocalConstants">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_lustre:constantDeclaration">
						<children>
							<condition>
								<children>
									<conditionbranch xpath="position() = 1">
										<children>
											<text fixtext="const"/>
											<newline/>
										</children>
									</conditionbranch>
								</children>
							</condition>
							<text fixtext="  "/>
							<template subtype="element" match="vdm_lustre:name">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
							<template subtype="element" match="vdm_lustre:dataType">
								<children>
									<text fixtext=" : "/>
									<calltemplate subtype="named" match="DataType">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<template subtype="element" match="vdm_lustre:definition">
								<children>
									<text fixtext=" = "/>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=";"/>
							<newline/>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="Properties">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_lustre:property">
						<children>
							<text fixtext="  --%PROPERTY "/>
							<template subtype="attribute" match="vdm_lustre:name">
								<children>
									<content subtype="regular"/>
									<text fixtext=" "/>
								</children>
								<variables/>
							</template>
							<template subtype="element" match="vdm_lustre:expression">
								<children>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=";"/>
							<newline/>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="Main">
				<parameters/>
				<children>
					<condition>
						<children>
							<conditionbranch xpath="string(@vdm_lustre:isMain) = (&apos;true&apos;, &apos;1&apos;)">
								<children>
									<text fixtext="  --%MAIN;"/>
									<newline/>
								</children>
							</conditionbranch>
						</children>
					</condition>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="Assertions">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_lustre:assertion">
						<children>
							<text fixtext="  assert "/>
							<calltemplate subtype="named" match="Expression">
								<parameters/>
							</calltemplate>
							<text fixtext=";"/>
							<newline/>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="Symbols">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_lustre:symbol">
						<children>
							<text fixtext="  "/>
							<condition>
								<children>
									<conditionbranch xpath="string(@vdm_lustre:isConstant) = (&apos;true&apos;, &apos;1&apos;)">
										<children>
											<text fixtext="const"/>
										</children>
									</conditionbranch>
									<conditionbranch>
										<children>
											<text fixtext="var"/>
										</children>
									</conditionbranch>
								</children>
							</condition>
							<text fixtext=" "/>
							<template subtype="element" match="vdm_lustre:name">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
							<template subtype="element" match="vdm_lustre:dataType">
								<children>
									<text fixtext=" : "/>
									<calltemplate subtype="named" match="DataType">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<template subtype="element" match="vdm_lustre:definition">
								<children>
									<text fixtext=" = "/>
									<calltemplate subtype="named" match="Expression">
										<parameters/>
									</calltemplate>
								</children>
								<variables/>
							</template>
							<text fixtext=";"/>
							<newline/>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
			<globaltemplate subtype="named" match="Modes">
				<parameters/>
				<children>
					<template subtype="element" match="vdm_lustre:mode">
						<children>
							<text fixtext="  mode "/>
							<template subtype="element" match="vdm_lustre:name">
								<children>
									<content subtype="regular"/>
								</children>
								<variables/>
							</template>
							<text fixtext=" ("/>
							<newline/>
							<template subtype="element" match="vdm_lustre:require">
								<children>
									<text fixtext="    require "/>
									<template subtype="element" match="vdm_lustre:expression">
										<children>
											<calltemplate subtype="named" match="Expression">
												<parameters/>
											</calltemplate>
										</children>
										<variables/>
									</template>
									<text fixtext=";"/>
									<newline/>
								</children>
								<variables/>
							</template>
							<template subtype="element" match="vdm_lustre:ensure">
								<children>
									<text fixtext="    ensure "/>
									<template subtype="element" match="vdm_lustre:expression">
										<children>
											<calltemplate subtype="named" match="Expression">
												<parameters/>
											</calltemplate>
										</children>
										<variables/>
									</template>
									<text fixtext=";"/>
									<newline/>
								</children>
								<variables/>
							</template>
							<text fixtext="  );"/>
							<newline/>
						</children>
						<variables/>
					</template>
				</children>
			</globaltemplate>
		</children>
	</designfragments>
	<xmltables/>
	<authentic-custom-toolbar-buttons/>
</structure>
