import { UserController } from "@generated/donut-backend-api/sdk.gen"
import MainMenu from "@/components/toolbars/MainMenu.vue"
import { useSignedInMenuData } from "@/composables/useSignedInMenuData"
import type { User } from "@generated/donut-backend-api"
import { fireEvent, screen } from "@testing-library/vue"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService, productionRouterAt } from "@tests/helpers"
import { resetRecallData } from "@tests/helpers/recallDataTestSupport"
import { beforeEach, afterEach, vi } from "vitest"
import { defineComponent, h, type PropType, toRef } from "vue"
import type { Router } from "vue-router"
import { defaultMenuData } from "./mainMenuMocks"

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

// Mounts the main menu beside the app-shell menu data, as DonutApp does.
const MainMenuInAppShell = defineComponent({
  props: { user: { type: Object as PropType<User>, required: false } },
  setup(props) {
    useSignedInMenuData(toRef(props, "user"))
    return () => h(MainMenu, { user: props.user })
  },
})

export const mountMainMenu = () =>
  helper
    .component(MainMenuInAppShell)
    .withProps({ user })
    .withRouter(router)
    .render()

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
  beforeEach(async () => {
    vi.clearAllMocks()
    router = await productionRouterAt({ name: "root" })
    createMatchMediaSpy(true)
    mockSdkService(UserController, "getMenuData", defaultMenuData)
    resetRecallData()
    user = makeMe.aUser.please()
  })

  afterEach(() => {
    document.body.innerHTML = ""
    vi.restoreAllMocks()
  })
}
