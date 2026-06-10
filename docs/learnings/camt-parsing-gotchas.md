# CAMT.053 Parsing Gotchas

While implementing and refining the CAMT.053 bank statement parser, several non-obvious issues were discovered.

## 1. Date Priority: Booking Date vs. Value Date

**Issue**: Some banks (e.g., ABN AMRO) provide a batch booking date for all transactions in a statement, which may differ from the actual transaction date.
- `BookgDt`: The date the bank officially processed the transaction.
- `ValDt`: The actual date the transaction occurred (Value Date).

**Resolution**: Always prioritize `ValDt` over `BookgDt`.
```kotlin
// In CamtParserService.kt
private fun findBookingDate(entry: ReportEntry2): LocalDate =
    entry.valDt?.dt ?: entry.valDt?.dtTm?.toLocalDate()
        ?: entry.bookgDt?.dt ?: entry.bookgDt?.dtTm?.toLocalDate()
        ?: LocalDate.now()
```

## 2. macOS Metadata in ZIP Archives

**Issue**: When users upload ZIP files created on macOS, the archive often contains hidden metadata files:
- `__MACOSX/` folder
- `._filename.xml` files

These files are binary "AppleDouble" metadata and not valid XML. Passing them to `MxCamt05300102.parse()` results in a `NullPointerException` or parsing error.

**Resolution**: Use a robust one-pass parsing approach that validates the XML content before processing.
```kotlin
// In BudgetImportService.kt
if (parseCamt053(xml) != null) {
    // process valid CAMT.053
} else {
    logger.warn("Skipping file as it's not a valid CAMT.053 XML")
}
```

## 3. Namespace Prefixes in Prowide Library

**Issue**: The Prowide library used for merging CAMT files generates XML with a `camt:` namespace prefix (e.g., `<camt:Document>`). Many external parsers and accounting tools (like Actual Budget) expect the standard schema to be the default namespace without prefixes.

**Resolution**: Perform manual string replacement on the generated XML message to remove prefixes.
```kotlin
val xmlString = merged.message()
    .replace("<camt:", "<")
    .replace("</camt:", "</")
    .replace("xmlns:camt=", "xmlns=")
```
