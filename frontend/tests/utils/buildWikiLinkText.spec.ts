import { describe, it, expect } from "vitest"
import { buildWikiLinkText } from "@/utils/buildWikiLinkText"

function makeTarget(title: string, notebookId: number, notebookName?: string) {
  return { noteTopology: { title }, notebookId, notebookName }
}

describe("buildWikiLinkText", () => {
  it("returns simple wiki link when source and target are in the same notebook", () => {
    expect(
      buildWikiLinkText(makeTarget("CI", 1, "Doughnut"), { notebookId: 1 })
    ).toBe("[[CI]]")
  })

  it("returns qualified wiki link when target is in a different notebook with a name", () => {
    expect(
      buildWikiLinkText(makeTarget("Deep Note", 2, "Other NB"), {
        notebookId: 1,
      })
    ).toBe("[[Other NB:Deep Note]]")
  })

  it("falls back to simple link when target notebook name is missing but different notebook", () => {
    expect(
      buildWikiLinkText(makeTarget("Deep Note", 2, undefined), {
        notebookId: 1,
      })
    ).toBe("[[Deep Note]]")
  })

  it("returns simple link when source notebook is unknown", () => {
    expect(
      buildWikiLinkText(makeTarget("Deep Note", 2, "Other NB"), {
        notebookId: undefined,
      })
    ).toBe("[[Deep Note]]")
  })

  it("handles empty title gracefully", () => {
    expect(buildWikiLinkText(makeTarget("", 1, "NB"), { notebookId: 1 })).toBe(
      "[[]]"
    )
  })

  it("adds display text after pipe when different from target inner", () => {
    expect(
      buildWikiLinkText(makeTarget("CI", 1, "Doughnut"), {
        notebookId: 1,
        displayText: "friendly",
      })
    ).toBe("[[CI|friendly]]")
  })

  it("adds display text for qualified cross-notebook link", () => {
    expect(
      buildWikiLinkText(makeTarget("Deep Note", 2, "Other NB"), {
        notebookId: 1,
        displayText: "friendly",
      })
    ).toBe("[[Other NB:Deep Note|friendly]]")
  })

  it("ignores whitespace-only display text", () => {
    expect(
      buildWikiLinkText(makeTarget("CI", 1, "Doughnut"), {
        notebookId: 1,
        displayText: "   \t\n",
      })
    ).toBe("[[CI]]")
  })

  it("omits pipe when display text equals default inner", () => {
    expect(
      buildWikiLinkText(makeTarget("CI", 1, "Doughnut"), {
        notebookId: 1,
        displayText: "CI",
      })
    ).toBe("[[CI]]")
    expect(
      buildWikiLinkText(makeTarget("Deep Note", 2, "Other NB"), {
        notebookId: 1,
        displayText: "Other NB:Deep Note",
      })
    ).toBe("[[Other NB:Deep Note]]")
  })

  it("trims display text before comparing and emitting", () => {
    expect(
      buildWikiLinkText(makeTarget("CI", 1, "Doughnut"), {
        notebookId: 1,
        displayText: "  CI  ",
      })
    ).toBe("[[CI]]")
    expect(
      buildWikiLinkText(makeTarget("CI", 1, "Doughnut"), {
        notebookId: 1,
        displayText: "  label  ",
      })
    ).toBe("[[CI|label]]")
  })

  it("adds display text when cross-notebook without notebook name uses title-only inner", () => {
    expect(
      buildWikiLinkText(makeTarget("Deep Note", 2, undefined), {
        notebookId: 1,
        displayText: "original",
      })
    ).toBe("[[Deep Note|original]]")
  })

  it("does not emit piped form when default inner is empty", () => {
    expect(
      buildWikiLinkText(makeTarget("", 1, "NB"), {
        notebookId: 1,
        displayText: "label",
      })
    ).toBe("[[]]")
  })
})
