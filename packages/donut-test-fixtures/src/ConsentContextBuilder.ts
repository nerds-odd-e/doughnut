import type { ConsentContext } from '@generated/donut-backend-api'
import Builder from './Builder'

export default class ConsentContextBuilder extends Builder<ConsentContext> {
  private grantedScopes = ['notes:read']

  scopes(...scopes: string[]) {
    this.grantedScopes = scopes
    return this
  }

  do(): ConsentContext {
    return {
      accountName: 'Old Learner',
      clientId: 'test-assistant',
      clientName: 'Test assistant',
      scopes: this.grantedScopes,
      resource: 'https://donut.example/api/mcp',
      redirectUri: 'https://assistant.example/callback',
      state: 'pending-state',
      formAction: '/api/oauth/authorize',
      csrfParameterName: '_csrf',
      csrfToken: 'csrf-token',
    }
  }
}
