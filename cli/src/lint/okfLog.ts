import { type OkfProblem, error } from './okfProblem.js'

const HEADING = '## '
const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/

function namesADay(line: string): boolean {
  const text = line.slice(HEADING.length).trim()
  if (!ISO_DATE.test(text)) return false
  const day = new Date(`${text}T00:00:00Z`)
  return !Number.isNaN(day.getTime()) && day.toISOString().startsWith(text)
}

/**
 * What a log file breaks in the Open Knowledge Format. A log is a flat list of
 * date-grouped entries, so every second-level heading in one names a day.
 *
 * @see https://github.com/GoogleCloudPlatform/knowledge-catalog
 */
export function logProblems(content: string): OkfProblem[] {
  return content
    .split('\n')
    .flatMap((line, index) =>
      line.startsWith(HEADING) && !namesADay(line)
        ? error('A log date heading is not `YYYY-MM-DD`', index + 1)
        : []
    )
}
