# WSO2 ESB Dpendency Document generator /ESBDoc)
![Build status](https://circleci.com/gh/Mystes/wso2-esb-vfs-mediator.svg?style=shield&circle-token=1d26db62821d6a3f03e9780657db6af6757e4fd2)
## What is WSO2 ESB?
[WSO2 ESB](http://wso2.com/products/enterprise-service-bus/) is an open source Enterprise Service Bus that enables interoperability among various heterogeneous systems and business applications.

## ESBDoc features
ESBDoc reads WSO2 ESB deployment car, analyzes it and generate dependency graphs from endpoints, proxies and sequences. Implmentation is Maven plugin
which can be added as part of the build process.
Resulting graph can be navigated with HTML-UI.
 


#### File system specific options
These options are not specified in mediator configuration as they are applicable to certain file systems only. Instead, they are configured using properties synapse message context ($ctx) scope. The properties must be applied before VFS mediator is used.

| Option | File system | Property name | Allowed values | Example |
| --- | --- | --- | --- | --- |
| Enable passive mode |	FTP |	vfs.ftp.passiveMode	| true/false (Default: false) | ```<property name="vfs.ftp.passiveMode" value="true"/>``` |

## Usage

### 1. Get the WSO2 ESB Vfs Mediator jar

You have two options:

a) Add as a Maven/Gradle/Ivy dependency to your project. Get the dependency snippet from [here](https://bintray.com/mystes/maven/wso2-esb-vfs-mediator/view).

b) Download it manually from [here](https://github.com/Mystes/wso2-esb-vfs-mediator/releases/tag/release-1.0).

### 2. Install the mediator to the ESB
Copy the `VfsMediator-x.y.jar` to `$WSO2_ESB_HOME/repository/components/dropins/`.

### 3. Use it

```xml
<vfs xmlns="http://ws.apache.org/ns/synapse">
    <operation value="copy" />
    <sourceDirectory value="tmp://bar" />
    <targetDirectory value="tmp://blaa" />
</vfs>
```

#### Example with filePattern
```xml
<vfs xmlns="http://ws.apache.org/ns/synapse">
    <operation value="copy" />
    <sourceDirectory value="tmp://bar" />
    <targetDirectory value="tmp://blaa" />
    <filePattern value="filename.txt" />
</vfs>
```
There are more examples under src/test/resources directory.

## Technical Requirements

#### Usage

* Oracle Java 8 and later
* WSO2 ESB
    * ESBDoc has been tested with WSO2 ESB versions 4.7.0 - 5.0.0 generated car-files.

#### Development

* All above + Maven 3.0.X

## [License](LICENSE)

Copyright &copy; 2018 [Mystes Oy](http://www.mystes.fi). Licensed under the [Apache 2.0 License](LICENSE).
