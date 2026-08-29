import {
  dailyProbePracticeSequence,
  dailyProbeScoredSequence,
  dailyProbeSpeed,
  mapDailyProbeKey,
  recordDailyProbeTrial,
} from "@/models/dailyProbe"

function recordAt(
  stimulus: "left" | "right",
  rtMs: number,
  key: string,
  stimulusOnsetMs = 1_000
) {
  return recordDailyProbeTrial({
    stimulus,
    stimulusOnsetMs,
    responseMs: stimulusOnsetMs + rtMs,
    key,
  })
}

describe("dailyProbe", () => {
  it("uses the protocol practice sequence", () => {
    expect(dailyProbePracticeSequence).toEqual([
      "left",
      "right",
      "right",
      "left",
    ])
  })

  it("uses the protocol scored sequence", () => {
    expect(dailyProbeScoredSequence).toEqual([
      "left",
      "right",
      "left",
      "left",
      "right",
      "right",
      "left",
      "right",
      "left",
      "right",
      "right",
      "left",
      "left",
      "right",
      "left",
      "right",
      "right",
      "left",
      "right",
      "left",
    ])
  })

  it("maps f/F/ArrowLeft to left and j/J/ArrowRight to right", () => {
    expect(mapDailyProbeKey("f")).toBe("left")
    expect(mapDailyProbeKey("F")).toBe("left")
    expect(mapDailyProbeKey("ArrowLeft")).toBe("left")
    expect(mapDailyProbeKey("j")).toBe("right")
    expect(mapDailyProbeKey("J")).toBe("right")
    expect(mapDailyProbeKey("ArrowRight")).toBe("right")
  })

  it("ignores keys that are not a Daily probe response", () => {
    expect(mapDailyProbeKey("x")).toBeUndefined()
  })

  it("records a correct valid trial with RT", () => {
    expect(recordAt("left", 250, "f")).toEqual({
      stimulus: "left",
      response: "left",
      rtMs: 250,
      correct: true,
    })
  })

  it("records a timeout with no response or RT", () => {
    expect(
      recordDailyProbeTrial({
        stimulus: "right",
        stimulusOnsetMs: 1_000,
      })
    ).toEqual({
      stimulus: "right",
      correct: false,
    })
  })

  it("records a false start as incorrect with no RT", () => {
    expect(recordAt("left", 50, "f")).toEqual({
      stimulus: "left",
      response: "left",
      correct: false,
    })
  })

  it("speed is 3.00 s⁻¹ for correct 250 ms and 500 ms, ignoring a wrong 250 ms", () => {
    const trials = [
      recordAt("left", 250, "f"),
      recordAt("right", 500, "j"),
      recordAt("left", 250, "j"),
    ]
    expect(dailyProbeSpeed(trials)?.toFixed(2)).toBe("3.00")
  })

  it("omits speed when there are no correct valid RTs", () => {
    expect(dailyProbeSpeed([recordAt("left", 250, "j")])).toBeUndefined()
  })
})
