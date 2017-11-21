# Externalize configuration
How do you make your service agnostic of the environment where it runs? 
E.g. connection details for QA and Prod environments will be different. 
Spring Boot provides easier way to externalize such configuration via a centralized service that serves as a configuration server.
The framework ingests the configuration values using @Value annotations. 
The values can be defaulted in case parameter is not available.
The configuration can be refreshed in the runtime without having to redeploy or even restart the application.
In this example we will see how to build microservices with externalized configuration.
The application processes JSON data sent by client and uses configuration to decide the persistent store. 
There are two options - RDBMS and CACHE. The service stores the JSON data either in MySQL (RDBMS) or Redis (CACHE).

# Configuration Server
*pom.xml*

Make sure you have following dependency 
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-config-server</artifactId>
</dependency>
Ideally this should be good enough but from experience I've learned that the version used in the POM can screw up lot of things without 
proper errors. So ensure you've have the correct versions in place. 
<dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>Camden.SR5</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

If you are new to Spring Boot or Spring Cloud getting used to some of these things will take time. It could be frustrating, but there is no other way to learn. You have to keep trying. Sometimes the IDE you are using also can cause trouble. I prefer STS. But that too can show weird errors. So patience is the key. 

*The appilication configuration files*

Configuration server loads the configuration from the files which are named <application-name>.properties format. The Client application name specified in spring.application.name property needs to match with this. 

*application.properties*

The most important property is the location of the GIT repository where the (client) configuration files reside. You can have a local repository as well. There too you may encouter errors. I had issues with local repository and nowhere it is mentioned that you need to use '.git' in the end for the repository location. If you are using github.com you can just copy the location of the restory and use. 
spring.cloud.config.server.git.uri=https://github.com/msathe-tech/config-server
In development you don't want to deal with security issues so you can just disable some of the checks as shown below - 
management.security.enabled=false
security.basic.enabled=false
I keep these two flags both, in server and client side .properties files. 

*Java class for the server*

The main class doesn't need any code. The opinionated Spring Cloud framework takes care of that. You just need to include @EnableConfigServer and @SpringBootApplication in your main class. 

You can clone the server side code using https://github.com/msathe-tech/configuration-service

# Configuration Client
The configuration client is basically your business service. Don't let the name client bother you, it is just for this demo. It is a client for the configuration service but otherwise your this will be a service for some business function. In this case the client perform two tasks: show a text message stored in the config files on the server and process a JSON and store it either in RDBMS or CACHE depening on the flag in the config file on the server. 

*pom.xml*
The POM will contain all the dependencies that your business service needs. From config client point of view following are the two key depencies that need to be there - 
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

The 'spring-cloud-starter-config' is meant for identifying the service as a config client. The 'spring-boot-starter-actuator' provides health indicators but the important feature of Actuator we are interested in here is the ability to refresh the configuration. Most significant advantage of this architecture is that you don't need to restart the server or the client to take into account updated configuration. If there is a change in configuration just invoke '/refresh' on the client service and it will request new configuration from the server. The client side @Value variables will be refreshed with the updated configuration. 





