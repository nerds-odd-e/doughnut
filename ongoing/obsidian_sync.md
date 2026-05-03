# User stories — Obsidian ↔ Doughnut (CLI)

**Pull:** Full tree from the notebook (optional root `index` note and the rest of the tree), but fetch/write **only what changed** (e.g. compare last-modified or equivalent timestamps on server vs local).

**Attachments:** Not downloaded; they stay in Doughnut.

**Conflicts (v1):** Last-write-wins plus a clear warning when the other side had newer or divergent edits.

---

1. **Doughnut UI:** Wiki-style `[[link]]` in note **details** are **clickable**. Treat the tree like **Wikipedia-style subpages**: within a notebook, the path from the notebook’s root notes down the hierarchy maps to link paths (top-level notes are the first segment; children extend the path).
   - **1.1** As a note taker, I want to edit wiki links as raw syntax in **markdown** mode and see them as clickable links in **rich** mode (not bracket literals).
   - **1.2** As a note taker, I want **rich**-mode edits to round-trip to **markdown** as well-formed wiki links (`[[…]]`).
   - **1.3** As a note taker, I want **pipe** syntax for alternate display text: `[[note title|display text]]`.
   - **1.4** As a note taker, I want links to notes that do not exist yet ("red link") and to **create** the target when I click.
   - **1.5** As a note taker, I want **cross-notebook** links (e.g. `[[notebook name:note title/child title]]`; grammar per product).
   - **1.6** As a note taker, I want an **insert link** helper (e.g. button or command) to pick a target and insert a wiki link into **details**.
   - **1.7** As a note taker, I want inline **autocomplete** when typing a wiki link in **markdown** mode.
   - **1.8** As a note taker, I want inline **autocomplete** when creating or editing a wiki link in **rich** mode.

2. **Doughnut UI:** **Auto-update** `[[link]]` display text (and/or target path where applicable) when the **link target** changes.
   - **2.1** As a note taker, I want `[[link]]` display (and path if needed) to update when the target’s **title** changes.
   - **2.2** As a note taker, I want links to stay correct when the target note **moves** (e.g. **reparent** or path change).
   - **2.3** As a note taker, I want clear behavior when the target note is **deleted** (broken link / prompt—defined in implementation).

3. User runs CLI to **init** a folder as the bound root for a notebook; the path is stored so **`/use notebook`** can show it in the status bar.

4. User can **download** the optional root **`index`** note (when present) and **download/sync children** per the rules above.

5. User can **upload** the optional root **`index`** note body (details) from local to Doughnut.

6. User can **push** local edits for notes that have ids in frontmatter (title, details, and related fields as defined by the sync contract).

7. User can **sync note detail** and **title** changes in both directions within that contract.

8. User can run a **dry run** before applying.

9. User can **sync parent** into downloaded markdown (e.g. a `[[parent]]`-style property) and **apply parent changes from Doughnut → local**.

10. User can **sync new notes Doughnut → local** when they appear remotely (within the changed-only pull model).

11. User can **sync deletion in both directions** under one story: local → Doughnut and Doughnut → local, with one explicit rule set (not two separate features).

12. Use wiki-style links in **graph RAG**.

---

## ユーザーストーリー（日本語）— Obsidian ↔ Doughnut（CLI）

**Pull:** **notebook** 全体（ルートの **`index`** ノートがあればそれを含む）を **full tree** の対象にするが、取得・書き込みは **変更分のみ**（例: **server** と **local** の **last-modified** や同等のタイムスタンプで比較）。

**Attachments:** ダウンロードしない。**Doughnut** 上に残す。

**Conflicts (v1):** **last-write-wins**。他方が新しい／分岐している場合は明確な **warning** を出す。

---

1. **Doughnut UI:** ノート **details** 内の wiki 形式 `[[link]]` をクリック可能にする。**notebook** 内のツリーは **Wikipedia** の **subpage** に近い考え方で扱う：**notebook** のルートから下る階層が **link** のパスに対応し、最上位ノートがパスの先頭セグメント、子がパスを延ばす。
   - **1.1** ノート利用者として、**markdown** モードでは wiki 記法をそのまま編集し、**rich** モードでは **link** をクリック可能に表示したい（括弧の生テキストのままにしない）。
   - **1.2** ノート利用者として、**rich** モードの編集を、保存時に正しい wiki 形式の **markdown**（`[[…]]`）へ **round-trip** したい。
   - **1.3** ノート利用者として、表示名の差し替えに **pipe** 記法を使いたい：`[[note title|display text]]`。
   - **1.4** ノート利用者として、未作成ノートへの **link**（**red link**）を張り、クリックで対象を**新規作成**したい。
   - **1.5** ノート利用者として、**cross-notebook** の **link** を使いたい（例: `[[notebook name:note title/child title]]`。**grammar** はプロダクトで定義）。
   - **1.6** ノート利用者として、**insert link** ヘルパー（例: **button** やコマンド）で対象を選び、**details** に wiki **link** を挿入したい。
   - **1.7** ノート利用者として、**markdown** モードで wiki **link** を入力するときにインライン **autocomplete** が欲しい。
   - **1.8** ノート利用者として、**rich** モードで wiki **link** を新規・編集するときにインライン **autocomplete** が欲しい。

2. **Doughnut UI:** **link** の**表示**や、必要に応じて**参照先のパス**を、**link target** の変化に合わせて **auto-update** する。
   - **2.1** ノート利用者として、対象の **title** が変わったときに `[[link]]` の表示（必要ならパスも）を更新したい。
   - **2.2** ノート利用者として、対象ノートが**移動**しても（**reparent** やパス変更）**link** が追従したい。
   - **2.3** ノート利用者として、対象ノートが**削除**されたとき壊れた **link** をどう扱うか分かりたい（実装で定義）。

3. **CLI** で **`init`** し、フォルダを **notebook** に紐づけた **root** とする。パスを記憶し、**`/use notebook`** 時に **status bar** に表示できる。

4. **download** でルートの **`index`** ノート（存在する場合）を取得し、上記ルールに従い **children** の **download**／**sync** ができる。

5. **upload** でルートの **`index`** ノートの本文（**details**）を **local** → **Doughnut** に送れる。

6. **frontmatter** に **id** のあるノートについて、**local** の編集を **push** できる（**title**、**details**、および **sync contract** で定めた関連フィールド）。

7. 同一 **sync contract** のもとで、**note detail** と **title** の変更を双方向に **sync** できる。

8. 適用前に **dry run** できる。

9. **download** した **markdown** に **parent** を書き込める（例: `[[parent]]` 形式のプロパティ）。**parent** の変更を **Doughnut → local** に反映できる。

10. **remote** に新規ノートが現れたら、（**changed-only** の **pull** モデルの範囲で）**Doughnut → local** に新規ノートを **sync** できる。

11. **deletion** は双方向を **ひとつのストーリー**で扱う：**local** → **Doughnut** と **Doughnut** → **local** を、ひと組の明示ルールで扱う（別機能に分けない）。

12. **graph RAG** で wiki 形式の **link** を使う。
