import DonutApp from "@/DonutApp.vue"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import { browserLocation } from "@/managedApi/window/browserLocation"
import {
  CurrentUserInfoController,
  TestabilityRestController,
} from "@generated/donut-backend-api/sdk.gen"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import createFetchMock from "vitest-fetch-mock"
import { nextTick } from "vue"

const fetchMock = createFetchMock(vi)
fetchMock.enableMocks()
const loadedEntry = "data:text/javascript,//entry-A"
const reminder = () =>
  document.querySelector('[data-testid="frontend-update-reminder"]')

describe("DonutApp frontend update reminder", () => {
  let entryScript: HTMLScriptElement

  beforeEach(() => {
    entryScript = document.createElement("script")
    entryScript.type = "module"
    entryScript.src = loadedEntry
    document.head.appendChild(entryScript)
    vi.spyOn(document, "visibilityState", "get").mockReturnValue("visible")
    mockSdkService(TestabilityRestController, "getFeatureToggle", false)
    mockSdkService(CurrentUserInfoController, "currentUserInfo", {
      user: undefined,
      externalIdentifier: undefined,
    })
  })

  afterEach(() => {
    entryScript.remove()
    fetchMock.resetMocks()
    vi.restoreAllMocks()
    teardownGlobalClientForTesting()
  })

  const returnToTabWhileServing = async (entry: string) => {
    const servedIndexHtml = document.documentElement.outerHTML.replace(
      loadedEntry,
      entry
    )
    fetchMock.mockResponse((request) =>
      new URL(request.url).pathname === "/" ? servedIndexHtml : ""
    )
    const servedHtmlRead = vi.spyOn(DOMParser.prototype, "parseFromString")
    helper.component(DonutApp).withRouter().render()
    await flushPromises()
    document.dispatchEvent(new Event("visibilitychange"))
    await vi.waitFor(() => expect(servedHtmlRead).toHaveBeenCalled())
    await nextTick()
  }

  it("offers a reload when the served frontend has a different entry", async () => {
    const reload = vi
      .spyOn(browserLocation, "reload")
      .mockImplementation(() => undefined)
    await returnToTabWhileServing("data:text/javascript,//entry-B")

    expect(reminder()?.textContent).toContain(
      "A newer version of Donut is available"
    )
    reminder()!.querySelector("button")!.click()
    expect(reload).toHaveBeenCalled()
  })

  it("shows no reminder when the served frontend is the loaded one", async () => {
    await returnToTabWhileServing(loadedEntry)

    expect(reminder()).toBeNull()
  })
})
