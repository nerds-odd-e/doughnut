const japaneseOrChineseWriting =
  /[\p{Script=Han}\p{Script=Hiragana}\p{Script=Katakana}\u30FC\u3000-\u303F\uFF00-\uFFEF]/u

/** Joins dictated segments onto a base by the CJK/space rule used for body passages. */
export function joinDictatedSegments(
  base: string,
  segments: readonly string[]
): string {
  return segments.reduce((text, segment) => {
    const before = text.match(/.$/u)?.[0] ?? ""
    const after = segment.match(/^./u)?.[0] ?? ""
    return text === "" ||
      /\s$/.test(text) ||
      japaneseOrChineseWriting.test(before + after)
      ? text + segment
      : `${text} ${segment}`
  }, base)
}
