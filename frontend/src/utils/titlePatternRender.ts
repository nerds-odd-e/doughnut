const DATE_TOKEN = "{{date}}"

/** Renders supported placeholders in a scoped title-pattern string (`title_pattern` frontmatter). Unknown tokens stay literal. */
export function renderTitleFromPattern(
  pattern: string,
  options?: { now?: Date }
): string {
  const d = options?.now ?? new Date()
  const yyyyMmDd = d.toISOString().slice(0, 10)
  return pattern.split(DATE_TOKEN).join(yyyyMmDd)
}
