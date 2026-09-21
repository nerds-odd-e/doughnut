/**
 * TEMPORARY MEASUREMENT MACHINERY — SEED-034#story-4, slices 3 and 13.
 * Deleted together with `note_save_measurement.feature` in slice 14.
 *
 * Node-side work against the measured notebook, as the logged-in user: the
 * Portable export capture and root attachment publication. Notebook-sized
 * bytes never pass through the browser, whose command log would otherwise
 * hold them and slow every later measured save.
 */

function measuredNotebookId(notebook: string, timeout: number) {
  return cy.request({ url: '/api/notebooks', timeout }).then((listing) => {
    const found = listing.body.notebooks.find(
      (realm: { notebook: { id: number; name: string } }) =>
        realm.notebook.name === notebook
    )
    expect(found, `notebook ${notebook} is owned by this user`).to.exist
    return found.notebook.id as number
  })
}

/** `timeout` is the measurement's own patience: whole-notebook work. */
export function runNodeSideNotebookTask(
  notebook: string,
  task: string,
  timeout: number
) {
  measuredNotebookId(notebook, timeout).then((notebookId) =>
    cy.getCookies().then((cookies) =>
      cy.task(
        task,
        {
          origin: Cypress.config('baseUrl'),
          notebookId,
          cookie: cookies.map((c) => `${c.name}=${c.value}`).join('; '),
        },
        { timeout, log: false }
      )
    )
  )
}
