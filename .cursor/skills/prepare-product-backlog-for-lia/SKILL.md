---
name: prepare-product-backlog-for-lia
description: >-
  Prepare, refine, order, and assess a real-product backlog for an upcoming
  LeSS in Action (LIA) class. Use when selecting class-sized product features,
  investigating how much of an idea already exists, turning feature themes
  into vertical interdependent user stories, adding high-value items, or
  facilitating Product Backlog refinement for teams working on Doughnut or
  another production product.
---

# Prepare a Product Backlog for LeSS in Action

<objective>
Create a production-worthy, class-sized set of Product Backlog Items that lets
participants learn general software engineering through real product
development.

Keep two outputs separate:

1. A facilitation analysis containing facts, criteria, trade-offs, ordering,
   dependencies, risks, and class sizing.
2. A clean Product Backlog artifact containing only user stories and their
   acceptance examples.

Do not put selection criteria, training rationale, evaluation tables, team
assignments, process notes, or research summaries in the Product Backlog
artifact.
</objective>

<context>
LeSS in Action is a five-day experiential developer training in which feature
teams collaborate on one real product and run a real Sprint. Product work is
the medium for learning; the product domain and implementation technology are
not the learning objective.

Participants should encounter requirements discovery, Specification by
Example, ATDD, TDD, pairing, continuous integration, refactoring, emergent
design, shared ownership, and cross-team coordination through the work itself.
Do not add training-only behavior or contrive architectural work merely to
exercise a technology.
</context>

<feature_criteria>
Select a feature set that:

1. Delivers authentic value to real users.
2. Is interesting, visible, demonstrable, and suitable for participant
   dogfooding.
3. Contains slightly more work than the class can finish, while leaving a
   useful and safe product if only the core is completed.
4. Has an accessible domain with common-sense entry points and enough depth to
   require genuine domain learning.
5. Has moderate technical difficulty: meaningful design and integration work
   without depending on specialist research.
6. Crosses multiple technical seams across the backlog, such as user interface
   or CLI, application APIs, persistence, formats, external systems, and tests.
7. Supports vertical, interdependent Product Backlog Items rather than
   frontend, backend, database, or infrastructure work packages.
8. Meets production expectations for data integrity, failure handling,
   security, observability, and maintainability.
9. Creates strategic leverage without making speculative future technology a
   prerequisite for current user value.
10. Prefers high user value per unit of effort and reuses established platform
    mechanisms where appropriate.
11. Can be developed and tested in the class environment and CI without
    fragile shared credentials or unavailable external services.
</feature_criteria>

<item_criteria>
Require each Product Backlog Item to:

1. Name a user, an observable outcome, and why the outcome matters.
2. Deliver one coherent end-to-end behavior that can be demonstrated.
3. Be a vertical slice, not an internal component or architectural layer.
4. Normally fit within roughly half to one-and-a-half team days; split an item
   likely to exceed two team days before Sprint Planning.
5. Include concrete acceptance examples for the normal case, a no-change case
   where relevant, and at least one failure, boundary, or conflict case.
6. Leave the product useful and safe even if later items are never completed.
7. Expose real product dependencies without assigning permanent component
   ownership to a team.
8. Express behavior and constraints without prescribing implementation
   unnecessarily.
9. Address credentials, partial failure, concurrency, deletion, and recovery
   wherever relevant.
10. Be testable through automation at the cheapest appropriate level.
11. Encourage shared code ownership and feature-team collaboration.
12. Avoid shortcuts or code that would be unacceptable outside the class.
</item_criteria>

<process>

<step name="establish_class_context">
Record the number of participants, number and size of feature teams, class
length, likely hands-on development time, product, and environment constraints.

Treat capacity as a hypothesis. Aim for a stop-safe core plus pull-forward work,
not a promise that every item will be completed.
</step>

<step name="understand_users_and_outcomes">
Identify the real users, their present workflow, their pain or opportunity, and
an observable outcome they would value.

Write a one-sentence feature outcome before generating items. Reject themes
whose primary justification is novelty, curriculum coverage, or technology
fashion.
</step>

<step name="audit_reality">
Inspect the live product, source, tests, history, APIs, CLI behavior, and current
documentation before treating a feature as new.

Classify relevant behavior as:

- implemented and usable;
- partially implemented or reusable foundation;
- removed or historical;
- documented but aspirational;
- genuinely missing.

Fact-check external standards, formats, products, and APIs against current
primary sources. Distinguish compatibility, storage, transport, and product
integration instead of treating them as one feature.

Produce a concise gap map: current capability, reusable foundation, missing
behavior, and unresolved decision.
</step>

<step name="set_boundaries">
Make the smallest coherent release boundary explicit. Resolve or flag:

- identity and mapping rules;
- no-change and repeat-operation behavior;
- partial failure and recovery;
- concurrent or divergent changes;
- credentials and secret storage;
- deletion and destructive behavior;
- offline or CI substitutes for external services;
- intentionally deferred variants.

Prefer standard platform mechanisms when they deliver the outcome. For example,
using an existing Git remote may provide GitHub value without repository
creation, token storage, or a GitHub-specific API.
</step>

<step name="form_a_walking_skeleton">
Choose the narrowest useful end-to-end behavior that establishes the central
product contract. It must be valuable on its own and production-safe enough to
ship if the Sprint ends there.

