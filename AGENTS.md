# AGENTS.md - LoF Research Lab Architecture

## Problem Statement

Building an academic resource indexer for the cybernetics research community, specifically focused on Laws of Form (LoF), George Spencer-Brown's work, and related systems theory/cybernetics literature.

**Starting point**: 185 seed papers from the George Spencer Brown society exported as Zotero XML.

**Core challenges**:
- Only 1 out of 185 papers has a DOI - need alternative canonical identification
- Diverse sources: academic journals, conference proceedings, online courses, personal websites, para-academic works
- Need to crawl, normalize, version, and enable long-running agentic research on top
- Learning Rama and web scraping simultaneously

## Issue Tracking with bd (beads)

**IMPORTANT**: This project uses **bd (beads)** for ALL issue tracking. Do NOT use markdown TODOs, task lists, or other tracking methods.

### Why bd?

- Dependency-aware: Track blockers and relationships between issues
- Git-friendly: Auto-syncs to JSONL for version control
- Agent-optimized: JSON output, ready work detection, discovered-from links
- Prevents duplicate tracking systems and confusion

### Quick Start

**Check for ready work:**
```bash
bd ready --json
```

**Create new issues:**
```bash
bd create "Issue title" -t bug|feature|task -p 0-4 --json
bd create "Issue title" -p 1 --deps discovered-from:bd-123 --json
```

**Claim and update:**
```bash
bd update bd-42 --status in_progress --json
bd update bd-42 --priority 1 --json
```

**Complete work:**
```bash
bd close bd-42 --reason "Completed" --json
```

### Issue Types

- `bug` - Something broken
- `feature` - New functionality
- `task` - Work item (tests, docs, refactoring)
- `epic` - Large feature with subtasks
- `chore` - Maintenance (dependencies, tooling)

### Priorities

- `0` - Critical (security, data loss, broken builds)
- `1` - High (major features, important bugs)
- `2` - Medium (default, nice-to-have)
- `3` - Low (polish, optimization)
- `4` - Backlog (future ideas)

### Workflow for AI Agents

1. **Check ready work**: `bd ready` shows unblocked issues
2. **Claim your task**: `bd update <id> --status in_progress`
3. **Work on it**: Implement, test, document
4. **Discover new work?** Create linked issue:
   - `bd create "Found bug" -p 1 --deps discovered-from:<parent-id>`
5. **Complete**: `bd close <id> --reason "Done"`
6. **Commit together**: Always commit the `.beads/issues.jsonl` file together with the code changes so issue state stays in sync with code state

### Auto-Sync

bd automatically syncs with git:
- Exports to `.beads/issues.jsonl` after changes (5s debounce)
- Imports from JSONL when newer (e.g., after `git pull`)
- No manual export/import needed!

### MCP Server (Recommended)

If using Claude or MCP-compatible clients, install the beads MCP server:

```bash
pip install beads-mcp
```

Add to MCP config (e.g., `~/.config/claude/config.json`):
```json
{
  "beads": {
    "command": "beads-mcp",
    "args": []
  }
}
```

Then use `mcp__beads__*` functions instead of CLI commands.

### Managing AI-Generated Planning Documents

AI assistants often create planning and design documents during development:
- PLAN.md, IMPLEMENTATION.md, ARCHITECTURE.md
- DESIGN.md, CODEBASE_SUMMARY.md, INTEGRATION_PLAN.md
- TESTING_GUIDE.md, TECHNICAL_DESIGN.md, and similar files

**Best Practice: Use a dedicated directory for these ephemeral files**

**Recommended approach:**
- Create a `history/` directory in the project root
- Store ALL AI-generated planning/design docs in `history/`
- Keep the repository root clean and focused on permanent project files
- Only access `history/` when explicitly asked to review past planning

**Example .gitignore entry (optional):**
```
# AI planning documents (ephemeral)
history/
```

**Benefits:**
- ✅ Clean repository root
- ✅ Clear separation between ephemeral and permanent documentation
- ✅ Easy to exclude from version control if desired
- ✅ Preserves planning history for archeological research
- ✅ Reduces noise when browsing the project

### Important Rules

