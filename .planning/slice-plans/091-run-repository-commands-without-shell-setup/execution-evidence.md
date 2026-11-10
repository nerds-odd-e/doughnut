# Execution evidence

## Slice 1: required Nix features

Accepted at unchanged source freshness on trunk `de3ec5af37e0a03538c17daa410b10af5252073c`. The coordinator inspected the two changed Nix invocations, the external stand-in requiring their feature options, all six observing Bach assertions, and hook consumers. No source-correct promise was narrowed; no named coordination sibling.

- `./scripts/run.sh bash scripts/test/run.sh.test`: 6/6 passed. `test-populates-recorded-environment-once-then-reuses-it` observes real runner population/reuse plus cwd, spaced/glob arguments and CURSOR_DEV; remaining cases observe changed definition, visible population failure, child exit 7, inside-shell and no-Nix direct execution. Fixture supplies a temporary repository and external Nix executable, not runner behavior.
- Real Nix cold and cached current-runner launches in an owned temporary repository both exited 0, with exactly one cached profile symlink. Clean environment supplies only HOME, PATH and explicitly disabled host experimental features. Production flake and hook sources are copied; no peer cache is removed. Exact command below.
- `./scripts/run.sh bash scripts/worktree_setup.sh`: passed public worktree setup without manual feature-enabling exports.
- `./scripts/run.sh bash scripts/test/pre-commit.test`: 2/2 passed hook invocation and pending-repository preservation.
- `./scripts/run.sh bash scripts/test/profile-commit-hook.test`: 1/1 passed corpus/hook dispatch and cleanup; clones committed HEAD, so compatibility evidence only. This run was broader than needed for current-runner proof.

Consumer trace: [generated record and dispositions](consumer-trace-nix.md), from `node .agents/skills/dough-execute-plan/scripts/consumer-trace.mjs --checkout /Users/terryyin/git/doughnut/.worktrees/run-donut-s-repository-commands-without-interact --base de3ec5af37e0a03538c17daa410b10af5252073c --term scripts/run.sh --term Nix --term nix-command --term flakes`. Wrapper callers reuse observed child-command semantics; external fixtures were inspected for argument parsing. Direct Nix installation recipes bypass the wrapper. Dated findings/planning entries are provenance, with generic Nix/Unix matches excluded.

```sh
python3 - <<'PROBE'
import os, pathlib, shutil, subprocess, tempfile
root=pathlib.Path.cwd()
with tempfile.TemporaryDirectory(prefix='seed116-nix-cold-') as d:
    probe=pathlib.Path(d)
    (probe/'scripts').mkdir()
    for f in ['scripts/run.sh','scripts/nix_shell_hook.sh','scripts/shell_setup.sh','scripts/dev_setup.sh','flake.nix','flake.lock']:
        shutil.copy2(root/f,probe/f)
    subprocess.run(['git','-C',d,'init','-q'],check=True)
    env={'HOME':os.environ['HOME'],'PATH':os.environ['PATH'],'NIX_CONFIG':'experimental-features ='}
    for phase in ['cold','cached']:
        r=subprocess.run(['zsh','-c','./scripts/run.sh true'],cwd=d,env=env)
        print(f'{phase} exit={r.returncode}',flush=True)
        if r.returncode: raise SystemExit(r.returncode)
    profiles=list((probe/'.git/donut-nix-env').glob('*/profile'))
    print(f'cached profile links={len(profiles)}',flush=True)
    assert len(profiles)==1 and profiles[0].is_symlink()
PROBE
```

Independent refactor `/root/nix_refactor`: consolidated the identical Nix prefix into one Bash command array; both external calls unchanged. Six runner cases and whitespace passed after the edit; accepted real-Nix/setup/hook proof retains matching boundaries. Refactor returned `## REFACTOR COMPLETE`, no gaps. Updated consumer trace `/tmp/seed116-nix-refactor-consumer-trace.md` retains all 123 dispositions. Coordinator inspected the resulting array and both call sites.

## Slice 2: mocked OpenAI credential default

Source freshness terminal unchanged at origin/main `de3ec5af37e0a03538c17daa410b10af5252073c` before broad proof and after implementation. No named coordination siblings. Product changes only the E2E YAML fallback; production/dev policy, endpoint routing, held filtering and service ownership retain their existing owners.

