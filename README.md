# restro-search-engine

REST API based Restaurants Search Engine written in Clojure based on Elasticsearch.
The API server exposes APIs to create, update, search, suggest documents using elasticsearch.


## Prerequisites

You will need [Java](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html), [Leiningen](https://leiningen.org/),
[Elasticsearch 5.0 and above](https://www.elastic.co/downloads/past-releases/kibana-5-6-7) to run this project.

## Running

Update the config file located at <a href="resources/configs.clj"> configs.clj </a>

### Using Lein
To start a web server for the application, run:

```
lein run
```

This will start the server on Port 7799. Run below curl command to verify
```
curl http://localhost:7799/ping
```

### Using Java
Build the Jar using below command
```
lein uberjar
```
and then run the following command to start the API server
```
java -jar target/restro-search-engine-0.1.0-SNAPSHOT-standalone.jar
```
Use the curl command mentioned above to verify.

### Using Postman to use APIs
Export the <a href="/Restaurant-Search-Engine.postman_collection.json">Postman Collection</a> to Postman. This collections
includes the sample request for all the APIs.


### Run tests
```
lein test :all
....
Ran 18 tests containing 54 assertions.
0 failures, 0 errors.
```
