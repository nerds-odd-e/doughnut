import { render } from 'markdansi'
import TurndownService from 'turndown'

const turndownService = new TurndownService()
turndownService.addRule('mark', {
  filter: 'mark',
  replacement: (content) => `**${content}**`,
})

function looksLikeHtml(content: string): boolean {
  return content.includes('</') || /<\w[^>]*>/.test(content)
}

export function htmlToMarkdown(content: string): string {
  return turndownService.turndown(content)
}

export function renderMarkdownToTerminal(md: string, width?: number): string {
  const trimmed = (md ?? '').trim()
  if (!trimmed) return ''
  const markdown = looksLikeHtml(trimmed) ? htmlToMarkdown(trimmed) : trimmed
  return render(markdown, {
    width: width ?? process.stdout.columns ?? 80,
    wrap: true,
    color: true,
  })
}
