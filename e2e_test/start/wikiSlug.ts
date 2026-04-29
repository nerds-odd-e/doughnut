import slugify from 'slugify'

/** Same rules as backend `WikiSlugGeneration.toBaseSlug`. */
const FALLBACK = 'untitled'

export function wikiBasenameFromTitle(input: string): string {
  const raw = input ?? ''
  const slug = slugify(raw.trim(), { lower: true, strict: true })
  if (slug === '') {
    return FALLBACK
  }
  return slug
}
