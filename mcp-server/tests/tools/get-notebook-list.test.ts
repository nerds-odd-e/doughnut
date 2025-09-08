import { describe, test, expect } from 'vitest'
import { findTool } from '../helpers/index.js'

describe('get_notebook_list tool', () => {
  test('should be defined and have correct name', () => {
    const getNotebookListTool = findTool('get_notebook_list')
    expect(getNotebookListTool).toBeDefined()
    expect(getNotebookListTool.name).toBe('get_notebook_list')
  })
})