- `./scripts/run.sh pnpm backend:test_only`: corrected full suite passed 3,093 tests, zero failures/errors, two existing skips. Coordinator inspected `backend/build/test-results/test/TEST-com.odde.donut.configs.OpenAiProfileConfigurationTest.xml` and the actual three parameterized assertions: E2E unset resolves `donut-e2e-placeholder`; supplied synthetic value resolves exactly; Development unset stays empty. The isolated environment removes system/environment sources and loads production source YAML by absolute file URI through Spring ConfigData; test application.properties supplies no key. Log `/tmp/seed116-backend-corrected.log`.
- First full run failed only three new configuration cases: classpath YAML setup could not resolve sibling db-test.properties. The source file URI repaired import context with unchanged credential assertions. The failed broad verdict is preserved in `/tmp/seed116-backend-initial.log` and `/tmp/seed116-initial-configuration-test.xml`; acceptance rests on one corrected full rerun.
- `./scripts/run.sh node --test scripts/e2e-runner-hold.cases.mjs`: 6/6 passed, including child environment observations of synthetic token withholding and paid forwarding. Those launch results paired with ConfigData cases establish application fallback and supplied-value resolution without paid calls.
- `env -i HOME="$HOME" PATH="$PATH" zsh -c './scripts/run.sh pnpm cy:run --spec e2e_test/features/assimilation/note_refinement.feature,e2e_test/features/ai_generated_recall_questions/question_contest.feature'`: 7/7 passed (4 refinement + 3 contest), 64-second test phase. Coordinator inspected terminal log `/tmp/seed116-e2e.log`. Boolean preflight confirmed token and NIX_CONFIG unset. Existing tagged hooks route through the invocation-owned private mock; refinement assertions observe removal, extraction/edit/retry results and contest assertions observe retained/replaced question stems.
- After terminal exit 0, the invocation's owned lock, exact control socket/directory and app listeners were gone (`/tmp/seed116-e2e-cleanup.json`). New invocation-only sut.log 43,563 bytes had no credential-precondition error; supporting diagnosis only. Actual canonical identity `wt_cdccb6067832479a8154ccb6463bddfd`, unit DB `doughnut_wt_cdccb6067832479a8154ccb6463bddfd_test`, E2E DB `doughnut_e2e_wt_cdccb6067832479a8154ccb6463bddfd`, origin `http://127.0.0.1:52338`; no Development or peer resources touched.

[Consumer trace and resource decisions](consumer-trace-credential.md) retain each consumer disposition and literal commands. Coordinator inspected official SDK/handler credential construction, held synthetic-environment assertions, actual ConfigData rows, batch-status nonblank observation and prod-only scheduler, scenario tags, private mock preflight/install/restore and concrete terminal results. Equivalent other mock-feature callers reuse shared credential-precondition proof; their feature-specific output is not claimed tested.

Only publication-dependent record remains: put the exact accepted credential code revision in DD-439 after a progress publication, then settle slice 2. Original dated observations remain.

Independent `/root/credential_refactor` returned `## REFACTOR COMPLETE`: none — already clean, no edits or rerun tests. Inspected YAML/test/config consumers and all retained dispositions; all accepted backend/held/Cypress proof remains unchanged. Whitespace passed. Only G1 publication attribution remains with coordinator.

Progress code publication accepted `be62cc90a427fc7663fb481a06e1f15a582fe99b` on remote story branch, suffix base `12e1f925a5e6199a27bbefcaf298f0b8c948045f`, observer reused. DD-439 now records that exact credential revision; G1 settled. Final attribution increment is records-only and reuses all accepted implementation/refactor proof. Initial guarded preparation refused malformed obligation structure and a same-slice receiving disposition; no commit/push occurred. Coordinator corrected the record to a current-slice return, preserved proof, and the repaired guarded progress publication succeeded.

## CI repair: isolate the LFS publication fixture

[Consumer trace, discrimination and retained failures](consumer-trace-ci-repair.md) includes controlled host-filter red and full seven-case LFS green. One fresh implementation agent moved the existing two environment stubs from the nested cost group to the outer LFS suite, preserving all assertions and deadlines. Coordinator inspected the exact move and hook setup/cleanup. Earlier full CLI parallel/serial timeout verdicts remain preserved; one supported run after measured headroom improved (CPU58%idle,load10.9, later63%idle) passed all442cases across68files in237.51s, using one worker, frozen dependency install and unchanged deadlines. No code or test expectation changed between the failed serial and this headroom-selected run. Full command and limitations are in the trace; no claim that this establishes the sole latency cause on an unmeasured CI host. Original story proof is unaffected.

Independent CI-repair refactor `/root/ci_repair_refactor`: none — already clean; exact `## REFACTOR COMPLETE`, no edits/tests, whitespace passed. Coordinator rechecked affected review conclusions; no product advice or source promise changed. Code-only repair is the moved LFS hook; original accepted story proof remains reusable.