- ✅ Use bd for ALL task tracking
- ✅ Always use `--json` flag for programmatic use
- ✅ Link discovered work with `discovered-from` dependencies
- ✅ Check `bd ready` before asking "what should I work on?"
- ✅ Store AI planning docs in `history/` directory
- ❌ Do NOT create markdown TODO lists
- ❌ Do NOT use external issue trackers
- ❌ Do NOT duplicate tracking systems
- ❌ Do NOT clutter repo root with planning documents

For more details, see README.md and QUICKSTART.md.

## Why Rama?

Rama enables us to:
1. **Design query-first**: Start with PStates that match client needs, not database schemas
2. **Evolve incrementally**: Add topologies and PStates as we learn, without migrations
3. **Distributed state**: Handle growing corpus + concurrent research jobs naturally
4. **Fault tolerance**: Built-in exactly-once processing for crawl jobs

## Key Insights from Navigating rama-examples

### 1. PState-First vs Schema-First Design

**Traditional SQL/NoSQL**: Start with tables/collections, add indices later
```sql
CREATE TABLE papers (id, title, authors, year, ...);
CREATE INDEX idx_author ON papers(authors);
CREATE INDEX idx_year ON papers(year);
```

**Rama approach**: Design PStates around access patterns upfront
```java
// How will clients query?
pstate("$$papersById", Map<String, Paper>)          // Get by ID
pstate("$$papersByAuthor", Map<String, Set<String>>) // Find by author
pstate("$$urlFrontier", Set<String>)                 // Crawler queue
```

The PStates ARE the query API. No impedance mismatch.

### 2. Topology = Data Flow, Not CRUD Operations

In SQL/NoSQL you write:
- INSERT statements
- UPDATE queries
- Batch jobs as separate scripts
- Message queues to connect them

In Rama:
- **Depots** are append-only event streams (immutable inputs)
- **Topologies** are dataflow graphs that transform and route data
- **PStates** are materialized views that update automatically
- Everything is declarative and exactly-once

**Example from ExampleMicrobatchTopologyModule**:
```java
mb.source("*depot").out("*microbatch")
  .explodeMicrobatch("*microbatch").out("*record")
  .each(Parser::parse, "*record").out("*paper")
  .hashPartition("*id")  // Distributed routing
  .localTransform("$$papersById", Path.key("*id").termVal("*paper"))
```

This is ETL, storage, and query optimization in one declaration.

### 3. Partitioning is First-Class

In SQL: Sharding is painful, manual, requires coordination layer (Vitess, Citus)
In NoSQL: Partition keys are rigid, rebalancing is complex

In Rama: Partitioning is declared upfront and automatic:
```java
setup.declareDepot("*depot", Depot.hashBy(Paper::getCanonicalId));
// Depot automatically partitions by paper ID across cluster
```

From RamaSpaceModule, we saw sophisticated partitioning:
```java
Depot.hashBy(Block.<Post>fn(p -> p.authorUserId))
// Posts automatically colocate with author's data
```

### 4. No "Query Language" - It's Java

SQL: Learn SQL, ORMs, query builders
NoSQL: Learn MongoDB query syntax, CQL, etc.

Rama: Just Java (or Clojure/Groovy)
```java
// Not a string query - it's code
.each((Paper p) -> p.getAuthors(), "*paper").out("*authors")
.each((List<String> authors) -> authors.get(0), "*authors").out("*firstAuthor")
```

Refactoring, IDE support, type safety all work naturally.

### 5. Aggregations are Composable Primitives

From AggregateModule and TopNWordsModule examples:
```java
CompoundAgg.map("*author",
  CompoundAgg.topN(10, "*paper", Ops.COMPARE_DESC))
```

vs SQL:
```sql
SELECT author, papers
FROM (
  SELECT author, paper, ROW_NUMBER() OVER (PARTITION BY author ORDER BY year DESC)
  FROM papers
) WHERE rn <= 10
```

The Rama version is compositional - `topN` is a building block you can nest.

### 6. ID Generation is a Pattern, Not a Feature

SQL: AUTO_INCREMENT, SERIAL, IDENTITY
NoSQL: ObjectId, UUID libraries

Rama: `TaskUniqueIdPState` helper (from RamaSpaceModule)
```java
TaskUniqueIdPState idGen = new TaskUniqueIdPState("$$postId").descending();
idGen.declarePState(topology);
idGen.genId("*id")  // Thread-safe, distributed, gap-free
```

