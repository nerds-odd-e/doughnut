import Popups from "@/components/commons/Popups/Popups.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import { cleanup, render, screen } from "@testing-library/vue"
import { productionRouterAt } from "@tests/helpers"
import { nextTick } from "vue"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import { userEvent } from "vitest/browser"

describe("confirmation popup", () => {
  beforeEach(async () => {
    render(Popups, {
      global: { plugins: [await productionRouterAt({ name: "root" })] },
    })
  })

  afterEach(cleanup)

  it("confirms when Enter is pressed after opening", async () => {
    const result = usePopups().popups.confirm("Continue?")
    await nextTick()

    await userEvent.keyboard("{Enter}")

    expect(await result).toBe(true)
  })

  it("cancels when Escape is pressed", async () => {
    const result = usePopups().popups.confirm("Continue?")
    await nextTick()

    await userEvent.keyboard("{Escape}")

    expect(await result).toBe(false)
  })

  it("cancels when Cancel is clicked", async () => {
    const result = usePopups().popups.confirm("Continue?")
    await nextTick()

    await userEvent.click(screen.getByText("Cancel"))

    expect(await result).toBe(false)
  })
})
