# Consumer trace

## Effect terms searched

| Term | Origin |
| --- | --- |
| `backend/src/main/resources/application.yml` | changed path |
| `application.yml` | basename of `backend/src/main/resources/application.yml` |
| `backend/src/test/java/com/odde/donut/configs/OpenAiProfileConfigurationTest.java` | changed path |
| `OpenAiProfileConfigurationTest.java` | basename of `backend/src/test/java/com/odde/donut/configs/OpenAiProfileConfigurationTest.java` |
| `docs/worktree-browser-tests.md` | changed path |
| `worktree-browser-tests.md` | basename of `docs/worktree-browser-tests.md` |
| `spring.openai.token` | supplied |
| `OPENAI_API_TOKEN` | supplied |
| `unset token` | supplied |
| `usingMockedOpenAiService` | supplied |
| `ensurePrivateOpenAiMockReady` | supplied |
| `backend/src/main/resources/application` | import path of `backend/src/main/resources/application.yml` |
| `application` | import path of `backend/src/main/resources/application.yml`; in 242 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `docs/worktree-browser-tests` | import path of `docs/worktree-browser-tests.md` |
| `worktree-browser-tests` | import path of `docs/worktree-browser-tests.md` |
| `backend/src/test/java/com/odde/donut/configs/OpenAiProfileConfigurationTest` | import path of `backend/src/test/java/com/odde/donut/configs/OpenAiProfileConfigurationTest.java` |
| `OpenAiProfileConfigurationTest` | import path of `backend/src/test/java/com/odde/donut/configs/OpenAiProfileConfigurationTest.java` |
| `token` | declared in `backend/src/main/resources/application.yml`; in 382 files, too broad to list consumers by itself; supply it as a `--term` to list them |

## Scope

Whole checkout at `12e1f925a5e6`, including uncommitted and untracked files: 4212 tracked and untracked non-ignored files searched with fixed-string, case-insensitive matching.

Changed files not searched: `backend/src/main/resources/application.yml`, `backend/src/test/java/com/odde/donut/configs/OpenAiProfileConfigurationTest.java`, `docs/worktree-browser-tests.md`.

## Located consumers

Kinds are path hints for inspection; each file appears once. Fill every Disposition with `run`, `reused` or `excluded: <reason the effect does not reach its boundary>`.

