/** Serializable cell-span checks after a string needle matches (Cypress-task–friendly). */

export type CellExpectation =
  | { kind: 'allBold' }
  | { kind: 'allBgPalette'; index: number }
  | {
      kind: 'noFgPaletteUnlessBgPalette'
      fgPalette: number
      unlessBgPalette: number
    }

export type CellExpectationBlock = {
  match: 'first' | 'last'
  expectations: CellExpectation[]
}

const GRAY_PALETTE = 8

export type CellExpectationsValidationInput = {
  surface: 'viewableBuffer' | 'fullBuffer' | 'strippedTranscript'
  needle: string | RegExp
  requireBold?: boolean
  rejectGrayForegroundOnlyWithoutGrayBackground?: boolean
  requireGrayBackgroundBlock?: boolean
  cellExpectations?: CellExpectationBlock[]
}

function legacyStyleFlagsSet(o: CellExpectationsValidationInput): boolean {
  return !!(
    o.requireBold ||
    o.rejectGrayForegroundOnlyWithoutGrayBackground ||
    o.requireGrayBackgroundBlock
  )
}

function expandLegacyCellExpectations(
  o: CellExpectationsValidationInput
): CellExpectationBlock[] {
  const blocks: CellExpectationBlock[] = []
  if (o.requireBold) {
    blocks.push({ match: 'first', expectations: [{ kind: 'allBold' }] })
  }
  const reject = o.rejectGrayForegroundOnlyWithoutGrayBackground ?? false
  const requireBg = o.requireGrayBackgroundBlock ?? false
  if (reject || requireBg) {
    const expectations: CellExpectation[] = []
    if (reject) {
      expectations.push({
        kind: 'noFgPaletteUnlessBgPalette',
        fgPalette: GRAY_PALETTE,
        unlessBgPalette: GRAY_PALETTE,
      })
    }
    if (requireBg) {
      expectations.push({ kind: 'allBgPalette', index: GRAY_PALETTE })
    }
    blocks.push({ match: 'last', expectations })
  }
  return blocks
}

/**
 * Validates style-related options and returns blocks to run on xterm (may be empty).
 * @throws On incompatible combinations or strippedTranscript / RegExp needle with cell checks.
 */
export function validateAndResolveCellExpectations(
  o: CellExpectationsValidationInput
): CellExpectationBlock[] {
  const fromUser = o.cellExpectations
  const userBlocks = fromUser?.length ? fromUser : undefined
  if (userBlocks && legacyStyleFlagsSet(o)) {
    throw new Error(
      'waitForTextInSurface: do not combine cellExpectations with requireBold / gray block boolean options; use one style API.'
    )
  }

  const resolved = userBlocks ?? expandLegacyCellExpectations(o)
  if (resolved.length === 0) return []

  if (o.surface === 'strippedTranscript') {
    throw new Error(
      'waitForTextInSurface: cell expectations (and legacy requireBold / gray block options) are only supported for viewableBuffer and fullBuffer.'
    )
  }
  if (typeof o.needle !== 'string') {
    throw new Error(
      'waitForTextInSurface: cell expectations require a string needle.'
    )
  }

  return resolved
}
