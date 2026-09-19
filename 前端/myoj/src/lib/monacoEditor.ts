/**
 * 做题页 Monaco：仅 editor API + cpp/java/python，单一 editor worker。
 * 避免 `import 'monaco-editor'` 拉入 TS/HTML/CSS/JSON 等 worker（合计约 9MB+）。
 */
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker.js?worker'
import * as monaco from 'monaco-editor/esm/vs/editor/editor.api.js'
import 'monaco-editor/esm/vs/basic-languages/cpp/cpp.contribution.js'
import 'monaco-editor/esm/vs/basic-languages/java/java.contribution.js'
import 'monaco-editor/esm/vs/basic-languages/python/python.contribution.js'

type MonacoEnvironment = {
  getWorker: () => Worker
}

const env = globalThis as typeof globalThis & { MonacoEnvironment?: MonacoEnvironment }
env.MonacoEnvironment = {
  getWorker: () => new editorWorker(),
}

export type LoadedMonaco = typeof monaco
export default monaco
