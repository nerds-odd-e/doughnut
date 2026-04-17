/** pdf.js 5.6+ uses `Map.prototype.getOrInsertComputed` (TC39); Cypress Electron may not ship it yet. */
type MapPrototypeWithGetOrInsertComputed = typeof Map.prototype & {
  getOrInsertComputed?: <K, V>(
    this: Map<K, V>,
    key: K,
    callbackFn: (key: K, map: Map<K, V>) => V
  ) => V
}

const mapProto = Map.prototype as MapPrototypeWithGetOrInsertComputed
if (!mapProto.getOrInsertComputed) {
  mapProto.getOrInsertComputed = function <K, V>(
    this: Map<K, V>,
    key: K,
    callbackFn: (key: K, map: Map<K, V>) => V
  ): V {
    if (this.has(key)) {
      return this.get(key) as V
    }
    const value = callbackFn(key, this)
    this.set(key, value)
    return value
  }
}

export {}
