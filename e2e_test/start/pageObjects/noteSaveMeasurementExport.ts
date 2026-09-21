/**
 * TEMPORARY MEASUREMENT MACHINERY — SEED-034#story-4, slice 3.
 * Deleted together with `note_save_measurement.feature` in slice 14.
 *
 * The measured notebook's own Portable export, taken before any measured edit,
 * so the two sides of the reconstructed comparison can be shown to hold the
 * same paths and the same content. Native commit identities are not compared.
 */

/** `timeout` is the measurement's own patience: a whole notebook is exported. */
export function capturePortableExport(notebook: string, timeout: number) {
  cy.request({ url: '/api/notebooks', timeout }).then((listing) => {
    const found = listing.body.notebooks.find(
      (realm: { notebook: { id: number; name: string } }) =>
        realm.notebook.name === notebook
    )
    expect(found, `notebook ${notebook} is owned by this user`).to.exist
    cy.request({
      url: `/api/notebooks/${found.notebook.id}/export`,
      encoding: 'base64',
      timeout,
    }).then((zip) =>
      cy.task('saveNoteSaveMeasurementExport', zip.body, { timeout })
    )
  })
}
