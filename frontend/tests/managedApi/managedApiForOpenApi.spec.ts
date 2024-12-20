import "vitest-fetch-mock"
import type { ApiStatus } from "@/managedApi/ManagedApi"
import ManagedApi from "@/managedApi/ManagedApi"

describe("managdApi", () => {
  const apiStatus: ApiStatus = { states: [], errors: [] }
  const managedApi = new ManagedApi(apiStatus)
  const baseUrl = "http://localhost:9081"

  beforeEach(() => {
    fetchMock.resetMocks()
    apiStatus.states = []
    apiStatus.errors = []
  })

  describe("set the loading status", () => {
    it("should set the loading status", async () => {
      let interimStateLength = 0
      fetchMock.mockResponse(
        () => {
          interimStateLength = apiStatus.states.length
          return Promise.resolve("")
        },
        {
          url: `${baseUrl}/api/user`,
        }
      )
      await managedApi.restUserController.getUserProfile()
      expect(interimStateLength).toBeGreaterThan(0)
      expect(apiStatus.states.length).toBe(0)
    })

    it("should not set the loading status in silent mode", async () => {
      let interimStateLength = 0
      fetchMock.mockResponse(
        () => {
          interimStateLength = apiStatus.states.length
          return Promise.resolve("")
        },
        {
          url: `${baseUrl}/api/user`,
        }
      )
      await managedApi.silent.restUserController.getUserProfile()
      expect(interimStateLength).toBe(0)
    })
  })

  describe("collect error msg", () => {
    beforeEach(() => {
      vitest.useFakeTimers()
      fetchMock.mockResponse(JSON.stringify({}), {
        url: `${baseUrl}/api/user`,
        status: 404,
      })
    })

    const callApiAndIgnoreError = async () => {
      try {
        await managedApi.restUserController.getUserProfile()
      } catch (_e) {
        // ignore
      }
    }

    it("should render note with one child", async () => {
      await callApiAndIgnoreError()
      expect(apiStatus.errors).toHaveLength(1)
    })

    it("disappear in 2 seconds", async () => {
      await callApiAndIgnoreError()
      vitest.advanceTimersByTime(2000)
      expect(apiStatus.errors).toHaveLength(0)
    })
  })
})
