import assert from 'node:assert/strict'
import { test } from 'node:test'
import { action, workflow } from './workflow-fixtures.mjs'

test('CI skips documentation-only pushes on every branch', () => {
  const ci = workflow('ci')

  assert.deepEqual(ci.on, {
    push: {
      branches: ['**'],
      'paths-ignore': ['.planning/**', 'docs/**'],
    },
  })
  assert.equal(ci.name, 'donut CI')
})

test('CI failure notification stays gated and receives only its Slack secret', () => {
  const notificationJob = workflow('ci').jobs['Notify-on-failure']
  const notification = action('notify_ci_failure')
  const slack = notification.runs.steps[1]

  assert.equal(
    notificationJob.if,
    "always() && contains(needs.*.result, 'failure')"
  )
  assert.equal(notificationJob.steps[1].uses, './.github/notify_ci_failure')
  assert.deepEqual(notificationJob.steps[1].with, {
    slack_webhook_url: '${{ secrets.SLACK_WEBHOOK_URL }}',
  })
  assert.equal(notification.on, undefined)
  assert.deepEqual(Object.keys(notification.inputs), ['slack_webhook_url'])
  assert.equal(notification.runs.using, 'composite')
  assert.equal(notification.runs.steps[0].shell, 'bash')
  assert.equal(slack.uses, 'slackapi/slack-github-action@v4.0.0')
  assert.equal(slack.with.webhook, '${{ inputs.slack_webhook_url }}')
  assert.equal(slack.with['webhook-type'], 'incoming-webhook')
  assert.match(slack.with.payload, /CI\/CD failure/)
})
