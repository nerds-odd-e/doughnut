import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"
import usePopups from "@/components/commons/Popups/usePopups"
import type { OptionsPopupInfo } from "@/components/commons/Popups/usePopups"
import { flushPromises } from "@vue/test-utils"

describe("usePopups", () => {
  let popups: ReturnType<typeof usePopups>["popups"]

  beforeEach(() => {
    popups = usePopups().popups
    // Clear any existing popups
    while (popups.peek()?.length) {
      popups.done(true)
    }
  })

  afterEach(() => {
    // Clean up any remaining event listeners
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
      expect(peeked?.length).toBe(1)
      expect(peeked?.[0]?.type).toBe("options")
      expect(peeked?.[0]?.message).toBe("Choose an option")
      expect((peeked?.[0] as OptionsPopupInfo)?.options).toEqual(testOptions)
    })

    it("resolves with selected value when done is called", async () => {
      const optionsPromise = popups.options("Choose an option", testOptions)

      popups.done("b")

      const result = await optionsPromise
      expect(result).toBe("b")
      expect(popups.peek()?.length).toBe(0)
    })

    it("resolves with null when ESC is pressed", async () => {
      const optionsPromise = popups.options("Choose an option", testOptions)
      expect(popups.peek()?.length).toBe(1)

      document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))
      await flushPromises()

      const result = await optionsPromise
      expect(result).toBeNull()
      expect(popups.peek()?.length).toBe(0)
    })
  })

  describe("ESC key handling", () => {
    it("closes single popup when ESC is pressed", async () => {
      const alertPromise = popups.alert("Single alert")
      expect(popups.peek()?.length).toBe(1)

      const escapeEvent = new KeyboardEvent("keydown", { key: "Escape" })
      document.dispatchEvent(escapeEvent)
      await flushPromises()

      const result = await alertPromise
      expect(result).toBe(true)
      expect(popups.peek()?.length).toBe(0)
    })

    it("closes nested popups one at a time", async () => {
      // Create three nested popups
      const alert1Promise = popups.alert("First alert")
      const confirm2Promise = popups.confirm("Second confirm")
      const alert3Promise = popups.alert("Third alert")

      expect(popups.peek()?.length).toBe(3)

      // Close third popup
      document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))
      await flushPromises()
      const result3 = await alert3Promise
      expect(result3).toBe(true)
      expect(popups.peek()?.length).toBe(2)

      // Close second popup
      document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))
      await flushPromises()
      const result2 = await confirm2Promise
      expect(result2).toBe(false)
      expect(popups.peek()?.length).toBe(1)

      // Close first popup
      document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))
      await flushPromises()
      const result1 = await alert1Promise
      expect(result1).toBe(true)
      expect(popups.peek()?.length).toBe(0)
    })

    it("properly handles event listeners", async () => {
      const addEventListenerSpy = vi.spyOn(document, "addEventListener")
      const removeEventListenerSpy = vi.spyOn(document, "removeEventListener")

      // Open first popup
      const alert1Promise = popups.alert("First alert")
      expect(addEventListenerSpy).toHaveBeenCalledTimes(1)

      // Open second popup
      const alert2Promise = popups.alert("Second alert")
      expect(addEventListenerSpy).toHaveBeenCalledTimes(1) // Should not add another listener

      // Close second popup
      document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))
      await flushPromises()
      await alert2Promise
      expect(removeEventListenerSpy).not.toHaveBeenCalled() // Should not remove listener yet

      // Close first popup
      document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))
      await flushPromises()
      await alert1Promise
      expect(removeEventListenerSpy).toHaveBeenCalledTimes(1) // Should remove listener

      addEventListenerSpy.mockRestore()
      removeEventListenerSpy.mockRestore()
    })
  })
})
