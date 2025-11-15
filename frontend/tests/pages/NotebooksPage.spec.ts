import NotebooksPage from "@/pages/NotebooksPage.vue"
import { describe, it } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

describe("Notebooks Page", () => {
  it("fetch API to be called ONCE", async () => {
    const notebook = makeMe.aNotebook.please()

    vi.spyOn(helper.managedApi.services, "myNotebooks").mockResolvedValue({
      notebooks: [notebook],
      subscriptions: [],
    } as never)
    helper.component(NotebooksPage).withRouter().render()
    expect(helper.managedApi.services.myNotebooks).toBeCalledTimes(1)
  })
})
