# VERDICT: An OSATE plugin for architectural and behavioral analysis of AADL models

## DARPA Cyber Assured Systems Engineering (CASE) Program

The goal of the DARPA
[CASE](https://www.darpa.mil/program/cyber-assured-systems-engineering)
Program is to develop the necessary design, analysis and verification
tools to allow system engineers to design-in cyber resiliency and
manage tradeoffs as they do other nonfunctional properties when
designing complex embedded computing systems.  Cyber resiliency means
the system is tolerant to cyberattacks in the same way that safety
critical systems are tolerant to random faults—they recover and
continue to execute their mission function.  Achieving this goal
requires research breakthroughs in:

* the elicitation of cyber resiliency requirements before the system
  is built;

* the design and verification of systems when requirements are not
  testable (i.e., when they are expressed in shall not statements);

* tools to automatically adapt software to new non-functional
  requirements; and

* techniques to scale and provide meaningful feedback from analysis
  tools that reside low in the development tool chain.

<img src="docs/images/CASE-Program-Diagram.png" alt="CASE Program Diagram" width="750"/>
<!--- ![CASE Program Diagram](docs/images/CASE-Program-Diagram.png) --->

## VERDICT Workflow

The VERDICT tool is intended to perform analysis of a system at the
architectural level.  Based on the typical workflow shown below, the
VERDICT user will capture an architectural model using AADL that
represents the high-level functional components of the system along
with the data flow between them.  The VERDICT Model Based Architecture
Synthesis (MBAS) back-end tool will analyze the architecture to
identify cyber vulnerabilities and recommend defenses.  The defenses
will typically be recommendations to improve the resiliency of
components, such as control access to and encrypt communications
links, or add components to reduce dependence on a specific source of
information.  For example, add position sensors and voting logic
rather than depend exclusively on a GPS signal to determine location.
Once the architectural analysis is complete, VERDICT supports
refinement of the architecture model with behavioral modeling
information using AGREE.  The VERDICT Cyber Resiliency Verifier (CRV)
back-end tool performs a formal analysis of the updated model with
respect to formal cyber properties to identify vulnerabilities to
cyber threat effects.  This valuable capability provides an additional
depth of analysis of a model that includes behavioral details of the
architectural component models which will help to catch design
mistakes earlier in the development process.  Once the CRV analysis is
complete, the developer will go off and create a detailed
implementation.  The intent of VERDICT Automated Test Generation is to
verify that the implementation is consistent with the architecture and
behavioral models analyzed by VERDICT MBAS and CRV.

<img src="docs/images/VERDICT-Workflow-Diagram.png" alt="VERDICT Workflow Diagram" width="750"/>
<!--- ![VERDICT Workflow Diagram](docs/images/VERDICT-Workflow-Diagram.png) --->

## Publications

If you are citing VERDICT project, please use the following BibTex entries:
```latex
@Article{systems9010018,
AUTHOR = {Meng, Baoluo and Larraz, Daniel and Siu, Kit and Moitra, Abha and Interrante, John and Smith, William and Paul, Saswata and Prince, Daniel and Herencia-Zapana, Heber and Arif, M. Fareed and Yahyazadeh, Moosa and Tekken Valapil, Vidhya and Durling, Michael and Tinelli, Cesare and Chowdhury, Omar},
TITLE = {VERDICT: A Language and Framework for Engineering Cyber Resilient and Safe System},
JOURNAL = {Systems},
VOLUME = {9},
YEAR = {2021},
NUMBER = {1},
ARTICLE-NUMBER = {18},
URL = {https://www.mdpi.com/2079-8954/9/1/18},
ISSN = {2079-8954},
DOI = {10.3390/systems9010018}
}
```

```latex
@inproceedings{siu2019architectural,
  title={Architectural and behavioral analysis for cyber security},
  author={Siu, Kit and Moitra, Abha and Li, Meng and Durling, Michael and Herencia-Zapana, Heber and Interrante, John and Meng, Baoluo and Tinelli, Cesare and Chowdhury, Omar and Larraz, Daniel and others},
  booktitle={2019 IEEE/AIAA 38th Digital Avionics Systems Conference (DASC)},
  pages={1--10},
  year={2019},
  organization={IEEE}
}
```
Please refer to the [Wiki page](https://github.com/ge-high-assurance/VERDICT/wiki/Publications) for a complete list of publications related to VERDICT. 

Distribution Statement A: Approved for Public Release, Distribution Unlimited

Copyright © 2020, General Electric Company, Board of Trustees of the University of Iowa
