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

* bootstrap.properties *

The resources folder of the applicatio project needs to include bootstrap.properties file. This is similar to application.properties file but it is loaded before the application.properties. From the docs - "A Spring Cloud application operates by creating a "bootstrap" context, which is a parent context for the main application. Out of the box it is responsible for loading configuration properties from the external sources, and also decrypting properties in the local external configuration files." 
Instead of .properties you can also use .yml files. 

spring.cloud.config.uri=<URL of the config service>
spring.application.name=<your app name, should match with .properties you created on config server>
management.security.enabled=false
security.basic.enabled=false

* Application code 

The client which is basically your business service needs to include few annotations other than your code for business logic. 
@RefreshScope - used to refresh the configuration in runtime, you can invoke /refresh on the client service to pull new configuration from the config server

@Value("${store:RDBMS}")
    private String store;
@Value("${message:Hello default}")
    private String message;
    
The @Value matches the key in the <application>.properties files to the variable and stores the value. If the key is not found in the properties file the value will be defaulted to :<value>. The default value is important to avoid runtime exceptions if the server is unable to find the key. It can also be used to progressively add dynamic configuration to the file. 

There is a lot more in this code, but I'll update that information in stages. 
For now you can refer to the client code here - https://github.com/msathe-tech/configuration-client

# Processing JSON 

This service accepts JSON payload using -
 @ResponseStatus(code=HttpStatus.CREATED)
    @PostMapping(value="/add-albums")
    public String addAlbums(@RequestBody ArrayList<Album> list) {
    .....
    
The @RequestBody takes care of mapping the HTTP request's JSON payload to the Array List of Album class. 
Sample JSON payload is given below -

[
   {
        "artist": "Ajay-Atul",
        "title": "Sairat",
        "releaseYear": "2015",
        "genre": "Bollywood"
    },
    {
        "artist": "Ed Sheeran",
        "title": "Divide",
        "releaseYear": "2017",
        "genre": "Pop"
    }
 ]

The HTTP request should have following header to make it a JSON payload.

Content-Type: application/json

# Using JPA and MySQL

Based on the value of 'store' it will either store the data in MySQL or Redis cache. Spring Boot really makes it easy to use JPA using CrudRepository interface. You don't need to write a single line of code to perform basic DB operations. 
public interface AlbumRepository extends CrudRepository<Album, String> {
}

You need following dependencies in the pom.xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
  <groupId>mysql</groupId>
  <artifactId>mysql-connector-java</artifactId>
  <scope>runtime</scope>
</dependency>
    
Make sure you have MySQL service created on the CF. You also need to bind the service to your application. The application manifest can have services: <MySQL service name>

Binding the service with the application ensures the connection details are passed on to the application. This makes the application location independent. If you move your code from dev to test to prod you don't need to worry about changing the connection URL. The CF will take care of passing the appropriate connection details to the app. 

# Using Redis Cache
Using Redis cache requires little more code than using JPA and MySQL. The CF maintains the environment information including that of the bound services in "VCAP_SERVICES". 'VCAP_SERVICES' environment variable is passed on to the application and can be accssed using System.getenv(). The String is actually a JSON data that you need to parse to retrive host, port and credentials of the Redis service. 
Take a look at the code to see how it is done. 
*Disclaimer: this is not the most efficient JSON parsing code, but you will be kind enough to forgive me for that :)

You need to create an instance of JedisPool as shown below 

pool = new JedisPool(new JedisPoolConfig(),
        	                credentials.getString("hostname"),
        	                Integer.parseInt(credentials.getString("port")),
        	                Protocol.DEFAULT_TIMEOUT,
        	                credentials.getString("password")); 
                          
                          
Then get a Jedis resource 

Jedis jedis = pool.getResource();
And then add Album to the jedis as key, value pair

savedAlbum = jedis.set(newAlbum.getTitle(), newAlbum.toString());


To count the size of the Jedis cache and access all the data in the cache you can use
Set<String> keys = jedis.keys("*");
You can process the all the Jedis objects using Java Lambda streams to improve performance. 

# Deploy application in the Cloud 
You need to have account on PCF web services or have your own PCF setup where you can deploy the applications. The PCF takes care of containerizing the application, creating public routes, etc. There is a lot more to PCF, but some other time. 
Following are the steps to compile and deploy the application on the Pivotal Cloud Foundry
1. mvn clean package
2. Make sure the build is clean and the 'target' folder contains the application JAR
3. Create a manifest.yml and include key parameters such as name, memory, instances, host, path, services. Refer to the code for sample
4. cf login -a https://api.run.pivotal.io 
5. Go to the application folder that has manifest.yml
6. cf push

That's it for now.
Good luck for building Cloud Native applications!

Cheers!


