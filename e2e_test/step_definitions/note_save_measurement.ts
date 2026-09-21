/**
 * TEMPORARY MEASUREMENT MACHINERY — SEED-034#story-4, slices 2-3.
 * Deleted together with `note_save_measurement.feature` in slice 14.
 */
import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import { noteSaveMeasurement } from '../start/pageObjects/noteSaveMeasurement'

Given('I have the generated large synchronized measurement notebook', () =>
  noteSaveMeasurement().seedLargeSynchronizedNotebook()
)

When("I capture the measurement notebook's Portable export", () =>
  noteSaveMeasurement().capturePortableExportForComparison()
)

When('I warm up the measured save path', () =>
  noteSaveMeasurement().warmUpTheMeasuredSavePath()
)

When(
  'I measure save {int} of note content that keeps its existing wiki links',
  (sampleNumber: number) =>
    noteSaveMeasurement().measureSaveKeepingExistingWikiLinks(sampleNumber)
)

When(
  'I measure save {int} of note content that adds a wiki link',
  (sampleNumber: number) =>
    noteSaveMeasurement().measureSaveAddingAWikiLink(sampleNumber)
)

Then('each measured save reports its debounce and its request separately', () =>
  noteSaveMeasurement().expectBothEditKindsMeasuredWithSeparateBoundaries()
)

Then('the measured note reloads with every measured edit and a live link', () =>
  noteSaveMeasurement().expectMeasuredNoteReloadsWithEveryMeasuredEdit()
)
