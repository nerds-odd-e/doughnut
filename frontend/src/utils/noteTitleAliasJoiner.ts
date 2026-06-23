/** Backend `NoteTitle` splits only on U+FF0F; use this between appended title aliases. */
export const NOTE_TITLE_ALIAS_JOINER = " ／ "

export function appendTitleAlias(currentTitle: string, alias: string): string {
  const trimmedTitle = currentTitle.trim()
  if (trimmedTitle === "") {
    return alias
  }
  return `${trimmedTitle}${NOTE_TITLE_ALIAS_JOINER}${alias}`
}
