import { flushPromises } from '@vue/test-utils'
import NotebookAssistantManagementDialog from '@/components/notebook/NotebookAssistantManagementDialog.vue'
import makeMe from 'tests/fixtures/makeMe'
import helper from "../helpers"

describe('NotebookAssistantManagementDialog.vue', () => {
  let wrapper
  const notebook = makeMe.aNotebook.please()
  const mockedDump = vitest.fn()
  const originalCreateObjectURL = URL.createObjectURL
  const originalRevokeObjectURL = URL.revokeObjectURL


  beforeEach(() => {
    global.URL.createObjectURL = vitest.fn()
    global.URL.revokeObjectURL = vitest.fn()
    helper.managedApi.restNotebookController.downloadNotebookDump =
      mockedDump
    wrapper = helper
    .component(NotebookAssistantManagementDialog)
    .withStorageProps({
      notebook,
    })
    .mount()
  })

  afterEach(() => {
    vitest.clearAllMocks()
    global.URL.createObjectURL = originalCreateObjectURL
    global.URL.revokeObjectURL = originalRevokeObjectURL
  })

  it('renders the download button', () => {
    const downloadButton = wrapper.findAll('button')
    expect(downloadButton[1].text()).toContain('Download Notebook Dump')
  })

  it('calls the API and triggers the file download when the button is clicked', async () => {
    // Mock API response
    const noteBriefs = [{ id: 1, topic: 'Note 1', content: 'Test content' }]
    mockedDump.mockResolvedValue(noteBriefs)

    // Find the button and trigger click
    const downloadButton = wrapper.findAll('button')[1]
    await downloadButton.trigger('click')

    // Wait for any pending promises to resolve
    await flushPromises()

    // Check if the URL.createObjectURL was called with the correct Blob
    expect(global.URL.createObjectURL).toHaveBeenCalledWith(expect.any(Blob))
  })

})
