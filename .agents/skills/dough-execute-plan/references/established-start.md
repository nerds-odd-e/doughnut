# Continue from an established start

Your instruction may carry an **Established start** block: the claim on the
work was already taken and published for you, and its workspace exists.

```text
Established start:
- identity: <story identity>
- publisher ID: <stable execution publisher ID>
- workspace: <owned workspace path>
- branch: <execution branch>
- mode: <trunk | story-branch>
- remote: <remote>
- target: <trunk branch>
- publishedSha: <published claim revision>
- agent, plan, startingRevision, candidateSha: only when known
```

When the block is present:

- Skip the `execution-start.mjs start` call in
  [Take or admit work](../SKILL.md#take-or-admit-work). Make no second claim,
  profile, or branch publication; the start already did.
- Retain `publishedSha`: the first increment's managed delivery uses it as its
  previously published base. Keep the other fields as your execution identity.
- Work in the named workspace and branch, not the directory you were opened in.
- Continue at the checkout-bound setup and project command under
  [execution location](execution-location.md), then the first slice.

Without the block, take or admit the work as
[Take or admit work](../SKILL.md#take-or-admit-work) describes.
