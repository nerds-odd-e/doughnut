import { marked, type Tokens } from "marked"
import markdownizer from "@/components/form/markdownizer"
import { resolveNotePropertyFromHref } from "@/routes/noteShowLocation"
import type { WikiLinkNoteIdentity } from "@/utils/buildWikiLinkText"
import { verbatimFrontmatterPrefixAndBody } from "@/utils/noteContentFrontmatter"
import {
  encodeWikiLinkPropertyKey,
  formatPortablePath,
} from "@/utils/portablePath"
import { authoredWikiLinkTokenFromOriginalPath } from "@/utils/sameNotebookWikiLinkAuthoring"

export type ConvertPastedNotePropertyLinksContext = {
  sourceNoteId: number
  sourceNotebookId: number | undefined
  resolveNote: (noteId: number) => Promise<WikiLinkNoteIdentity | undefined>
}

function pastedAnchorDisplay(label: string, href: string): string | undefined {
  const trimmed = label.trim()
  if (trimmed.length === 0) {
    return undefined
  }
  if (trimmed === href) {
    return undefined
  }
  try {
    const parsed = new URL(href, "https://paste.invalid")
    if (trimmed === parsed.pathname || trimmed === parsed.href) {
      return undefined
    }
  } catch {
    /* href is not URL-shaped */
  }
  return trimmed
}

type PendingLinkReplacement = {
  token: Tokens.Link
  sourceNoteId: number
  destinationNoteId: number
  originalPortablePath: string
  displayText: string | undefined
}

/** Every resolved paste is spelled by the backend authoring operation — same-notebook, colliding, and cross-notebook alike. */
function collectPendingLinkReplacements(
  tokens: ReturnType<typeof marked.lexer>,
  context: ConvertPastedNotePropertyLinksContext,
  identities: Map<number, WikiLinkNoteIdentity | undefined>
): PendingLinkReplacement[] {
  const pending: PendingLinkReplacement[] = []
  marked.walkTokens(tokens, (token) => {
    if (token.type !== "link") {
      return
    }
    const linkToken = token as Tokens.Link
    const property = resolveNotePropertyFromHref(linkToken.href)
    if (property === undefined) {
      return
    }
    if (identities.get(property.noteId) === undefined) {
      return
    }
    const displayText = pastedAnchorDisplay(
      linkToken.text || "",
      linkToken.href
    )
    pending.push({
      token: linkToken,
      sourceNoteId: context.sourceNoteId,
      destinationNoteId: property.noteId,
      originalPortablePath: formatPortablePath({
        qualifiedNotePortion: "",
        encodedPropertyKey: encodeWikiLinkPropertyKey(property.propertyKey),
      }),
      displayText,
    })
  })
  return pending
}

function applyWikiLinkTokenReplacement(token: Tokens.Link, wiki: string) {
  const asRecord = token as unknown as Record<string, unknown>
  delete asRecord.href
  delete asRecord.title
  delete asRecord.tokens
  Object.assign(token, {
    type: "text",
    raw: wiki,
    text: wiki,
    escaped: false,
  } as Tokens.Text)
}

async function replaceResolvedPropertyLinkTokens(
  markdown: string,
  context: ConvertPastedNotePropertyLinksContext,
  identities: Map<number, WikiLinkNoteIdentity | undefined>
): Promise<string> {
  const tokens = marked.lexer(markdown)
  const pending = collectPendingLinkReplacements(tokens, context, identities)
  for (const {
    token,
    sourceNoteId,
    destinationNoteId,
    originalPortablePath,
    displayText,
  } of pending) {
    const wiki = await authoredWikiLinkTokenFromOriginalPath(
      sourceNoteId,
      destinationNoteId,
      originalPortablePath,
      displayText
    )
    if (wiki === undefined) {
      continue
    }
    applyWikiLinkTokenReplacement(token, wiki)
  }
  const html = marked.parser(tokens)
  return markdownizer.htmlToMarkdown(html).trim()
}

async function convertPastedNotePropertyLinks(
  markdown: string,
  context: ConvertPastedNotePropertyLinksContext
): Promise<string> {
  const tokens = marked.lexer(markdown)
  const noteIds = new Set<number>()
  marked.walkTokens(tokens, (token) => {
    if (token.type !== "link") {
      return
    }
    const property = resolveNotePropertyFromHref((token as Tokens.Link).href)
    if (property !== undefined) {
      noteIds.add(property.noteId)
    }
  })
  if (noteIds.size === 0) {
    return markdown
  }
  const identities = new Map<number, WikiLinkNoteIdentity | undefined>()
  for (const noteId of noteIds) {
    identities.set(noteId, await context.resolveNote(noteId))
  }
  if ([...identities.values()].every((identity) => identity === undefined)) {
    return markdown
  }
  return replaceResolvedPropertyLinkTokens(markdown, context, identities)
}

export async function convertPastedNotePropertyLinksInNoteContent(
  markdown: string,
  context: ConvertPastedNotePropertyLinksContext
): Promise<string> {
  const split = verbatimFrontmatterPrefixAndBody(markdown)
  if (!split) {
    return convertPastedNotePropertyLinks(markdown, context)
  }
  return (
    split.prefix + (await convertPastedNotePropertyLinks(split.body, context))
  )
}
