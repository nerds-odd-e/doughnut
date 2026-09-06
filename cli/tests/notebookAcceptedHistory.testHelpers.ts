import * as fs from 'node:fs'
import { tmpdir } from 'node:os'

export function acceptedHistoryStagingDirsUnderTmp(): string[] {
  return fs
    .readdirSync(tmpdir())
    .filter((name) =>
      name.startsWith(`donut-notebook-accepted-history-${process.pid}-`)
    )
}
