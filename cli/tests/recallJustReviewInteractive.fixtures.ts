import makeMe from 'doughnut-test-fixtures/makeMe'

export function alphaNoteRealm() {
  return makeMe.aNoteRealm.title('Alpha').content('body').please()
}

export function childNoteUnderEnglish() {
  return makeMe.aNoteRealm
    .title('Sedition')
    .content('Sedition means incite violence')
    .inFolder(1, 'English')
    .please()
}
