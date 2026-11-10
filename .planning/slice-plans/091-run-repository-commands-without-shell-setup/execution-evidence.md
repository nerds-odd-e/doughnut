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
