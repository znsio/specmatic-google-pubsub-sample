# Specmatic Google PubSub Sample

* [Specmatic Website](https://specmatic.in)
* [Specmatic Documenation](https://specmatic.in/documentation.html)

This sample project demonstrates how we can run contract tests against a service which interacts with a Google PubSub instance. 


## Order Service
The service under test is: **Order Service**.  
It listens for messages on a Google PubSub topic called **'place-order'**.  
For every message it receives, it sends a message on a Google PubSub topics: **'process-order'** and **notification**.  
We also have an async api specification defined for the Order Service in the **order_service.yaml** file.  

### Order Service in Production

![Order Service Production Architecture](assets/order_service_prod_architecture.gif)

## Contract Tests
To run contract tests, Specmatic leverages the "named examples" defined in the async api specification of the Order Service.  
**Note**: As of now, contract tests will be generated only if there are named examples defined in the async api specification. 

![Order Service Production Architecture](assets/order_service_contract_test.gif)

Here's the structure of a single contract test:
- Specmatic will use the named example defined for the **'place-order'** topic and publish a message on the **'place-order'** topic.  
  If the example message does not match the message schema of the the **'place-order'** topic, the test will fail.
- Specmatic will wait for the Order Service to process the message and publish a message on the **'process-order'** and **'notification'** topics.
- Specmatic will then assert that :
  - Exactly one message each is received on the **'process-order'** and **'notification'** topics
  - The message received on the **'process-order'** topic is as per the named example defined for the **'process-order'** topic.  
    The message received should match the message schema for the **'process-order'** topic
  - The message received on the **'notification'** topic is as per the named example defined for the **'notification'** topic.  
    The message received should match the message schema for the **'notification'** topic

## Google PubSub project setup

### Use our test Google PubSub project

We have a test Google PubSub project where the three channels: **place-order**, **process-order** and **notification** are already created.
The tests are setup to run using a service account whose key is present in the **keys** directory.

### Use your personal Google PubSub project

If you wish to run the tests against your personal Google PubSub project, make sure that you have the three channels: **'place-order'**, **'process-order'** and **'notification'**  created there.  
You will need to update the projectId constant in the ContractTest class to point to your Google PubSub project:
```kotlin
 private const val projectId = "pub-sub-demo-414308"
```
You would also need to then setup some authentication mechanism using one of the options described [here](https://cloud.google.com/docs/authentication/provide-credentials-adc#how-to).


## Running Tests
You can directly run tests from your IDE from the ContractTest class, or via the shell:
```shell
./gradlew test
```



