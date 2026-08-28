import { BazaarController } from "@generated/donut-backend-api/sdk.gen"
import BazaarPage from "@/pages/BazaarPage.vue"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"

describe("bazaar page", () => {
  it("fetch API to be called ONCE on mount", async () => {
    const notebook = makeMe.aNotebook.please()
    const bazaarNotebooks = makeMe.bazaarNotebooks.notebooks(notebook).please()
    const bazaarSpy = mockSdkService(
      BazaarController,
      "bazaar",
      bazaarNotebooks
    )
    helper.component(BazaarPage).withRouter().render()
    expect(bazaarSpy).toBeCalledTimes(1)
  })

  it("describes notes from donut users", async () => {
    mockSdkService(BazaarController, "bazaar", makeMe.bazaarNotebooks.please())
    helper.component(BazaarPage).withRouter().render()
    await flushPromises()
    expect(screen.getByText(/donut users/)).toBeTruthy()
  })
})
