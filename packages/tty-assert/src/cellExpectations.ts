/** Serializable cell-span checks after a string needle matches (Cypress-task–friendly). */

export type CellExpectation =
  | { kind: 'allBold' }
  | { kind: 'allBgPalette'; index: number }

export type CellExpectationBlock = {
  match: 'first' | 'last'
  expectations: CellExpectation[]
}

export type CellExpectationsValidationInput = {
  surface: 'viewableBuffer' | 'fullBuffer' | 'strippedTranscript'
  needle: string | RegExp
  cellExpectations?: CellExpectationBlock[]
}

/**
 * Validates style-related options and returns blocks to run on xterm (may be empty).
 * @throws On strippedTranscript / RegExp needle with cell checks.
 */
export function validateAndResolveCellExpectations(
  o: CellExpectationsValidationInput
): CellExpectationBlock[] {
  const blocks = o.cellExpectations?.length ? o.cellExpectations : []
  if (blocks.length === 0) return []

  if (o.surface === 'strippedTranscript') {
    throw new Error(
      'waitForTextInSurface: cell expectations are only supported for viewableBuffer and fullBuffer.'
    )
  }
  if (typeof o.needle !== 'string') {
    throw new Error(
      'waitForTextInSurface: cell expectations require a string needle.'
    )
  }

  return blocks
}
