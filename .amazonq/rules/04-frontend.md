# Frontend Rules

Use React + TypeScript.

Reuse existing components and styles. Keep components focused. Use typed API models.

Provide loading, error and empty states. Validate forms client-side, but never rely on client validation for security.

Use AG Grid for large/complex tables and server-side pagination/filtering/sorting for large datasets.

Do not load millions of records into the browser.

Financial totals must come from backend aggregate APIs; do not make the browser the source of truth for financial calculations.

Do not expose secrets in frontend code.
