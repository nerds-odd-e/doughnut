import { UserController } from "@generated/donut-backend-api/sdk.gen"
import RecallStatsSettingsTab from "@/pages/settings/RecallStatsSettingsTab.vue"
import {
  isoDateDaysBefore,
  localDayIso,
} from "@/components/recallStats/dailyProbeWindow"
import helper, { mockSdkService } from "@tests/helpers"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { describe, it, expect, beforeEach, vi } from "vitest"

function isoDateDaysAgo(daysAgo: number): string {
  return isoDateDaysBefore(localDayIso(timezoneParam()), daysAgo)
}

function dailyProbeSpeedTrendDayCount(wrapper: VueWrapper): number {
  const points = wrapper
    .find('[data-testid="daily-probe-speed-polyline"]')
    .attributes("points")
  return (points ?? "").trim().split(/\s+/).filter(Boolean).length
}

describe("RecallStatsSettingsTab Daily probe", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("shows the Daily probe trend with speed, lapses, and variability", async () => {
    mockSdkService(UserController, "getRecallStats", {
      totals: { totalReviewsAllTime: 0 },
      dailyProbe: [
        {
          date: isoDateDaysAgo(7),
          speed: 3.2,
          lapses: 1,
          variability: 0.4,
        },
        {
          date: isoDateDaysAgo(0),
          speed: 3.5,
          lapses: 0,
          variability: 0.3,
        },
      ],
    })

    const wrapper = helper
      .component(RecallStatsSettingsTab)
      .withRouter()
      .mount()
    await flushPromises()

    const trend = wrapper.find('[data-testid="daily-probe-trend"]')
    expect(trend.exists()).toBe(true)
    expect(trend.find('[data-testid="daily-probe-speed-chart"]').exists()).toBe(
      true
    )
    expect(
      trend.find('[data-testid="daily-probe-lapses-chart"]').exists()
    ).toBe(true)
    expect(
      trend.find('[data-testid="daily-probe-variability-chart"]').exists()
    ).toBe(true)
  })

  it("filters Daily probe trend points by the selected window", async () => {
    mockSdkService(UserController, "getRecallStats", {
      totals: { totalReviewsAllTime: 0 },
      dailyProbe: [
        {
          date: isoDateDaysAgo(100),
          speed: 2.0,
          lapses: 2,
          variability: 0.5,
        },
        {
          date: isoDateDaysAgo(40),
          speed: 2.5,
          lapses: 1,
          variability: 0.4,
        },
        {
          date: isoDateDaysAgo(10),
          speed: 3.0,
          lapses: 0,
          variability: 0.3,
        },
      ],
    })

    const wrapper = helper
      .component(RecallStatsSettingsTab)
      .withRouter()
      .mount()
    await flushPromises()

    expect(dailyProbeSpeedTrendDayCount(wrapper)).toBe(2)

    await wrapper.find('[data-testid="trend-window-30"]').trigger("click")
    await flushPromises()
    expect(dailyProbeSpeedTrendDayCount(wrapper)).toBe(1)

    await wrapper.find('[data-testid="trend-window-all"]').trigger("click")
    await flushPromises()
    expect(dailyProbeSpeedTrendDayCount(wrapper)).toBe(3)
  })

  it("hides the Daily probe trend when the series is empty", async () => {
    mockSdkService(UserController, "getRecallStats", {
      totals: { totalReviewsAllTime: 0 },
      dailyProbe: [],
    })

    const wrapper = helper
      .component(RecallStatsSettingsTab)
      .withRouter()
      .mount()
    await flushPromises()

    expect(wrapper.find('[data-testid="daily-probe-trend"]').exists()).toBe(
      false
    )
  })
})
