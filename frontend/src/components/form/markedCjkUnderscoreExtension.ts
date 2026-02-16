import type { MarkedExtension, Tokens } from "marked"

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
  if (!char) return false
  return cjkCharRegex.test(char) || cjkPunctuationRegex.test(char)
}

/**
 * Check if a string contains CJK content
 */
function containsCjk(str: string): boolean {
  return cjkCharRegex.test(str) || cjkPunctuationRegex.test(str)
}

/**
 * Marked extension to handle CJK emphasis correctly.
 *
 * This extension addresses two issues:
 *
 * 1. Underscore handling: In CJK text, underscores adjacent to CJK characters
 *    or punctuation should NOT be treated as emphasis delimiters. This is because
 *    CJK languages don't use spaces between words/characters and users may want
 *    to use underscores as visual markers.
 *
 * 2. Asterisk handling: marked's default emphasis rules don't work well when
 *    the content inside **...** starts or ends with CJK punctuation (like 「」).
 *    This extension enables asterisk-based emphasis in CJK context where the
 *    default rules would fail.
 *
 * Examples that now work correctly:
 * - てっきり今日は水曜日だ_とばかり思っていました_。 (underscores preserved)
 * - 日本語**「太字」**テスト (bold applied correctly)
 * - 本質や内実を隠した、**「中身」**という (bold applied correctly)
 */
export const markedCjkUnderscoreExtension: MarkedExtension = {
  tokenizer: {
    emStrong(
      this: { lexer: { inlineTokens: (src: string) => Tokens.Generic[] } },
      src: string,
      _maskedSrc: string,
      prevChar?: string
    ): Tokens.Strong | Tokens.Em | false | undefined {
      // Handle ** for strong (bold) in CJK context
      const strongMatch = /^\*\*([^*]+)\*\*/.exec(src)
      if (strongMatch) {
        const content = strongMatch[1] as string
        const fullMatch = strongMatch[0]

        // Check if content contains CJK
        const hasCJK = containsCjk(content)
        const prevIsCJK = isCjkContext(prevChar)
        const afterMatch = src.slice(fullMatch.length)
        const afterChar = afterMatch.length > 0 ? afterMatch[0] : undefined
        const afterIsCJK = isCjkContext(afterChar)

        // If in CJK context and content has CJK, enable emphasis
        if (hasCJK && (prevIsCJK || afterIsCJK)) {
          return {
            type: "strong",
            raw: fullMatch,
            text: content,
            tokens: this.lexer.inlineTokens(content),
          }
        }
      }

      // Handle * for em (italic) in CJK context
      const emMatch = /^\*([^*]+)\*/.exec(src)
      if (emMatch && !src.startsWith("**")) {
        const content = emMatch[1] as string
        const fullMatch = emMatch[0]

        const hasCJK = containsCjk(content)
        const prevIsCJK = isCjkContext(prevChar)
        const afterMatch = src.slice(fullMatch.length)
        const afterChar = afterMatch.length > 0 ? afterMatch[0] : undefined
        const afterIsCJK = isCjkContext(afterChar)

        if (hasCJK && (prevIsCJK || afterIsCJK)) {
          return {
            type: "em",
            raw: fullMatch,
            text: content,
            tokens: this.lexer.inlineTokens(content),
          }
        }
      }

      // Handle underscore-based emphasis - block in CJK context
      if (src.startsWith("_")) {
        if (isCjkContext(prevChar)) {
          return
        }
      }

      return false // Let default handle it
    },
  },
}
