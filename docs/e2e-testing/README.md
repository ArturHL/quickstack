# E2E Behavioral Testing

This directory contains documentation and reports related to End-to-End (E2E) behavioral testing of the QuickStack application.

## Purpose
The goal of this directory is to maintain a historical record of manual and automated smoke tests, ensuring that critical user flows (Registration, Login, Core Features) work as expected before major releases or after significant refactors.

## E2E Testing Protocol

To perform quality behavioral testing, follow this protocol:

1.  **Environment Preparation**
    *   Ensure a clean database state or use a dedicated testing tenant.
    *   Verify that `Vite` (frontend) and `Spring Boot` (backend) are running with the latest changes.
    *   Check that `.env` configurations match the testing environment (e.g., correct database URL, JWT secrets).

2.  **Core Flows to Verify**
    *   **Registration:** Create a new user with a unique email and strong password. Verify success message and database entry.
    *   **Authentication:** Logout and login with the new credentials. Verify session persistence.
    *   **User UI/UX:** Check for layout shifts, broken images, or unresponsive buttons (e.g., password strength meter displacing fields).
    *   **Error Handling:** Intentionally input invalid data (e.g., weak password, existing email) to verify error messages.

3.  **Documentation**
    *   Record the test session (video or screenshots).
    *   Log any temporary changes made to code or database (e.g., hardcoded IDs, seed migrations).
    *   Categorize findings into "Pass", "Fail", or "Blocked".

4.  **Reporting**
    *   Generate a Walkthrough report (like `smoke-test-report-YYYY-MM-DD.md`) summarizing the results.
    *   Include evidence (screenshots) and list code changes required to make the test pass.
