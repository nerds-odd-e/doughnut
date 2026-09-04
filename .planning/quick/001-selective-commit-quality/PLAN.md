# Selective commit quality

## Outcome and boundaries

Developers and agents can prepare formatted changes before committing, while the
pre-commit hook validates affected components without mutating the working tree
or Git index. This keeps deliberate partial commits possible. Agent guidance
prefers committing all changes and leaving no local changes, without making that
an absolute hook-enforced constraint.

Representative behavior: given staged frontend changes, when a commit is
attempted, then frontend lint runs without formatting or staging files.

Outside this story: enforcing one worktree per agent, prohibiting partial
commits, changing full CI validation, or changing formatter/linter rules.

## Outside-in proof

Script tests exercise the real changed-component command boundary and the
version-controlled pre-commit hook. Skill validation and focused review confirm
the agent workflow delegates formatting before commit.

## Slices

### 1. Selective quality commands share changed-component knowledge
Type: Behavior
Status: done
Proof: Focused script tests show working-tree formatting and staged linting
dispatch only the affected component commands, including untracked formatting
inputs and shared configuration fan-out.

Behavior: Given changed files in one or more repository components, when
selective format or lint is requested, then the matching component commands run
from one shared component mapping without duplicating detection logic.

### 2. Pre-commit validates without mutation
Type: Behavior
Status: done
Proof: A focused hook test demonstrates that a commit attempt invokes
`lint:changed`, never invokes formatting, and never stages files.

Behavior: Given an intended commit, when the pre-commit hook runs, then it
lints the affected staged components and either succeeds or blocks the commit
without changing the worktree or index.

### 3. Agents prepare formatting explicitly
Type: Behavior
Status: planned
Proof: The new skill passes skill validation, and execution/rule documentation
consistently delegates selective formatting to a fresh minimal-context agent
before staging and committing.

Behavior: Given a completed implementation slice, when an agent prepares to
commit, then a fresh formatting agent runs selective formatting and reports
success or a judgment stop before the coordinator stages the intended changes.

## Current decisions

- The pre-commit hook is check-only; it does not enforce committing every local
  change.
- Formatting examines working-tree changes, including untracked files. Linting
  selects components from staged changes because it validates the intended
  commit.
- The reusable scripts own mechanics. The skill owns agent orchestration and
  does not duplicate component knowledge.
