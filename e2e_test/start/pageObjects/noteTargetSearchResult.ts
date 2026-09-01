const searchResultHeadingSelector = '.result-section-info'

export function expectSearchResultHeading() {
  cy.get(searchResultHeadingSelector).should(($el) => {
    const actual = $el.text().trim()
    expect(
      actual,
      'search dialog must show Search result before using a note (not Recently updated notes)'
    ).to.equal('Search result')
  })
}

export function searchResultSection() {
  expectSearchResultHeading()
  return cy
    .get(searchResultHeadingSelector)
    .contains('Search result')
    .closest('.result-section, .dropdown-section')
}

function clickUniqueUseThisNote(
  uniquenessMessage: (actualCount: number) => string,
  matches: (el: Element) => boolean
) {
  searchResultSection()
    .find('.search-result [role=listitem]')
    .filter((_, el) => matches(el))
    .should(($rows) => {
      expect($rows.length, uniquenessMessage($rows.length)).to.equal(1)
    })
    .findByRole('button', { name: 'Use this note' })
    .click()
}

function noteHitTitle(el: Element) {
  return el
    .querySelector('.search-result-item-title a:not(.notebook-hit-title)')
    ?.textContent?.trim()
}

export function clickUseThisNoteOnTargetNote(toNoteTopic: string) {
  clickUniqueUseThisNote(
    (n) => `expected 1 search hit titled "${toNoteTopic}", found ${n}`,
    (el) => noteHitTitle(el) === toNoteTopic
  )
}

export function clickUseThisNoteOnTargetNoteInFolder(
  toNoteTopic: string,
  folderName: string
) {
  clickUniqueUseThisNote(
    (n) =>
      `expected 1 search hit titled "${toNoteTopic}" in folder "${folderName}", found ${n}`,
    (el) =>
      noteHitTitle(el) === toNoteTopic &&
      el.querySelector('.folder-name-label')?.textContent?.trim() === folderName
  )
}
