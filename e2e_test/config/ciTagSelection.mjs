// One home for the opt-in publication profile tags excluded from ordinary
// runs, so adding a new profile scenario edits this list once instead of
// both the CI and local tag expressions below.
const publicationProfileTags = [
  '@publicationProfile',
  '@publicationProfileRejection',
  '@publicationProfileHttp',
  '@publicationProfileHttpRejection',
  '@publicationProfileHttpUpdate',
]
const excludeTags = (...tags) => tags.map((tag) => `not ${tag}`).join(' and ')

export const cucumberTagExpression = (inCi) =>
  inCi
    ? excludeTags('@ignore', '@wip', ...publicationProfileTags)
    : excludeTags('@ignore', ...publicationProfileTags)
