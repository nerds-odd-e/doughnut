import { describe, expect, it } from "vitest"
import markdownizer from "@/components/form/markdownizer"
import { prepareQuestionStemMarkdown } from "@/utils/prepareQuestionStemMarkdown"

describe("prepareQuestionStemMarkdown", () => {
  it("unpiped link uses full inner as visible text", () => {
    expect(prepareQuestionStemMarkdown("See [[Alpha]] here")).toBe(
      "See Alpha here"
    )
  })

  it("piped link uses display side", () => {
    expect(
      prepareQuestionStemMarkdown("x [[LinkTarget|friendly label]] y")
    ).toBe("x friendly label y")
  })

  it("replaces multiple links on one line", () => {
    expect(prepareQuestionStemMarkdown("[[A]] and [[B|bee]]")).toBe("A and bee")
  })

  it("leaves unclosed brackets unchanged", () => {
    expect(prepareQuestionStemMarkdown("a [[open only")).toBe("a [[open only")
  })

  it("leaves empty bracket inner unchanged", () => {
    expect(prepareQuestionStemMarkdown("a [[ ]] b")).toBe("a [[ ]] b")
  })

  it("preserves cloze mark markup and still strips wikilinks", () => {
    const md = `<mark title='Hidden text that is matching the answer'>[...]</mark> uses [[T|shown]] end`
    expect(prepareQuestionStemMarkdown(md)).toBe(
      `<mark title='Hidden text that is matching the answer'>[...]</mark> uses shown end`
    )
  })

  it("turns literal backslash-n into real newlines", () => {
    expect(
      prepareQuestionStemMarkdown(
        "次の文の（　）に入る最も自然な組み合わせはどれですか。\\n\\n「親に（　）て、（　）増長してしまった。」"
      )
    ).toBe(
      "次の文の（　）に入る最も自然な組み合わせはどれですか。\n\n「親に（　）て、（　）増長してしまった。」"
    )
  })

  it("leaves real newlines unchanged", () => {
    expect(prepareQuestionStemMarkdown("a\n\nb")).toBe("a\n\nb")
  })

  it("markdownToHtml renders cloze and unescaped blank lines", () => {
    const md = `<mark title='Hidden text that is matching the answer'>[...]</mark>\\n\\nuses [[T|shown]]`
    const html = markdownizer.markdownToHtml(prepareQuestionStemMarkdown(md))
    expect(html).toContain("mark")
    expect(html).toContain("shown")
    expect(html).not.toContain("[[")
    expect(html).not.toContain("\\n")
  })
})
