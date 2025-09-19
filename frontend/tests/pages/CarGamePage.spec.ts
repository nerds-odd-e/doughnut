import { describe, it } from "vitest"
import helper from "@tests/helpers"
import { screen } from "@testing-library/vue"
import CarGamePage from "@/pages/CarGamePage.vue"

describe("Car Game page", () => {
  it("should screen init correctly", async () => {
    helper.component(CarGamePage).withRouter().render()

    // Check initial number of rounds displayed (0)
    expect(
      screen.getByText("Rounds").parentElement?.querySelector("div")
    ).toHaveTextContent("0")
  })
})
