import { render } from 'markdansi'
import TurndownService from 'turndown'

const turndownService = new TurndownService()
turndownService.addRule('mark', {
  filter: 'mark',
  replacement: (content) => `**${content}**`,
})

const BLOCK_LEVEL_HTML =
  /<\/?(?:p|div|h[1-6]|ul|ol|li|table|thead|tbody|tr|td|th|pre|blockquote|section|article|header|footer|nav|main|figure|figcaption|form|fieldset|dl|dt|dd)(?:\s[^>]*)?>|<hr\b(?:\s[^>]*)?\/?>/i

function hasBlockLevelHtml(content: string): boolean {
  return BLOCK_LEVEL_HTML.test(content)
}

function inlineCodeMarkdown(inner: string): string {
  let maxRun = 0
  let run = 0
  for (const ch of inner) {
    if (ch === '`') {
      run += 1
      maxRun = Math.max(maxRun, run)
    } else {
      run = 0
    }
  }
  const fence = '`'.repeat(Math.max(1, maxRun + 1))
  return `${fence}${inner}${fence}`
}

const CODE_PLACEHOLDER_PREFIX = '\uE000CODE'
const CODE_PLACEHOLDER_SUFFIX = '\uE001'

function convertDoughnutInlineHtmlToMarkdown(content: string): string {
  let s = content.replace(/<br\s*\/?>/gi, '\n')

  const codeChunks: string[] = []
  s = s.replace(
    /<code([^>]*)>([\s\S]*?)<\/code>/gi,
    (_, _attrs, inner: string) => {
      const i = codeChunks.length
      codeChunks.push(inlineCodeMarkdown(inner))
      return `${CODE_PLACEHOLDER_PREFIX}${i}${CODE_PLACEHOLDER_SUFFIX}`
    }
  )

  let prev = ''
  while (s !== prev) {
    prev = s
    s = s.replace(/<em([^>]*)>([^<]*)<\/em>/gi, '*$2*')
    s = s.replace(/<i([^>]*)>([^<]*)<\/i>/gi, '*$2*')
    s = s.replace(/<strong([^>]*)>([^<]*)<\/strong>/gi, '**$2**')
    s = s.replace(/<b([^>]*)>([^<]*)<\/b>/gi, '**$2**')
    s = s.replace(/<s([^>]*)>([^<]*)<\/s>/gi, '~~$2~~')
    s = s.replace(/<del([^>]*)>([^<]*)<\/del>/gi, '~~$2~~')
    s = s.replace(/<u([^>]*)>([^<]*)<\/u>/gi, '*$2*')
    s = s.replace(/<mark([^>]*)>([^<]*)<\/mark>/gi, '**$2**')
  }

  for (let i = 0; i < codeChunks.length; i += 1) {
    s = s.replace(
      `${CODE_PLACEHOLDER_PREFIX}${i}${CODE_PLACEHOLDER_SUFFIX}`,
      codeChunks[i]!
    )
  }

  return s
}

function htmlToMarkdown(content: string): string {
  return turndownService.turndown(content)
}

export function renderMarkdownToTerminal(md: string, width?: number): string {
  const trimmed = (md ?? '').trim()
  if (!trimmed) return ''
  const markdown = hasBlockLevelHtml(trimmed)
    ? htmlToMarkdown(trimmed)
    : convertDoughnutInlineHtmlToMarkdown(trimmed)
  return render(markdown, {
    width: width ?? process.stdout.columns ?? 80,
    wrap: true,
    color: true,
  })
}
