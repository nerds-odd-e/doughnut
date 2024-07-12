import { DataTable, Then } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Then(
  'I must see the following assessments in my assessment history:',
  (dataTable: DataTable) => {
    const rows = dataTable.hashes()
    start.navigateToAssessmentHistory().expectAssessmentHistory(rows)
  }
)
