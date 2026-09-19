# Reuse the backend atomic-publication test context

## Source and provenance

- Requested 2026-09-19 through `dough-test-optimization` for backend unit tests.
- Bounded retrospective correction: reduce trustworthy local backend-test feedback time using the measured full-suite profile at revision `68086e145bdb8fc69404ee5d659e097e2be560fe`.
- Beneficiary: a Donut developer running the ordinary backend unit-test command locally.

## Goal and scope

Shorten `backend:test_only` without weakening behavioral coverage. The authorized scope is the ten controller test classes using the `notebook-git-publication-atomic-test` profile, their shared failure-injection support, and this plan. Preserve all sixteen cases, their controller/real-database boundary, transaction rollback observations, and per-test reset of the static failure hook.

Excluded: product behavior changes, production-code changes, changing Gradle parallelism, skipping tests, narrowing the ordinary backend command, and unrelated slow families.

## Baseline and family analysis

Environment: macOS local Nix development shell; Gradle test runner with `--build-cache --parallel`, single-use Gradle daemons, no filters, existing local Gradle/Nix caches, worktree-specific MySQL test database, and no profile exclusions.

Literal command:

```bash
/usr/bin/time -p env CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

- Revision: `68086e145bdb8fc69404ee5d659e097e2be560fe`.
- Run 1: 94.43s wall, 2,535 executed cases, zero failures or skips; atomic-publication family summed JUnit suite time 9.897s.
- Run 2: 94.78s wall, 2,535 executed cases, zero failures or skips; atomic-publication family summed JUnit suite time 10.624s.
- Execution-worktree setup run: 99.25s wall, excluded because it installed workspace dependencies and provisioned the isolated database.
- Execution-worktree comparable baseline: 85.30s wall, 2,535 executed cases, zero failures or skips; atomic-publication family summed JUnit suite time 10.373s. This is the direct comparator for the post-change run in the same checkout and isolated database.
- Baseline range is 0.35s; median/mean wall time is 94.61s. Summed JUnit times are used only to locate cost, not as wall or CPU time.
- The family has one behavioral responsibility: prove that late `NotebookGitBinding` persistence failures atomically roll back note/folder projections and accepted Git state.
- All ten classes activate the same test profile and use the same primary `FailableEntityPersister`. All ten reset its static `FAIL_ON_BINDING_SAVE` hook in `@AfterEach`, yet all ten use `@DirtiesContext(AFTER_CLASS)`. Each class reports roughly 0.96-1.04s despite individual business operations generally taking much less, so repeated Spring context destruction/recreation dominates the family cost.
- Fast siblings using the ordinary `test` profile reuse their context. The backend testing rules explicitly warn that multiplying Spring contexts adds ApplicationContext and Hikari-pool cost.

## Preserved promises and outside-in proof

| Promise | Setup and observation | Owning proof |
| --- | --- | --- |
| Every existing atomic-publication scenario remains executable | The ten profiled controller classes retain all sixteen test methods and their existing fixtures/assertions | Slice 1 full `backend:test_only` run executes 2,535 cases with no failures/skips |
| Failure injection cannot leak between cases or classes | Every profiled class retains `@AfterEach resetFailureInjection`, which sets `FAIL_ON_BINDING_SAVE` false | Slice 1 source inspection plus passing full suite |
| The ordinary developer feedback path gets measurably faster | Same revision lineage, machine, Nix wrapper, Gradle mode, filters, caches, and full-suite command as baseline | Slice 1 post-change wall time compared with both baseline runs |

## Current decisions

- Execution identity: Story Branch Mode; origin `/Users/terryyin/git/doughnut` on `main`; execution checkout `/Users/terryyin/.codex/worktrees/backend-unit-test-optimization/doughnut` on `codex/backend-unit-test-optimization`; authorized remote target `origin/codex/backend-unit-test-optimization`; later integration target `main`.
- CI observer: GitHub Actions workflow `ci.yml` / `donut CI` for `nerds-odd-e/doughnut:codex/backend-unit-test-optimization`; Codex cell `52`, stream session `97527`, directory `/tmp/dough-ci-501/watch-W049il`, PID `19165`.
- Replanning permission is inherited from the requested optimization workflow. The repository's approximately five-minute slice target and ten-minute hard scrutiny threshold apply.
- Experiment hypothesis: removing only the ten redundant `@DirtiesContext(AFTER_CLASS)` annotations/imports will let Spring cache one profile-specific context across the family. Expected family saving is approximately 8-9s and expected execution-worktree full-suite wall time is approximately 76-77s.
- Retain the experiment only if all behavioral proof passes and the ordinary full-suite wall time improves beyond the observed 0.35s baseline range. Otherwise revert the experiment and record the rejected finding.
- When family and full-suite measurements contradict, repeat the comparable full-suite measurement before deciding because the baseline guidance forbids trusting a noisy wall-time sample.

## Ordered slices

### 1. Reuse the atomic-publication Spring context

Type: Structure

Status: done

Correction: Remove the redundant class-level context eviction from the ten tests sharing the atomic-publication profile. Keep each class's existing per-test hook reset, all fixtures, all scenarios, and all assertions unchanged.

Hypothesize: context eviction forces roughly one second of avoidable startup per class; one shared profile context should eliminate nine of those starts.

Try and measure: make only the annotation/import removal, inspect the surviving hook resets and case count, run the literal full-suite command above, and derive the same family total from `backend/build/test-results/test/TEST-*.xml`.

Decide and reassess: retain only a passing improvement outside baseline variance. If family and wall time contradict, repeat under comparable conditions before deciding. If supported, stop because no second family is needed for this bounded pass; if unsupported or regressing across the comparable repeats, undo only this experiment and record the remaining cost in `.planning/test-optimization-candidates.md`.

Proof: all 2,535 cases pass with zero skips; all sixteen atomic-family cases remain; ten `@AfterEach` resets remain; post-change full-suite wall time and atomic-family summed JUnit time are recorded under comparable conditions.

Accepted proof: paired A/B at revision `68086e145bdb8fc69404ee5d659e097e2be560fe` using `/usr/bin/time -p env CURSOR_DEV=true nix develop -c pnpm backend:test_only`. Original A passed 2,535 cases with zero failures/errors/skips in 95.76s and the ten-suite/sixteen-case family summed to 11.150s. Context-reuse B passed the same 2,535 cases with zero failures/errors/skips in 88.48s and the family summed to 1.542s. The 7.28s wall-time improvement (7.60%) exceeds the measured 0.35s baseline variance. Inspection confirmed zero remaining family `DirtiesContext` references plus ten unchanged `@AfterEach resetFailureInjection` methods and ten `FAIL_ON_BINDING_SAVE.set(false)` observations.

## Learnings

- First post-change run: all 2,535 cases passed with zero failures or skips; atomic-family time fell from 10.373s to 2.330s, but full-suite wall time rose from 85.30s to 88.11s. The agent reverted the exact ten annotation/import deletions under the original retention rule. The strong family saving contradicts the noisy wall-time regression, so the experiment requires repeated comparable measurements before a supported decision.
- Two further post-change runs passed all 2,535 cases with atomic-family times of 2.397s and 2.501s, but whole-suite wall times varied to 146.34s and 112.66s. This confirms the family saving while also showing machine/suite load large enough to swamp an unpaired whole-suite comparison. Run one final adjacent A/B pair—original annotations, then context reuse—before the retention decision.
- The adjacent pair resolved the timing noise: original A took 95.76s and context-reuse B took 88.48s, with identical case/results counts. Retain the context reuse; the developer's ordinary feedback path improved by 7.28s (7.60%), while all behavioral proof and per-test reset isolation remain.
