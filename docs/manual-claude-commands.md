# Manual claude launch commands

Same provider options the Argus UI offers, run by hand from a terminal on another machine. Each block is the env vars Argus injects, then `claude`.

## Providers

- Anthropic cloud (default): no backend env, model comes from `DEFAULT_MODEL` (`claude-opus-4-8` unless overridden).
- `rig` — Gaming Rig Ollama at `http://192.168.0.150:11434`
- `host` — Host 3080 Ollama at `http://localhost:11434`

## Panel Options (new UI)

The panel now uses a dropdown selector with two columns: **(Provider)** | **(Model)**.

Selecting a model launches a terminal that runs the matching command. Every command sets `MAX_THINKING_TOKENS=0 CLAUDE_CODE_DISABLE_ALTERNATE_SCREEN=1` and appends the project system prompt.

### Default Provider

- **Provider**: default
- **Model**: claude-opus-4-8
- **Command**: `MAX_THINKING_TOKENS=0 CLAUDE_CODE_DISABLE_ALTERNATE_SCREEN=1 claude --model claude-opus-4-8 --append-system-prompt "..."`

### Ollama Provider

Only available if the `ollama` command exists on your PATH. Always uses local Ollama at `http://localhost:11434`.

**Models to choose from:**

| Model | Description |
|-------|-------------|
| qwen3.5:9b | Fast general-purpose model (recommended default) |
| qwen2.5-coder:14b | Coder-focused model |
| glm-4.7-flash:latest | Flash-style fast model |

**Environment:** `ANTHROPIC_BASE_URL=http://localhost:11434`, `ANTHROPIC_AUTH_TOKEN=ollama`, `AGENT_BACKEND=host`, `CLAUDE_CODE_MAX_OUTPUT_TOKENS=16000`, `CLAUDE_CODE_DISABLE_BUNDLED_SKILLS=1`.

### OpenRouter Provider

Uses the OpenRouter API at `https://openrouter.ai/api/v1`.

**Models to choose from:**

| Model | Description |
|-------|-------------|
| z-ai/glm-5 | Cost effective model to use |
| z-ai/glm-5.3-flash | Even cheaper quantized version |
| cohere/north-mini-code:free | Free model for testing if it works |

**Environment:** `ANTHROPIC_BASE_URL=https://openrouter.ai/api/v1`, `ANTHROPIC_AUTH_TOKEN=openrouter`, `AGENT_BACKEND=openrouter`, `CLAUDE_CODE_MAX_OUTPUT_TOKENS=16000`, `CLAUDE_CODE_DISABLE_BUNDLED_SKILLS=1`.

## Anthropic cloud

```bash
MAX_THINKING_TOKENS=0 \
CLAUDE_CODE_DISABLE_ALTERNATE_SCREEN=1 \
claude --model claude-opus-4-8 \
  --append-system-prompt "You are a Claude Code agent for the project in <dir>. Stay within this directory — do not read or write files outside of it unless the user explicitly asks you to."
```

Drop `MAX_THINKING_TOKENS=0` to enable thinking (Argus defaults it off). For the argus project itself, use `--append-system-prompt-file <path>/ARGUS.md` instead of the inline prompt.

## Gaming Rig (Ollama)

```bash
ANTHROPIC_BASE_URL=http://192.168.0.150:11434 \
ANTHROPIC_AUTH_TOKEN=ollama \
AGENT_BACKEND=rig \
CLAUDE_CODE_MAX_OUTPUT_TOKENS=16000 \
CLAUDE_CODE_AUTO_COMPACT_WINDOW=<ctx> \
CLAUDE_CODE_DISABLE_BUNDLED_SKILLS=1 \
MAX_THINKING_TOKENS=0 \
CLAUDE_CODE_DISABLE_ALTERNATE_SCREEN=1 \
claude --model <model> \
  --append-system-prompt "You are a Claude Code agent for the project in <dir>. Stay within this directory — do not read or write files outside of it unless the user explicitly asks you to."
```

Models: `qwen3-coder:30b`, `qwen3.5:9b`, `devstral:24b`, `glm-4.7-flash:latest`, `gpt-oss:20b`, `qwen3.6:35b`, `qwen3.6:27b`, `qwen3.8:27b`, `qwen2.5-coder:14b`, `qwen2.5-coder:7b`, `qwen3:0.6b`

## Host 3080 (Ollama)

```bash
ANTHROPIC_BASE_URL=http://localhost:11434 \
ANTHROPIC_AUTH_TOKEN=ollama \
AGENT_BACKEND=host \
CLAUDE_CODE_MAX_OUTPUT_TOKENS=16000 \
CLAUDE_CODE_AUTO_COMPACT_WINDOW=<ctx> \
CLAUDE_CODE_DISABLE_BUNDLED_SKILLS=1 \
MAX_THINKING_TOKENS=0 \
CLAUDE_CODE_DISABLE_ALTERNATE_SCREEN=1 \
claude --model <model> \
  --append-system-prompt "You are a Claude Code agent for the project in <dir>. Stay within this directory — do not read or write files outside of it unless the user explicitly asks you to."
```

Models: `qwen2.5-coder:14b`, `qwen2.5-coder:7b`, `qwen3.8:27b`, `qwen3.5:9b`, `qwen3:8b`, `ornith:9b`, `llama3.1:8b`, `qwen3:0.6b`

## Variables

- `<ctx>` — `CLAUDE_CODE_AUTO_COMPACT_WINDOW` = `min(model context length, 131072)`. Argus queries Ollama live (`/api/show`) and injects it; manually, get the context with:
  ```bash
  curl -s http://192.168.0.150:11434/api/show -d '{"model":"<model>"}' | grep -o '"context_length":[0-9]*'
  ```
  If you skip it, Claude Code assumes 200k and small local models overflow instead of compacting.

- `CLAUDE_CODE_MAX_OUTPUT_TOKENS` — 16000 configured, but Argus clamps it to `window/4` (min 1024) when 16000 would be half or more of the context window. Do the same manually for small-context models.

- `AGENT_BACKEND` — set on the claude process so guard hooks can apply stricter rules; the agent cannot clear it.

- `CLAUDE_CODE_DISABLE_BUNDLED_SKILLS=1` — alternate models hallucinate tasks from Claude Code's bundled skill/agent menu; drops it. User skills/plugins are unaffected.

- `--dangerously-skip-permissions` — the UI's opt-in bypass checkbox; skips the per-tool permission classifier (big latency win on local models). Add it to any block if wanted.

- `--resume <sessionId>` — replaces `--model` on a resume; the session restores its original model itself.
