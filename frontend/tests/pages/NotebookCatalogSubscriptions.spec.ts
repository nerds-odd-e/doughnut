import NotebooksPageView from "@/pages/NotebooksPageView.vue"
import { beforeEach, describe, expect, it } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { clearNotebooksPageStorage } from "./notebooksPageTestSupport"

describe("subscribed notebooks in merged catalog", () => {
  beforeEach(() => {
    clearNotebooksPageStorage()
  })

  it("shows subscription actions for a subscribed member inside a group", async () => {
    const ownedMember = makeMe.aNotebook.title("Owned In Group").please()
    const subMember = makeMe.aNotebook.title("Subscribed In Group").please()
    const catalogItems = [
      makeMe.notebookCatalogGroup
        .id(1)
        .name("Mixed Group")
        .createdAt("2020-01-01T00:00:00.000Z")
        .membersFromNotebooks([ownedMember, subMember])
        .please(),
    ]
    const subscriptions = [
      {
        id: 99,
        dailyTargetOfNewNotes: 5,
        notebook: subMember,
        user: makeMe.aUser.please(),
      },
    ]

    const wrapper = helper
      .component(NotebooksPageView)
      .withProps({
        catalogItems,
        subscriptions,
        user: makeMe.aUser.please(),
      })
      .withCurrentUser(makeMe.aUser.please())
      .withRouter()
      .mount()

    await flushPromises()

    expect(wrapper.findAll('button[title="Unsubscribe"]')).toHaveLength(1)
    const subMemberCard = wrapper
      .findAll('[data-cy="notebook-card"]')
      .find((c) => c.text().includes("Subscribed In Group"))
    expect(subMemberCard).toBeDefined()
    await fireEvent.click(
      subMemberCard!.get('[data-cy="notebook-catalog-overflow"]').element
    )
    await flushPromises()
    expect(screen.getByTitle("Edit subscription")).toBeInTheDocument()
  })
})
