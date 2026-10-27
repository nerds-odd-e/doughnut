import type { ConsentContext } from '@generated/donut-backend-api'
import Builder from './Builder'

export default class ConsentContextBuilder extends Builder<ConsentContext> {
  do(): ConsentContext {
    return {
      accountName: 'Old Learner',
      clientId: 'test-assistant',
      clientName: 'Test assistant',
      scopes: ['notes:read'],
      resource: 'https://donut.example/api/mcp',
      redirectUri: 'https://assistant.example/callback',
      state: 'pending-state',
      formAction: '/api/oauth/authorize',
      csrfParameterName: '_csrf',
      csrfToken: 'csrf-token',
    }
  }
}
