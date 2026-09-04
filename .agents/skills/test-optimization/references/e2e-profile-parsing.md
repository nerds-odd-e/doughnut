# E2E Profile Parsing

The JSON reporter prints one `{ "stats": …, "tests": […] }` block per spec.
After teeing stdout to `/tmp/e2e-profile.log`, track the current spec from
`Running: <name>.feature`, accumulate each JSON block until braces balance and
the buffer contains `"stats"`, then parse it. Collect test titles and durations,
attach the current spec, sort descending, and select the top 10%. Skip-tagged
scenarios should already be absent.

Create a reusable `scripts/` helper only for repeated team use; otherwise use a
one-off inline Node script.
