import type { RecallLog } from '@generated/doughnut-backend-api'
import Builder from './Builder'
import generateId from './generateId'

class RecallLogBuilder extends Builder<RecallLog> {
  data: RecallLog

  constructor() {
    super()
    this.data = {
      id: generateId(),
      recordedAt: new Date('2024-01-01T12:00:00Z').toISOString(),
      elapsedHours: 0,
      productOutcome: 'GOOD',
      memoryTrackerId: generateId(),
    }
  }

  recordedAt(recordedAt: string): RecallLogBuilder {
    this.data.recordedAt = recordedAt
    return this
  }

  elapsedHours(elapsedHours: number): RecallLogBuilder {
    this.data.elapsedHours = elapsedHours
    return this
  }

  productOutcome(
    productOutcome: RecallLog['productOutcome']
  ): RecallLogBuilder {
    this.data.productOutcome = productOutcome
    return this
  }

  tutorFeedback(tutorFeedback: string): RecallLogBuilder {
    this.data.tutorFeedback = tutorFeedback
    return this
  }

  do(): RecallLog {
    return this.data
  }
}

export default RecallLogBuilder
