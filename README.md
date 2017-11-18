# configuration-client
Externalize configuration
How do you make your service agnostic of the environment where it runs? 
E.g. connection details for QA and Prod environments will be different. 
Spring Boot provides easier way to externalize such configuration via a centralized service that serves as a configuration server.
The framework ingests the configuration values using @Value annotations. 
The values can be defaulted in case parameter is not available.
The configuration can be refreshed in the runtime without having to redeploy or even restart the application.
In this example we will see how to build microservices with externalized configuration.
The application processes JSON data sent by client and uses configuration to decide the persistent store. 
There are two options - RDBMS and CACHE. The service stores the JSON data either in MySQL (RDBMS) or Redis (CACHE).
