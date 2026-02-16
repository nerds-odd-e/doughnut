export const timestampToSeconds = (timestamp: string): number | undefined => {
  const [hms, millisecondsString] = timestamp.split(",")
  if (!hms || !millisecondsString) return

  const [hours, minutes, seconds] = hms.split(":").map(Number)
  const milliseconds = Number(millisecondsString)

  if (
    hours === undefined ||
    minutes === undefined ||
    seconds === undefined ||
    milliseconds === undefined
  ) {
    return
  }

  const totalSeconds =
    hours * 3600 + minutes * 60 + seconds + milliseconds / 1000
  return totalSeconds
}
