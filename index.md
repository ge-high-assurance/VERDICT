## About

VERDICT is a framework to perform analysis of a system at the architectural level. It consists of two major functionalities: Model Based Architecture Analysis and Synthesis (MBAAS) and Cyber Resiliency Verification (CRV). VERDICT user starts with capturing an architectural model using AADL that represents the high-level functional components of the system along with the data flow between them; and then annotate the model with VERDICT properties for analysis. The VERDICT MBAA back-end tool will analyze the architecture to identify cyber vulnerabilities and recommend defenses. User may also use the VERDICT MBAS feature to synthesize a minimal set of defenses with respect to their implementation costs. Once the architectural analysis is complete, VERDICT supports refinement of the architecture model with behavioral modeling information using AGREE. The VERDICT CRV back-end tool performs a formal analysis of the updated model with respect to formal cyber properties to identify vulnerabilities to cyber threat effects. This valuable capability provides an additional depth of analysis of a model that includes behavioral details of the architectural component models which will help to catch design mistakes earlier in the development process.

### Main Features

- Model Based Architecture Analysis (MBAA)
  - Identification of CAPEC threats and recommendation of NIST controls
  - Determination of the likelihood of successful attack of top-level event
  - Calculation of the probability of system failure
  - Generation of cut-sets, fault-tree and attack-defense tree
  
- Model Based Architecture Syntehsis (MBAS)
  - Synthesis of a minimal set of defenses that mitigate the top-level event with respect to implementation costs without existing implemented defenses
  - Synthesis of a minimal set of defenses that mitigate the top-level event with existing implemented defenses 
    - If existing implemented defenses already mitigate the top-level event, MBAS recommends elimination of unnecessary defenses and downgrade of defense Design Assurance Level (DAL). 
    - if existing implemented defenses do not mitigate the top-level event, MBAS recommends elimination of unnecessary defenses and upgrade of defense DALs. 
    
- Cyber Resillency Verification (CRV)
  - Proof of cyber resillency properties
    - Merit assignment
  - Disproof of cyber resillency properties
    - Blame assignment
  