Or roll your own - it's just a PState that maintains a counter.

### 7. Testing is Built-In

From all examples:
```java
try (InProcessCluster cluster = InProcessCluster.create()) {
  cluster.launchModule(new MyModule(), new LaunchConfig(4, 4));
  Depot depot = cluster.clusterDepot(moduleName, "*depot");
  depot.append(data);
  cluster.waitForMicrobatchProcessedCount(moduleName, "topology", 1);
  // Assert on PState
}
```

No Docker Compose, no test databases, no fixtures. The entire distributed system runs in-process.

## Architecture Phases

### V0: XML Seeder (Current Focus)
**Goal**: Make first contact with Rama. Parse XML → canonical records → seed PStates.

**Components**:
- `Paper.java`: POJO implementing RamaSerializable
- `ZoteroXmlParser.java`: Parse Zotero XML export to Paper objects
- `PaperSeederModule.java`: Microbatch topology to ingest records

**PStates**:
- `$$papersById`: Map<String, Paper> - canonical paper storage
- `$$urlFrontier`: Set<String> - URLs to crawl (seed for V1)

**Canonical ID Strategy**:
1. If DOI exists: use it
2. Else: generate slug from `firstAuthor-year-titleWords` (normalized, lowercase)
3. Handle collisions with incremental suffixes

**Learning objectives**:
- Depot → Topology → PState flow
- MicrobatchTopology vs StreamTopology
- InProcessCluster testing
- PState schemas and Path queries

### V1: Basic Crawler
**Goal**: Dumb topologies. Hardcoded URLs → fetch → normalize → store → simple search CLI.

**New Components**:
- HTTP fetcher service (external to Rama, or integrated via async operations)
- HTML parser/normalizer
- Simple text search over abstracts/content

**New PStates**:
- `$$htmlCache`: Map<String, CachedHtml> - raw fetched content
- `$$paperContent`: Map<String, NormalizedText> - parsed/cleaned text
- `$$invertedIndex`: Map<String, Set<String>> - term → paperIds

**Learning objectives**:
- ETL from external services
- Agg.list and Agg.set for building indices
- Query composition with Path API

### V2: Frontier Expansion
**Goal**: Add snapshotting, frontier expansion, link expansion.

**New topology**: FrontierExpansionTopology
- Extracts citations from papers
- Matches citations to known papers or adds to frontier
- Tracks discovered-at timestamps

**New PStates**:
- `$$citationGraph`: Map<String, Set<String>> - paperId → citedPaperIds
- `$$paperVersions`: Map<String, Map<Long, Paper>> - versioned snapshots
- `$$frontierMetadata`: Map<String, FrontierEntry> - URL + priority + discovered-at

**Learning objectives**:
- StreamTopology for real-time frontier updates
- Versioned PStates (using timestamp keys)
- Graph traversal patterns

### V3: Research Job State Machine
**Goal**: Plan → gather → synthesize. Resumable long-running jobs.

**New topology**: ResearchJobTopology
- State machine: PLANNING → GATHERING → SYNTHESIZING → COMPLETE
- LLM integration for agentic research
- Checkpointing for resume-ability

**New PStates**:
- `$$researchJobs`: Map<String, ResearchJob> - job state
- `$$jobResults`: Map<String, Map<String, Artifact>> - jobId → outputs

**Learning objectives**:
- FSM patterns in Rama
- External service integration (LLM APIs)
- Long-running workflows

### V4: Simulation Harness
**Goal**: Add minimal simulation harness for agent invariants.

Property testing for research agents:
- Idempotency: re-running same query gives same results
- Monotonicity: more papers → more comprehensive results
- Citation consistency: if A cites B, B appears in corpus

**Learning objectives**:
- Rama's testing utilities
- Invariant checking patterns

## Current Status

Please check `bd ready` for open tasks, or `bd list` for all tasks.

## References

- [Rama Documentation](https://redplanetlabs.com/docs/~/index.html)
- [Rama Demo Gallery](https://github.com/redplanetlabs/rama-demo-gallery)
- George Spencer Brown Society: [GSB Bibliography](gsbbib__pretty.xml)
