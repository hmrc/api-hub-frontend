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

## Data model
Should we have fixed or variable structure?
* Bad normalisation
* Fixed means four environments everywhere

Can we abstract code from structure via lenses?
* Done in some places but maybe not all?

We will at some point need a data migration for production to add environments.
* Would this include credentials and scopes?

## Configuration
What does our configuration look like?
* We can go simple and configure environments on or off
  * More code (eg one tab per environment) but simpler 
* We can go complicated and fully-data driven
  * Less code (eg one tab repeated) but more complex

This spike is going to use the first option. There will probably be some configuration other than simply on/off.

This spike will have configuration for four environments with two turned off (as we're working locally). This will
use the current environments of Production and Test.

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
This is a live service so how can we evolve to environment parity while maintaining a path to production?

## Backend endpoints
Analysis of backend endpoints called from the frontend and whether any change might be necessary.

### registerApplication
When an application is registered we create credentials in Test and Production. The Production credential is hidden from 
the user.

Should we add credentials for all "on" environments?

### getApplication
This call has the option to enrich with information from Test and Production IDMS.

Should the backend fetch IDMS information for each "on" environment?
* Note this may impact performance in production, especially if two calls have to go via eBridge 

### deleteApplication
When we delete an application we also call IDMS to delete credentials from Test and Production.
* Deleting a credential implicitly removes scopes granted to it. 

The backend should delete credentials in all "on" environments.

### addApi
When we add/remove endpoints for an API we grant scopes in Test and revoke scopes in both Test and Production. 

Should we grant in all non-production "on" environments?

Should we revoke in all "on" environments?

### removeApi
When we remove an API we revoke scopes from Test and Production.

Should we revoke scopes from all "on" environments?

### addCredential
This specifies the environment to add the credential to.

Make sure this works for all four environments.

### deleteCredential
This specifies the environment to delete the credential from.

Make sure this works for all four environments.

### approveAccessRequest
This grants scopes in Production.

Presumably no change if scopes are granted automatically in lower environments.

### generateDeployment
This creates a new API in Test.

We will need some configuration telling us which environment to create APIs in.

### updateDeployment
This updates an API in Test.

We will need some configuration telling us which environment to update APIs in.

### promoteToProduction
Promotes an API from Test to Production.

We need the API lifecycle to know what changes to make. We will then probably need configuration to specify which 
environments can promote and which environments they can promote to.

### getApiDeploymentStatuses
Gets an API's deployment status from Test and Production.

Should this fetch the status in each "on" environment?

### apisInProduction
Calls APIM Production to get the list of APIs deployed there.

Presumably no change.

### No call to APIM
The following endpoints do not result in any call to APIM and are presumably environment agnostic.
* getApplications
* getApplicationsUsingApi
* getApplicationsByTeam
* updateApplicationTeam
* removeApplicationTeam
* createAccessRequest
* getAccessRequests
* getAccessRequest
* rejectAccessRequest
* addTeamMember
* findTeamById
* findTeamByName
* findTeams
* createTeam
* addTeamMemberToTeam
* removeTeamMemberFromTeam
* changeTeamName
* getUserContactDetails
* updateApiTeam
* removeApiTeam
