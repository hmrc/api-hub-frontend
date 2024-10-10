# Investigate how to support a configurable number of environments in The Hub
[Jira ticket](https://jira.tools.tax.service.gov.uk/browse/HIPP-1577)

## Environment names
We are using these at the moment:
* Primary
* Secondary

We need to stop that as it would get confusing. Simply use the environment names.

New environment names:
* Production
* PreProduction
* Test
* Development

Old to new environment name mapping:
* Primary -> Production
* Secondary -> Test

The spike has done some limited renaming. Completely changing "primary" to "production" etc is a purely mechanical 
process we know we can perform. There is a little gain in actually doing that in a spike branch.

Full renaming should probably happen almost as a first step, even if just to remove the "development" naming of what
is the Test environment. We'll have confusion adding the actual Development environment if we don't do that. 

## Data model
Should we have fixed or variable structure?
* We have bad normalisation at present with a repeating data structure
* Fixed structure means four environments in all environments
* Fixed might require less data migration

Fixed structure is this:

```json
{
  "environments": {
    "primary": {
      "scopes": [],
      "credentials": []
    },
    "secondary": {
      "scopes": [],
      "credentials": []
    }
  }
}
```

Variable (normalised) structure is this:

```json
{
  "environments": [
    {
      "name": "production",
      "scopes": [],
      "credentials": []
    },
    {
      "name": "test",
      "scopes": [],
      "credentials": []
    }
  ]
}
```

The spike has assumed a normalised data model, just to see if that can be done and whether this causes issues. We know
a static structure works already. This does not represent a decision/recommendation. 

Can we abstract code from structure via lens methods?
* This is done already in most places but not all
* The spike attempts to do more of this and appears to work well

We will at some point need a data migration for production to add environments.
* Would this include adding credentials and scopes to new environments?

## Configuration
What does our configuration look like?
* We can go simple and configure environments on or off
  * More code (eg one tab per environment) but simpler 
* We can go complicated and fully-data driven
  * Less code (eg one tab repeated) but more complex

This spike is going to use the first option. There will probably be some configuration other than simply on/off.

This spike will have configuration for four environments with two turned off (as we're working locally). This will
use the current environments of Production and Test.

See `config.Environments` for the implementation of config. This is used on the Environments and Credentials page to
control which environment tabs are displayed. 

```
Environments {
  production {
    on: true
  },
  preProduction {
    on: false
  },
  test {
    on: true
  },
  development {
    on: false
  },
  deployTo: test
}
```

## Live service
This is a live service so how can we evolve toward environment parity while maintaining a path to production for other 
work?

## Widgets
We have a few widgets that are re-used within the service. They will need updating to cover four environments.

A couple of examples follow.

### ApplicationApiBuilder
This is a frontend component that stitches together data from:
* An enriched application
* Its selected APIs
* Its access requests

It then tells us useful things such as:
* Endpoint availability per environment
* Whether the application has pending access requests
* Whether a selected API is missing 

This drives UI behaviour on multiple pages.

Questions:
* Does this component still stitch together all the information we require?
* Is this information needed for all four environments?

### ApplicationEnrichers
A container of backend helper functions that typically:
1. Call IDMS
2. Manipulate an application's model with the results

We'll need to make sure this works for four environments. There are some environment-specific rules.

### ScopeFixer
This Backend component is called when adding/removing endpoints to grant and revoke scopes.
* Scopes can only be added to Test
* Scopes can be revoked from Test and Production

Questions:
* Should we automatically grant scopes in all "on" non-production environments?
* Should we revoke scopes from all "on" environments?

## APIM Connectors
In the backend we have several connectors that speak to APIM. Most of them accept an environment parameter and should 
need little to no change.

The configuration should change to match the new environment names and add the additional environments too. We might
need some thought around configuration for unused environments. Hopefully just configuring an environment off means
its remaining configuration is ignored.

There are some methods that specify environments in their names, for example:
* validateInPrimary
* deployToSecondary

These are on the whole V2 journeys that we expect to work on further. We should try and make them configuration driven
through. So `validateInPrimary` should be something like `validateOas` and take an environment parameter. The value
of that parameter should come from configuration.

```
Environments {
  production {
    on: true
  },
  ...,
  deployTo: test,
  validateOasIn: production
}
```

## Journeys
Analysis of all user journeys based on backend service methods.

### approveAccessRequest
This grants scopes in Production.

Presumably no change if scopes are granted automatically in lower environments.

### addApi
This adds either a new selected API or adds/removes endpoints for an already selected API.

When we add/remove endpoints for an API we grant scopes in Test and revoke scopes in both Test and Production.

Questions:
* Should we grant scopes in all non-production "on" environments?
* Should we revoke scopes in all "on" environments?

### removeApi
When we remove an API we revoke scopes from Test and Production.

Questions:
* Should we revoke scopes from all "on" environments?

### addCredential
This adds a credential to a specified environment and copies all scopes from the "master" credential for that
environment.

Questions:
* This should work as is but in all four environments?

### deleteCredential
Deletes a specified credential from a specified environment. All scopes granted to the credential are implicitly
revoked within APIM.

Questions:
* This should work as is but in all four environments?

### registerApplication
When an application is registered we create credentials in Test and Production. The Production credential is hidden from
the user. This hidden credential is used to grant scopes in the production environment before the user creates a known
production credential.

Questions:
* We should create credentials in all "on" environments?
* We retain the hidden production credential?

### deleteApplication
When we delete an application we also call IDMS to delete credentials from Test and Production.
* Deleting a credential implicitly removes scopes granted to it.

There is a "soft delete" rule that only soft deletes that have production access requests.

Questions:
* Should we delete credentials in all "on" environments?

### findById (application)
This call has the option to enrich with information from Test and Production IDMS.

Questions:
* Should the backend fetch IDMS information for each "on" environment?
  * Note this may impact performance in production, especially if two calls have to go via eBridge

### deployToSecondary
This creates a new API in Test from the "generate" page.

Questions:
* Which environment do we create an API in?

### redeployToSecondary
This updates an API in test from the "update" page.

Questions:
* Which environment do we update an API in?

### getDeployment
This fetches the deployed status of an API in a specified environment.

Questions:
* This should work as is but in all four environments?

### getDeploymentDetails
This fetches an API's meta-data from Test for use on the "update" page.

Questions:
* Which environment do we update an API in?

### promoteToProduction
This promotes the version of an API in Test to Production.

Questions:
* What are the valid combinations or "from" and "to" environments when promoting an API?

### apisInProduction
This is used on the new HUB Stats page.

Questions:
* Are we still only interested in production?

### No change
The following journeys have no change:
* createAccessRequest
* getAccessRequests
* getAccessRequest
* rejectAccessRequest
* rejectAccessRequest
* changeOwningTeam (application)
* removeOwningTeamFromApplication
* addTeamMember (application)
* findAll (applications)
* findAllUsingApi (applications)
* findByTeamId (applications)
* updateApiTeam
* removeOwningTeamFromApi
* create (team)
* findAll (teams)
* findById (team)
* findByName (team)
* addTeamMember (team)
* removeTeamMember (team)
* renameTeam
* getUniqueEmails
