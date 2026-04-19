# User stories — Obsidian ↔ Doughnut (CLI)

**Pull:** Full tree from the notebook head, but fetch/write **only what changed** (e.g. compare last-modified or equivalent timestamps on server vs local).

**Attachments:** Not downloaded; they stay in Doughnut.

**Conflicts (v1):** Last-write-wins plus a clear warning when the other side had newer or divergent edits.

---

1. **Doughnut UI:** Wiki-style `[[link]]` in note **details** are **clickable**. Treat the tree like **Wikipedia-style subpages**: within a notebook, the path from the **head note** down the hierarchy maps to link paths (top-level notes are the first segment; children extend the path).
   - **1.1** In **markdown** mode, users edit raw wiki syntax; in **rich** mode, links render as clickable links (not bracket literals).
   - **1.2** In **rich** mode, edits round-trip to **markdown** as well-formed wiki links (`[[…]]`).
   - **1.3** Support **pipe syntax** for alternate display text: `[[note title|display text]]`.
   - **1.4** Allow links to notes that **do not exist yet** (“red link”); clicking **creates** the target note.
   - **1.5** Support **cross-notebook** links, e.g. `[[notebook title:note title/child title]]` (exact grammar as defined by the product).

2. **Doughnut UI:** **Auto-update** `[[link]]` display text (and/or target path where applicable) when the **link target** changes.
   - **2.1** The target’s **title** changes.
   - **2.2** The target note is **moved** (e.g. reparented or path in the hierarchy changes).
   - **2.3** The target note is **deleted** (broken link handling / prompt—behavior defined in implementation).

3. User runs CLI to **init** a folder as the bound root for a notebook; the path is stored so **`/use notebook`** can show it in the status bar.

4. User can **download** the head note and **download/sync children** per the rules above.

5. User can **upload** head note body (details) from local to Doughnut.

6. User can **push** local edits for notes that have ids in frontmatter (title, details, and related fields as defined by the sync contract).

7. User can **sync note detail** and **title** changes in both directions within that contract.

8. User can run a **dry run** before applying.

9. User can **sync parent** into downloaded markdown (e.g. a `[[parent]]`-style property) and **apply parent changes from Doughnut → local**.

10. User can **sync new notes Doughnut → local** when they appear remotely (within the changed-only pull model).

11. User can **sync deletion in both directions** under one story: local → Doughnut and Doughnut → local, with one explicit rule set (not two separate features).

12. Use wiki-style links in **graph RAG**.

---

## ユーザーストーリー（日本語）— Obsidian ↔ Doughnut（CLI）

**Pull:** **notebook** の **head note** から **full tree** を対象にするが、取得・書き込みは **変更分のみ**（例: **server** と **local** の **last-modified** や同等のタイムスタンプで比較）。

**Attachments:** ダウンロードしない。**Doughnut** 上に残す。

**Conflicts (v1):** **last-write-wins**。他方が新しい／分岐している場合は明確な **warning** を出す。

---

1. **Doughnut UI:** ノート **details** 内の wiki 形式 **`[[link]]`** をクリック可能にする。**notebook** 内のツリーは **Wikipedia** の **subpage** に近い考え方で扱う：**head note** から下る階層が **link** のパスに対応し、最上位ノートがパスの先頭セグメント、子がパスを延ばす。
   - **1.1** **markdown** モードでは生の wiki 記法を編集し、**rich** モードでは **link** をクリック可能な要素として表示する（括弧の生テキストのままにしない）。
   - **1.2** **rich** モードでの編集は、保存時に正しい wiki 形式の **markdown**（`[[…]]`）へ **round-trip** する。
   - **1.3** 表示名の差し替えに **pipe** 記法をサポートする：`[[note title|display text]]`。
   - **1.4** まだ存在しないノートへの **link**（いわゆる **red link**）を許可し、クリックで対象ノートを**新規作成**する。
   - **1.5** **cross-notebook** の **link** をサポートする（例: `[[notebook title:note title/child title]]`。**grammar** はプロダクトで定義）。

2. **Doughnut UI:** **link** の**表示**や、必要に応じて**参照先のパス**を、**link target** の変化に合わせて **auto-update** する。
   - **2.1** 対象の **title** が変わったとき。
   - **2.2** 対象ノートが**移動**したとき（例: **reparent** や階層上のパス変更）。
   - **2.3** 対象ノートが**削除**されたとき（壊れた **link** の扱い／確認は実装で定義）。

3. **CLI** で **`init`** し、フォルダを **notebook** に紐づけた **root** とする。パスを記憶し、**`/use notebook`** 時に **status bar** に表示できる。

4. **download** で **head note** を取得し、上記ルールに従い **children** の **download**／**sync** ができる。

5. **upload** で **head note** の本文（**details**）を **local** → **Doughnut** に送れる。

6. **frontmatter** に **id** のあるノートについて、**local** の編集を **push** できる（**title**、**details**、および **sync contract** で定めた関連フィールド）。

7. 同一 **sync contract** のもとで、**note detail** と **title** の変更を双方向に **sync** できる。

8. 適用前に **dry run** できる。

9. **download** した **markdown** に **parent** を書き込める（例: **`[[parent]]`** 形式のプロパティ）。**parent** の変更を **Doughnut → local** に反映できる。

10. **remote** に新規ノートが現れたら、（**changed-only** の **pull** モデルの範囲で）**Doughnut → local** に新規ノートを **sync** できる。

11. **deletion** は双方向を **ひとつのストーリー**で扱う：**local** → **Doughnut** と **Doughnut** → **local** を、ひと組の明示ルールで扱う（別機能に分けない）。

12. **graph RAG** で wiki 形式の **link** を使う。
