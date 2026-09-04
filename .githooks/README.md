# Git hooks

Tracked hooks for this repo. Git does not use them automatically — activate once
per clone:

```sh
git config core.hooksPath .githooks
```

The hook files are committed executable (`100755`). If git ever reports a hook was
ignored for not being executable, run `chmod +x .githooks/*`.

## pre-push

Two Claude Code checks on a push, in order:

1. **Tests + coverage** — the `unit-tester` subagent (`.claude/agents/unit-tester.md`)
   runs `./mvnw test` and prints a per-class JaCoCo coverage table. **Blocks the
   push on a genuine test failure** (`TEST_RESULT: FAIL`). A coverage number never
   blocks. If the suite can't run for an environmental reason — Postgres not
   listening on host port 5332, missing generated sources — it reports
   `TEST_RESULT: SKIP` and the push is allowed.

   This phase runs Maven against the **working tree**, not the pushed diff, so it
   only runs when *all* of these hold: the push changes something under `src/` or
   `pom.xml`; the working tree is clean; and `HEAD` is one of the pushed tips.
   Otherwise it prints why it skipped and the push proceeds. Needs Postgres up
   (`docker compose up -d db`) — the suite now also covers the MCP server, and
   `SoftwareEngineerMcpIntegrationTest` hits real Postgres on 5332 like the other
   `@SpringBootTest` classes.
2. **Code review** — the `code-reviewer` subagent (`.claude/agents/code-reviewer.md`)
   reviews the pushed diff. **Blocks the push on a 🔴 blocker**
   (`REVIEW_RESULT: BLOCK`).

Handles multi-ref pushes (branch + tags) and first pushes of a new branch. Each
check is abandoned after its timeout (`TEST_TIMEOUT` / `CODE_REVIEW_TIMEOUT`,
default 600s each; worst-case push latency is their sum) rather than hanging the
push. Fails open: if the `claude` CLI is missing, a check errors out, times out,
or returns no verdict, the push is allowed. Requires the `claude` CLI on `PATH`
(or at `~/.local/bin/claude`, or set `CLAUDE_BIN`).

Bypass:

```sh
SKIP_TESTS=1 git push         # skip just the test run
SKIP_CODE_REVIEW=1 git push   # skip just the review (the test run still happens)
git push --no-verify          # skip all hooks
```
