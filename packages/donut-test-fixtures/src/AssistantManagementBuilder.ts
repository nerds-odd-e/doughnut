import type {
  AssistantManagement,
  ConnectedAssistant,
} from '@generated/donut-backend-api'
import Builder from './Builder'

export default class AssistantManagementBuilder extends Builder<AssistantManagement> {
  private connected: ConnectedAssistant[] = [
    {
      clientId: 'test-assistant',
      clientName: 'Test assistant',
      scopes: ['notes:read'],
    },
  ]

  assistants(...assistants: ConnectedAssistant[]) {
    this.connected = assistants
    return this
  }

  do(): AssistantManagement {
    return {
      assistants: this.connected,
      csrfHeaderName: 'X-XSRF-TOKEN',
      csrfToken: 'csrf-token',
    }
  }
}
