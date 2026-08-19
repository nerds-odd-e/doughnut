import makeMe from "doughnut-test-fixtures/makeMe"
import { beforeEach, describe, expect, it } from "vitest"
import {
  historyFromLogs,
  mockMemoryTrackerPageViewDefaults,
  mountMemoryTrackerPageViewReady,
} from "./memoryTrackerPageViewTestSupport"

describe("MemoryTrackerPageView recall history", () => {
  beforeEach(() => {
    mockMemoryTrackerPageViewDefaults()
  })

  it("shows empty state when there is no recall history", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallHistory: [],
    })

    expect(wrapper.text()).toContain("No recall history found")
  })

  it("shows a GOOD recall log with recorded time and elapsed hours", async () => {
    const recordedAt = new Date("2024-01-01T12:00:00Z").toISOString()
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallHistory: historyFromLogs([
        makeMe.aRecallLog.recordedAt(recordedAt).elapsedHours(24).please(),
      ]),
    })

    const log = wrapper.find('[data-testid="recall-log"]')
    expect(log.exists()).toBe(true)
    expect(log.text()).toContain("GOOD")
    expect(log.text()).toContain(new Date(recordedAt).toLocaleString())
    expect(log.text()).toMatch(/Elapsed hours:\s*24/)
  })

  it("shows an AGAIN recall log as a second log", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallHistory: historyFromLogs([
        makeMe.aRecallLog.please(),
        makeMe.aRecallLog.productOutcome("AGAIN").please(),
      ]),
    })

    const outcomes = wrapper.findAll(
      '[data-testid="recall-log-product-outcome"]'
    )
    expect(outcomes).toHaveLength(2)
    expect(outcomes.map((el) => el.text())).toContain("AGAIN")
  })

  it("shows a paired recall log and question in the same card", async () => {
    const wrapper = await mountMemoryTrackerPageViewReady({
      recallHistory: [
        makeMe.aRecallHistoryItem
          .recallLog(makeMe.aRecallLog.productOutcome("GOOD").please())
          .recallPrompt(
            makeMe.aRecallPromptHistoryItem
              .withQuestionStem("Paired question")
              .please()
          )
          .please(),
      ],
    })

    const cards = wrapper.findAll('[data-testid="recall-history-item"]')
    expect(cards).toHaveLength(1)
    expect(cards[0]!.text()).toContain("GOOD")
    expect(cards[0]!.text()).toContain("Paired question")
  })
})
