# Internal quality

- No “code smells” or lint  errors
- Treat warnings as errors
- Unit tested (all types of code)
- End-to-end tested with all services integrated
- No dead code (code that is not useful “yet,” even if it comes with UT)
- use English in code, test, and documents (translation can be kept, but the original need to be in English)

# CI/CD

- Any local changes need to be checked-in to “main” (“trunk”) within an hour, or as frequently as possible
- Running and passing in CI System
- Deployed to production
- A successful build should be deployed to production in less than 10 mins

# External quality

- No downtime during deployment
- No known defects (Bugs should be fixed with the highest priority)
- Accepted by the Product Owner (this doesn't block deployment)
