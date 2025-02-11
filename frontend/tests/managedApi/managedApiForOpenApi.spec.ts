import "vitest-fetch-mock"
import type { ApiStatus } from "@/managedApi/ManagedApi"
import ManagedApi from "@/managedApi/ManagedApi"
import { vi } from "vitest"

const mockToast = {
  error: vi.fn(),
}

vi.mock("vue-toastification", () => ({
  useToast: () => mockToast,
}))

describe("managdApi", () => {
  const apiStatus: ApiStatus = { states: [], errors: [] }
  const managedApi = new ManagedApi(apiStatus)
  const baseUrl = "http://localhost:9081"

  beforeEach(() => {
    fetchMock.resetMocks()
    apiStatus.states = []
    apiStatus.errors = []
    mockToast.error.mockClear()
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

  describe("error handling", () => {
    beforeEach(() => {
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

    it("should show error toast", async () => {
      await callApiAndIgnoreError()
      expect(mockToast.error).toHaveBeenCalled()
    })

    it("should not show error toast in silent mode", async () => {
      try {
        await managedApi.silent.restUserController.getUserProfile()
      } catch (_e) {
        // ignore
      }
      expect(mockToast.error).not.toHaveBeenCalled()
    })
  })
})
