import { describe, it, expect, beforeEach, afterEach } from "vitest"
import usePopups from "@/components/commons/Popups/usePopups"
import type { OptionsPopupInfo } from "@/components/commons/Popups/usePopups"

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0))

describe("usePopups", () => {
  let popups: ReturnType<typeof usePopups>["popups"]

  beforeEach(() => {
    popups = usePopups().popups
    while (popups.peek()?.length) {
      popups.done(true)
    }
  })

  afterEach(() => {
    while (popups.peek()?.length) {
      popups.done(true)
    }
  })

  describe("options popup", () => {
    const testOptions = [
      { label: "Option A", value: "a" },
      { label: "Option B", value: "b" },
      { label: "Option C", value: "c" },
    ]

    it("creates an options popup with correct type and options", () => {
      popups.options("Choose an option", testOptions)

      const peeked = popups.peek()
      expect(peeked).toHaveLength(1)
      expect(peeked?.[0]?.type).toBe("options")
      expect(peeked?.[0]?.message).toBe("Choose an option")
      expect((peeked?.[0] as OptionsPopupInfo)?.options).toEqual(testOptions)
    })

    it("resolves with selected value when done is called", async () => {
      const optionsPromise = popups.options("Choose an option", testOptions)

      popups.done("b")

      expect(await optionsPromise).toBe("b")
    })

    it("resolves with null when ESC is pressed", async () => {
      const optionsPromise = popups.options("Choose an option", testOptions)

      document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))
      await flushPromises()

      expect(await optionsPromise).toBeNull()
    })
  })

  describe("ESC key handling", () => {
    it("closes single popup when ESC is pressed", async () => {
      const alertPromise = popups.alert("Single alert")
      expect(popups.peek()).toHaveLength(1)

      document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))
      await flushPromises()

      expect(await alertPromise).toBe(true)
      expect(popups.peek()).toHaveLength(0)
    })

    it("closes nested popups one at a time", async () => {
      const alert1Promise = popups.alert("First alert")
      const confirm2Promise = popups.confirm("Second confirm")
      const alert3Promise = popups.alert("Third alert")

      expect(popups.peek()).toHaveLength(3)

      document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))
      await flushPromises()
      expect(await alert3Promise).toBe(true)
      expect(popups.peek()).toHaveLength(2)

      document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))
      await flushPromises()
      expect(await confirm2Promise).toBe(false)
      expect(popups.peek()).toHaveLength(1)

      document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))
      await flushPromises()
      expect(await alert1Promise).toBe(true)
      expect(popups.peek()).toHaveLength(0)
    })
  })
})
