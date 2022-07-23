/// <reference types="cypress" />
// @ts-check

import "../support/string.extensions"

class NotePath {
  static regex = /"(My Notes|Bazaar|Circle)\/([^"]*)"/

  path: string[]
  root

  constructor(value: string) {
    const m = value.match(NotePath.regex)
    if (!m) {
      throw new Error("the note path should be something like `My Notes/path/to/note`")
    }
    this.root = m[1]
    this.path = m[2].commonSenseSplit("/")
  }
}

export default NotePath
