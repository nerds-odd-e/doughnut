import type { ApiStatus } from "@/managedApi/ApiStatusHandler"
import { apiCallWithLoading, setupGlobalClient } from "@/managedApi/clientSetup"
import { UserController } from "@generated/doughnut-backend-api/sdk.gen"
import { beforeEach, describe, expect, it, vi } from "vitest"
import createFetchMock from "vitest-fetch-mock"

const fetchMock = createFetchMock(vi)
fetchMock.enableMocks()

const okApiResult = {
  data: {},
  error: undefined,
  request: {} as Request,
  response: {} as Response,
}

describe("apiCallWithLoading loading state management", () => {
  const apiStatus: ApiStatus = { states: [] }
  const baseUrl = "http://localhost:9081"

  beforeEach(() => {
    fetchMock.resetMocks()
    apiStatus.states = []
    setupGlobalClient(apiStatus)
  })

  it("sets loading state synchronously before API call", async () => {
    let loadingStateBeforeCall = false
    let loadingStateDuringCall = false

    fetchMock.mockResponse(
      () => {
        loadingStateDuringCall = apiStatus.states.length > 0
        return Promise.resolve(JSON.stringify({ user: {} }))
      },
      { url: `${baseUrl}/api/user` }
    )

    const promise = apiCallWithLoading(() => {
      loadingStateBeforeCall = apiStatus.states.length > 0
      return UserController.getUserProfile({})
    })

    expect(apiStatus.states.length).toBe(1)

    await promise

    expect(loadingStateBeforeCall).toBe(true)
    expect(loadingStateDuringCall).toBe(true)
    expect(apiStatus.states.length).toBe(0)
  })

  it("clears loading state after successful API call", async () => {
    fetchMock.mockResponse(JSON.stringify({ user: {} }), {
      url: `${baseUrl}/api/user`,
    })

    await apiCallWithLoading(() => UserController.getUserProfile({}))

    expect(apiStatus.states.length).toBe(0)
  })

  it("clears loading state even on API error", async () => {
    fetchMock.mockResponse(JSON.stringify({}), {
      url: `${baseUrl}/api/user`,
      status: 500,
    })

    await apiCallWithLoading(() => UserController.getUserProfile({}))

    expect(apiStatus.states.length).toBe(0)
  })

  it("returns the API result correctly", async () => {
    const mockUser = { name: "Test User" }
    fetchMock.mockResponse(JSON.stringify(mockUser), {
      url: `${baseUrl}/api/user`,
      headers: { "Content-Type": "application/json" },
    })

    const { data } = await apiCallWithLoading(() =>
      UserController.getUserProfile({})
    )

    expect(data).toEqual(mockUser)
  })

  it("handles multiple concurrent calls correctly", async () => {
    fetchMock.mockResponse(JSON.stringify({ user: {} }), {
      url: `${baseUrl}/api/user`,
    })

    const promise1 = apiCallWithLoading(() => UserController.getUserProfile({}))
    const promise2 = apiCallWithLoading(() => UserController.getUserProfile({}))

    expect(apiStatus.states.length).toBe(2)

    await Promise.all([promise1, promise2])

    expect(apiStatus.states.length).toBe(0)
  })

  it("adds blocking state with message while a blocking API call is pending", async () => {
    let resolveCall: (value: typeof okApiResult) => void = () => undefined

    const promise = apiCallWithLoading(
      () =>
        new Promise<typeof okApiResult>((resolve) => {
          resolveCall = resolve
        }),
      { blockUi: true, message: "Loading next note..." }
    )

    expect(apiStatus.states).toEqual([
      expect.objectContaining({
        blockUi: true,
        message: "Loading next note...",
      }),
    ])

    resolveCall(okApiResult)
    await promise

    expect(apiStatus.states).toEqual([])
  })

  it("keeps the outer blocking state after a nested blocking call finishes", async () => {
    await apiCallWithLoading(
      async () => {
        expect(apiStatus.states.map((state) => state.message)).toEqual([
          "Outer",
        ])

        await apiCallWithLoading(
          async () => {
            expect(apiStatus.states.map((state) => state.message)).toEqual([
              "Outer",
              "Inner",
            ])
            return okApiResult
          },
          { blockUi: true, message: "Inner" }
        )

        expect(apiStatus.states.map((state) => state.message)).toEqual([
          "Outer",
        ])
        return okApiResult
      },
      { blockUi: true, message: "Outer" }
    )

    expect(apiStatus.states).toEqual([])
  })

  it("clears only the completed blocking state for concurrent calls", async () => {
    let resolveFirst: (value: typeof okApiResult) => void = () => undefined
    let resolveSecond: (value: typeof okApiResult) => void = () => undefined

    const firstCall = apiCallWithLoading(
      () =>
        new Promise<typeof okApiResult>((resolve) => {
          resolveFirst = resolve
        }),
      { blockUi: true, message: "First" }
    )
    const secondCall = apiCallWithLoading(
      () =>
        new Promise<typeof okApiResult>((resolve) => {
          resolveSecond = resolve
        }),
      { blockUi: true, message: "Second" }
    )

    expect(apiStatus.states.map((state) => state.message)).toEqual([
      "First",
      "Second",
    ])

    resolveSecond(okApiResult)
    await secondCall

    expect(apiStatus.states.map((state) => state.message)).toEqual(["First"])

    resolveFirst(okApiResult)
    await firstCall

    expect(apiStatus.states).toEqual([])
  })

  it("cleans up blocking state when the API call rejects", async () => {
    await expect(
      apiCallWithLoading(() => Promise.reject(new Error("network failed")), {
        blockUi: true,
        message: "Loading next note...",
      })
    ).rejects.toThrow("network failed")

    expect(apiStatus.states).toEqual([])
  })
})
