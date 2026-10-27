import {
  AssistantManagementController,
  UserController,
} from "@generated/donut-backend-api/sdk.gen"
import { cleanup, fireEvent } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import {
  afterAll,
  afterEach,
  beforeAll,
  describe,
  expect,
  it,
  vi,
} from "vitest"
import { RouterView } from "vue-router"
import makeMe from "donut-test-fixtures/makeMe"
import helper, {
  mockSdkService,
  productionRouterAt,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"
import {
  showToastsOnPage,
  toastMessage,
  toastShown,
} from "@tests/helpers/toastTestSupport"
import ConnectedAssistantsSettingsTab from "@/pages/settings/ConnectedAssistantsSettingsTab.vue"
import { namedLocationHref } from "@/routes/namedLocationHref"
import {
  setupGlobalClient,
  teardownGlobalClientForTesting,
} from "@/managedApi/clientSetup"

function renderAssistants() {
  return helper.component(ConnectedAssistantsSettingsTab).render()
}

describe("ConnectedAssistantsSettingsTab", () => {
  showToastsOnPage()
  beforeAll(() => setupGlobalClient({ states: [] }))
  afterAll(teardownGlobalClientForTesting)
  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it("identifies the connected assistant and refreshes the list after withdrawing with the returned CSRF header", async () => {
    const management = makeMe.anAssistantManagement.please()
    const list = mockSdkService(
      AssistantManagementController,
      "getConnectedAssistants",
      management
    )
    const withdraw = mockSdkService(
      AssistantManagementController,
      "withdrawAssistant",
      undefined
    )
    const view = renderAssistants()
    await flushPromises()
    expect(view.getByText("Test assistant")).toBeVisible()
    expect(
      view.getByText(/read notebooks you can currently read/)
    ).toBeVisible()
    list.mockResolvedValue(
      wrapSdkResponse(makeMe.anAssistantManagement.assistants().please())
    )

    await fireEvent.click(
      view.getByLabelText("Withdraw access for Test assistant")
    )
    await flushPromises()

    expect(withdraw).toHaveBeenCalledWith({
      path: { clientId: "test-assistant" },
      headers: { "X-XSRF-TOKEN": "csrf-token" },
    })
    expect(list).toHaveBeenCalledTimes(2)
    expect(view.getByText("No connected assistants.")).toBeVisible()
  })

  it("shows pending loading and prevents another withdrawal while the action is unfinished", async () => {
    const management = makeMe.anAssistantManagement.please()
    const list = mockSdkService(
      AssistantManagementController,
      "getConnectedAssistants",
      management
    )
    let finishLoad!: (
      value: ReturnType<typeof wrapSdkResponse<typeof management>>
    ) => void
    list.mockReturnValueOnce(
      new Promise((resolve) => {
        finishLoad = resolve
      })
    )
    const view = renderAssistants()
    await flushPromises()
    expect(view.container.querySelector("[data-app-busy]")).toBeVisible()
    finishLoad(wrapSdkResponse(management))
    await flushPromises()
    const withdraw = mockSdkService(
      AssistantManagementController,
      "withdrawAssistant",
      undefined
    )
    let finishWithdrawal!: (
      value: ReturnType<typeof wrapSdkResponse<undefined>>
    ) => void
    withdraw.mockReturnValueOnce(
      new Promise((resolve) => {
        finishWithdrawal = resolve
      })
    )
    const button = view.getByLabelText("Withdraw access for Test assistant")
    await fireEvent.click(button)
    expect(button).toBeDisabled()
    finishWithdrawal(wrapSdkResponse(undefined))
    await flushPromises()
    expect(button).toBeEnabled()
  })

  it("offers retry when the list cannot be loaded", async () => {
    const list = mockSdkService(
      AssistantManagementController,
      "getConnectedAssistants",
      makeMe.anAssistantManagement.assistants().please()
    )
    list.mockResolvedValueOnce(wrapSdkError({ message: "Please try again" }))
    const view = renderAssistants()
    await flushPromises()
    expect(view.getByText("Unable to load connected assistants.")).toBeVisible()

    await fireEvent.click(view.getByText("Try again"))
    await flushPromises()

    expect(view.getByText("No connected assistants.")).toBeVisible()
  })

  it("keeps the current assistant and reports a refused withdrawal", async () => {
    const list = mockSdkService(
      AssistantManagementController,
      "getConnectedAssistants",
      makeMe.anAssistantManagement.please()
    )
    mockSdkService(
      AssistantManagementController,
      "withdrawAssistant",
      undefined
    ).mockResolvedValue(
      wrapSdkError({ message: "Access could not be withdrawn" })
    )
    const view = renderAssistants()
    await flushPromises()
    await fireEvent.click(
      view.getByLabelText("Withdraw access for Test assistant")
    )
    await flushPromises()

    expect(view.getByText("Test assistant")).toBeVisible()
    expect(list).toHaveBeenCalledTimes(1)
    expect(toastMessage(await toastShown("error"))).toContain(
      "Access could not be withdrawn"
    )
  })

  it("opens connected assistants from Settings and retains personal-token management", async () => {
    mockSdkService(
      AssistantManagementController,
      "getConnectedAssistants",
      makeMe.anAssistantManagement.please()
    )
    mockSdkService(UserController, "getTokens", [])
    const router = await productionRouterAt({ name: "settingsAccessTokens" })
    const view = helper
      .component(RouterView)
      .withRealRouter(router)
      .withRoutedPages()
      .render()
    await flushPromises()
    expect(view.getByText("Generate Token")).toBeVisible()
    const link = view.getByText("Connected Assistants")
    expect(link.getAttribute("href")).toBe(
      namedLocationHref({ name: "settingsConnectedAssistants" })
    )

    await fireEvent.click(link)
    await flushPromises()

    expect(router.currentRoute.value.name).toBe("settingsConnectedAssistants")
    expect(view.getByText("Test assistant")).toBeVisible()
    expect(view.getByText("Access Tokens")).toBeVisible()
  })
})
