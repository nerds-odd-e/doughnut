export function dueRecallQuery(dueindays: number) {
  return {
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    dueindays,
  }
}
