# ODM Transformation Advisor
The IBM Operational Decision Manager (ODM) platform has evolved quickly in the past few years: a new management console, a new engine (the Decision Engine),
new projects organization (Decision Services), a SaaS offering (ODM on Cloud), and we can expect more to come as new platform deployment options are made available.
But inevitably, as new features and capabilities are released, older ones get deprecated.
Also, customization which require repackaging the platform EARs in the on-prem delivery model are obviously not supported by the SaaS delivery model.

To help you assess the state of your existing projects, the ODM Transformation Advisor application (OTA) application performs a number of sanity checks on the rule projects and
decision services found in your Decision Center repository.
In particular, it will look for, and flag the use of deprecated features as well as features which would prevent the projects 
from being managed by the ODM on Cloud SaaS platform.
The application will also look for rule project artifacts that do not align with some common implementation good practices.

See the list of checkpoints in the following section, and check-out a [sample report](https://rawgit.com/ODMDev/odm-transformation-advisor/master/docs/sample-ota-report.html) in the [docs](docs) folder.

## List of checkpoints 
The table below lists the current list of checkpoints that are performed by the tool.

| Check                           | Deprecated | Not Available on SaaS | Not Available on OCP |
| --------------------------------| ---------- | --------------------- | -------------------- |
| Classic rule engine             | &#10004;   | &#10004;      | &#10004;      | 
| Classic rule projects           | &#10004;   | &#10004;      | &#10004;      | 
| CRE API in B2X                  | &#10004;   | &#10004;      | &#10004;      | 
| EC fine-grain permissions       | &#10004;   | &#10004;      | &#10004;      | 
| Rule templates                  | &#10004;   |               |               | 
| Decision trees                  | &#10004;   |               |               | 
| Domain providers                |            | &#10004;      |               | 
| Custom valueInfo                |            | &#10004;      |               | 
| Custom valueEditor              |            | &#10004;      |               | 
| Custom rule properties          |            | &#10004;      |               |

Additionally, the advisor is looking for the following situations:

* Projects with too many branches     
* Rules that are defined in same project as a BOM entry     
* Rule tasks using an exit criteria                 
* Different algorithms used the same rule flow    
* Rule tasks using the Rete or Sequential algorithm                  
* Use of technical rules                               
* Definition of rules under the root folder            
* Use of rule priorities                               
* Rules derived from rule templates            
* Java-like verbalization in the BOM                   
* Spelling error in BOM member verbalizations          
* Rules with same name across packages                
* Rules using 'else' statement                          
* B2X code for BOM members that is too long

For each of these cases, the advisor points to the rule project artifact in the repository and provides a recommendation associated with a common rule project development good practice.

# Building the ODM Transformation Advisor
If you have a Java 1.8 install available, you can readily use the pre-packaged advisor jars to run the application (see the next section: _Running the ODM Transformation Advisor_). Otherwise, follow the build steps below.

## Build the jars
Under the `ota-xom` project:
* Edit the `build.properties` file to set the `odm.dir` property to your ODM install location.
* Run the default `jar-ota-xom` Ant target to build `ota-xom.jar`

Under the `ota-driver` project:
* Edit the `build.properties` file to set the `odm.dir` property to your ODM install location.
* Run the `jar-ota` Ant target to build `ota-driver.jar`

## Package the advisor executable
Under the `ota-driver` project:
* Run the `package-ota` Ant target to package the executable advisor application under the `ota-driver/pkg/ota` folder.

# Running the ODM Transformation Advisor
The executable advisor application is packaged under [ota-driver/pkg/ota](ota-driver/pkg/ota).

## Prerequisites
Running the advisor requires the following to be installed and/or available:
* Java runtime and the JAVA_HOME environment variable pointing to the Java install folder. Note that the pre-packaged jars have been compiled with Java 1.8.
* [Apache Ant](https://ant.apache.org/) version 1.7.1 or above, and the ANT_HOME environment variable pointing to the Ant install folder.
* Access to your Operational Decision Manager install folder (e.g. `C:/Program Files/IBM/ODM810`)

## Configuration

### Mandatory settings
Before running advisor, you need to edit the **[ota.properties](ota.properties)** file and set a few properties about your environment. At a minimum, you will need to set the following properties:
* `ota.url` is the URL to the ODM repository you want to review.
* `ota.version` is the name of the ODM version you are using. It has to be one of `v86`, `v87`, `v88`, `v89` or `v810`.
* `odm.dir` is the Operational Decision Manager install folder.

### Optional settings
Other configuration properties that can be set are:

* `ota.datasource`: the name of the target Decision Center datasource, when it is not the default `jdbc/ilogDataSource` name.
* `ota.projects`: a list of rule project names (separated by a colon). Use this property when you only want a specific list of projects to be examined.
Otherwise, leave it blank to go through all the projects in the repository.
* `ota.report`: the file path and name for the report that will be produced by advisor.

### Spellchecking
Spelling errors in BOM vocabularies are commonplace, and the advisor allows you to spell-check the BOM verbalizations. To enable spellchecking, you will need to provide a dictionary of known words. This dictionary should be a text file containing one word per line. For English, you can use the word lists found at [12Dicts](http://wordlist.aspell.net/12dicts/) for example. Make sure that the words in your dictionary file only contain alphabetic characters. Also note that case is ignored in the spellchecking process.

Once your dictionary file is created, save it with the name `spelling.txt` under the `resources` folder of the advisor runtime package.
Spelling is only active when the dictionary file present under `resources` folder and gets automatically disabled when the file is removed.

### More configurable parameters
Some validations are parametrized, allowing to specify what _"too many"_ means in _"projects with too many branches"_
or what is _"too long"_ when flagging _"B2X code for BOM members that is is too long"_.
The parameters are defined in the PARAMETERS column (column H) of the [findings.xlsx](resources/findings.xlsx) spreadsheet.
You can change the value of these parameters in the spreadsheet to your desired values before running the advisor application.

## Running the checks
After you have configured the properties, simply run the `run-ota.bat` (or `run-ota.sh` for Linux) shell script to start checking your projects.

* The script will start by asking you for the Decision Center credentials you want to use to run the validation.
* Once the script completes, the report will be available in the `ota-report.html` default file (or the file you have specified for the `ota.report` property).
* Items flagged in the report include the permalink to the Decision Center rule artifact involved when available, in order to facilitate its review.
* Log entries are persisted in the `ota.log` file. The logger settings can be updated in the `logging.properties` file under the `resources` folder.

Out of the box, a sample execution will look like the following:

```
C:\ota\ota-driver\pkg\ota>run-ota

C:\ota\ota-driver\pkg\ota>C:\Java\apache-ant-1.9.6\bin\ant -f ota.xml -inputhandler org.apache.tools.ant.input.SecureInputHandler run-ota
Buildfile: C:\ota\ota-driver\pkg\ota\ota.xml

run-ota:
Decision Center - Username:
Decision Center - Password:
     [java] Sep 14, 2018 11:11:56 AM com.ibm.odm.ota.OTARunner run
     [java] INFO: Starting repository analysis for http://localhost:9090/teamserver
     [java] Sep 14, 2018 11:11:56 AM com.ibm.odm.ota.Findings load
     [java] INFO: Loading findings configuration file file:/C:/ota/ota-driver/pkg/ota/resources/findings.xlsx
     [java] Sep 14, 2018 11:11:56 AM com.ibm.odm.ota.checker.ProjectChecker run
     [java] INFO: @ Checking individual rule projects from repository
     [java] Sep 14, 2018 11:11:57 AM com.ibm.odm.ota.checker.ProjectChecker runOne
     [java] INFO: Checking project Loan Validation Service
     [java] Sep 14, 2018 11:11:59 AM com.ibm.odm.ota.checker.ProjectGroupChecker run
     [java] INFO: @ Checking project groups or decision services from repository
     [java] Sep 14, 2018 11:12:00 AM com.ibm.odm.ota.checker.ProjectGroupChecker runOne
     [java] INFO: Checking project group or decision service Loan Validation Service
     [java] Sep 14, 2018 11:12:00 AM com.ibm.odm.ota.checker.RepositoryChecker run
     [java] INFO: @ Checking repository characteristics
     [java] Sep 14, 2018 11:12:00 AM com.ibm.odm.ota.checker.BOMChecker run
     [java] INFO: @ Checking BOM projects from repository
     [java] Sep 14, 2018 11:12:00 AM com.ibm.odm.ota.checker.BOMChecker checkBOM
     [java] INFO: Checking model BOM from project Loan Validation Base
     [java] Sep 14, 2018 11:12:00 AM com.ibm.odm.ota.checker.BOMChecker checkB2X
     [java] INFO: Checking model B2X from project Loan Validation Base
     [java] Sep 14, 2018 11:12:00 AM com.ibm.odm.ota.checker.BOMChecker checkVoc
     [java] INFO: Checking model vocabulary from project Loan Validation Base
     [java] Sep 14, 2018 11:12:00 AM com.ibm.odm.ota.checker.VOCSpeller <init>
     [java] WARNING: Cannot find the spelling dictionary file 'spelling.txt' in the resources folder. Spell check will not be performed.
     [java] Sep 14, 2018 11:12:00 AM com.ibm.odm.ota.OTARunner run
     [java] INFO: Analysis completed, results available in ota-report.html
     [java] Sep 14, 2018 11:12:00 AM com.ibm.odm.ota.DCConnection endSession
     [java] INFO: Ending use of DC session

BUILD SUCCESSFUL
Total time: 14 seconds
```

# Jenkins integration

You can integrate the OTA as part of your continuous integration practice, to perform continuous code quality inspection on your rule projects. Direction for integration with Jenkins are provided here.

# FAQ

- [Does OTA alter my ODM projects in any way?](#does-ota-alter-my-odm-projects-in-any-way)

## Does OTA alter my ODM projects in any way?
The advisor only reads the content of your ODM repository. It does not make any modification to the ODM repository, or capture any data on your rule projects.

## What about older ODM versions

The source code in this repo supports running against repositories from ODM 8.6 and above.
If you are running older versions such as `8.5.x` or `8.0.x`, pre-packaged executable versions are available in the [`OLDER_ODM_VERSIONS`](OLDER_ODM_VERSIONS) folder.

The list of compatibility checkpoints that are executed is the same. However, less best practices validations are available.


# More information and feedback

The advisor implements most of its validations using ODM business rules that are applied to the rule artifacts from your repository.
For example, the check for rules that are defined directly under the root folder of the project is implemented with the following simple rule:
```
definitions
  set rule to an ilr business rule in elements ;
if
  the rule package of rule is null  
then
  add entry Rule Under Root : < project, rule > to report ;
```

Aside from enjoying a drink of our own Champagne, the goal of using rules was to make the advisor easily expandable to new validations, specific to your enterprise standards, without having to write any code. The advisor can then be used as a standard validation step, possibly as part of a Continuous Integration toolchain.

This application was built based on the expertise of the IBM Cloud Integration Expert Labs and IBM Garage Solution Engineering.
If you would like to discuss expanding it and/or integrating it into your operations or have other questions, comments, or feedback, please send email to **Pierre Berlandier** at *pberland@us.ibm.com*


# License

This project is licensed under the Apache License - see the [LICENSE](LICENSE) file for details.

# Notice

© Copyright IBM Corporation 2020.

