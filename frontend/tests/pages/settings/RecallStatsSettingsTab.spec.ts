import { UserController } from "@generated/donut-backend-api/sdk.gen"
import RecallStatsSettingsTab from "@/pages/settings/RecallStatsSettingsTab.vue"
import helper, { mockSdkService } from "@tests/helpers"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { saveAs } from "file-saver"
import type {
  RecallHistoryExport,
  RecallStatsDto,
  DayCount,
  DayAvgResponseTime,
  DayRetention,
} from "@generated/donut-backend-api"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"

vi.mock("file-saver", () => ({ saveAs: vi.fn() }))

function buildCalendar(): DayCount[] {
  const days: DayCount[] = []
  for (let i = 0; i < 365; i++) {
    days.push({
      date: `1989-01-${String((i % 28) + 1).padStart(2, "0")}`,
      count: i === 100 ? 12 : 0,
    })
  }
  return days
}

function buildTrend(): DayAvgResponseTime[] {
  const days: DayAvgResponseTime[] = []
  for (let i = 0; i < 90; i++) {
    days.push({
      date: `1989-02-${String((i % 28) + 1).padStart(2, "0")}`,
      avgMs: i % 3 === 0 ? undefined : 4000,
      sampleSize: i % 3 === 0 ? 1 : 4,
    })
  }
  return days
}

function buildRetentionTrend(): DayRetention[] {
  const days: DayRetention[] = []
  for (let i = 0; i < 90; i++) {
    const sufficient = i % 4 !== 0
    days.push({
      date: `1989-02-${String((i % 28) + 1).padStart(2, "0")}`,
      retentionPct: sufficient ? 75 : undefined,
    })
  }
  return days
}

function emptyGrid(): number[][] {
  return Array.from({ length: 7 }, () => Array.from({ length: 24 }, () => 0))
}

const fixture: RecallStatsDto = {
  calendar: buildCalendar(),
  trend: buildTrend(),
  retentionTrend: buildRetentionTrend(),
  amPm: {
    morningMs: 5000,
    morningSamples: 5,
    afternoonMs: 6000,
    afternoonSamples: 5,
    eveningMs: undefined,
    eveningSamples: 2,
    nightMs: undefined,
    nightSamples: 1,
  },
  weekdayHourCounts: emptyGrid(),
  weekdayHourCorrect: emptyGrid(),
  totals: {
    totalReviewsAllTime: 200,
    totalReviews365: 100,
    reviewsToday: 5,
    retentionPct365: 85,
    currentStreak: 7,
    longestStreak: 10,
    totalTimeSpentMs: 3_600_000,
    bestHour: 10,
    bestHourRetentionPct: 100,
    worstHour: 20,
    worstHourRetentionPct: 40,
  },
  pace: {
    pctVsUsual: 15,
    sampleSize: 4,
    totalAnsweredToday: 6,
  },
  accuracy: {
    standardizedResidual: 1.8,
    sampleSize: 6,
  },
  dailyProbe: [],
}

describe("RecallStatsSettingsTab", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("renders retention headline, charts, and best/worst hours from the fixture", async () => {
    const spy = mockSdkService(UserController, "getRecallStats", fixture)

    const wrapper = helper
      .component(RecallStatsSettingsTab)
      .withRouter()
      .mount()
    await flushPromises()

    expect(spy).toHaveBeenCalledWith({ query: { timezone: timezoneParam() } })
    expect(wrapper.find('[data-testid="retention-pct-tile"]').text()).toContain(
      "85"
    )
    expect(
      wrapper.find('[data-testid="recall-activity-calendar"]').exists()
    ).toBe(true)
    expect(
      wrapper.find('[data-testid="response-time-trend-chart"]').exists()
    ).toBe(true)
    expect(wrapper.find('[data-testid="retention-trend-chart"]').exists()).toBe(
      true
    )
    expect(
      wrapper.find('[data-testid="weekday-hour-heatmap-count"]').exists()
    ).toBe(true)
    expect(
      wrapper.find('[data-testid="weekday-hour-heatmap-retention"]').exists()
    ).toBe(true)
    expect(
      wrapper.find('[data-testid="am-pm-response-time-chart"]').exists()
    ).toBe(true)
    const bestWorst = wrapper.find('[data-testid="best-worst-hours"]')
    expect(bestWorst.text()).toContain("10")
    expect(bestWorst.text()).toContain("20")
    expect(wrapper.find('[data-testid="pace-tile"]').text()).toContain(
      "15% slower than usual"
    )
    expect(wrapper.find('[data-testid="accuracy-tile"]').text()).toContain(
      "Recalling better than expected"
    )
  })

  it("shows an empty state when there are no reviews yet", async () => {
    mockSdkService(UserController, "getRecallStats", {
      totals: { totalReviewsAllTime: 0 },
    })

    const wrapper = helper
      .component(RecallStatsSettingsTab)
      .withRouter()
      .mount()
    await flushPromises()

    expect(wrapper.find('[data-testid="recall-stats-empty"]').exists()).toBe(
      true
    )
  })

  it("retries after an error state", async () => {
    vi.spyOn(UserController, "getRecallStats").mockResolvedValue({
      data: undefined,
      error: { statusCode: 500 },
      // biome-ignore lint/suspicious/noExplicitAny: error-path mock shape
    } as any)

    const wrapper = helper
      .component(RecallStatsSettingsTab)
      .withRouter()
      .mount()
    await flushPromises()

    await wrapper.find('[data-testid="recall-stats-retry"]').trigger("click")
    await flushPromises()
    expect(UserController.getRecallStats).toHaveBeenCalledTimes(2)
  })

  describe("recall history download", () => {
    const exported: RecallHistoryExport = {
      timezone: timezoneParam(),
      exportedAt: "2026-11-03T12:00:00Z",
      recallEvents: [
        {
          recordedAt: "2026-11-02T08:00:00Z",
          grade: "GOOD",
          kind: "JUST_REVIEW",
          elapsedHours: 24,
          memoryTrackerId: 7,
          memoryTrackerType: "UNDERSTANDING",
        },
      ],
      dailyProbes: [],
    }

    const mountTab = async (stats: RecallStatsDto) => {
      mockSdkService(UserController, "getRecallStats", stats)
      const wrapper = helper
        .component(RecallStatsSettingsTab)
        .withRouter()
        .mount()
      await flushPromises()
      return wrapper
    }

    afterEach(() => {
      vi.useRealTimers()
    })

    it.each([
      ["with data", fixture],
      ["in the empty state", { totals: { totalReviewsAllTime: 0 } }],
    ])("offers the download control %s", async (_, stats) => {
      const wrapper = await mountTab(stats)

      expect(
        wrapper.find('[data-testid="download-recall-history"]').text()
      ).toContain("Download recall history")
    })

    it("saves the exported JSON as a dated file using the browser timezone", async () => {
      vi.useFakeTimers({ toFake: ["Date"] })
      vi.setSystemTime(new Date("2026-11-03T12:00:00Z"))
      const spy = mockSdkService(
        UserController,
        "exportRecallHistory",
        exported
      )
      const wrapper = await mountTab({ totals: { totalReviewsAllTime: 0 } })

      await wrapper
        .find('[data-testid="download-recall-history"]')
        .trigger("click")
      await flushPromises()

      expect(spy).toHaveBeenCalledWith({ query: { timezone: timezoneParam() } })
      const [blob, filename] = vi.mocked(saveAs).mock.calls[0]!
      expect(filename).toBe("recall-history-2026-11-03.json")
      expect(JSON.parse(await (blob as Blob).text())).toEqual(exported)
    })
  })
})
