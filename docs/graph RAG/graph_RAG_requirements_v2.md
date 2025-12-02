# Doughnut Note Graph – Updated Requirements

## 1. Core Graph Model

Each **Note** has:

* `uri`, `title`, `details`
* `parent` (hierarchy)
* `object` (reified object via `targetNote`)
* inbound references: notes having this note as object (via `targetNote` relationship)

The **four fundamental relationships** for RAG traversal are:

1. **Parent** (up)
2. **Children** (down)
3. **Reified Object** (object edge)
4. **Inbound References** (reverse edges)

All other relationships (siblings, cousins, inbound-reference-contexts, etc.) are *derived* in memory during traversal.

---

## 2. Traversal Strategy (Wavefront / BFS by Depth)

We expand outward from the focus note, depth by depth:

* **Depth 0:** focus note
* **Depth 1:** parent, children, object, inbound refs of focus
* **Depth 2:** same relationships from depth 1 nodes **and additional children and inbound refs of the focus note** (subject to per-depth caps)
* **Depth 3:** same relationships from depth 2 nodes **plus additional children and inbound refs of both the focus note and any depth‑1 notes** (subject to per-depth caps)

At each depth, we generate *candidates* and add them to a global pool (not directly to the result).

Traversal stops early when:

* max depth reached (max depth is 3)
* global candidate cap reached (150–250 candidates)
* token budget would be exceeded after scoring+selection (estimated during traversal)

Track during traversal:

* `depth_fetched[note]` → first depth where the note was discovered
* `discovery_path[note]` → sequence of relationship types from focus to this note (for relationship derivation)
* `children_emitted[parent]` → how many children we took from that parent
* `inbound_emitted[target]` → how many inbound refs we took from that target note

---

## 3. Per-Depth Relationship Caps

This prevents "monster parents" with many children or inbound refs from starving the traversal and dominating the candidate pool.

### 3.1 Child Cap Function

For any parent note `p`:

```text
child_cap(p, d) = 2 * (d - depth_fetched[p])
remaining_child_budget = child_cap(p, d) - children_emitted[p]
```

This yields:

* **Focus note (depth_fetched = 0):**
  * depth 1 → 2 children
  * depth 2 → 4 children total
  * depth 3 → 6 children total
  (i.e., focus can expand more aggressively)

* **Non-focus notes:**
  * if discovered at depth 1, then at depth 2 they get 2 children
  * at depth 3 they get 4 children
  * and so on

This matches the desired behavior: focus has more fan-out privilege than others.

### 3.2 Inbound Reference Cap Function

Randomized selection with the same style cap:

```text
inbound_cap(p, d) = 2 * (d - depth_fetched[p])
remaining_inbound_budget = inbound_cap(p, d) - inbound_emitted[p]
```

---

## 4. Children Selection Logic (Ordered)

Children of a parent have a meaningful order (e.g., outline position via `siblingOrder`). We want:

* **If no children of this parent have been selected yet → choose a random contiguous block**
* **If some children were already selected → choose children closest to those (prior and after, in original order)**

### 4.1 Data Structures

For each parent `p`, maintain:

* `children_sorted[p] = [c0, c1, c2, ...]` ordered by `siblingOrder`
* `picked_child_indices[p] = { … }` indices of children selected so far

### 4.2 Case A: first time selecting children of this parent (random contiguous block)

If `picked_child_indices[p]` is empty and we need `k = remaining_child_budget` children:

```text
N = length(children_sorted[p])
max_start = N - k
start = random(0..max_start)
chosen = [start, start+1, ..., start+k-1]
```

This gives:

* diversity (different runs explore different sections)
* sibling locality (a contiguous chunk)

### 4.3 Case B: already selected some children (prefer nearby siblings)

For each unpicked child index `i`, compute:

```text
distance(i) = min( |i - j| for j in picked_child_indices[p] )
```

Then:

* shuffle candidates (break ties)
* sort by distance ascending
* pick up to `remaining_child_budget`

This yields natural sibling clusters like: `[3,4] → next picks [2,5]`.

---

## 5. Inbound Reference Selection Logic (Random)

Inbound references do **not** have meaningful ordering, so we select them randomly under the per-depth caps.

For parent/target note `p` at depth `d`:

1. Compute

```text
inbound_cap(p, d) = 2 * (d - depth_fetched[p])
remaining_inbound_budget = inbound_cap(p, d) - inbound_emitted[p]
```

