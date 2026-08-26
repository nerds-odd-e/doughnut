import { UserController } from "@generated/donut-backend-api/sdk.gen"
import MainMenu from "@/components/toolbars/MainMenu.vue"
import { useRecallData } from "@/composables/useRecallData"
import { useGoToNextAssimilation } from "@/composables/useGoToNextAssimilation"
import routes from "@/routes/routes"
import type { User } from "@generated/donut-backend-api"
import { fireEvent, screen } from "@testing-library/vue"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { beforeEach, afterEach, vi } from "vitest"
import { createMemoryHistory, createRouter, type Router } from "vue-router"
import { createUseRecallDataMock, defaultMenuData } from "./mainMenuMocks"

export let router: Router
export let user: User

export const createMatchMediaSpy = (matches: boolean) =>
  vi.spyOn(window, "matchMedia").mockImplementation((query: string) => {
    const mediaQueryList = {
      matches,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    } as MediaQueryList
    return mediaQueryList
  })

export const expandMenuIfNeeded = async () => {
  const expandButton = screen.queryByLabelText("Toggle menu")
  if (expandButton) {
    await fireEvent.click(expandButton)
  }
}

export const mountMainMenu = () =>
  helper.component(MainMenu).withProps({ user }).withRouter(router).render()

export const renderComponent = async () => {
  const result = mountMainMenu()
  await expandMenuIfNeeded()
  return result
}

export const expectNavLinkPrimary = (ariaLabel: string) => {
  const link = screen.getByLabelText(ariaLabel)
  expect(link.closest(".nav-item")).toHaveClass("text-primary")
}

export function setupMainMenuTests() {
  beforeEach(() => {
    vi.clearAllMocks()
    router = createRouter({
      history: createMemoryHistory(),
      routes,
    })
    createMatchMediaSpy(true)
    mockSdkService(UserController, "getMenuData", defaultMenuData)
    vi.mocked(useRecallData).mockReturnValue(createUseRecallDataMock())
    vi.mocked(useGoToNextAssimilation).mockReturnValue({
      goToNextAssimilation: vi.fn(),
    })
    user = makeMe.aUser.please()
  })

  afterEach(() => {
    document.body.innerHTML = ""
    vi.restoreAllMocks()
  })
}
