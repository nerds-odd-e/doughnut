import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import NotebooksPage from "@/pages/NotebooksPage.vue"
import { beforeEach, describe, expect, it } from "vitest"
import makeMe, {
  type NotebookCatalogEntry,
} from "doughnut-test-fixtures/makeMe"
import { NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY } from "@/composables/useNoteSidebarPeerSort"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"

describe("notebook updates", () => {
  beforeEach(() => {
    localStorage.removeItem("doughnut.notebooksPage.sortOrder")
    localStorage.removeItem("doughnut.notebooksPage.layout")
    sessionStorage.removeItem(NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY)
  })

  it("patches grouped notebook in catalogItems when notebook-updated fires", async () => {
    const member = {
      ...makeMe.aNotebook.please(),
      name: "Member Title",
    }
    const catalogItems = [
      makeMe.notebookCatalogGroup
        .id(1)
        .name("G")
        .createdAt("2020-01-01T00:00:00.000Z")
        .membersFromNotebooks([member])
        .please(),
    ]

    mockSdkService(NotebookController, "myNotebooks", {
      notebooks: [{ notebook: member }],
      catalogItems,
      subscriptions: [],
    })

    const wrapper = helper
      .component(NotebooksPage)
      .withCurrentUser(makeMe.aUser.please())
      .withRouter()
      .mount()

    await flushPromises()

    const vm = wrapper.vm as unknown as {
      catalogItems: NotebookCatalogEntry[] | undefined
    }

    const updated = { ...member, name: "Renamed Member" }
    const buttons = wrapper.findComponent({ name: "NotebookButtons" })
    buttons.vm.$emit("notebook-updated", updated)
    await flushPromises()

    const grp = vm.catalogItems?.[0]
    expect(grp?.type).toBe("notebookGroup")
    if (grp?.type === "notebookGroup") {
      expect(grp.notebooks[0]?.notebook.name).toBe("Renamed Member")
    }
  })

  it("preserves hasAttachedBook when notebook-updated payload omits it", async () => {
    const notebookEntity = { ...makeMe.aNotebook.please(), name: "T" }
    const updatedNotebook = { ...notebookEntity, name: "Updated" }

    mockSdkService(NotebookController, "myNotebooks", {
      notebooks: [{ notebook: notebookEntity, hasAttachedBook: true }],
      catalogItems: makeMe.notebookCatalog
        .notebooks({ ...notebookEntity, hasAttachedBook: true })
        .please(),
      subscriptions: [],
    })

    const wrapper = helper
      .component(NotebooksPage)
      .withCurrentUser(makeMe.aUser.please())
      .withRouter()
      .mount()

    await flushPromises()

    const vm = wrapper.vm as unknown as {
      catalogItems: NotebookCatalogEntry[] | undefined
    }

    const notebookButtons = wrapper.findComponent({ name: "NotebookButtons" })
    notebookButtons.vm.$emit("notebook-updated", updatedNotebook)
    await flushPromises()

    if (vm.catalogItems?.[0]?.type === "notebook") {
      expect(vm.catalogItems[0].hasAttachedBook).toBe(true)
      expect(vm.catalogItems[0].notebook.name).toBe("Updated")
    }
  })
})
