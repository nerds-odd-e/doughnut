import {
  dailyProbeAccuracy,
  dailyProbeLapseCount,
  dailyProbePracticeSequence,
  dailyProbeScoredSequence,
  dailyProbeSpeed,
  dailyProbeVariability,
  mapDailyProbeKey,
  recordDailyProbeTrial,
} from "@/models/dailyProbe"

function recordAt(
  stimulus: "left" | "right",
  rtMs: number,
  response: "left" | "right",
  stimulusOnsetMs = 1_000
) {
  return recordDailyProbeTrial({
    stimulus,
    stimulusOnsetMs,
    responseMs: stimulusOnsetMs + rtMs,
    response,
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
    expect(recordAt("left", 250, "left")).toEqual({
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
    expect(recordAt("left", 50, "left")).toEqual({
      stimulus: "left",
      response: "left",
      correct: false,
    })
  })

  it("speed is 3.00 s⁻¹ for correct 250 ms and 500 ms, ignoring a wrong 250 ms", () => {
    const trials = [
      recordAt("left", 250, "left"),
      recordAt("right", 500, "right"),
      recordAt("left", 250, "right"),
    ]
    expect(dailyProbeSpeed(trials)?.toFixed(2)).toBe("3.00")
  })

  it("omits speed when there are no correct valid RTs", () => {
    expect(dailyProbeSpeed([recordAt("left", 250, "right")])).toBeUndefined()
  })

  it("accuracy is 95 for 19 correct of 20 scored trials", () => {
    const trials = [
      ...Array.from({ length: 19 }, () => recordAt("left", 250, "left")),
      recordAt("left", 250, "right"),
    ]
    expect(dailyProbeAccuracy(trials)).toBe(95)
  })

  it("counts a 500 ms trial as a lapse", () => {
    expect(dailyProbeLapseCount([recordAt("left", 500, "left")])).toBe(1)
  })

  it("does not count a 499 ms trial as a lapse", () => {
    expect(dailyProbeLapseCount([recordAt("left", 499, "left")])).toBe(0)
  })

  it("counts a timeout as a lapse", () => {
    expect(
      dailyProbeLapseCount([
        recordDailyProbeTrial({
          stimulus: "right",
          stimulusOnsetMs: 1_000,
        }),
      ])
    ).toBe(1)
  })

  it("does not count a false start as a lapse", () => {
    expect(dailyProbeLapseCount([recordAt("left", 50, "left")])).toBe(0)
  })

  it("variability is 1.41 s⁻¹ for reciprocal RTs 4.00 and 2.00", () => {
    const trials = [
      recordAt("left", 250, "left"),
      recordAt("right", 500, "right"),
    ]
    expect(dailyProbeVariability(trials)?.toFixed(2)).toBe("1.41")
  })

  it("omits variability when fewer than 2 correct valid RTs", () => {
    expect(
      dailyProbeVariability([recordAt("left", 250, "left")])
    ).toBeUndefined()
  })
})
