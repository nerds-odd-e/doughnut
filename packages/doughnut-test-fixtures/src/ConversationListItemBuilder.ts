import type { ConversationListItem } from '@generated/doughnut-backend-api'
import Builder from './Builder'
import generateId from './generateId'

class ConversationListItemBuilder extends Builder<ConversationListItem> {
  data: ConversationListItem = {
    id: generateId(),
    subject: 'Test subject',
    partnerName: 'Partner',
  }

  withId(id: number) {
    this.data.id = id
    return this
  }

  withSubject(subject: string) {
    this.data.subject = subject
    return this
  }

  withPartnerName(partnerName: string | undefined) {
    this.data.partnerName = partnerName
    return this
  }

  do(): ConversationListItem {
    return { ...this.data }
  }
}

export default ConversationListItemBuilder
