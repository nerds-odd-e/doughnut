import NotebookGroupPage from "@/pages/NotebookGroupPage.vue"
import { describe, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { donutMenuLaunchersIn } from "@tests/toolbars/donutMenuTestSupport"
import { mockMyNotebooks } from "./notebooksPageTestSupport"

const mountGroupPage = async (groupId: number) => {
  const wrapper = helper
    .component(NotebookGroupPage)
    .withCurrentUser(makeMe.aUser.please())
    .withRouter()
    .withProps({ groupId })
    .mount()
  await flushPromises()
  return wrapper
}

describe("Notebook group page", () => {
  it("keeps one Donut menu beside the group name", async () => {
    const member = makeMe.aNotebook.please()
    mockMyNotebooks({
      notebooks: [{ notebook: member }],
      catalogItems: [
        makeMe.notebookCatalogGroup
          .id(7)
          .name("Languages")
          .membersFromNotebooks([member])
          .please(),
      ],
    })
    const wrapper = await mountGroupPage(7)

    expect(wrapper.get("h1").text()).toBe("Languages")
    expect(donutMenuLaunchersIn(wrapper)).toHaveLength(1)
  })

  it("keeps the Donut menu when the group cannot be found", async () => {
    mockMyNotebooks({ notebooks: [] })
    const wrapper = await mountGroupPage(404)

    expect(wrapper.text()).toContain("This notebook group could not be found.")
    expect(donutMenuLaunchersIn(wrapper)).toHaveLength(1)
  })
})
