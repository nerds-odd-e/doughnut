import { firstSentenceTitle } from "@/lib/book-reading/firstSentenceTitle"
import { describe, expect, it } from "vitest"

const words = (n: number) => Array(n).fill("word").join(" ")

describe("firstSentenceTitle", () => {
  it.each([
    ["a short sentence unchanged", "Hello world.", "Hello world."],
    ["two sentences give the first", "One. Two.", "One."],
    ["question mark ends a sentence", "Why? Because.", "Why?"],
    ["exclamation mark ends a sentence", "Stop! Now.", "Stop!"],
    [
      "a trailing sentence end with no space",
      "Whole sentence.",
      "Whole sentence.",
    ],
    [
      "a dot inside a word does not end it",
      "Version 3.5 is out. Next.",
      "Version 3.5 is out.",
    ],
  ])("%s", (_name, text, expected) => {
    expect(firstSentenceTitle(text)).toBe(expected)
  })

  it("cuts a long first sentence at a word", () => {
    const title = firstSentenceTitle(`${words(40)}. Next.`)
    expect(title.length).toBeLessThanOrEqual(80)
    expect(title).toBe(words(16))
  })

  it("cuts a single long word at 80 characters", () => {
    expect(firstSentenceTitle("x".repeat(100))).toBe("x".repeat(80))
  })

  it("cuts text with no sentence end at a word", () => {
    const title = firstSentenceTitle(words(60))
    expect(title.length).toBeLessThanOrEqual(80)
    expect(title).toBe(words(16))
  })

  it("gives an empty title for empty text", () => {
    expect(firstSentenceTitle("")).toBe("")
  })
})
