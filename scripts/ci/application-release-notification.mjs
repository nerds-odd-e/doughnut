import { writeReleaseOutput } from './application-release-output.mjs'

const env = process.env
const tag =
  env.RELEASE_TAG ||
  (env.GITHUB_REF?.startsWith('refs/tags/')
    ? env.GITHUB_REF.slice('refs/tags/'.length)
    : 'unknown')
const repositoryUrl = `${env.GITHUB_SERVER_URL}/${env.GITHUB_REPOSITORY}`
const context = [
  ['tag', tag],
  ['commit', env.RELEASE_SHA || 'unknown (identity not validated)'],
  ['stage', env.FAILURE_STAGE || 'unknown'],
  [
    'CI run',
    env.RELEASE_CI_RUN_ID
      ? `${repositoryUrl}/actions/runs/${env.RELEASE_CI_RUN_ID}/attempts/${env.RELEASE_CI_RUN_ATTEMPT}`
      : 'unknown (CI not selected)',
  ],
  ['release run', `${repositoryUrl}/actions/runs/${env.GITHUB_RUN_ID}`],
]
if (env.FAILURE_STAGE === 'artifact admission') {
  context.push([
    'recovery',
    `Rerun CI ${env.RELEASE_CI_RUN_ID} for exact commit ${env.RELEASE_SHA}, then rerun release ${env.GITHUB_RUN_ID} with the same immutable tag ${tag}. Do not substitute current main. If GitHub can no longer regenerate that history and its artifacts, test a correction and release a new patch.`,
  ])
}
const payload = {
  text: `Production release failed: ${tag}`,
  attachments: [
    {
      color: '#E01E5A',
      blocks: [
        {
          type: 'section',
          fields: context.map(([label, value]) => ({
            type: 'plain_text',
            text: `${label}\n${value}`,
          })),
        },
      ],
    },
  ],
}
writeReleaseOutput({ payload: JSON.stringify(payload) })
