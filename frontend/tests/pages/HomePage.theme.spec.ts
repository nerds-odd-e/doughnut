import HomePage from "@/pages/HomePage.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { afterEach, describe, expect, it } from "vitest"

/** Hardcoded light homepage gradient (white → #f5f5f5). */
const HARDCODED_UPPER_GRADIENT =
  "linear-gradient(rgb(255, 255, 255), rgb(245, 245, 245))"
/** Hardcoded light homepage gradient (#f5f5f5 → white). */
const HARDCODED_LOWER_GRADIENT =
  "linear-gradient(rgb(245, 245, 245), rgb(255, 255, 255))"
const HARDCODED_WHITE = "rgb(255, 255, 255)"
const HARDCODED_LIGHT_GRAY = "rgb(245, 245, 245)"

function setTheme(theme: "light" | "dark") {
  document.documentElement.setAttribute("data-theme", theme)
}

function backgroundImageOf(el: Element): string {
  return window.getComputedStyle(el).backgroundImage
}

function backgroundColorOf(el: Element): string {
  return window.getComputedStyle(el).backgroundColor
}

afterEach(() => {
  document.documentElement.removeAttribute("data-theme")
})

describe("HomePage backgrounds follow bright/dark theme", () => {
  it("section and card backgrounds are not locked to light hex and adapt in dark theme", () => {
    const wrapper = helper
      .component(HomePage)
      .withRouter()
      .withCurrentUser(makeMe.aUser.please())
      .mount({
        attachTo: document.body,
        global: { stubs: { GlobalBar: true } },
      })

    const upperHalf = wrapper.find(".upper-half").element
    const lowerHalf = wrapper.find(".lower-half").element
    const noteCard = wrapper.find(".note-card").element
    const cliCode = wrapper.find(".cli-install-code").element

    setTheme("light")
    const lightUpper = backgroundImageOf(upperHalf)
    const lightLower = backgroundImageOf(lowerHalf)
    const lightCard = backgroundColorOf(noteCard)
    const lightCli = backgroundColorOf(cliCode)

    setTheme("dark")
    const darkUpper = backgroundImageOf(upperHalf)
    const darkLower = backgroundImageOf(lowerHalf)
    const darkCard = backgroundColorOf(noteCard)
    const darkCli = backgroundColorOf(cliCode)

    expect(darkUpper).not.toEqual(HARDCODED_UPPER_GRADIENT)
    expect(darkLower).not.toEqual(HARDCODED_LOWER_GRADIENT)
    expect(darkCard).not.toEqual(HARDCODED_WHITE)
    expect(darkCli).not.toEqual(HARDCODED_LIGHT_GRAY)

    expect(darkUpper).not.toEqual(lightUpper)
    expect(darkLower).not.toEqual(lightLower)
    expect(darkCard).not.toEqual(lightCard)
    expect(darkCli).not.toEqual(lightCli)
  })
})
