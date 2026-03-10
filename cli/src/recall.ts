import { RecallsController } from '@generated/doughnut-backend-api/sdk.gen'
import { runWithDefaultBackendClient } from './accessToken.js'

function getTimezone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone
}

export async function recallStatus(): Promise<string> {
  const result = await runWithDefaultBackendClient(() =>
    RecallsController.recalling({
      query: { timezone: getTimezone(), dueindays: 0 },
    })
  )
  const count = result.data?.toRepeat?.length ?? 0
  if (count === 1) {
    return '1 note to recall today'
  }
  return `${count} notes to recall today`
}

export const recallCommandDocs = [
  {
    name: '/recall-status',
    usage: '/recall-status',
    description: 'Show how many notes to recall today',
    category: 'interactive' as const,
  },
]
