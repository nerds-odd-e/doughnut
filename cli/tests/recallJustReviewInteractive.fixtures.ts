import makeMe from 'doughnut-test-fixtures/makeMe'

export const baseNoteTimes = {
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

export const leaveRecallWithYnRe = /(?=.*Leave recall\?)(?=.*\(y\/n\))/s

export function alphaNoteRealm() {
  return makeMe.aNoteRealm
    .title('Alpha')
    .content('body')
    .createdAt(baseNoteTimes.createdAt)
    .updatedAt(baseNoteTimes.updatedAt)
    .please()
}

export function childNoteUnderEnglish() {
  const english = makeMe.aNoteRealm
    .title('English')
    .content('')
    .createdAt(baseNoteTimes.createdAt)
    .updatedAt(baseNoteTimes.updatedAt)
    .please()
  const child = makeMe.aNoteRealm
    .title('Sedition')
    .content('Sedition means incite violence')
    .under(english)
    .createdAt(baseNoteTimes.createdAt)
    .updatedAt(baseNoteTimes.updatedAt)
    .please()
  child.ancestorFolders = [{ id: '1', name: 'English' }]
  return child
}
