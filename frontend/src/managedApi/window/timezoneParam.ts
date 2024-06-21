export default () => {
  const { timeZone } = Intl.DateTimeFormat().resolvedOptions()
  return timeZone
}
