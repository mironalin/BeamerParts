## BeamerParts Docs Index

Authoritative (use these first)
- Engineering guide: `docs/beamerparts_development_guide.md`
- API contract (all endpoints, phased): `docs/beamerparts_api_contract.md`
- Project rules for Cursor: `.cursorrules`
- Backlog and tickets: `docs/tickets/README.md`

Standards
- OpenAPI approach (external/admin spec classes, internal minimal): see `.cursorrules` and ticket `docs/tickets/cross-cutting/CC-openapi-alignment.md`
- Testing policy and coverage targets: `.cursorrules`
- Service structure conventions: `.cursorrules` → Standard service structure

Legacy/extended references (keep for context)
- Phase guides: `docs/beamerparts_phase1_guide.md`, `docs/beamerparts_phase2_guide.md`, `docs/beamerparts_phase3_guide.md` (superseded by guides above)
- Dependencies and bootstrap guides
- Project structure (legacy, see updated structure section at top): `docs/beamerparts_project_structure.md`

Run/build quick links
- Start infra/services: `scripts/dev-start.sh`
- Build all: `scripts/dev/clean-build.sh`
- Run tests: `scripts/dev/run-tests.sh`
- Health: `scripts/dev/check-status.sh`
