/// <reference types="cypress" />
// @ts-check
import { commonSenseSplit } from './string_util'

class NotePath {
  path: string[]

  constructor(value: string) {
    this.path = commonSenseSplit(value ?? '', '/')
  }
}

export default NotePath