2. If `remaining_inbound_budget <= 0`, skip. Otherwise, select up to that many inbound references randomly (e.g. via `ORDER BY RAND()` or in-memory shuffling of a limited subset).

Each selected inbound ref becomes a candidate with relationship type `InboundReference` and the current depth.

---

## 6. Discovery Limits (Global)

To prevent explosion, also enforce global caps:

* `max_depth = 3` (hard limit)
* `max_total_candidates = 150–250`

Rationale: depth 3 provides the outer edge of useful semantic context without overwhelming the candidate pool or introducing excessive noise.

If `max_total_candidates` is reached, stop expanding further depths even if depth < 3.

* Children selection preserves ordered sibling locality, with randomized starting blocks.
* Inbound references are selected randomly under caps.
* All discovered notes are scored globally, then selected under a token budget.

Result: a stable, dense, meaningful local context around the focus note that doesn't get overwhelmed by huge child sets or inbound-reference hubs, but still has some variability and exploration across runs.

---

## 7. Relationship Type Derivation

When a note is discovered during BFS traversal, we need to determine its `relationToFocusNote`. The relationship type is derived from the **shortest discovery path** from the focus note to the candidate.

### 7.1 Path Tracking

For each candidate note discovered, track:

* `discovery_path`: List of relationship types representing the path from focus to this note
* `depth_fetched`: The depth at which this note was first discovered

Example paths:
* Focus → Parent → `[Parent]`
* Focus → Child → `[Child]`
* Focus → Parent → Sibling → `[Parent, SiblingOfParent]`
* Focus → Child → Object → `[Child, ObjectOfReifiedChild]`
* Focus → InboundRef → Subject → `[InboundReference, SubjectOfInboundReference]`

### 7.2 Relationship Type Priority

When a note can be reached via multiple paths, use the **shortest path** (fewest hops). If multiple shortest paths exist, use the following priority order:

1. **Direct relationships** (highest priority):
   - `Self` (focus note only)
   - `Parent`
   - `Child`
   - `Object`
   - `InboundReference`

2. **One-hop derived relationships**:
   - `PriorSibling` (via Parent → Sibling)
   - `YoungerSibling` (via Parent → Sibling)
   - `ObjectOfReifiedChild` (via Child → Object)
   - `SubjectOfInboundReference` (via InboundReference → Subject)

3. **Two-hop derived relationships**:
   - `AncestorInContextualPath` (via Parent → Parent...)
   - `AncestorInObjectContextualPath` (via Object → Parent...)
   - `SiblingOfParent` (via Parent → Sibling)
   - `SiblingOfParentOfObject` (via Object → Parent → Sibling)
   - `ChildOfSiblingOfParent` (via Parent → Sibling → Child)
   - `ChildOfSiblingOfParentOfObject` (via Object → Parent → Sibling → Child)
   - `InboundReferenceContextualPath` (via InboundReference → Ancestor)
   - `SiblingOfSubjectOfInboundReference` (via InboundReference → Subject → Sibling)
   - `InboundReferenceToObjectOfReifiedChild` (via Child → Object → InboundReference)

