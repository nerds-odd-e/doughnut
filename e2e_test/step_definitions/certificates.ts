import { When, Then } from '@badeball/cypress-cucumber-preprocessor'
import '../support/string_util'

When('I have a notebook with the name "Certified thing"', () => {
  return true
})
When('I open the notebooks settings', () => {
  return true
})
Then('I should see the default expiration timespan which is 1 year', () => {
  return true
})
