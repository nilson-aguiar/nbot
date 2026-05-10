# Design Spec: Actual Budget Integration & AI Transaction Mapping

## Context
Integrate `nbot` with Actual Budget to automate bank transaction management. The system will parse CAMT.053 XML exports, use AI to map bank payees to Actual Budget payees, identify internal transfers, and provide a web-based approval workflow.

## Proposed Changes

### 1. Infrastructure: Actual HTTP API Wrapper
`nbot` will communicate with Actual Budget via the `actual-http-api` sidecar.
- New Interface: `ActualBudgetApi` using Spring's `@HttpExchange`.
- New Service: `ActualBudgetService` to handle high-level operations (fetching accounts, pushing transactions).

### 2. Data Model (PostgreSQL)
#### `PayeeMapping`
- `id`: UUID (PK)
- `bank_pattern`: String (Regex or exact match for bank description/payee)
- `actual_payee_name`: String (Target payee in Actual Budget)
- `actual_payee_id`: String (UUID from Actual Budget)
- `is_internal_transfer`: Boolean
- `target_account_id`: String (If it's a transfer between internal accounts)
- `confidence_score`: Float (For AI-learned mappings)

#### `TransactionDraft`
- `id`: UUID (PK)
- `booking_date`: LocalDate
- `amount`: Long (Cents/integers as per Actual Budget standard)
- `currency`: String
- `bank_payee_name`: String
- `bank_description`: String
- `suggested_payee_id`: String
- `suggested_payee_name`: String
- `status`: Enum (PENDING, APPROVED, IGNORED, SYNCED)
- `export_file_id`: String (To group transactions from the same upload)

### 3. Core Services

#### `CamtParserService`
- Parses ISO 20022 CAMT.053 XML files.
- Extracts `Stmt/Ntry` elements into `TransactionDraft` objects.

#### `MappingEngineService`
- **Phase 1 (Deterministic):** Exact/Regex matching against `PayeeMapping` table.
- **Phase 2 (AI):** If no match, use Spring AI (Gemini) to analyze the transaction and suggest a payee.
- **Phase 3 (Transfer Identification):** AI detects transfers between the accounts listed in the user's prompt (Main, Joint, Savings accounts).

#### `LearningService`
- Bulk process historical CAMT files.
- Fetch historical transactions from Actual Budget.
- Use AI to correlate bank records with existing Actual Budget payees and populate `PayeeMapping`.

### 4. Presentation Layer

#### Web Dashboard (HTMX + Thymeleaf)
- **New Tab:** "Budget"
- **Features:** 
  - File upload (drag & drop or button).
  - Table of `PENDING` transactions.
  - Inline editing for Payee/Category.
  - "Sync to Actual" button (HTMX request).

#### Telegram Integration
- Handle document uploads.
- Detect `.xml` (CAMT.053) files.
- Process and reply with a link to the dashboard for review.

### 5. AI Prompt Strategy
The AI will be provided with:
1.  A list of known "Clean" Payees from Actual Budget.
2.  The list of your specific Bank Accounts to identify internal transfers.
3.  The raw bank transaction details.
4.  Instruction to return a structured JSON response with `payee_id` or `is_transfer: true`.

## Testing Strategy
- **Unit Tests:** 
  - `CamtParserServiceTest` with sample XML files.
  - `MappingEngineServiceTest` mocking the AI and DB.
- **Integration Tests:**
  - `ActualBudgetApiIT` (using a mock server or local instance).
- **Manual Verification:**
  - Upload a real CAMT.053 file and verify the "Drafts" table on the dashboard.

## Success Criteria
- Successful parsing of CAMT.053 files.
- Accurate identification of internal transfers (ignoring them from balance sum).
- AI suggests payees for 80%+ of unknown bank descriptions.
- Approved transactions appear correctly in Actual Budget.
