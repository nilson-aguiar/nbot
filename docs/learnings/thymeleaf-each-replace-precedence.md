# Thymeleaf: `th:each` + `th:replace` on the same element silently doesn't iterate

## Symptom

```html
<tr th:each="draft : ${drafts}" th:replace="~{fragments/budget :: draftRow}"></tr>
```

With a non-empty `drafts` list, rendering blew up with:

```
EL1007E: Property or field 'id' cannot be found on null
```

The fragment's `${draft.id}` evaluated against a `null` `draft`, even though we were apparently iterating.

## Root cause: attribute precedence

Thymeleaf processors run in a fixed numeric order. **Lower number = runs first.**

| Attribute | Precedence |
|---|---|
| `th:insert` / `th:replace` | **100** |
| `th:each` | **200** |
| `th:if` | 300 |

When `th:each` and `th:replace` are on the same element, `th:replace` wins. The element is replaced by the fragment **once**, with no iteration ever happening — so `draft` is never set, and the fragment NPEs.

The pattern *looks* like it should iterate, but it silently doesn't.

## Why tests didn't catch it

`DashboardControllerTest` stubbed:

```kotlin
every { transactionDraftRepository.findByStatus(TransactionStatus.PENDING) } returns emptyList()
```

Iterating zero times = zero fragment invocations = zero NPEs. The bug only shows up with real data.

The view tests also use a mocked `TemplateEngine` and only assert the view name, so templates are never actually rendered in unit tests.

## The fix

Separate the two responsibilities by putting `th:each` on a wrapper that has nothing else to do:

```html
<th:block th:each="draft : ${drafts}">
    <tr th:replace="~{fragments/draft-row :: draftRow}"></tr>
</th:block>
```

1. `th:block th:each` iterates, creating one `<tr>` per draft, each with `draft` in scope.
2. For each iterated `<tr>`, `th:replace` runs and pulls in the fragment, finding `draft` in the calling context.

`<th:block>` is a Thymeleaf-only element that emits nothing in the output — the perfect "do this for each item" wrapper.

Alternative form (also valid): pass the iteration variable explicitly via the fragment expression:

```html
<tr th:each="draft : ${drafts}" th:replace="~{fragments/draft-row :: draftRow(${draft})}"></tr>
```

This requires the fragment to declare a parameter: `th:fragment="draftRow(draft)"`.

## Related fix: put reusable fragments in their own file

The original code defined `draftRow` inline at the bottom of `budget.html`. A refactor wrapped it in `<th:block th:if="false">` to "hide" the standalone definition — but that introduced HTML parsing issues (stray `<tr>` outside a `<table>` is special-cased by the HTML5 parser).

Attempted workaround `<table th:remove="all">` failed because **`th:remove` runs after child attributes are processed**, so `${draft.id}` still evaluated against `null` during page render.

Final structure:

- `templates/fragments/draft-row.html` — standalone file, `<tr>` wrapped in a real `<table><tbody>` so the parser keeps it intact.
- The standalone file is never rendered as a full page; Thymeleaf only ever asks for the named fragment via `:: draftRow`, so the wrapper is invisible by construction.

No `th:remove` / `th:if="false"` hacks needed.

## Takeaways

1. **Don't put `th:each` and `th:replace`/`th:insert` on the same element.** Wrap with `<th:block th:each="...">` or pass the iteration variable explicitly via fragment parameters.
2. **Test view fragments with non-empty data.** Empty-list tests skip the entire iteration body and miss this class of bug.
3. **Thymeleaf precedence is numeric, lower-first.** When two attributes on one tag do the unexpected, check whether they're competing.
4. **Reusable fragments belong in their own files.** Avoids HTML structural issues (orphan `<tr>`/`<td>` outside tables), avoids hide-from-render hacks, and makes the fragment's purpose obvious.
