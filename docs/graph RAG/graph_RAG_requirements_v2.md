Graph RAG V2 — Simplified Relationship-Driven BFS Traversal

Purpose:
Given a focus note, collect a compact, meaningful context graph using a deterministic, relationship-guided BFS.
This V2 removes the old scoring, trimming, handlers, and caps.
Traversal is driven by:
	•	Six primitive relationships
	•	One relationship expansion per candidate per depth
	•	Deterministic weighted priority ordering
	•	BFS wavefront traversal
	•	Relationship-path tracking

⸻

1. Primitive Relationship Types

Every edge in the traversal is one of the following six relationships:
	1.	parent
The structural parent in the contextual path.
	2.	target
The note's relation target.
(The note represents a relationship between its parent and this target.)
	3.	sibling
The nearest structural sibling (in left-to-right or right-to-left order).
	4.	target-sibling
The nearest co-relator sharing the same relation target.
	5.	child
One child note of the current note.
This is a seed; additional children emerge later via sibling expansion.
	6.	inbound-ref
One note that has this note as its target.
Also a seed; additional inbound refs emerge via target-sibling expansion.

These six are the only primitive edges.
All extended relationships appear as multi-hop paths.

⸻

2. Relationship Path

Each candidate note stores:

path = [relationship, relationship, ...]

Examples:
	•	[] → focus note
	•	["parent"]
	•	["child", "sibling"] → another child of the focus
	•	["inbound-ref", "target-sibling"] → another inbound reference to the focus

The path:
	•	affects which relationships are allowed at the next step
	•	determines priority ordering
	•	constrains certain expansions

⸻

3. Path-Based Constraint Rules

Rule 1: No child after parent

If the final relationship in the path is:

lastRel = "parent"

then the candidate cannot expand via:

"child"

This avoids trivial oscillation like:

focus → parent → child(focus)

Rule 2: One expansion per candidate

Each candidate expands at most one relationship per depth.
This creates controlled, balanced growth of the context graph.

⸻

4. Weighted Priority Ordering (Deterministic)

When expanding a candidate:
	1.	Build the set of allowed relationships
(e.g., remove child when lastRel = parent).
	2.	Compute weights based on context.
	3.	Sort allowed relationships by:
	•	weight (descending)
	•	tie-break using a fixed base order

parent, target, sibling, target-sibling, child, inbound-ref


	4.	Try relationships in this final ordered list; use the first one that yields a neighbor.

Weighting Policy (Example)
	•	Focus note (path is empty)
Prefer structural and semantic anchors:
	•	parent ↑↑
	•	target ↑↑
	•	sibling ↑
	•	target-sibling ↑
	•	child ↑
	•	inbound-ref (default)
	•	When lastRel = child
Prefer pulling in other children of the same parent via siblings:
	•	sibling ↑↑↑
	•	target-sibling ↑
	•	When lastRel = inbound-ref
Prefer other inbound refs via target-siblings:
	•	target-sibling ↑↑↑
	•	Default
	•	parent ↑
	•	target ↑
	•	others default

This policy is deterministic, but easy to tweak.

⸻

5. Neighbor Resolution

For a chosen relationship rel:

Relationship	Neighbor Definition
parent	ParentOf(node)
target	RelationTargetOf(node)
sibling	NearestStructuralSibling(node)
target-sibling	NearestSiblingOfTarget(node)
child	PickOneChild(node) (seed)
inbound-ref	PickOneInbound(node) (seed)

If a relationship has no resolvable neighbor, the expansion moves to the next relationship in priority order.

⸻

6. BFS Wavefront Traversal

This algorithm explores the graph by depth layers.
Each node at depth d contributes one relationship-based neighbor to depth d+1.

Algorithm

queue = [ Candidate(focusNode, path=[], depth=0) ]
visited = { focusNode }
results = []

while queue is not empty and depth <= maxDepth:

    nextQueue = []

    for each cand in queue:

        append cand to results

        if cand.depth == maxDepth:
            continue

        neighbor = ExpandOneByWeightedPriority(cand)

        if neighbor exists and neighbor.node not in visited:
            visited.add(neighbor.node)
            append neighbor to nextQueue

    queue = nextQueue
    depth = depth + 1

return results

Properties
	•	Breadth-first: depth increases layer by layer.
	•	Balanced: each candidate expands exactly once per depth.
	•	Compact: graph size ≈ total nodes across wavefronts.
	•	Deterministic: given the same graph + focus, results are identical.
	•	Lateral growth: children/inbounds appear as sparse seeds; siblings and target-siblings propagate them.

⸻

7. Output Format

The traversal returns:

{
  "focusNote": { ... },
  "relatedNotes": [
    {
      "note": "<nodeId>",
      "path": ["relationship", "..."],
      "depth": <number>
    }
  ]
}
