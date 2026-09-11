import { When, Then } from '@badeball/cypress-cucumber-preprocessor'
import { notebookPublicationProfile } from '../start/pageObjects/cli/notebookPublicationProfile'

When('I prepare the small deterministic publication profile', () =>
  notebookPublicationProfile.prepare()
)
When('I start recording the owned publication JVM', () =>
  notebookPublicationProfile.start()
)
When('I stop recording the owned publication JVM', () =>
  notebookPublicationProfile.stop()
)
Then('the received checkout contains every profiling document unchanged', () =>
  notebookPublicationProfile.expectReceived()
)
