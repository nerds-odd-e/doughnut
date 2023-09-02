# Definition of Done

## External Quality

- Zero downtime during deployment
- Guaranteed user data integrity and security, at all times
- No known defects; all bugs fixed with highest priority
- User manual updated, and users notified
- Approved by the Product Owner (does not block deployment)

## Internal Quality

- No lint errors
- Treat compiler or runtime warnings as errors
- All code smells either resolved or justified
  - Eliminate code duplication
  - Minimize code elements for functionality
  - Use domain-specific names to reveal intent
  - No dead code, even if accompanied by unit tests
  - Comment where necessary
- End-to-end tested with all services integrated
- Complete unit testing for all code
- All external dependencies up-to-date and tested against latest stable versions
- Use English for code, tests, and documentation

## CI/CD System

- All changes checked into the mono-repo on the `main` (or `trunk`) branch
- Pass CI system
- Automatic production deployment on every successful CI build
- CI build completion within 10 minutes
- Any existing automation must have accompanying 'autonomation' to halt the CI system if assumptions for the automation are invalidated
