export default (zonename: string, beforeEach, afterEach) => {
  beforeEach(() => {
    const resolvedOptionsOriginal =
      Intl.DateTimeFormat.prototype.resolvedOptions;

    vitest
      .spyOn(Intl.DateTimeFormat.prototype, "resolvedOptions")
      .mockImplementation(function mockResolvedOptions(
        this: Intl.DateTimeFormat,
      ) {
        const options = resolvedOptionsOriginal.call(this);
        options.timeZone = zonename;
        return options;
      });
  });
  afterEach(() => {
    vitest.restoreAllMocks();
  });
};
