# Definition of Done

**Source Quality**
------------------

* Changes are committed to the trunk or main branch of the mono-repo.
* The code, tests, and documentation are free of lint errors.
* All code changes are reviewed by at least one other developer
* Code from pair/mob programming is considered reviewed.
* English is used for all code, tests, and documentation.

**Modular Integrity**
---------------------

* Compiler and runtime warnings are treated as errors.
* All code is fully small tested.
* Code quality is maintained through the resolution or justification of all code smells, by:
  * Eliminating duplicate code.
  * Reducing code elements to the minimum required for intended functionality.
  * Using domain language to clearly convey intent.
  * Removing all unused code, regardless of test coverage.
  * Adding comments for clarity where needed.

**System Integration**
----------------------

* No known defects; all bugs fixed with highest priority
* Automated end-to-end tested with all internal services integrated
* Exploratory testing is performed to ensure the system is usable and reliable.
* All external dependencies are current and verified against the latest stable versions.
* The CI system successfully processes all changes.
* CI system builds are completed in 10 minutes or less.

**Deployment and User Experience**
----------------------------------

* Application Release starts only on increasing immutable `vMAJOR.MINOR.PATCH` tags; confirm exact-commit CI success and artifact availability before tagging. Ordinary main pushes run CI only. Overlapping tags select the highest pending numeric version; after premature tagging, explicitly rerun the release once CI succeeds. Retry a recoverable release with the same immutable tag; when its identity must change or historical artifacts are unrecoverable, test the correction or revert on main and release the next patch as a forward correction, without automatic schema rollback. Follow the [release runbook](../gcp/conditional-backend-deploy.md).
* Less than 10 second downtime while deploying \*
* User data integrity and security are assured at all times.
* The user manual is kept up-to-date, and users are informed of changes.
* Product Owner approval is obtained but does not hinder deployment.

<br>

\* Currently, there is only one active service under the load balancer in our production GCP. So less than 10 seconds downtime is expected while deploying.
