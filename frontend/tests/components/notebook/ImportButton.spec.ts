import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ImportButton from '@/components/notebook/ImportButton.vue'
import { useToast } from 'vue-toastification'
import { useBackendApi } from '@/composables/useBackendApi'
import type { Notebook } from '@/generated/backend'

vi.mock('vue-toastification')
vi.mock('@/composables/useBackendApi')

describe('ImportButton', () => {
  const mockToast = {
    success: vi.fn(),
    error: vi.fn(),
    clear: vi.fn(),
    updateDefaults: vi.fn(),
    dismiss: vi.fn(),
    update: vi.fn(),
    info: vi.fn(),
    warning: vi.fn(),
  }

  const mockApi = {
    importController: {
      importNotebook: vi.fn().mockResolvedValue({ success: true }),
      importAllNotebooks: vi.fn().mockImplementation(
        () => new Promise(resolve => setTimeout(() => resolve({ success: true }), 100))
      ),
    },
    exportController: {},
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
    vi.useFakeTimers()
    vi.mocked(useToast).mockReturnValue(mockToast as any)
    vi.mocked(useBackendApi).mockReturnValue(mockApi as any)
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders import button', () => {
    const wrapper = mount(ImportButton)
    expect(wrapper.find('button').exists()).toBe(true)
    expect(wrapper.find('button').text()).toBe('Import All')
  })

  it('shows file input when button is clicked', async () => {
    const wrapper = mount(ImportButton)
    const button = wrapper.find('button')
    const fileInput = wrapper.find('input[type="file"]')

    expect(fileInput.exists()).toBe(true)
    expect(fileInput.isVisible()).toBe(false)

    await button.trigger('click')
    // ファイル選択ダイアログが表示されることを確認（実際のクリックイベントはブラウザが処理）
    expect(fileInput.attributes('accept')).toBe('.json')
  })

  it('handles file selection for single notebook import', async () => {
    const wrapper = mount(ImportButton, {
      props: {
        notebook: createMockNotebook(1),
      },
    })

    const mockFile = new File(['{"test": "data"}'], 'test.json', { type: 'application/json' })

    // FileReaderのモック
    const mockFileReader = {
      readAsText: vi.fn().mockImplementation(() => {
        setTimeout(() => {
          if (mockFileReader.onload) {
            mockFileReader.onload({ target: { result: '{"test": "data"}' } })
          }
        }, 0)
      }),
      onload: null as any,
      result: '{"test": "data"}',
    }
    global.FileReader = vi.fn(() => mockFileReader) as any

    // ファイル選択をシミュレート
    const input = wrapper.find('input[type="file"]')
    Object.defineProperty(input.element, 'files', {
      value: [mockFile],
      configurable: true,
    })
    await input.trigger('change')

    // タイマーを進める
    await vi.runAllTimersAsync()
    await wrapper.vm.$nextTick()

    // APIが正しく呼び出されたことを確認
    expect(mockApi.importController.importNotebook).toHaveBeenCalledWith({ test: 'data' })
    expect(mockToast.success).toHaveBeenCalledWith('Import completed successfully')
  })

  it('handles file selection errors', async () => {
    const wrapper = mount(ImportButton)
    const mockFile = new File(['invalid json'], 'test.json', { type: 'application/json' })

    // FileReaderのモック
    const mockFileReader = {
      readAsText: vi.fn(),
      onload: null as any,
      result: 'invalid json',
    }
    global.FileReader = vi.fn(() => mockFileReader) as any

    // ファイル選択をシミュレート
    const input = wrapper.find('input[type="file"]')
    const files = [mockFile]
    Object.defineProperty(input.element, 'files', {
      value: files,
    })
    await input.trigger('change')

    // FileReaderの処理を完了（エラーケース）
    if (mockFileReader.onload) {
      await mockFileReader.onload({ target: { result: mockFileReader.result } })
    }

    expect(mockToast.error).toHaveBeenCalledWith('Failed to import notebook(s)')
  })

  it('disables button during import', async () => {
    const wrapper = mount(ImportButton)

    const mockFile = new File(['{"test": "data"}'], 'test.json', { type: 'application/json' })

    // FileReaderのモック
    const mockFileReader = {
      readAsText: vi.fn().mockImplementation(() => {
        setTimeout(() => {
          if (mockFileReader.onload) {
            mockFileReader.onload({ target: { result: '{"test": "data"}' } })
          }
        }, 0)
      }),
      onload: null as any,
      result: '{"test": "data"}',
    }
    global.FileReader = vi.fn(() => mockFileReader) as any

    // ファイル選択をシミュレート
    const input = wrapper.find('input[type="file"]')
    Object.defineProperty(input.element, 'files', {
      value: [mockFile],
      configurable: true,
    })
    await input.trigger('change')

    // タイマーを進める
    await vi.runAllTimersAsync()
    await wrapper.vm.$nextTick()

    // ボタンが無効化されていることを確認
    const button = wrapper.find('button')
    expect(button.attributes('disabled')).toBeDefined()
    expect(button.text()).toContain('Importing...')
  })
}) 