### Production

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.github/workflows/ci.yml` | `OPENAI_API_TOKEN` | 26: `OPENAI_API_TOKEN: ${{ secrets.OPENAI_API_TOKEN }}` |reused: supplied-token ConfigData case proves exact precedence for caller input, without real network credentials or paid calls. |
| `backend/src/main/java/com/odde/donut/configs/OpenAiApiConfig.java` | `spring.openai.token` | 33: `@Value("${spring.openai.token}") String openAiToken,` |run: production-YAML configuration contract and two real mocked Cypress journeys observe the SDK credential precondition and handler route; no key is injected by the test profile. |
| `backend/src/main/java/com/odde/donut/services/openAiApis/OpenAiApiHandler.java` | `spring.openai.token` | 47: `@Value("${spring.openai.token}") String defaultOpenAiToken) {` |run: production-YAML configuration contract and two real mocked Cypress journeys observe the SDK credential precondition and handler route; no key is injected by the test profile. |
| `backend/src/main/java/com/odde/donut/services/QuestionGenerationBatchAdminStatusService.java` | `spring.openai.token` | 31: `@Value("${spring.openai.token}") String openAiToken,` |reused: getStatus checks the injected value for nonblank; configuration contract observes E2E fallback and dev empty, full backend suite retains configured/missing status semantics. The prod-only maintenance job is not activated in E2E. |
| `backend/src/main/resources/application-prod.yml` | `OPENAI_API_TOKEN` | 67: `token: ${OPENAI_API_TOKEN}` |excluded: Production selects its existing token configuration; changed placeholder belongs only to the e2e document. |
| `infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh` | `OPENAI_API_TOKEN` | 109: `export OPENAI_API_TOKEN=$(curl "https://secretmanager.googleapis.com/v1/projects/${PROJECTID}/secrets/openai_api_token/versions/latest:access" \` |excluded: Production selects its existing token configuration; changed placeholder belongs only to the e2e document. |
| `scripts/browser-worktree-isolation.mjs` | `docs/worktree-browser-tests.md`, `worktree-browser-tests.md`, `docs/worktree-browser-tests`, `worktree-browser-tests` | 83: `` )} (id). Missing or invalid: ${missing.join(', ')}. See docs/worktree-browser-tests.md.` `` |excluded: guidance link, dated record or unchanged environment allocation diagnostic; no parsing or runtime dependency on the E2E token default. |
| `scripts/isolated-cypress-spec-selection.mjs` | `usingMockedOpenAiService` | 41: `` * This includes files whose `@usingMockedOpenAiService` tag appears on a `` |excluded: spec/tag selection and mock provisioning use unchanged registry/ownership contracts; these stand-ins do not load the application credential. Real selected journeys observe the shared resulting route. |
| `scripts/sut-start-spawn.mjs` | `OPENAI_API_TOKEN` | 24: `` * shell's `OPENAI_API_TOKEN` is withheld so they cannot make paid OpenAI calls. `` |run: full hold suite observes child environment withholding or retaining synthetic token; configuration contract observes the consuming E2E fallback and supplied-token precedence. |

### Test

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `backend/src/test/resources/application.properties` | `spring.openai.token` | 7: `spring.openai.token=test_token_for_openai` |run: full backend suite preserves existing test credential; new configuration contract loads explicit production YAML with isolated inputs, bypassing this override. |
| `scripts/isolated-cypress-openai-mock.test.mjs` | `usingMockedOpenAiService` | 205: `` // These files carry `@usingMockedOpenAiService` on their scenarios, not on `` |excluded: spec/tag selection and mock provisioning use unchanged registry/ownership contracts; these stand-ins do not load the application credential. Real selected journeys observe the shared resulting route. |
| `scripts/isolated-cypress.test.mjs` | `usingMockedOpenAiService` | 37: `'@usingMockedOpenAiService',` |excluded: spec/tag selection and mock provisioning use unchanged registry/ownership contracts; these stand-ins do not load the application credential. Real selected journeys observe the shared resulting route. |

### Test support or stand-in

none found

### Journey or feature

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.agents/skills/e2e-authoring/SKILL.md` | `docs/worktree-browser-tests.md`, `worktree-browser-tests.md`, `usingMockedOpenAiService`, `docs/worktree-browser-tests`, `worktree-browser-tests` | 23: `` `http://127.0.0.1:<lbListenPort>` — see `docs/worktree-browser-tests.md`. Isolated `` |excluded: guidance link, dated record or unchanged environment allocation diagnostic; no parsing or runtime dependency on the E2E token default. |
| `e2e_test/config/notebookPublicationProfile.ts` | `backend/src/main/resources/application.yml`, `application.yml`, `backend/src/main/resources/application` | 237: `join(repoRoot, 'backend/src/main/resources/application.yml'),` |excluded: captures the entire YAML as loggingConfiguration provenance; it neither parses the token nor launches credential-consuming behavior. |
| `e2e_test/features/ai_generated_content/note_content_completion.feature` | `usingMockedOpenAiService` | 1: `@usingMockedOpenAiService` |reused: same official-client credential default and tagged private-mock route as the two selected real journeys; their credential precondition is equivalent. Feature-specific outputs do not depend on the placeholder value. |
| `e2e_test/features/ai_generated_recall_questions/question_contest.feature` | `usingMockedOpenAiService` | 1: `@usingMockedOpenAiService` |run: selected real clean-shell Cypress batch, no token or feature-enabling exports. |
| `e2e_test/features/assimilation/note_refinement.feature` | `usingMockedOpenAiService` | 1: `@usingMockedOpenAiService` |run: selected real clean-shell Cypress batch, no token or feature-enabling exports. |
| `e2e_test/features/bazaar/bazaar_subscription.feature` | `usingMockedOpenAiService` | 1: `@usingMockedOpenAiService` |reused: same official-client credential default and tagged private-mock route as the two selected real journeys; their credential precondition is equivalent. Feature-specific outputs do not depend on the placeholder value. |
| `e2e_test/features/book_reading/ai_reorganize_layout.feature` | `usingMockedOpenAiService` | 1: `@usingMockedOpenAiService` |reused: same official-client credential default and tagged private-mock route as the two selected real journeys; their credential precondition is equivalent. Feature-specific outputs do not depend on the placeholder value. |
| `e2e_test/features/cli/cli_recall.feature` | `usingMockedOpenAiService` | 59: `@usingMockedOpenAiService` |reused: same official-client credential default and tagged private-mock route as the two selected real journeys; their credential precondition is equivalent. Feature-specific outputs do not depend on the placeholder value. |
| `e2e_test/features/messages/conversation_about_a_note.feature` | `usingMockedOpenAiService` | 1: `@usingMockedOpenAiService` |reused: same official-client credential default and tagged private-mock route as the two selected real journeys; their credential precondition is equivalent. Feature-specific outputs do not depend on the placeholder value. |
| `e2e_test/features/note_creation_and_update/mcq_management.feature` | `usingMockedOpenAiService` | 18: `@usingMockedOpenAiService` |reused: same official-client credential default and tagged private-mock route as the two selected real journeys; their credential precondition is equivalent. Feature-specific outputs do not depend on the placeholder value. |
| `e2e_test/features/note_creation_and_update/record_live_audio.feature` | `usingMockedOpenAiService` | 1: `@usingMockedOpenAiService` |reused: same official-client credential default and tagged private-mock route as the two selected real journeys; their credential precondition is equivalent. Feature-specific outputs do not depend on the placeholder value. |
| `e2e_test/features/note_topology/export_highlighted_property.feature` | `usingMockedOpenAiService` | 1: `@mockBrowserTime @usingMockedOpenAiService` |reused: same official-client credential default and tagged private-mock route as the two selected real journeys; their credential precondition is equivalent. Feature-specific outputs do not depend on the placeholder value. |
| `e2e_test/features/recall/ambiguous_question_exclusion.feature` | `usingMockedOpenAiService` | 1: `@usingMockedOpenAiService` |reused: same official-client credential default and tagged private-mock route as the two selected real journeys; their credential precondition is equivalent. Feature-specific outputs do not depend on the placeholder value. |
| `e2e_test/features/recall/property_memory_tracker.feature` | `usingMockedOpenAiService` | 20: `@usingMockedOpenAiService` |reused: same official-client credential default and tagged private-mock route as the two selected real journeys; their credential precondition is equivalent. Feature-specific outputs do not depend on the placeholder value. |
| `e2e_test/features/recall/recall_quiz_ai_question.feature` | `usingMockedOpenAiService` | 1: `@usingMockedOpenAiService` |reused: same official-client credential default and tagged private-mock route as the two selected real journeys; their credential precondition is equivalent. Feature-specific outputs do not depend on the placeholder value. |
| `e2e_test/features/user_admin/manage_ai_models.feature` | `usingMockedOpenAiService` | 1: `@usingMockedOpenAiService` |reused: same official-client credential default and tagged private-mock route as the two selected real journeys; their credential precondition is equivalent. Feature-specific outputs do not depend on the placeholder value. |
| `e2e_test/step_definitions/hook.ts` | `usingMockedOpenAiService`, `ensurePrivateOpenAiMockReady` | 17: `ensurePrivateOpenAiMockReady,` |run: selected real Cypress batch traverses tag ownership preflight, private mock install, scenario endpoint routing and cleanup. |
| `e2e_test/step_definitions/worktreeOpenAiMockIsolation.ts` | `ensurePrivateOpenAiMockReady` | 53: `export function ensurePrivateOpenAiMockReady() {` |run: selected real Cypress batch traverses tag ownership preflight, private mock install, scenario endpoint routing and cleanup. |
| `scripts/e2e-runner-hold.cases.mjs` | `OPENAI_API_TOKEN` | 157: `withEnv(t, 'OPENAI_API_TOKEN', 'shell-openai-token')` |run: full hold suite observes child environment withholding or retaining synthetic token; configuration contract observes the consuming E2E fallback and supplied-token precedence. |
| `scripts/sut-e2e-ports.mjs` | `docs/worktree-browser-tests.md`, `worktree-browser-tests.md`, `docs/worktree-browser-tests`, `worktree-browser-tests` | 83: `` `Missing or invalid: ${missing.join(', ')}. See docs/worktree-browser-tests.md.` `` |excluded: guidance link, dated record or unchanged environment allocation diagnostic; no parsing or runtime dependency on the E2E token default. |

### Guidance reader

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.agents/agent-map.md` | `docs/worktree-browser-tests.md`, `worktree-browser-tests.md`, `docs/worktree-browser-tests`, `worktree-browser-tests` | 96: `` `pnpm e2e:hold` ([`docs/worktree-browser-tests.md`](../docs/worktree-browser-tests.md)): `` |excluded: guidance link, dated record or unchanged environment allocation diagnostic; no parsing or runtime dependency on the E2E token default. |
| `.planning/NORTH-STAR.md` | `usingMockedOpenAiService` | 40: `` Every other audio end-to-end test uses `@usingMockedOpenAiService`. Prove new `` |excluded: guidance link, dated record or unchanged environment allocation diagnostic; no parsing or runtime dependency on the E2E token default. |
| `.planning/seeds/SEED-116-agent-shell-independent-commands.md` | `OPENAI_API_TOKEN` | 22: `` `NIX_CONFIG` or `OPENAI_API_TOKEN` by hand. Routine setup and verification `` |excluded: guidance link, dated record or unchanged environment allocation diagnostic; no parsing or runtime dependency on the E2E token default. |
| `.planning/slice-plans/091-run-repository-commands-without-shell-setup/consumer-trace-nix.md` | `docs/worktree-browser-tests.md`, `worktree-browser-tests.md`, `docs/worktree-browser-tests`, `worktree-browser-tests` | 181: `` \| `docs/worktree-browser-tests.md` \| `Nix` \| 52: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/worktree_note_ed… `` |excluded: guidance link, dated record or unchanged environment allocation diagnostic; no parsing or runtime dependency on the E2E token default. |
| `.planning/slice-plans/091-run-repository-commands-without-shell-setup/PLAN.md` | `backend/src/main/resources/application.yml`, `application.yml`, `spring.openai.token`, `OPENAI_API_TOKEN`, `unset token`, `backend/src/main/resources/application` | 29: `` `backend/src/main/resources/application.yml`: its `` |excluded: guidance link, dated record or unchanged environment allocation diagnostic; no parsing or runtime dependency on the E2E token default. |
| `.planning/slice-plans/091-run-repository-commands-without-shell-setup/source-review.md` | `OPENAI_API_TOKEN`, `unset token` | 12: `` \| C04 \| Mocked-OpenAI E2E features run without hand-exporting `OPENAI_API_TOKEN`. \| slice 2 \| \| `` |excluded: guidance link, dated record or unchanged environment allocation diagnostic; no parsing or runtime dependency on the E2E token default. |
| `docs/development-setup.md` | `worktree-browser-tests.md`, `worktree-browser-tests` | 66: `` [`worktree-browser-tests.md`](worktree-browser-tests.md)). A configured `` |excluded: guidance link, dated record or unchanged environment allocation diagnostic; no parsing or runtime dependency on the E2E token default. |
| `docs/gcp/prod_env.md` | `worktree-browser-tests.md`, `worktree-browser-tests` | 116: `` [`worktree-browser-tests.md`](../worktree-browser-tests.md). Isolated start does `` |excluded: guidance link, dated record or unchanged environment allocation diagnostic; no parsing or runtime dependency on the E2E token default. |
| `docs/notebook-git-attachments.md` | `backend/src/main/resources/application.yml`, `application.yml`, `backend/src/main/resources/application` | 150: `` in `backend/src/main/resources/application.yml`, and an oversize upload gets `` |excluded: guidance link, dated record or unchanged environment allocation diagnostic; no parsing or runtime dependency on the E2E token default. |
| `docs/worktree-retire-databases.md` | `docs/worktree-browser-tests.md`, `worktree-browser-tests.md`, `docs/worktree-browser-tests`, `worktree-browser-tests` | 9: `` [`docs/worktree-browser-tests.md`](worktree-browser-tests.md). `` |excluded: guidance link, dated record or unchanged environment allocation diagnostic; no parsing or runtime dependency on the E2E token default. |
| `DonutRetrospectiveFindings.md` | `OPENAI_API_TOKEN` | 54: `` The coordinator's and agents' non-interactive shells do not load `~/.zshrc`, which exports `NIX_CONFIG` (flakes) and `OPENAI_API_TOKEN`. `./scripts/run.sh` ref… `` |excluded: guidance link, dated record or unchanged environment allocation diagnostic; no parsing or runtime dependency on the E2E token default. |

## Observed proof and limits

- `./scripts/run.sh pnpm backend:test_only`: corrected full run exit 0, migration build 44s plus test build 5m6s. XML totals: 3,093 tests, zero failures/errors, two existing skips. `OpenAiProfileConfigurationTest.resolvesProfileCredential` observes E2E unset -> non-secret fallback, E2E supplied synthetic token -> exact supplied value, Development unset -> empty. It removes host/system property sources and loads the actual source YAML by absolute file URI with sibling DB imports, avoiding the test application.properties credential. No extra application context.
- Initial full run exit 1: only the three new contract cases failed; classpath loader setup resolved relative db-test.properties from the wrong location. Files existed beside production YAML, not at test working-directory root. Changing to the source file URI repaired that setup; no assertion/default was weakened. Initial log `/tmp/seed116-backend-initial.log`, failing XML `/tmp/seed116-initial-configuration-test.xml`; corrected log `/tmp/seed116-backend-corrected.log`. The initial failed verdict is retained, and final acceptance rests on the corrected full run.
- `./scripts/run.sh node --test scripts/e2e-runner-hold.cases.mjs`: all six cases passed. Token cases record real spawn environment through an external process stand-in, synthetic caller token withheld ordinarily and retained with paidOpenAi. Pair with the production ConfigData cases for consuming application resolution. No paid calls.
- `env -i HOME="$HOME" PATH="$PATH" zsh -c './scripts/run.sh pnpm cy:run --spec e2e_test/features/assimilation/note_refinement.feature,e2e_test/features/ai_generated_recall_questions/question_contest.feature'`: exit 0, seven of seven scenarios, 64s test phase. The exact env-i shell's boolean-only preflight observed both OPENAI_API_TOKEN and NIX_CONFIG unset. Refinement checks resulting content, extracted/edited notes and retry; contest checks preserved or replaced question stems. Existing tags choose the invocation-owned private mock. Log `/tmp/seed116-e2e.log`.
- This checkout's canonical runner-selected identity is wt_cdccb6067832479a8154ccb6463bddfd; Unit Test database doughnut_wt_cdccb6067832479a8154ccb6463bddfd_test, E2E database doughnut_e2e_wt_cdccb6067832479a8154ccb6463bddfd, browser origin http://127.0.0.1:52338. No peer or persistent Development data touched. After terminal E2E return, owner lock and exact owned control socket/directory were gone. A post-return `lsof -nP -iTCP:<port> -sTCP:LISTEN` reading found no listeners on owned application ports 52336, 52337 and 52338 (each exit 1). sut.log did not exist before this invocation; its new 43,563-byte log contained the startup and no credential-precondition error. Supporting diagnosis, not a permanent absence test. Cleanup observation `/tmp/seed116-e2e-cleanup.json`.

Expensive-proof decision: local Territory macOS, 16 CPUs / 48 GiB. At 19:57 high load and active Playwright burst led to running only lightweight held cases and inspection first; at 19:58 current CPU activity and 11.5 GiB inactive memory supported one full backend run. After the diagnosed loader setup failure, at 20:05 load was 23 with 4 GiB free plus 11 GiB inactive, so one corrected full run was selected. At 20:12 load was 21 with 2 GiB free plus 11.4 GiB inactive and no other owned heavy proof; selected one two-feature Cypress batch after backend terminal. Other processes stayed with their owners. No concurrency between backend and E2E, no reduced coverage or timeout changes; no required observation deferred.
