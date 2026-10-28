# Live connection evidence

SEED-002#story-3 · 2026-10-28. Original slice numbering is retained for dated
observations; active plan now numbers connection 3 and renewal 4.

## Connection and consent

ChatGPT plugin `asdk_app_6ae1514907ec8191b8cf93be18b67c47` at
`https://chatgpt.com/plugins/plugin_asdk_app_6ae1514907ec8191b8cf93be18b67c47`
shows **Terry's Donut account (Primary)**, connected Oct 28 with OAuth.
Terry selected the non-sensitive acceptance note **実話**. Direct plugin search
returned note 28467 plus distinguishable example/relationship candidates.
Direct full reads returned title 実話, notebook 日本語学習, folder 漢字 / 実,
updatedAt `2026-08-17T13:57:04.000+00:00`, full content and notebook revision
`4220da3ee0994d2f0c464a3464c2d7e51c2a72d8`.

Terry waived the live cancellation test. Existing consent denial/retry tests
remain the product proof; no live denial was observed. Terry explicitly said
“I did not click Allow.” Read-only audit found required PKCE/consent settings,
a stored `SCOPE_notes:read` consent, and an authorization code issued at
`01:35:56.218578 UTC`. Load-balancer logs show a Chrome POST to
`/api/oauth/authorize` at `01:35:56.073487Z`, returning 302. This establishes a
submitted consent form, not its actor or a click. No automatic submission was
found in the current consent page. Terry accepted continued execution after
the discrepancy was explained. Never describe an observed owner Allow click.
Sanitized operator metadata: `/tmp/donut-chatgpt-consent-audit.txt`.

Actual normal ChatGPT read results are tracked in [compatibility evidence](READ-COMPATIBILITY.md).
Connection discovery, installation and direct authenticated reading are observed;
full browser attribution remains open pending that repair/recheck.

## Actual expiry and renewal

Read-only production metadata selected timestamps, public client fields and
authorities only. No token value, authorization attributes or secret was read.
Same authorization ID throughout: `06ecf6f8-d60f-4122-81fc-fbca664fba21`.

- Before, observed UTC 01:50:22: access issued 01:46:36.832515, expiry
  01:51:36.832515; refresh issued 01:46:36.832989, expiry Nov 27 01:46:36.832989.
- At 01:53:51 the stored access was still expired; authorization-code issue
  remained 01:35:56.218578. No browser reauthorization occurred.
- A direct plugin `read_note(28467)` then succeeded with the full note and the
  exact source/revision above, not a reused model answer.
- After, observed UTC 01:59:01: access issued 01:58:26.406088, expiry
  02:03:26.406088; refresh issued 01:58:26.406615, expiry Nov 27 01:58:26.406615.
  The same authorization and original code issue timestamp remained.

This proves real expiry followed by credential renewal and a fresh plugin read.
No lifetime or clock was changed. Evidence files:
`/tmp/donut-chatgpt-renewal-before.txt`, `/tmp/donut-chatgpt-renewal-after.txt`.
The existing configured rotating-credential policy complements these issue times;
raw credentials were not compared. Reuse this observation while its candidate
and conditions remain applicable. Browser attribution is a distinct open proof.

## Bounded graph and full related sources

The direct plugin `get_note_graph(28467, tokenLimit:1500)` returned focus 実話
with contentTruncated false and linked example/relationship context. Some deeper
context was explicitly truncated, including 基づく; that excerpt was not cited.
Full `read_note` calls for candidate 29017 (実話 confused with 与中文含义不同)
and 28209 (この映画は実話に基づいて制作されました。) returned the same notebook
revision above, with their own IDs/titles/folders. The relationship note explicitly
contrasts Japanese 実話 (events that happened) with Chinese 实话 (truthful speech).
The example title illustrates 実話に基づく. This supports the explanation of the
connection without attributing complete-note status to truncated graph text.

The first concurrent read of 28209 returned `McpServerError: Connection failed`
(UNAVAILABLE/network_error), while 29017 succeeded. One isolated retry of 28209
succeeded; production health still identified build 46c1729c. The error's root
cause is unknown; a passing retry is recovery evidence, not a diagnosed fix.
