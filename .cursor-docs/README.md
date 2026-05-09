# `.cursor-docs/` — AI-oriented Companion Documentation

This folder holds **lightweight markdown summaries** of source files in this project.
The goal is to give Cursor (and other LLM agents) cheap, pre-digested context about
what each class/module does, **without polluting the source code with comments and
without burning tokens re-reading large files on every question**.

## How it works

1. The rule at `.cursor/rules/find-or-create-docs.mdc` instructs the agent to follow
   a **Find-or-Create** workflow whenever you ask "incele / analiz et / explain"
   about a class.
2. If a corresponding `.md` summary already exists here, the agent reads only that
   summary (cheap).
3. If not, the agent reads the source once, writes the summary here, and then
   answers from the summary. Every subsequent question is cheap.
4. When the agent modifies source code, it also updates the corresponding summary
   to keep them in sync.

## Folder layout

The structure mirrors the source tree 1:1:

```
project-root/
├── src/
│   └── services/
│       └── CustomerService.ts        ← source
└── .cursor-docs/
    └── src/
        └── services/
            └── CustomerService.md    ← AI summary (mirror path)
```

## File status

Each summary has a header with `Last verified` date and `Status` (`fresh` | `stale`).
- Mark a doc as `stale` if you know the source has changed and the summary may no
  longer reflect reality. The agent will regenerate it on the next "incele" request.
- The agent will also regenerate on demand if you say "yenile" / "refresh" /
  "kodu tekrar oku".

## Useful commands you can give the agent

- `CustomerService'i incele` — find or create the summary, then explain.
- `CustomerService dökümanını yenile` — force regenerate the summary from current source.
- `OrderProcessor'ı oku ve değiştir, X metodunu ekle` — read source, modify, then
  update the summary in the same turn.
- `Tüm stale dökümanları yenile` — sweep `.cursor-docs/` and refresh anything marked stale.

## What goes in a summary?

See `_TEMPLATE.md` and the example under `_examples/`. Keep summaries **short**
(target 60–150 lines). They are not full documentation — they are cheat sheets
for the agent.

## What does NOT go here

- API reference / end-user docs → use a `docs/` folder for those.
- Architecture decisions → use `docs/adr/` or similar.
- Full code excerpts — reference symbols by name, do not paste bodies.

## Manual editing

You can edit these files by hand at any time. The agent will respect manual edits;
it only regenerates on stale/missing docs or when you ask it to.