Do not start with schema, framework, parser, API, or infrastructure items.
</step>

<step name="slice_vertically">
Grow from the walking skeleton by varying user scenarios and policies:

- first use, then repeated or incremental use;
- preview, then mutation;
- common content, then additional content types;
- existing entities, then creation, rename, move, or deletion;
- success, then conflict and recovery;
- local outcome, then versioning or sharing.

Split by user-observable behavior, workflow step, business rule, data variation,
or operational boundary. Do not split by technical layer or assign stories to
component teams.
</step>

<step name="order_for_value_learning_and_dependencies">
Order items using all three lenses:

1. Earliest authentic user value.
2. Earliest learning about risky product assumptions.
3. Prerequisites imposed by product behavior.

Insert high-value, low-effort items as soon as their minimum trustworthy
prerequisite exists. Do not delay them behind a theoretically complete feature.

Create purposeful cross-team interaction through product dependencies, shared
contracts, and integration—not through separate frontend/backend ownership.
</step>

<step name="size_for_the_class">
Build:

- a core sequence that would leave a coherent product increment;
- pull-forward items that add valuable breadth or harder lifecycle behavior;
- an explicit scope-reduction order that preserves integrity.

Use half to one-and-a-half team days as a refinement heuristic, not an estimate
to impose. Split likely two-team-day items. Account for learning, pairing,
integration, refinement, and retrospectives rather than converting eight people
into forty uninterrupted person-days.
</step>

<step name="write_acceptance_examples">
For each item, add a small set of concrete examples covering:

- successful behavior;
- observable output or state;
- idempotent or no-change behavior where meaningful;
- a boundary, invalid input, partial failure, or conflict;
- safety and recovery when data or external systems are involved.

Use examples to expose domain decisions. Avoid turning them into a technical
task checklist.
</step>

<step name="assess_and_refine">
Assess the whole feature set against every feature criterion and each story
against the item criteria. Use evidence and cautions, not unsupported pass/fail
labels.

Look especially for:

- attractive technology without a real user outcome;
- a walking skeleton that is still horizontal;
- independent items that create no cross-team learning;
- an unsafe "simple first version";
- stories too large to finish or too small to demonstrate;
- live-service dependencies that make class or CI work brittle;
- speculative future value displacing current value.

Refine the items or recommend a different feature theme when material weaknesses
remain.
</step>

<step name="separate_the_outputs">
Present the facilitation analysis in the conversation or a dedicated
facilitation artifact. Include the criteria assessment, gap map, dependency
shape, core and pull-forward boundary, sizing cautions, and decisions still
needed.

Write the Product Backlog separately using only this form:

```markdown
# Feature name

## User stories

### 1. Observable outcome

As a [user], I want [outcome] so that [reason].

Acceptance examples:

- [Normal example]
- [No-change or repeat example, when relevant]
- [Failure, boundary, or conflict example]
```

Keep the ordered stories and acceptance examples in the backlog. Keep all
selection criteria, process, evaluations, class logistics, and team allocation
outside it.
</step>

</process>

<facilitation_prompts>
Use questions like these during refinement:

- Who experiences this problem today, and what do they do instead?
- What is the smallest result they could use immediately?
- How will a user demonstrate that this story is done?
- What happens when nothing changed?
- What happens halfway through a failure?
- How do we know two versions diverged?
- If the Sprint stopped after this item, would the product be useful and safe?
- Which dependency is a product fact, and which one did our proposed design
  invent?
- Can this be split by a user scenario instead of a technical component?
- Which cheap item creates disproportionate user value once the walking
  skeleton exists?
</facilitation_prompts>

<lessons_from_prior_use>
Retain these lessons:

1. Audit implementation and history before brainstorming. An old story document
   may describe removed code or aspirations rather than current capability.
2. Use trends and standards to identify strategic options, not to justify work
   by themselves.
3. Separate format compatibility, application interoperability, local
   synchronization, version control, and hosted sharing. They can be related
   without being one indivisible feature.
4. Place an inexpensive, familiar mechanism such as local Git history or
   publishing to an existing remote early once the underlying state is
   trustworthy.
5. Delegate authentication to existing secure mechanisms and design CI around
   local substitutes when possible.
6. Preserve conflicts, idempotency, partial failure, and recovery while slicing;
   these are product behavior, not optional hardening.
7. Design interdependence through one product and shared behavior, not through
   team specialization.
8. Keep the backlog clean. Mixing selection criteria and facilitation analysis
   into the story document makes it harder to use in refinement.
</lessons_from_prior_use>

<success_criteria>
- Current product reality and external assumptions are fact-checked.
- The selected theme meets the feature criteria with evidence.
- The backlog begins with a useful vertical walking skeleton.
- Items are ordered by value, learning, and genuine dependencies.
- Core and pull-forward scope exceed likely capacity without endangering a
  useful increment.
- Each item meets the item criteria or carries a specific refinement warning.
- External services have workable class and CI strategies.
- The Product Backlog artifact contains only ordered user stories and acceptance
  examples.
- Facilitation analysis remains separate.
</success_criteria>

<out_of_scope>
- Do not implement product features unless the user separately asks.
- Do not fabricate product value to cover a desired technology.
- Do not estimate on behalf of participating teams.
- Do not assign permanent components or layers to feature teams.
- Do not put the skill's criteria or process into the Product Backlog artifact.
</out_of_scope>
