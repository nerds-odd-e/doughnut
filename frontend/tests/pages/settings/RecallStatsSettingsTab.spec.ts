import { UserController } from "@generated/doughnut-backend-api/sdk.gen"
import RecallStatsSettingsTab from "@/pages/settings/RecallStatsSettingsTab.vue"
import helper, { mockSdkService } from "@tests/helpers"
import timezoneParam from "@/managedApi/window/timezoneParam"
import type {
  RecallStatsDto,
  DayCount,
  DayAvgResponseTime,
  DayRetention,
  HourRetention,
} from "@generated/doughnut-backend-api"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect, beforeEach, vi } from "vitest"

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
      correctCount: sufficient ? 3 : 1,
      answeredCount: sufficient ? 4 : 1,
      sampleSize: sufficient ? 4 : 1,
    })
  }
  return days
}

function emptyGrid(): number[][] {
  return Array.from({ length: 7 }, () => Array.from({ length: 24 }, () => 0))
}

function buildHourlyRetention(): HourRetention[] {
  const hours: HourRetention[] = []
  for (let h = 0; h < 24; h++) {
    hours.push({
      hour: h,
      retentionPct: h === 10 ? 100 : 50,
      correctCount: 5,
      answeredCount: 6,
    })
  }
  return hours
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
  hourlyRetention: buildHourlyRetention(),
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
}

describe("RecallStatsSettingsTab", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("renders the retention headline, tiles, five charts, AM/PM bars, and best/worst hours from the fixture", async () => {
    const spy = mockSdkService(UserController, "getRecallStats", fixture)

    const wrapper = helper
      .component(RecallStatsSettingsTab)
      .withRouter()
      .mount()
    await flushPromises()

    // Fetch called with the browser timezone param
    expect(spy).toHaveBeenCalledWith({ query: { timezone: timezoneParam() } })

    // Prominent retention % headline tile
    const retentionTile = wrapper.find('[data-testid="retention-pct-tile"]')
    expect(retentionTile.exists()).toBe(true)
    expect(retentionTile.text()).toContain("85")

    // Other headline tiles
    expect(
      wrapper.find('[data-testid="total-reviews-365-tile"]').exists()
    ).toBe(true)
    expect(wrapper.find('[data-testid="reviews-today-tile"]').exists()).toBe(
      true
    )
    expect(wrapper.find('[data-testid="current-streak-tile"]').exists()).toBe(
      true
    )
    expect(wrapper.find('[data-testid="best-worst-hours"]').exists()).toBe(true)

    // Five chart components + AM/PM bars
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

    // Best/worst hour list text
    const bestWorst = wrapper.find('[data-testid="best-worst-hours"]')
    expect(bestWorst.text()).toContain("10")
    expect(bestWorst.text()).toContain("20")
  })
})
