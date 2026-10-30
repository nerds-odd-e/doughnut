# Property list presentation

<a id="story-1"></a>

### Read list properties with stable tracker actions

**Identity:** SEED-095#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planless"}
```

**Goal:** Make expanded list properties readable and keep tracker actions stable while their information loads.

The supplied Safari tablet screenshot shows narrow wrapped list values, a Reify explanation and button, and a Skip button. The user also observes Assimilate flashing before an existing tracker's View tracker link loads.

Scope: remove the list property's Reify button and its explanatory message, remove Skip for list properties, repair list row spacing, and display a small spinner until tracker lookup completes. Reserve the same action height during loading and after it resolves. Retain scalar property actions and list item assimilation and tracker navigation.

Known expectations: linked list values remain readable beside their tracker actions; pending lookups display a spinner; resolved tracked items display View tracker and scheduling information; resolved untracked items offer Assimilate. Loading completion does not change row height.

Evidence: user report and Safari tablet screenshot supplied 2026-10-30. Cause and existing test coverage remain to be investigated. Authorized bounded repair proceeds planlessly under dough-bug-fixing's shared execution handoff, with a ten-minute implementation limit and no replanning.
