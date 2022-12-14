# API Hub Frontend

This service provides a frontend for the API Hub. 

## Summary

This service provides the following functionality:

* tbd

## Requirements

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.

## Run the application

To run the application use `sbt run` to start the service. All local dependencies should be running first:
* MongoDb
* The API_HUB_ALL Service Manager group

Once everything is up and running you can access the application at

```
http://localhost:9000/api-hub
```

## Unit tests
```
sbt test
```

## Integration tests
```
sbt it:test
```

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
