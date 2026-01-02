import type { MarkedExtension } from "marked"

// CJK character ranges:
// - CJK Unified Ideographs: \u4E00-\u9FFF
// - Hiragana: \u3040-\u309F
// - Katakana: \u30A0-\u30FF
// - Hangul: \uAC00-\uD7AF
const cjkCharRegex = /[\u4E00-\u9FFF\u3040-\u309F\u30A0-\u30FF\uAC00-\uD7AF]/

// CJK punctuation and fullwidth forms:
// - CJK Symbols and Punctuation: \u3000-\u303F (includes 。、「」etc.)
// - Halfwidth and Fullwidth Forms: \uFF00-\uFFEF (includes （）etc.)
const cjkPunctuationRegex = /[\u3000-\u303F\uFF00-\uFFEF]/

/**
 * Check if a character is CJK content (character or punctuation)
 */
function isCjkContext(char: string | undefined): boolean {
  return (
    char !== undefined &&
    (cjkCharRegex.test(char) || cjkPunctuationRegex.test(char))
  )
}

/**
 * Marked extension to handle CJK underscore emphasis correctly.
 *
 * In CJK text, underscores adjacent to CJK characters or punctuation
 * should NOT be treated as emphasis delimiters. This is because:
 * 1. CJK languages don't use spaces between words/characters
 * 2. Users may want to use underscores as visual markers without emphasis
 * 3. Standard markdown emphasis rules don't work well with CJK punctuation
 *
 * This extension prevents underscore-based emphasis when the underscore
 * is adjacent to CJK content (characters or punctuation like 「」。、（）etc.)
 */
export const markedCjkUnderscoreExtension: MarkedExtension = {
  tokenizer: {
    emStrong(src: string, _maskedSrc: string, prevChar?: string) {
      // Only handle underscore-based emphasis
      if (!src.startsWith("_")) {
        return false // Let default handle asterisk-based emphasis
      }

      // Check if in CJK context - prevChar is CJK character or punctuation
      if (isCjkContext(prevChar)) {
        // Return undefined to skip emphasis parsing for this underscore
        return undefined
      }

      return false // Let default handle it
    },
  },
}
