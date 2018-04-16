# WSO2 ESB Dependency Document generator /ESBDoc)
![Build status](https://circleci.com/gh/Mystes/esbdoc.svg?style=svg)
## What is WSO2 ESB?
[WSO2 ESB](http://wso2.com/products/enterprise-service-bus/) is an open source Enterprise Service Bus that enables interoperability among various heterogeneous systems and business applications.

## ESBDoc features
ESBDoc reads WSO2 ESB deployment car, analyzes it and generate dependency graphs from end-points, proxies and sequences. 
Resulting graph can be navigated with included HTML-UI.
Implementation is Maven plugin which can be added as part of the build process.
### UI
UI is available as embedded HTML-document. Here are samples of the main functionality,
#### Home screen
Documentation consist of main window with search and list of found resources.
![MainWindow](https://github.com/Mystes/esbdoc/blob/master/ESBDoc-plugin/images/main.png)
#### Dependency graph
This window contains graphical dependency graph.  Different graphical cues are used when documenting
node types(proxy, api, sequence...) and edges (call, send...). Click node to see it's parameters and dependencies.
![GraphWindow](https://github.com/Mystes/esbdoc/blob/master/ESBDoc-plugin/images/dependencygraph.png)
#### Dependency list
Offers list for directly called stpes and also reverse caller list. 
![ListWindow](https://github.com/Mystes/esbdoc/blob/master/ESBDoc-plugin/images/dependencylist.png)
#### SoapUI Test coverage
Offers list of those SoapUI tests, which tests given proxy or sequence.
![TesttWindow](https://github.com/Mystes/esbdoc/blob/master/ESBDoc-plugin/images/testlist.png)
## Usage
Whole process is  packed as a maven plugin so it is relatively easy to add existing build process.
#### Configuration 
User can  add several car-files for analysis and if they refer to each other, their dependencies are shown.

| Part | Usage 
| --- | --- 
| carFiles |	Specifies car-file location  |
| soapUIFileSet |	Specifies SoapUI-test locations |
### 1. Add following plugin to pom.xml

You have two options:

a) Add as a Maven/Gradle/Ivy dependency to your project. Get the dependency snippet from [here](https://bintray.com/mystes/maven/esbdoc/view).

b) Download it manually from [here](https://github.com/Mystes/esbdoc/releases/latest).

### 2. Use it to generate documentation
Additional parameter information can be added to proxies and sequences with following description fields.

| Field | Usage 
| --- | --- 
| purpose |	Free text field  describing purpose of this proxy/sequence/endpoint  |
| receives | Incoming parameters and whether it is mandatory or optional |
| returns |	outgoing values. They can be saved into contexts or just values like HTTP status code depending upon case |

#### Sample proxy 
Here is a sample of proxy description where you can see how these fields are used. 
```xml
   <description>
        <purpose>
            Public API for managing users.
        </purpose>
        <receives>
            Create
            <field path="$.givenName" optional="false" description="User given name"/>
            <field path="$.sn" optional="false" description="User surname"/>
            <field path="$.mail" optional="false" description="User email"/>
            <field path="$.uniqueId" optional="true" description="User unique ID"/>
            <field path="$.verified" optional="false" description="If user is verified or not"/>

            Update
            <field path="$.givenName" optional="true" description="User given name"/>
            <field path="$.middleName" optional="true" description="User middle name"/>
            <field path="$.sn" optional="true" description="User surname"/>
            <field path="$.mail" optional="true" description="User email"/>
            <field path="$.status" optional="true" description="User status"/>
            <field path="$.uniqueId" optional="true" description="User unique ID"/>

            Remove
            <field path="$.uniqueId" optional="true" description="User unique ID"/>

            Check Email
            <field path="$.email" optional="false" description="Email to check if available"/>
        </receives>
        <returns>
            Response returned by email checker
            <field path="$.status" optional="false" description="success|failure|error"/>
            <field path="$.message" optional="false" description="Result of the email check"/>
        </returns>
    </description>
```

#### Generate documentation usin maven
Notice that  plugin is bound to install phase so it get called after build has created deployable car. But that is not mandatory as long as  analyzable file can be found  somewhere.

```xml
  <build>
      <plugins>
          <plugin>
              <groupId>fi.mystes.esbdoc</groupId>
              <artifactId>esbdoc</artifactId>
              <version>1.0.11</version>
              <executions>
                  <execution>
                      <phase>install</phase>
                      <goals>
                          <goal>generate</goal>
                      </goals>
                  </execution>
              </executions>
              <configuration>
                  <carFiles>
                    <carFile>target/deployment.car</carFile> 
                    <carFile>target/deployment2.car</carFile> 
                  </carFiles>
                 <soapUIFileSet>
                      <includes>
                          <include>**/test/soapui/*-soapui-project.xml</include>
                          <include>**/soapUI/*-soapui-project.xml</include> 
                      </includes>
                 </soapUIFileSet>
              </configuration>
          </plugin>
      </plugins>
  </build>
```

Car-files and soapUiFileSet accept standard maven filePatterns as parameters.

#### Sample generating only docs
SoapUI-test part can be also empty so it is not added to generated graph.
```xml
  <build>
      <plugins>
          <plugin>
              <groupId>fi.mystes.esbdoc</groupId>
              <artifactId>esbdoc</artifactId>
              <version>1.0.11</version>
              <executions>
                  <execution>
                      <phase>install</phase>
                      <goals>
                          <goal>generate</goal>
                      </goals>
                  </execution>
              </executions>
              <configuration>
                  <carFiles>
                    <carFile>target/deployment.car</carFile> 
                  </carFiles>
                 <soapUIFileSet/>
              </configuration>
          </plugin>
      </plugins>
  </build>
```
### 3. Result
Run build
```
     mvn clean install
```

Navigate browser to file://path_to_buil/target/UI/index.html
  
Documentation can be published to some store if needed. Just copy contents of the UI-directory to the appropriate location.

## Technical Requirements

#### Usage

* Oracle Java 8 and later
* WSO2 ESB
    * ESBDoc has been tested with WSO2 ESB versions 4.7.0 - 5.0.0 generated car-files.

#### Development

* All above + Maven 3.0.X

## [License](LICENSE.txt)

Copyright &copy; 2018 [Mystes Oy](http://www.mystes.fi). Licensed under the [Apache 2.0 License](LICENSE.txt).
