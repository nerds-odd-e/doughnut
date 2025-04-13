import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ExportButton from '@/components/notebook/ExportButton.vue'
import { useToast } from 'vue-toastification'
import { useBackendApi } from '@/composables/useBackendApi'
import type { Notebook } from '@/generated/backend'

vi.mock('vue-toastification')
vi.mock('@/composables/useBackendApi')

describe('ExportButton', () => {
  const mockToast = {
    success: vi.fn(),
    error: vi.fn(),
  }

  const mockApi = {
    exportController: {
      exportNotebook: vi.fn(),
      exportAllNotebooks: vi.fn(),
    },
  }

  const createMockNotebook = (id: number): Notebook => ({
    id,
    title: 'Test Notebook',
    notebookSettings: {
      skipMemoryTrackingEntirely: false,
    },
    headNoteId: 1,
    updated_at: new Date().toISOString(),
  })

  beforeEach(() => {
    vi.clearAllMocks()
    ;(useToast as any).mockReturnValue(mockToast)
    ;(useBackendApi as any).mockReturnValue(mockApi)
    // Mock URL.createObjectURL and URL.revokeObjectURL
    URL.createObjectURL = vi.fn()
    URL.revokeObjectURL = vi.fn()
  })

  it('renders export button', () => {
    const wrapper = mount(ExportButton)
    expect(wrapper.find('button').exists()).toBe(true)
    expect(wrapper.find('button').text()).toBe('Export All')
  })

  it('exports single notebook when notebook prop is provided', async () => {
    const notebook = createMockNotebook(1)
    const mockResponse = { notebooks: [notebook] }
    mockApi.exportController.exportNotebook.mockResolvedValue(mockResponse)

    const wrapper = mount(ExportButton, {
      props: { notebook },
    })

    await wrapper.find('button').trigger('click')

    expect(mockApi.exportController.exportNotebook).toHaveBeenCalledWith(notebook.id)
    expect(mockToast.success).toHaveBeenCalledWith('Export completed successfully')
  })

  it('exports all notebooks when no notebook prop is provided', async () => {
    const mockResponse = { notebooks: [] }
    mockApi.exportController.exportAllNotebooks.mockResolvedValue(mockResponse)

    const wrapper = mount(ExportButton)

    await wrapper.find('button').trigger('click')

    expect(mockApi.exportController.exportAllNotebooks).toHaveBeenCalled()
    expect(mockToast.success).toHaveBeenCalledWith('Export completed successfully')
  })

  it('shows error toast when export fails', async () => {
    mockApi.exportController.exportAllNotebooks.mockRejectedValue(new Error('Export failed'))

    const wrapper = mount(ExportButton)

    await wrapper.find('button').trigger('click')

    expect(mockToast.error).toHaveBeenCalledWith('Failed to export notebook(s)')
  })

  it('disables button during export', async () => {
    const wrapper = mount(ExportButton)
    const button = wrapper.find('button')
    
    // APIレスポンスを遅延させる
    mockApi.exportController.exportAllNotebooks.mockImplementation(
      () => new Promise(resolve => setTimeout(resolve, 100))
    )
    
    // エクスポートを開始
    await button.trigger('click')
    await wrapper.vm.$nextTick()
    
    // ボタンが無効化され、テキストが「Exporting...」に変更されていることを確認
    expect(button.attributes('disabled')).toBeDefined()
    expect(button.text()).toBe('Exporting...')
  })
}) 