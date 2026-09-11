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

When('I seed the representative publication baseline', () =>
  notebookPublicationProfile.seed()
)
When('I publish and time the profiling proposal', () =>
  notebookPublicationProfile.publish()
)

When('I invalidate the last processed profiling addition', () =>
  notebookPublicationProfile.invalidateLast()
)
When('I publish and time the rejected profiling proposal', () =>
  notebookPublicationProfile.publishRejection()
)
Then('the profiling baseline and learning state are preserved', () =>
  notebookPublicationProfile.expectPreserved()
)

When('I publish and time the profiling proposal through benchmark HTTP', () =>
  notebookPublicationProfile.publishHttp()
)
When(
  'I publish and time the rejected profiling proposal through benchmark HTTP',
  () => notebookPublicationProfile.publishHttpRejection()
)
