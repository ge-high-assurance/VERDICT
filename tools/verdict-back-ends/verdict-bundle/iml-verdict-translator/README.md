iml2vdm-verdict-translator:
--------------------------
Dependency:
The translator require  HERMES-IML as a dependency
#cd HERMES-IML/releng/com.utc.utrc.hermes.iml.parent
#mvn clean install

iml2vdm complie:
# mvn clean install
# cd target

Run:
#java -jar target/iml-verdict-translator-\<VERSION\>-capsule.jar <Input_IMLFile> <OUTPUT_VDMFILE>


Examples:
#java -jar target/iml-verdict-translator-\<VERSION\>-capsule.jar hawkeye-UAV/iml/hawkeyeUAV_model_A.iml hawkeye-UAV/xml/hawkeyeUAV_model_A.xml
