import {
  BazaarController,
  CircleController,
} from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, describe, expect, it } from "vitest"
import { page } from "vitest/browser"
import {
  atViewportWidth,
  mountNotebookWorkspace,
} from "./notebookWorkspaceHeaderTestSupport"
import {
  deepNote,
  notebookLocationControl,
} from "./notebookWorkspaceContextTestSupport"

describe("Notebook workspace realm navigation", () => {
  let wrapper: VueWrapper
  afterEach(() => {
    wrapper.unmount()
    document.body.innerHTML = ""
  })
  it.each(["circle", "bazaar"])(
    "opens the %s destination from the actual location disclosure",
    async (realmKind) => {
      await atViewportWidth(768)
      const realm = deepNote()
      const circle = { ...makeMe.aCircleNote.please(), name: "Orchard circle" }
      if (realmKind === "circle") {
        realm.notebookRealm.notebook.circle = circle
        mockSdkService(CircleController, "showCircle", circle)
      } else {
        realm.notebookRealm.readonly = true
        mockSdkService(BazaarController, "bazaar", [])
      }
      const mounted = await mountNotebookWorkspace([realm])
      wrapper = mounted.wrapper
      await page.elementLocator(notebookLocationControl()).click()
      await page.elementLocator(document.querySelector("dialog a")!).click()
      await flushPromises()
      if (realmKind === "circle") {
        expect(mounted.router.currentRoute.value).toMatchObject({
          name: "circleShow",
          params: { circleId: String(circle.id) },
        })
        expect(document.body.textContent).toContain(`Circle: ${circle.name}`)
      } else {
        expect(mounted.router.currentRoute.value.name).toBe("bazaar")
        expect(document.body.textContent).toContain("Welcome To The Bazaar")
      }
    }
  )
})
