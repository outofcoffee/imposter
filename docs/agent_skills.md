# Agent skills

AI coding assistants (Claude Code, GitHub Copilot, Cursor, and others) can help you create and manage Imposter mocks — but they need to know *how*. Agent skills are reusable instruction files that teach your AI assistant everything it needs to work with Imposter: from installation and configuration to CI/CD pipelines and test embedding.

The [imposter-project/agent-skills](https://github.com/imposter-project/agent-skills) repository contains the official Imposter skill, covering the CLI, engine types, Docker, GitHub Actions, and test integration for both JVM and JavaScript projects.

## How it works

Agent skills let your AI assistant:

- Start and stop mock servers using the Imposter CLI
- Scaffold configuration from OpenAPI specifications
- Choose the right engine type (native, Docker, or JVM)
- Embed mocks in JUnit or Jest test suites
- Run mocks in CI/CD pipelines with GitHub Actions
- Build self-contained Docker images with mock configuration

The skill is written as a structured markdown file (following the [Agent Skills specification](https://agentskills.io/specification)) that AI coding tools can discover and use automatically.

## Quick start

### Claude Code (plugin — recommended)

The simplest way to install is as a Claude Code plugin.

Add the marketplace (one-time setup):

```
/plugin marketplace add imposter-project/agent-skills
```

Install the plugin:

```
/plugin install imposter@imposter-agent-skills
```

Once installed, invoke the skill with:

```
/imposter:mocks
```

### Manual installation

**Personal (all projects):**

```bash
git clone https://github.com/imposter-project/agent-skills.git
mkdir -p ~/.claude/skills
ln -s "$(pwd)/agent-skills/skills/mocks" ~/.claude/skills/mocks
```

**Project-level (single project):**

```bash
mkdir -p .claude/skills
cp -r /path/to/agent-skills/skills/mocks .claude/skills/mocks
```

### GitHub Copilot (VS Code)

Copilot discovers skills from the same locations:

```bash
# Copilot-specific directory
mkdir -p ~/.copilot/skills
ln -s /path/to/agent-skills/skills/mocks ~/.copilot/skills/mocks

# Or use the shared ~/.claude/skills directory (works for both)
mkdir -p ~/.claude/skills
ln -s /path/to/agent-skills/skills/mocks ~/.claude/skills/mocks
```

## What's included

```
skills/
  mocks/
  ├── SKILL.md                          # Main skill instructions
  ├── scripts/
  │   ├── healthcheck.sh                # Check if the mock server is healthy
  │   └── wait-for-mock.sh              # Wait for the mock server to be ready
  └── references/
      ├── ci-cd-github-actions.md       # GitHub Actions integration guide
      ├── docker.md                     # Running Imposter in Docker
      ├── imposter-js.md                # JavaScript/Node.js test bindings
      ├── jvm-embedding.md              # JVM test embedding (JUnit/TestNG)
      └── configuration.md              # Detailed configuration reference
```

## What the skill covers

### Local development

The skill teaches your AI assistant how to install the Imposter CLI, start and stop mock servers, scaffold configuration from OpenAPI specs, and check mock server health.

### Engine types

Imposter can run under several engine types — native single-binary (v5+), Docker, or JVM. The skill explains when to use each one and how to configure them.

### CI/CD pipelines

Includes step-by-step guidance for running Imposter mocks in GitHub Actions, using the three official actions: `setup`, `start-mocks`, and `stop-mocks`.

### Docker

Covers running Imposter as a Docker container directly (without the CLI), building self-contained images with your mock configuration baked in, and Docker Compose setup.

### Test integration

The skill includes reference guides for embedding Imposter mocks in both JVM tests (JUnit/TestNG) and JavaScript tests (Jest/Mocha) using the `imposter-js` Node.js library.

## Team setup

For teams using Claude Code, add the marketplace to your project's `.claude/settings.json` so team members are prompted to install automatically:

```json
{
  "extraKnownMarketplaces": {
    "imposter-agent-skills": {
      "source": {
        "source": "github",
        "repo": "imposter-project/agent-skills"
      }
    }
  }
}
```

Team members can then install with:

```
/plugin install imposter@imposter-agent-skills
```

## Verify installation

In Claude Code, ask:

```
What skills are available?
```

You should see the `mocks` skill listed. If installed as a plugin, invoke it directly with:

```
/imposter:mocks
```

## Links

- [Agent skills repository](https://github.com/imposter-project/agent-skills)
- [Agent Skills specification](https://agentskills.io/specification)
- [Imposter CLI](https://github.com/imposter-project/imposter-cli)
- [Imposter GitHub Actions](https://github.com/imposter-project/imposter-github-action)
- [Configuration examples](https://github.com/imposter-project/examples)