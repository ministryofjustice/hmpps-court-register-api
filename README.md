# hmpps-court-register-api
[![repo standards badge](https://img.shields.io/badge/endpoint.svg?&style=flat&logo=github&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-court-register-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-court-register-api "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-court-register-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-court-register-api)
[![Docker Repository on Quay](https://img.shields.io/badge/quay.io-repository-2496ED.svg?logo=docker)](https://quay.io/repository/hmpps/hmpps-court-register-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://court-register-api-dev.hmpps.service.justice.gov.uk/swagger-ui.html)

This is a skeleton project from which to create new kotlin projects from.

# Instructions

If this is a HMPPS project then the project will be created as part of bootstrapping -
see https://github.com/ministryofjustice/dps-project-bootstrap.

## Running tests

### Using test containers
Start Docker making sure no containers are running, execute the following command:
```shell
./gradlew check
```

### Using docker compose

Start Docker, execute the following commands:
```shell
docker compose up -d 
./gradlew check
```
