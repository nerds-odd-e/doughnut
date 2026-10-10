const japaneseOrChineseWriting =
  /[\p{Script=Han}\p{Script=Hiragana}\p{Script=Katakana}\u30FC\u3000-\u303F\uFF00-\uFFEF]/u

const japaneseOrChineseAtJoin = (left: string, right: string) =>
  japaneseOrChineseWriting.test(
    (left.match(/.$/u)?.[0] ?? "") + (right.match(/^./u)?.[0] ?? "")
  )

/** Joins dictated segments onto a base by the CJK/space rule used for body passages. */
export function joinDictatedSegments(
  base: string,
  segments: readonly string[]
): string {
  return segments.reduce(
    (text, segment) =>
      text === "" || /\s$/.test(text) || japaneseOrChineseAtJoin(text, segment)
        ? text + segment
        : `${text} ${segment}`,
    base
  )
}

/** The text that puts dictated segments between a before and an after by the same rule on both sides. */
export function dictatedInsertion(
  before: string,
  segments: readonly string[],
  after: string
): string {
  const joined = joinDictatedSegments(before, segments).slice(before.length)
  return after === "" ||
    /^\s/.test(after) ||
    japaneseOrChineseAtJoin(joined, after)
    ? joined
    : `${joined} `
}
