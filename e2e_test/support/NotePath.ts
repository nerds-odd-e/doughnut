/// <reference types="cypress" />
// @ts-check
import { commonSenseSplit } from './string_util'

class NotePath {
  path: string[]

  constructor(value: string) {
    // Remove quotes from start and end if present
    const cleanValue = (value ?? '').replace(/^"|"$/g, '')
    this.path = commonSenseSplit(cleanValue, '/')
  }
}

export default NotePath
