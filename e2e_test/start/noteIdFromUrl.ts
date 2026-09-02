/// <reference types="Cypress" />
// @ts-check

export function noteIdFromUrl(url: string): number {
  const match =
    url.match(/\/n(\d+)/) ??
    url.match(/\/n\/(\d+)/) ??
    url.match(/\/d\/n\/(\d+)/)
  expect(
    match,
    `could not parse note id from URL (expected /n<id>, /n/<id>, or legacy /d/n/<id>): ${url}`
  ).to.not.be.null
  return Number(match![1])
}
