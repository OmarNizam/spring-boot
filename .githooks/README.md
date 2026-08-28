# Git hooks

Tracked hooks for this repo. Git does not use them automatically — activate once
per clone:

```sh
git config core.hooksPath .githooks
```

The hook files are committed executable (`100755`). If git ever reports a hook was
ignored for not being executable, run `chmod +x .githooks/*`.

## pre-push

Runs the Claude Code `code-reviewer` subagent (`.claude/agents/code-reviewer.md`)
against the commits being pushed and prints the review. It **blocks the push only
when the review reports a 🔴 blocker** (`REVIEW_RESULT: BLOCK`).

Handles multi-ref pushes (branch + tags) and first pushes of a new branch, and
abandons the review after `CODE_REVIEW_TIMEOUT` seconds (default 600) rather than
hanging the push. Fails open: if the `claude` CLI is missing, the review errors
out, times out, or returns no verdict, the push is allowed. Requires the `claude`
CLI on `PATH` (or at `~/.local/bin/claude`, or set `CLAUDE_BIN`).

Bypass:

```sh
SKIP_CODE_REVIEW=1 git push   # skip just this review
git push --no-verify          # skip all hooks
```
