const commonSenseSplit = function (str: string, separator: string) {
  return str
    .trim()
    .split(separator)
    .filter((s) => s !== '')
    .map((s) => s.trim())
}

export { commonSenseSplit }
