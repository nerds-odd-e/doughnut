import type { ComponentType } from 'react'
import { describe, expect, test, vi } from 'vitest'
import type {
  InteractiveSlashCommand,
  InteractiveSlashCommandStageProps,
} from '../src/commands/interactiveSlashCommand.js'
import { applyResolvedInteractiveSlashCommand } from '../src/commands/interactiveSlashCommandDispatch.js'

const DummyStage = (() =>
  null) as ComponentType<InteractiveSlashCommandStageProps>

const stageCmd: InteractiveSlashCommand = {
  literal: '/nest',
  doc: {
    name: '/nest',
    usage: '/nest <id>',
    description: 'nested stage',
  },
  argument: { name: 'id', optional: false },
  stageComponent: DummyStage,
  stageIndicator: 'Nested',
}

const runCmd: InteractiveSlashCommand = {
  literal: '/ping',
  doc: {
    name: '/ping',
    usage: '/ping',
    description: 'ping',
  },
  run: () => ({ assistantMessage: 'pong' }),
}

const runNeedsArg: InteractiveSlashCommand = {
  literal: '/q',
  doc: {
    name: '/q',
    usage: '/q <query>',
    description: 'needs arg',
  },
  argument: { name: 'query', optional: false },
  run: () => ({ assistantMessage: 'ok' }),
}

function dispatchHandlers() {
  return {
    appendScrollbackError: vi.fn(),
    setStageArgumentRef: vi.fn(),
    openStage: vi.fn(),
    onRunSuccess: vi.fn(),
  }
}

describe('applyResolvedInteractiveSlashCommand', () => {
  test('stage: missing required argument reports usage', () => {
    const h = dispatchHandlers()
    applyResolvedInteractiveSlashCommand(
      { command: stageCmd, argument: undefined, line: '/nest' },
      h
    )
    expect(h.appendScrollbackError).toHaveBeenCalledWith(
      'Missing id. Usage: /nest <id>'
    )
    expect(h.openStage).not.toHaveBeenCalled()
  })

  test('stage: opens with argument ref and indicator', () => {
    const h = dispatchHandlers()
    applyResolvedInteractiveSlashCommand(
      { command: stageCmd, argument: 'abc', line: '/nest abc' },
      h
    )
    expect(h.setStageArgumentRef).toHaveBeenCalledWith('abc')
    expect(h.openStage).toHaveBeenCalledWith({
      component: DummyStage,
      stageIndicator: 'Nested',
    })
  })

  test('stage: omits empty stageIndicator', () => {
    const bareStage: InteractiveSlashCommand = {
      literal: '/bare',
      doc: {
        name: '/bare',
        usage: '/bare',
        description: 'bare',
      },
      stageComponent: DummyStage,
    }
    const openStage = vi.fn()
    applyResolvedInteractiveSlashCommand(
      { command: bareStage, argument: undefined, line: '/bare' },
      {
        appendScrollbackError: vi.fn(),
        setStageArgumentRef: vi.fn(),
        openStage,
        onRunSuccess: vi.fn(),
      }
    )
    expect(openStage).toHaveBeenCalledWith({
      component: DummyStage,
      stageIndicator: undefined,
    })
  })

  test('run: missing required argument reports usage', () => {
    const h = dispatchHandlers()
    applyResolvedInteractiveSlashCommand(
      { command: runNeedsArg, argument: undefined, line: '/q' },
      h
    )
    expect(h.appendScrollbackError).toHaveBeenCalledWith(
      'Missing query. Usage: /q <query>'
    )
    expect(h.onRunSuccess).not.toHaveBeenCalled()
  })

  test('run: success invokes onRunSuccess', async () => {
    const h = dispatchHandlers()
    applyResolvedInteractiveSlashCommand(
      { command: runCmd, argument: undefined, line: '/ping' },
      h
    )
    await vi.waitFor(() => {
      expect(h.onRunSuccess).toHaveBeenCalledWith(runCmd, 'pong')
    })
  })

  test('run: a synchronous throw reaches the user, not the terminal', async () => {
    const boom: InteractiveSlashCommand = {
      literal: '/boom',
      doc: {
        name: '/boom',
        usage: '/boom',
        description: 'boom',
      },
      run: () => {
        throw new Error('explode')
      },
    }
    const appendScrollbackError = vi.fn()
    applyResolvedInteractiveSlashCommand(
      { command: boom, argument: undefined, line: '/boom' },
      {
        appendScrollbackError,
        setStageArgumentRef: vi.fn(),
        openStage: vi.fn(),
        onRunSuccess: vi.fn(),
      }
    )
    await vi.waitFor(() => {
      expect(appendScrollbackError).toHaveBeenCalledWith('explode')
    })
  })

  test('run: rejection maps through userVisibleSlashCommandError', async () => {
    const boom: InteractiveSlashCommand = {
      literal: '/boom',
      doc: {
        name: '/boom',
        usage: '/boom',
        description: 'boom',
      },
      run: () => Promise.reject(new Error('explode')),
    }
    const appendScrollbackError = vi.fn()
    applyResolvedInteractiveSlashCommand(
      { command: boom, argument: undefined, line: '/boom' },
      {
        appendScrollbackError,
        setStageArgumentRef: vi.fn(),
        openStage: vi.fn(),
        onRunSuccess: vi.fn(),
      }
    )
    await vi.waitFor(() => {
      expect(appendScrollbackError).toHaveBeenCalledWith('explode')
    })
  })
})
