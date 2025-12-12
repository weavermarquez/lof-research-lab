# LoF Research Lab

**A "mentions feed" for academic papers** — follow papers, see who's talking about them.

Built with [Rama](https://redplanetlabs.com/docs/~/index.html) (distributed dataflow) + [Frappe](https://frappeframework.com/) (Python web framework) + [Exa](https://exa.ai/) (search API for agents).

## What This Is

You follow a paper. The system finds mentions of that paper across the web (blog posts, tweets, forum discussions, other papers citing it) and shows them in a feed. Like Twitter, but for tracking discourse around specific academic works.

**Demo target**: AI Tinkerers, December 9th, 2025.

## Current Status

### What Exists

- **RecordSeederModule.java**: Working Rama module that parses Zotero XML → Record objects → PState
- **RestAPIIntegrationModule.java**: Working Rama module with async HTTP client and Exa API integration
- **ZoteroXmlParser.java**: Parses the GSB bibliography XML
- **Record.java**: Data class with canonicalId, title, authors, year, doi, url
- **185 seed papers**: George Spencer-Brown Society bibliography (`gsbbib__pretty.xml`)
- **Rama deployed**: Running on VPS
- **Frappe deployed**: Running on VPS
- **Agent-o-rama**: Available for agentic workflows

### What Needs to Be Built (MVP)

See `MVP.md` for the ruthlessly scoped data model.

**Rama side:**
- [ ] Depot for user follow/unfollow actions
- [ ] PState: `followed-works` (user-id → [work-urls])
- [ ] PState: `mentions-by-work` (work-url → [mentions])
- [ ] Topology that calls Frappe endpoint on follow action

**Frappe side:**
- [ ] `search_mentions(work_url)` endpoint that calls Exa
- [ ] Mention DocType for display
- [ ] Simple feed UI showing mentions for followed works

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         RAMA (JVM)                              │
│                                                                 │
│  ┌─────────────────┐    ┌─────────────┐    ┌─────────────────┐  │
│  │ user-actions    │    │  Topology   │    │ PStates         │  │
│  │ Depot           │───▶│  (ETL)      │───▶│ followed-works  │  │
│  │ {follow/unfollow}│    │             │    │ mentions-by-work│  │
│  └─────────────────┘    └──────┬──────┘    └────────┬────────┘  │
│                                │                    │           │
│                                │ HTTP               │ query     │
└────────────────────────────────┼────────────────────┼───────────┘
                                 │                    │
                                 ▼                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                      FRAPPE (Python)                            │
│                                                                 │
│  ┌──────────────────────┐              ┌──────────────────────┐ │
│  │ @frappe.whitelist()  │              │ Feed UI              │ │
│  │ def search_mentions()│              │ (list of mentions)   │ │
│  │   → exa.search()     │              │                      │ │
│  └──────────┬───────────┘              └──────────────────────┘ │
│             │                                                   │
│             ▼                                                   │
│        ┌─────────┐                                              │
│        │ Exa SDK │                                              │
│        └─────────┘                                              │
└─────────────────────────────────────────────────────────────────┘
```

**Why this split?**
- Rama handles state management, event sourcing, distributed processing
- Frappe provides the Python bridge to Exa (no Java SDK exists) and nice UI scaffolding
- Exa finds the mentions

## Quick Start

```bash
# Parse and seed records (existing functionality)
cd server
mvn compile exec:java -Dexec.mainClass="lof.research.lab.RecordSeederModule"
```

## Project Structure

```
lof-research-lab/
├── server/                     # Rama modules (Java)
│   └── src/main/java/
│       ├── lof/research/lab/   # Core research module
│       │   ├── RecordSeederModule.java
│       │   ├── data/Record.java
│       │   └── parsers/ZoteroXmlParser.java
│       └── lof/restapi/exa/    # Exa integration
│           ├── RestAPIIntegrationModule.java
│           └── data/SearchRequest.java
├── client/                     # Frappe app (TODO)
├── gsbbib__pretty.xml          # 185 seed papers
├── MVP.md                      # Scoped data model
├── AGENTS.md                   # Architecture details + bd workflow
└── README.md                   # You are here
```

## References

- [Rama Documentation](https://redplanetlabs.com/docs/~/index.html)
- [Rama Demo Gallery](https://github.com/redplanetlabs/rama-demo-gallery)
- [Frappe Framework](https://frappeframework.com/)
- [Exa API](https://docs.exa.ai/)
- [George Spencer Brown Society](https://lof50.com)

## Task Tracking

This project uses `bd` (beads) for issue tracking. See `AGENTS.md` for workflow.

```bash
bd ready          # Show unblocked tasks
bd list           # Show all tasks
```
