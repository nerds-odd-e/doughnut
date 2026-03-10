import { render } from 'markdansi'

export function renderMarkdownToTerminal(md: string, width?: number): string {
  const trimmed = (md ?? '').trim()
  if (!trimmed) return ''
  return render(trimmed, {
    width: width ?? process.stdout.columns ?? 80,
    wrap: true,
    color: true,
  })
}
