# API Hub Frontend

This service provides a frontend for the API Hub.

For more information on the project please visit this space in Confluence:
https://confluence.tools.tax.service.gov.uk/display/AH/The+API+Hub+Home

## Summary

This service provides the following functionality:
- Registration of applications and their team members
- Request mechanism for production scopes and credentials
- Approval of production scope requests
- Support of applications

## Requirements

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.

## Dependencies
Beyond the typical HMRC Digital platform dependencies this service relies on:
- api-hub-applications
- MongoDb

The full set of dependencies can be started using Service Manager and the group API_HUB_ALL.

You can view service dependencies using the Tax Catalogue's Service Relationships 
section here:
https://catalogue.tax.service.gov.uk/service/api-hub-frontend

### api-hub-applications
The api-hub-applications microservice is the backend component of The API Hub.

This service authenticates to api-hub-applications using internal-auth's service-to-service
auth pattern. See this GitHub repo 
for more information:
https://github.com/hmrc/internal-auth

Configuration for service-to-service auth is here:
https://github.com/hmrc/internal-auth-config

To configure authentication modify this configuration setting in `application.conf`
- `internal-auth.token`

### MongoDb
This service uses MongoDb to persist session-level information. As a scaffold-based frontend
this service stores the user's answers to a series of questions that later form the data used
to register an application. 

The MongoDb version should be 4.2 or 4.4 and is constrained by the wider platform not this service.

- Database: api-hub-frontend
- Collection: user-answers

## Using the service

### Running the application

To run the application use `sbt run` to start the service. All local dependencies should be running first.

Once everything is up and running you can access the application at

```
http://localhost:9000/api-hub
```

### Authentication
The service can authenticate users via LDAP or Stride. Stride login will only work on a
Stride machine.

Any attempt to access the service while unauthenticated will result in redirection to a 
page that presents the user with a choice to login via LDAP or Stride. 

More information on how to login is available on Confluence:
https://confluence.tools.tax.service.gov.uk/display/AH/Login+Examples

More information on the security model of The API Hub is on confluence:
https://confluence.tools.tax.service.gov.uk/display/AH/Permissions+Matrix+and+Mappings

#### LDAP
The service uses internal-auth LDAP authentication. This is stubbed at various
times depending on the environment. We have two possible stubbings:
* internal-auth-frontend has a stub that will run locally only
* our own stub hosted on api-hub-frontend which can run locally and in all deployed non-production environments

Locally the service is configured to use:
* our own stub when running in dev mode (`sbt run`)
* the internal-auth-frontend stub otherwise (`sbt start` or running within Service Manager)

The configuration for local internal-auth-frontend is in `application.conf`.

The configuration for our own stub in dev mode is in the `PlayKeys.devSettings`
section of `build.sbt`.

#### Stride
The service uses a stub for Stride login in all environments except production.

## Building the service
This service can be built on the command line using sbt.
```
sbt compile
```

### Unit tests
This microservice has many unit tests that can be run from the command line:
```
sbt test
```

### Integration tests
This microservice has some integration tests that can be run from the command line:
```
sbt it/test
```

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
