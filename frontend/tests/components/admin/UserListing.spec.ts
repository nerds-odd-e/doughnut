import UserListing from "@/components/admin/UserListing.vue"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"

describe("UserListing", () => {
  it("displays loading message initially", async () => {
    mockSdkService("listUsers", makeMe.aUserListingPage.please())
    const wrapper = helper.component(UserListing).mount()
    expect(wrapper.text()).toContain("Loading users...")
  })

  it("displays users after loading", async () => {
    const user1 = makeMe.aUserForListing.withName("Alice").please()
    const user2 = makeMe.aUserForListing.withName("Bob").please()
    const userPage = makeMe.aUserListingPage.withUsers([user1, user2]).please()
    mockSdkService("listUsers", userPage)

    const wrapper = helper.component(UserListing).mount()
    await flushPromises()

    expect(wrapper.text()).toContain("Alice")
    expect(wrapper.text()).toContain("Bob")
  })

  it("displays note count for users", async () => {
    const user = makeMe.aUserForListing
      .withName("Alice")
      .withNoteCount(42)
      .please()
    const userPage = makeMe.aUserListingPage.withUsers([user]).please()
    mockSdkService("listUsers", userPage)

    const wrapper = helper.component(UserListing).mount()
    await flushPromises()

    expect(wrapper.text()).toContain("42")
  })

  it("displays memory tracker count for users", async () => {
    const user = makeMe.aUserForListing
      .withName("Alice")
      .withMemoryTrackerCount(15)
      .please()
    const userPage = makeMe.aUserListingPage.withUsers([user]).please()
    mockSdkService("listUsers", userPage)

    const wrapper = helper.component(UserListing).mount()
    await flushPromises()

    expect(wrapper.text()).toContain("15")
  })

  it("displays formatted times", async () => {
    const user = makeMe.aUserForListing
      .withName("Alice")
      .withLastNoteTime("2025-06-15T10:30:00Z")
      .please()
    const userPage = makeMe.aUserListingPage.withUsers([user]).please()
    mockSdkService("listUsers", userPage)

    const wrapper = helper.component(UserListing).mount()
    await flushPromises()

    expect(wrapper.text()).toContain("2025")
  })

  it("displays dash for missing times", async () => {
    const user = makeMe.aUserForListing.withName("Alice").please()
    const userPage = makeMe.aUserListingPage.withUsers([user]).please()
    mockSdkService("listUsers", userPage)

    const wrapper = helper.component(UserListing).mount()
    await flushPromises()

    expect(wrapper.text()).toContain("-")
  })

  describe("pagination", () => {
    it("shows pagination when there are multiple pages", async () => {
      const user = makeMe.aUserForListing.please()
      const userPage = makeMe.aUserListingPage
        .withUsers([user])
        .withTotalCount(25)
        .please()
      mockSdkService("listUsers", userPage)

      const wrapper = helper.component(UserListing).mount()
      await flushPromises()

      expect(wrapper.text()).toContain("Page 1 of 3")
    })

    it("hides pagination when there is only one page", async () => {
      const user = makeMe.aUserForListing.please()
      const userPage = makeMe.aUserListingPage.withUsers([user]).please()
      mockSdkService("listUsers", userPage)

      const wrapper = helper.component(UserListing).mount()
      await flushPromises()

      expect(wrapper.text()).not.toContain("Page")
    })
  })
})
