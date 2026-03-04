# QuickStack POS Interface System

## Intent
*   **Who:** Cashiers, Shift Managers, Store Owners in a fast-paced Mexican restaurant environment.
*   **What:** Managing orders (comandas), catalog items, and End-of-Day cash register cuts quickly and without errors.
*   **Feel:** Professional, structured, hardware-like solidity, high-contrast, fast.

## Palette (Color World)
*   **Ticket White (`#ffffff`):** Pure background for active content areas (receipts, order details).
*   **Terminal Charcoal (`#18181b`):** Deep, grounded color for sidebars and primary actions.
*   **Salsa Red / Alert (`#ef4444`):** Urgent, culturally relevant accent for deletions, cancellations. 
*   **Agave Green / Success (`#10b981`):** Go ahead, paid, clear status. A fresh, distinct green.
*   **Amber / Warning (`#f59e0b`):** Pending or intermediate status.
*   **Parchment / Neutral Surface (`#f5f5f4`):** Soft off-white for the main App background, reducing eye strain.

## Depth Strategy
*   **Borders-Only / Flat Design:** No pillowy drop shadows. Use 1px solid light grey borders (`rgba(0,0,0,0.08)` or `rgba(0,0,0,0.15)`) for separation.

## Spacing
*   **Base Unit:** 4px micro-grid. `8px` standard gaps. `16px` or `24px` paddings for card interiors. denser vertical padding (`8px`) on tabular data.

## Signature Element: "The Comanda Edge"
*   Cards representing active orders or critical actions use a highly structured layout with a subtle left-edge accent or shadow indicating status:
    *   `comanda-edge` (Base edge)
    *   `comanda-edge-success` (Agave Green for PAID/Ready)
    *   `comanda-edge-warning` (Amber for PENDING/In Progress)
    *   `comanda-edge-error` (Salsa Red for CANCELLED/Unpaid)

## Key Component Patterns
*   **Cards:** `<Card variant="outlined">` is preferred, no elevation.
*   **Buttons:** MuiButton uses Terminal Charcoal primary. Flat, no elevation.
*   **Typography:** Tabular numbers (`.tabular-nums`) for all monetary values.