4. **Three-hop and beyond**:
   - `GrandChild` (via Child → Child)
   - `RemotelyRelated` (any path with 3+ hops that doesn't match a specific relationship type)

### 7.3 Relationship Type Mapping Rules

Map discovery paths to relationship types:

| Path Pattern | Relationship Type |
|--------------|-------------------|
| `[]` (empty, focus note) | `Self` |
| `[Parent]` | `Parent` |
| `[Child]` | `Child` |
| `[Object]` | `Object` |
| `[InboundReference]` | `InboundReference` |
| `[Parent, Sibling]` (where sibling is prior) | `PriorSibling` |
| `[Parent, Sibling]` (where sibling is younger) | `YoungerSibling` |
| `[Child, Object]` | `ObjectOfReifiedChild` |
| `[InboundReference, Subject]` | `SubjectOfInboundReference` |
| `[Parent, Parent, ...]` (ancestors) | `AncestorInContextualPath` |
| `[Object, Parent, ...]` (object ancestors) | `AncestorInObjectContextualPath` |
| `[Parent, Sibling]` (parent's sibling) | `SiblingOfParent` (siblings surrounding the parent in `siblingOrder` are prioritized) |
| `[Object, Parent, Sibling]` | `SiblingOfParentOfObject` |
| `[Parent, Sibling, Child]` | `ChildOfSiblingOfParent` |
| `[Object, Parent, Sibling, Child]` | `ChildOfSiblingOfParentOfObject` |
| `[InboundReference, Ancestor, ...]` | `InboundReferenceContextualPath` |
| `[InboundReference, Subject, Sibling]` | `SiblingOfSubjectOfInboundReference` |
| `[Child, Object, InboundReference]` | `InboundReferenceToObjectOfReifiedChild` |
| `[Child, Child, ...]` (grandchildren) | `GrandChild` |
| Any path with 3+ hops not matching above | `RemotelyRelated` |

### 7.4 Implementation Notes

* Track the discovery path as relationships are traversed
* When a note is discovered via multiple paths, keep only the shortest path
* If multiple shortest paths exist, prefer the one with higher priority relationship types
* The relationship type is determined once when the note is first discovered and stored with the candidate

---

## 8. Scoring & Final Selection

After BFS-style discovery, we have a pool of candidate notes (with `depth_fetched`, `discovery_path`, and relationship types). The next step is to assign each candidate a **`relevanceScore`** and then select as many as fit into the token budget.

> **Implementation Status**: ✅ **COMPLETED (Step 3.4)** - Full scoring system implemented with relationship type weight, depth bonus, recency bonus, and jitter.

### 8.1 Scoring Dimensions

Each candidate note gets a scalar score computed from several dimensions:

* **Relationship type** to the focus note (strongest signal) ✅ *Implemented (Step 2.5)*
* **Graph distance** (`distanceFromFocus` = depth where first discovered) ✅ *Implemented (Step 3.4)*
* **Recency** (based on `createdAt`) ✅ *Implemented (Step 3.4)*
* **Light randomness** (jitter) to break ties and avoid overly deterministic outputs ✅ *Implemented (Step 3.4)*

Implementation as a weighted sum:

```text
relevanceScore = w_rel * rel_weight(relationToFocusNote)
               + w_depth * depth_bonus(distanceFromFocus)
               + w_time * recency_bonus(createdAt)
               + jitter()
```

### 8.2 Default Scoring Weights

**Relationship Type Weights** (w_rel = 100.0):

| Relationship Group | Weight | Relationships |
|-------------------|--------|---------------|
| Core context (highest) | 10.0 | `Parent`, `Child`, `Object`, `ObjectOfReifiedChild`, `InboundReference`, `SubjectOfInboundReference` |
| Structural context (medium) | 5.0 | `AncestorInContextualPath`, `AncestorInObjectContextualPath`, `PriorSibling`, `YoungerSibling`, `SiblingOfParent`, `SiblingOfParentOfObject`, `ChildOfSiblingOfParent`, `ChildOfSiblingOfParentOfObject`, `InboundReferenceContextualPath`, `SiblingOfSubjectOfInboundReference` |
| Soft / remote context (lowest) | 2.0 | `GrandChild`, `RemotelyRelated` |

**Depth Bonus** (w_depth = 20.0):

* depth 1 → bonus 1.0
* depth 2 → bonus 0.7
* depth 3 → bonus 0.4

**Recency Bonus** (w_time = 5.0):

* Compute age in days: `age_days = (now - createdAt) / (24 * 60 * 60 * 1000)`
* Apply exponential decay: `recency_bonus = exp(-age_days / 365.0)`
* This gives:
  * 0 days old → 1.0
  * 1 year old → 0.368
  * 2 years old → 0.135
  * 5 years old → 0.007

**Jitter** (random component):

* Add random value: `jitter = random(-0.5, 0.5)`
* This provides ±0.5 points of randomness to break ties

### 8.3 Example Score Calculation

For a `Parent` note discovered at depth 1, created 30 days ago:

```text
rel_weight = 10.0
depth_bonus = 1.0
recency_bonus = exp(-30/365) ≈ 0.921
jitter = random(-0.5, 0.5) ≈ 0.1

relevanceScore = 100.0 * 10.0 + 20.0 * 1.0 + 5.0 * 0.921 + 0.1
              = 1000.0 + 20.0 + 4.6 + 0.1
              = 1024.7
```

For a `ChildOfSiblingOfParent` note discovered at depth 2, created 2 years ago:

```text
rel_weight = 5.0
depth_bonus = 0.7
recency_bonus = exp(-730/365) ≈ 0.135
jitter = random(-0.5, 0.5) ≈ -0.2

relevanceScore = 100.0 * 5.0 + 20.0 * 0.7 + 5.0 * 0.135 - 0.2
              = 500.0 + 14.0 + 0.675 - 0.2
              = 514.475
```

### 8.4 Token-Budget-Based Selection

Once all candidates are scored:

1. Set `distanceFromFocus` (internal field) for each note to its `depth_fetched` (0–3).
2. Sort candidates by `relevanceScore` (internal field) descending.
3. Iterate in order, accumulating an estimated token cost for each note (title + truncated details + minimal relationship metadata).
4. Stop when adding the next note would exceed the remaining token budget.

The focus note is always included separately and not counted against the candidate pool.

**Note**: `relevanceScore` and `distanceFromFocus` are internal-only fields used for ordering and selection. They are **not included in the final JSON output**.

### 8.5 Hybrid Token Budget Checking

To avoid over-discovery, check token budget during traversal:

* After each depth completes, estimate total tokens for all candidates discovered so far
* If estimated tokens exceed budget (with some safety margin, e.g., 120% of budget), stop expanding further depths
* This prevents discovering 250 candidates when only 50 will fit in the budget

---

## 9. SQL Query Efficiency

To minimize database queries, fetch all notes for a depth in **one query per depth**.

### 9.1 Depth 1 Query

Fetch all depth 1 candidates in a single query:

```sql
-- Get parent, object, children, and inbound refs of focus note
WITH focus_note AS (
  SELECT id, parent_id, target_note_id 
  FROM note 
  WHERE id = :focusNoteId AND deleted_at IS NULL
)
SELECT 
  n.id, n.uri, n.topic_constructor, n.description, n.created_at,
  n.parent_id, n.target_note_id, n.sibling_order,
  'parent' as relationship_type
FROM note n
JOIN focus_note f ON n.id = f.parent_id
WHERE n.deleted_at IS NULL

UNION ALL

SELECT 
  n.id, n.uri, n.topic_constructor, n.description, n.created_at,
  n.parent_id, n.target_note_id, n.sibling_order,
  'object' as relationship_type
FROM note n
JOIN focus_note f ON n.id = f.target_note_id
WHERE n.deleted_at IS NULL

UNION ALL

SELECT 
  n.id, n.uri, n.topic_constructor, n.description, n.created_at,
  n.parent_id, n.target_note_id, n.sibling_order,
  'child' as relationship_type
FROM note n
JOIN focus_note f ON n.parent_id = f.id
WHERE n.deleted_at IS NULL
ORDER BY n.sibling_order

UNION ALL

SELECT 
  n.id, n.uri, n.topic_constructor, n.description, n.created_at,
  n.parent_id, n.target_note_id, n.sibling_order,
  'inbound_reference' as relationship_type
FROM note n
JOIN focus_note f ON n.target_note_id = f.id
WHERE n.deleted_at IS NULL
ORDER BY RAND()
```

### 9.2 Depth 2+ Queries

For each subsequent depth, fetch all candidates from all notes discovered at previous depths:

```sql
-- Get all relationships from depth N-1 notes
WITH depth_n_minus_1_notes AS (
  SELECT DISTINCT id FROM note WHERE id IN (:depthNMinus1NoteIds)
)
SELECT 
  n.id, n.uri, n.topic_constructor, n.description, n.created_at,
  n.parent_id, n.target_note_id, n.sibling_order,
  parent_note.id as source_note_id,
  'parent' as relationship_type
FROM note n
JOIN depth_n_minus_1_notes d ON n.id = (
  SELECT parent_id FROM note WHERE id = d.id
)
JOIN note parent_note ON parent_note.id = d.id
WHERE n.deleted_at IS NULL

UNION ALL

-- Similar for object, children, inbound refs...
-- Group by source_note_id to track which note led to this discovery
```

### 9.3 Query Optimization Considerations

* Use `IN` clauses with prepared statement parameters (limit to reasonable batch sizes, e.g., 100-200 note IDs per query)
* Fetch only necessary fields: `id`, `uri`, `topic_constructor`, `description`, `created_at`, `parent_id`, `target_note_id`, `sibling_order`
* Use `deleted_at IS NULL` filter in all queries
* For children, maintain `ORDER BY sibling_order` to preserve ordering
* For inbound refs, use `ORDER BY RAND()` or fetch all and shuffle in memory (depending on result set size)

### 9.4 Batch Processing

If a depth has many source notes (e.g., 100+), consider batching:

* Split source notes into batches of 50-100
* Execute one query per batch
* Combine results in memory
* Apply per-depth caps and selection logic after fetching

---

## 10. Output Structure

The external JSON structure remains broadly the same as before, with a **focus note** plus a list of **related notes**, but now includes scoring and a slightly richer relationship enum.

### 10.1 GraphRAGResult

Top-level result:

```json
{
  "focusNote": { /* FocusNote */ },
  "relatedNotes": [ /* RelatedNote */ ]
}
```

### 10.2 FocusNote

The `focusNote` structure remains the same as your current implementation:

* `uri`
* `title`
* `details` (full, not truncated)
* `parent` (or `parentUriAndTitle`)
* `children`
* `priorSiblings`
* `youngerSiblings`
* `inboundReferences`
* `contextualPath`
* `relationToFocusNote`: always `"Self"` for the focus note

> Any existing fields not mentioned here are unchanged and should continue to be emitted as before.

### 10.3 RelatedNote

Each related note in `relatedNotes` will include:

* `uri`
* `title`
* `parent` / `parentUriAndTitle` (if applicable)
* `subjectUriAndTitle` / `objectUriAndTitle` (for reification notes)
* `details` (truncated according to `RELATED_NOTE_DETAILS_TRUNCATE_LENGTH`)
* **`relationToFocusNote`**: computed by the algorithm, using the discovered shortest/primary path from the focus note.

**Internal-only fields** (used for ordering and selection, not included in JSON):
* `relevanceScore`: numeric score used for final ordering (higher = more relevant)
* `distanceFromFocus`: integer depth (0–3) where the note was discovered

All legacy relationship values are kept, plus two new ones:

* `GrandChild`: for notes that are descendants of the focus note via at least one intermediate child (focus → child → grandchild…), but are not direct children.
* `RemotelyRelated`: for notes that are selected by the scoring system but do not have a well-defined, explicit graph relationship to the focus note under the current enum.

Example:

```json
{
  "uri": "/n3020",
  "title": "〜たところで〜だけだ",
  "parent": {
    "uri": "/n3036",
    "title": "〜たところで（逆接）"
  },
  "relationToFocusNote": "ObjectOfReifiedChild"
}
```

Notes:

* If a field existed previously, it remains as before unless explicitly updated here.
* `relevanceScore` and `distanceFromFocus` are internal-only fields used for ordering and selection during processing, but are **not included in the JSON output**.
* `relationToFocusNote` remains important for downstream prompts, as it makes the graph structure explicit to the LLM instead of forcing it to infer all relationships from raw text.
* The `relatedNotes` array is ordered by `relevanceScore` (descending), but this ordering is implicit in the array order, not exposed as a field.

---

## 11. Configuration Parameters

The following parameters should be configurable:

* `MAX_DEPTH = 3` (hard limit on traversal depth)
* `MAX_TOTAL_CANDIDATES = 200` (global candidate pool cap, range: 150-250)
* `CHILD_CAP_MULTIPLIER = 2` (children per depth increment)
* `INBOUND_CAP_MULTIPLIER = 2` (inbound refs per depth increment)
* Scoring weights:
  * `W_RELATIONSHIP = 100.0`
  * `W_DEPTH = 20.0`
  * `W_RECENCY = 5.0`
  * Relationship type weights (see section 8.2)
* `JITTER_RANGE = 0.5` (random jitter range: ±0.5)
* `TOKEN_BUDGET_SAFETY_MARGIN = 1.2` (stop discovery when estimated tokens exceed 120% of budget)

---

## 12. Algorithm Summary

1. **Initialize**: Set focus note at depth 0, initialize candidate pool and tracking structures
2. **For each depth (1 to MAX_DEPTH)**:
   a. Build list of source notes (focus note for depth 1, all notes from previous depth for depth 2+)
   b. Execute single SQL query to fetch all candidates for this depth (parent, object, children, inbound refs)
   c. Apply per-depth caps for children and inbound refs
   d. Apply children selection logic (random contiguous block or nearby siblings)
   e. Apply inbound reference selection logic (random selection)
   f. Add candidates to global pool with `depth_fetched`, `discovery_path`, and relationship type
   g. Check token budget estimate - if exceeded (with safety margin), stop traversal
   h. If `MAX_TOTAL_CANDIDATES` reached, stop traversal
3. **Score all candidates**: Compute `relevanceScore` for each candidate
4. **Select final notes**: Sort by score, select top candidates that fit in token budget
5. **Build result**: Create `GraphRAGResult` with focus note and selected related notes

