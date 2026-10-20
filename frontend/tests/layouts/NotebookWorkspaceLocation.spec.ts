import {
  NotebookAttachmentController,
  NotebookController,
  NotebookFolderController,
} from "@generated/donut-backend-api/sdk.gen"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { notePropertyLocation } from "@/routes/noteShowLocation"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { page, userEvent } from "vitest/browser"
import {
  atViewportWidth,
  mountNotebookWorkspace,
} from "./notebookWorkspaceHeaderTestSupport"
import {
  deepNote,
  innerFolder,
  longNotebook,
  mountDeepNote,
  notebookLocationControl,
  parentFolder,
} from "./notebookWorkspaceContextTestSupport"

const rect = (el: Element) => el.getBoundingClientRect()

describe("Notebook workspace location", () => {
  let wrapper: VueWrapper

  beforeEach(() => {
    useAssimilationView().dismiss()
  })

  afterEach(() => {
    wrapper.unmount()
    document.body.innerHTML = ""
  })

  it("discloses the complete narrow trail and opens a parent folder by its link", async () => {
    const mounted = await mountDeepNote(320)
    const { router, realm } = mounted
    wrapper = mounted.wrapper
    const folderRealm = makeMe.aFolderRealm
      .folder(parentFolder.id, parentFolder.name)
      .please()
    folderRealm.notebookRealm = realm.notebookRealm
    mockSdkService(NotebookFolderController, "getFolderPage", folderRealm)

    await page.elementLocator(notebookLocationControl()).click()
    await flushPromises()
    const link = [...document.querySelectorAll("dialog a")].find((a) =>
      a.textContent?.includes(parentFolder.name)
    )!
    await page.elementLocator(link).click()
    await flushPromises()

    await vi.waitFor(() => {
      expect(router.currentRoute.value.name).toBe("folderPage")
      expect(router.currentRoute.value.params.folderId).toBe(
        String(parentFolder.id)
      )
      expect(document.querySelector("[data-note-toolbar]")).toBeNull()
      expect(
        document.querySelector(
          '[data-note-sidebar-toolbar] [title="New folder"]'
        )
      ).not.toBeNull()
    })
  })

  it.each([320, 768, 1280])(
    "opens full names by keyboard, fits every target and restores focus at %ipx",
    async (width) => {
      const mounted = await mountDeepNote(width)
      wrapper = mounted.wrapper
      const control = notebookLocationControl()
      const current = mounted.router.currentRoute.value.fullPath
      expect(control.scrollWidth).toBeLessThanOrEqual(control.clientWidth)
      const notebookText = control.querySelector(".font-semibold")!
      const trailText = control.querySelector(".text-xs")!
      expect(rect(notebookText).bottom).toBeLessThanOrEqual(rect(trailText).top)
      control.focus()
      await userEvent.keyboard("{Enter}")
      await flushPromises()
      const dialog = document.querySelector<HTMLDialogElement>(
        'dialog[aria-label="Notebook location"]'
      )!
      expect(dialog.open).toBe(true)
      expect(mounted.router.currentRoute.value.fullPath).toBe(current)
      const links = [...dialog.querySelectorAll("a")]
      expect(links.map((link) => link.textContent?.trim())).toEqual([
        longNotebook,
        parentFolder.name,
        innerFolder.name,
      ])
      for (const link of links) {
        expect(rect(link).left).toBeGreaterThanOrEqual(0)
        expect(rect(link).right).toBeLessThanOrEqual(width)
        expect(rect(link).height).toBeGreaterThanOrEqual(44)
        expect(link.scrollWidth).toBeLessThanOrEqual(link.clientWidth)
      }
      await page.screenshot({
        path: `../../vitest-test-results/sidebar-location-${width}-open.png`,
      })
      await userEvent.keyboard("{Escape}")
      await flushPromises()
      expect(document.activeElement).toBe(control)
      expect(control.getAttribute("aria-expanded")).toBe("false")
      await page.elementLocator(control).click()
      await page
        .elementLocator(document.querySelector('[aria-label="Close modal"]')!)
        .click()
      await flushPromises()
      expect(document.activeElement).toBe(control)
      await page.screenshot({
        path: `../../vitest-test-results/sidebar-location-${width}-compact.png`,
      })
    }
  )

  it.each([320, 768])(
    "navigates to notebook root and keeps useful identity with an empty trail at %ipx",
    async (width) => {
      const mounted = await mountDeepNote(width)
      wrapper = mounted.wrapper
      mockSdkService(NotebookController, "get", mounted.realm.notebookRealm)
      await page.elementLocator(notebookLocationControl()).click()
      await page.elementLocator(document.querySelector("dialog a")!).click()
      await flushPromises()
      expect(mounted.router.currentRoute.value.name).toBe("notebookPage")
      expect(mounted.router.currentRoute.value.params.notebookId).toBe(
        String(mounted.realm.notebookRealm.notebook.id)
      )
      if (width < 768) {
        await page
          .elementLocator(
            document.querySelector('[aria-label="Show sidebar"]')!
          )
          .click()
        await flushPromises()
      }
      expect(
        document.querySelector("aside")!.contains(notebookLocationControl())
      ).toBe(true)
      expect(notebookLocationControl().textContent).toContain(longNotebook)
      expect(rect(notebookLocationControl()).height).toBeGreaterThanOrEqual(44)
      expect(notebookLocationControl().scrollWidth).toBeLessThanOrEqual(
        notebookLocationControl().clientWidth
      )
      await page.elementLocator(notebookLocationControl()).click()
      expect(
        [...document.querySelectorAll("dialog a")].map((link) =>
          link.textContent?.trim()
        )
      ).toEqual([longNotebook])
      const rootLink = document.querySelector<HTMLAnchorElement>("dialog a")!
      expect(rect(rootLink).right).toBeLessThanOrEqual(width)
      expect(rootLink.scrollWidth).toBeLessThanOrEqual(rootLink.clientWidth)
      await page.screenshot({
        path: `../../vitest-test-results/sidebar-location-${width}-root.png`,
      })
    }
  )

  it("retains folder context on a focused property route", async () => {
    await atViewportWidth(768)
    const realm = deepNote()
    ;({ wrapper } = await mountNotebookWorkspace(
      [realm],
      notePropertyLocation(realm.id, "topic")
    ))
    await page.elementLocator(notebookLocationControl()).click()
    expect(document.querySelector("dialog nav")?.textContent).toContain(
      innerFolder.name
    )
  })

  it("uses the attachment endpoint realm and navigates to its containing folder", async () => {
    const mounted = await mountDeepNote(768)
    wrapper = mounted.wrapper
    mockSdkService(NotebookAttachmentController, "getAttachmentPage", {
      notebookRealm: mounted.realm.notebookRealm,
      ancestorFolders: [parentFolder],
      attachment: { id: 123, filename: "orchard.pdf" },
      size: 100,
      image: false,
      references: [],
    })
    mockSdkService(NotebookFolderController, "getFolderPage", {
      ...makeMe.aFolderRealm
        .folder(parentFolder.id, parentFolder.name)
        .please(),
      notebookRealm: mounted.realm.notebookRealm,
      ancestorFolders: [parentFolder],
    })
    await mounted.router.push({
      name: "attachmentPage",
      params: {
        notebookId: String(mounted.realm.notebookRealm.notebook.id),
        attachmentId: "123",
      },
    })
    await flushPromises()
    expect(notebookLocationControl().textContent).toContain(parentFolder.name)
    await page.elementLocator(notebookLocationControl()).click()
    await page
      .elementLocator(
        [...document.querySelectorAll("dialog a")].find((a) =>
          a.textContent?.includes(parentFolder.name)
        )!
      )
      .click()
    await flushPromises()
    expect(mounted.router.currentRoute.value.name).toBe("folderPage")
    expect(mounted.router.currentRoute.value.params.folderId).toBe(
      String(parentFolder.id)
    )
    expect(notebookLocationControl().textContent).toContain(parentFolder.name)
  })
})
