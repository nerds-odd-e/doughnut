export default (zonename: string, beforeEach, afterEach) => {
  beforeEach(() => {
    const resolvedOptionsOriginal =
      Intl.DateTimeFormat.prototype.resolvedOptions

    vitest
      .spyOn(Intl.DateTimeFormat.prototype, "resolvedOptions")
      .mockImplementation(function (this: unknown) {
        const options = resolvedOptionsOriginal.call(
          this as Intl.DateTimeFormat
        )
        return {
          ...options,
          timeZone: zonename,
        }
      } as (
        this: Intl.ResolvedDateTimeFormatOptions
      ) => Intl.ResolvedDateTimeFormatOptions)
  })

  afterEach(() => {
    vitest.restoreAllMocks()
  })
}
