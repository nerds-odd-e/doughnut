import type { ApiStatus } from "@/managedApi/ApiStatusHandler"
import {
  apiCallWithLoading,
  type CancelableApiLoadingOptions,
  type CancelableApiResult,
  setupGlobalClient,
} from "@/managedApi/clientSetup"
import { UserController } from "@generated/doughnut-backend-api/sdk.gen"
import { beforeEach, describe, expect, expectTypeOf, it, vi } from "vitest"
import createFetchMock from "vitest-fetch-mock"

const fetchMock = createFetchMock(vi)
fetchMock.enableMocks()

const okApiResult = {
  data: {},
  error: undefined,
  request: {} as Request,
  response: {} as Response,
}

function controlledApiCall() {
  let resolve: (value: typeof okApiResult) => void = () => undefined
  let reject: (reason: unknown) => void = () => undefined
  const promise = new Promise<typeof okApiResult>(
    (resolvePromise, rejectPromise) => {
      resolve = resolvePromise
      reject = rejectPromise
    }
  )
  return { promise, resolve, reject }
}

describe("apiCallWithLoading loading state management", () => {
  const apiStatus: ApiStatus = { states: [] }
  const baseUrl = "http://localhost:9081"

  beforeEach(() => {
    fetchMock.resetMocks()
    apiStatus.states = []
    setupGlobalClient(apiStatus)
  })

  it("preserves the synchronous raw-result contract for default calls", async () => {
    const call = controlledApiCall()
    let stateWasVisibleDuringCall = false
    const resultPromise = apiCallWithLoading(() => {
      stateWasVisibleDuringCall = apiStatus.states.length === 1
      return call.promise
    })

    expectTypeOf(resultPromise).toEqualTypeOf<Promise<typeof okApiResult>>()
    expect(stateWasVisibleDuringCall).toBe(true)
    expect(apiStatus.states).toEqual([
      expect.not.objectContaining({ cancel: expect.any(Function) }),
    ])

    call.resolve(okApiResult)
    await expect(resultPromise).resolves.toBe(okApiResult)
    expect(apiStatus.states).toEqual([])
  })

  it("clears loading state after an SDK error", async () => {
    fetchMock.mockResponse(JSON.stringify({}), {
      url: `${baseUrl}/api/user`,
      status: 500,
    })

    await apiCallWithLoading(() => UserController.getUserProfile({}))

    expect(apiStatus.states).toEqual([])
  })

  it("keeps the outer blocking state after a nested blocking call finishes", async () => {
    await apiCallWithLoading(
      async () => {
        expect(apiStatus.states.map((state) => state.message)).toEqual([
          "Outer",
        ])
        await apiCallWithLoading(async () => okApiResult, {
          blockUi: true,
          message: "Inner",
        })
        expect(apiStatus.states.map((state) => state.message)).toEqual([
          "Outer",
        ])
        return okApiResult
      },
      { blockUi: true, message: "Outer" }
    )

    expect(apiStatus.states).toEqual([])
  })

  it("clears only the completed state for concurrent calls", async () => {
    const first = controlledApiCall()
    const second = controlledApiCall()
    const firstResult = apiCallWithLoading(() => first.promise, {
      blockUi: true,
      message: "First",
    })
    const secondResult = apiCallWithLoading(() => second.promise, {
      blockUi: true,
      message: "Second",
    })

    second.resolve(okApiResult)
    await secondResult
    expect(apiStatus.states.map((state) => state.message)).toEqual(["First"])

    first.resolve(okApiResult)
    await firstResult
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

  it("provides an identity-bound opt-in cancellation result", async () => {
    const olderCall = controlledApiCall()
    const cancelableCall = controlledApiCall()
    const olderResult = apiCallWithLoading(() => olderCall.promise, {
      blockUi: true,
      message: "Older",
    })
    let receivedSignal: AbortSignal | undefined
    const cancelableResult = apiCallWithLoading(
      (signal) => {
        receivedSignal = signal
        return cancelableCall.promise
      },
      { blockUi: true, message: "Newest", cancelable: true }
    )

    expectTypeOf(cancelableResult).toEqualTypeOf<
      Promise<CancelableApiResult<typeof okApiResult>>
    >()
    expect(receivedSignal).toBeInstanceOf(AbortSignal)
    const cancel = apiStatus.states.at(-1)?.cancel
    expect(cancel).toEqual(expect.any(Function))

    cancel?.()
    expect(receivedSignal?.aborted).toBe(true)
    expect(apiStatus.states.map((state) => state.message)).toEqual(["Older"])
    await expect(cancelableResult).resolves.toEqual({ status: "cancelled" })

    cancel?.()
    expect(apiStatus.states.map((state) => state.message)).toEqual(["Older"])

    olderCall.resolve(okApiResult)
    await olderResult
  })

  it("requires narrowing before a completed result is available", async () => {
    const options: CancelableApiLoadingOptions = {
      blockUi: true,
      cancelable: true,
    }
    expectTypeOf(options.blockUi).toEqualTypeOf<true>()
    expectTypeOf(options.cancelable).toEqualTypeOf<true>()
    const outcome = await apiCallWithLoading(async () => okApiResult, {
      blockUi: true,
      cancelable: true,
    })

    if (outcome.status === "completed") {
      expectTypeOf(outcome.result).toEqualTypeOf<typeof okApiResult>()
      expect(outcome.result).toBe(okApiResult)
    } else {
      // @ts-expect-error cancelled outcomes intentionally expose no result
      expect(outcome.result).toBeUndefined()
    }
  })
})
