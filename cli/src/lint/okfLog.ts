import { type OkfProblem, error } from './okfProblem.js'

const HEADING = '## '
const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/

/**
 * What a log file breaks in the Open Knowledge Format. A log is a flat list of
 * date-grouped entries, so every second-level heading in one names a day.
 *
 * @see https://github.com/GoogleCloudPlatform/knowledge-catalog
 */
export function logProblems(content: string): OkfProblem[] {
  return content
    .split('\n')
    .filter((line) => line.startsWith(HEADING))
    .filter((line) => !ISO_DATE.test(line.slice(HEADING.length).trim()))
    .flatMap(() => error('A log date heading is not `YYYY-MM-DD`'))
}
