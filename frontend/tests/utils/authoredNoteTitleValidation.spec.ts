import { describe, expect, it } from "vitest"
import {
  AUTHORED_NOTE_TITLE_PLAIN_ALIAS_MESSAGE,
  authoredNoteTitleValidationError,
  hasPlainTitleAliasSegments,
} from "@/utils/authoredNoteTitleValidation"

describe("authoredNoteTitleValidation", () => {
  it.each([
    "colour／color",
    "cat／kitten (animal)",
    "word／alias",
  ])("rejects plain title alias %s", (title) => {
    expect(hasPlainTitleAliasSegments(title)).toBe(true)
    expect(authoredNoteTitleValidationError(title)).toBe(
      AUTHORED_NOTE_TITLE_PLAIN_ALIAS_MESSAGE
    )
  })

  it.each([
    "colour",
    "word／~suffix",
    "~logy／~logical",
    "cat／／kitten",
    "TCP／／IP： Overview",
  ])("allows title without plain aliases: %s", (title) => {
    expect(hasPlainTitleAliasSegments(title)).toBe(false)
    expect(authoredNoteTitleValidationError(title)).toBeUndefined()
  })
})
