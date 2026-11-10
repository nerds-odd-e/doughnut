export const aiMarkdownTextarea = () =>
  document.querySelector<HTMLTextAreaElement>(
    '[data-testid="ai-context-markdown-textarea"]'
  )
export const copyButton = () =>
  document.querySelector<HTMLButtonElement>(
    '[data-testid="copy-ai-context-md-btn"]'
  )!
export const downloadButton = () =>
  document.querySelector<HTMLButtonElement>(
    '[data-testid="download-ai-context-md-btn"]'
  )!
