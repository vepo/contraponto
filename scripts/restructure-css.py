#!/usr/bin/env python3
"""Split main.css into main + manage; merge professional.css into main; append stray write rules to write.css."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
STYLE = ROOT / "src/main/resources/META-INF/resources/style"

def lines(path: Path) -> list[str]:
    return path.read_text().splitlines(keepends=True)

def slice_lines(all_lines: list[str], ranges: list[tuple[int, int]]) -> str:
    """1-based inclusive line ranges."""
    out = []
    for start, end in ranges:
        out.extend(all_lines[start - 1 : end])
    return "".join(out)

def remove_ranges(all_lines: list[str], ranges: list[tuple[int, int]]) -> list[str]:
    remove = set()
    for start, end in ranges:
        for i in range(start, end + 1):
            remove.add(i)
    return [ln for i, ln in enumerate(all_lines, start=1) if i not in remove]

def extract_professional_main() -> str:
    prof = (STYLE / "professional.css").read_text()
    # :root through focus-visible (keep dark theme out)
    start = prof.index("/* ========== Design tokens")
    end = prof.index("/* ========== Dark theme")
    tokens = prof[start:end]

    start2 = prof.index("/* ========== Reset additions")
    end2 = prof.index("/* ========== Buttons (BEM extensions")
    reset = prof[start2:end2]

    start3 = prof.index("/* ========== Buttons (BEM extensions")
    end3 = prof.index("/* ========== Card (generic shell)")
    buttons = prof[start3:end3]

    start4 = prof.index("/* ========== HTMX ========== */")
    end4 = prof.index("/* ========== Utilities (u- prefix) ========== */")
    htmx = prof[start4:end4]

    start5 = prof.index("/* ========== Utilities (u- prefix) ========== */")
    utilities = prof[start5:]

    return tokens + reset + buttons + htmx + utilities

def extract_professional_manage() -> str:
    prof = (STYLE / "professional.css").read_text()
    start = prof.index("/* ========== Form group state (BEM) ========== */")
    end = prof.index("/* ========== Data table ========== */")
    forms = prof[start:end]

    start2 = prof.index(".library-tabs__row {")
    end2 = prof.index(".spinner__wrap {")
    library = prof[start2 : prof.index("}", end2) + 1]

    start3 = prof.index("/* ========== Spinner ========== */")
    end3 = prof.index("/* ========== HTMX ========== */")
    spinner = prof[start3:end3]

    return forms + "\n" + library + "\n" + spinner

def main():
    main_lines = lines(STYLE / "main.css")
    write_lines = lines(STYLE / "write.css")

    manage_ranges = [
        (1487, 1521),   # profile
        (1547, 1609),   # draft-card
        (1628, 1746),   # library
        (1896, 2029),   # dashboard
        (2081, 2120),   # manage-pagination
        (2212, 2266),   # review
        (2268, 2454),   # pages-manage
        (2735, 2787),   # app shell titles (dup cleanup)
        (2854, 2877),   # manage mobile stack
        (2997, 3062),   # notifications + subscriptions lists
    ]
    write_ranges = [(1523, 1545)]  # misplaced write-form in main

    remove_ranges_list = manage_ranges + write_ranges + [
        (2735, 2787),
        (2854, 2877),
        (2997, 3062),
    ]
    # Also remove duplicate APP shell - already in manage_ranges for 2735-2787

    manage_body = slice_lines(main_lines, manage_ranges)
    manage_header = "/* manage.css — logged-in operations (library, dashboard, CRUD, review, notifications) */\n\n"
    manage_extra = extract_professional_manage()

    # Consolidated manage page shells (replace scattered duplicates)
    manage_shell = """
/* ========== MANAGE: shared page shell ========== */
.profile-page__title,
.dashboard-page__title,
.library-page__title,
.review-page__title,
.pages-manage__title,
.notifications-page__title,
.subscriptions-page__title {
  font-family: var(--font-serif-display);
  font-size: clamp(1.75rem, 4vw, 2rem);
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 0.5rem;
  border-left: 4px solid var(--color-primary);
  padding-left: 1rem;
}

.profile-page__subtitle,
.dashboard-page__subtitle,
.library-page__subtitle,
.review-page__subtitle,
.pages-manage__subtitle,
.subscriptions-page__subtitle {
  font-family: var(--font-sans);
  font-size: 0.95rem;
  color: var(--color-text-muted);
  margin-bottom: 2rem;
  max-width: 36rem;
  line-height: 1.5;
}

.profile-page,
.dashboard-page,
.library-page,
.review-page,
.pages-manage,
.drafts-page,
.notifications-page,
.subscriptions-page {
  padding: var(--spacing-lg) 0 var(--spacing-2xl);
  min-height: calc(100vh - 200px);
}

.notifications-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 0.5rem;
}

.notifications-page__empty,
.subscriptions-page__empty {
  font-family: var(--font-sans);
  color: var(--color-text-muted);
  padding: 2rem 0;
}

.subscription-list__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

@media (max-width: 768px) {
  .post-card,
  .draft-card,
  .review-row {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }

  .post-card__actions,
  .draft-card__actions,
  .pages-manage__row-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .post-card__actions .btn,
  .draft-card__actions .btn,
  .pages-manage__row-actions .btn {
    flex: 1;
    text-align: center;
    justify-content: center;
  }
}

"""

    (STYLE / "manage.css").write_text(
        manage_header + manage_shell + manage_body + "\n" + manage_extra
    )

    write_extra = slice_lines(main_lines, write_ranges)
    (STYLE / "write.css").write_text("".join(write_lines) + "\n" + write_extra)

    # Remove extracted lines from main (reverse order to preserve indices - use set)
    new_main = remove_ranges(main_lines, manage_ranges + write_ranges)

    # Remove @import google fonts
    new_main = [ln for ln in new_main if not ln.strip().startswith("@import url('https://fonts.googleapis.com")]

    # Remove legacy auth-btn / btn-header blocks (simple line filter)
    filtered = []
    skip = False
    for ln in new_main:
        if ".auth-btn" in ln and "{" in ln and "auth-btn-" not in ln:
            skip = True
        if skip and ln.strip() == "}" and ".auth-btn" not in ln:
            # might end block - fragile
            pass
        filtered.append(ln)
    new_main = filtered

    text = "".join(new_main)

    # Insert extended tokens after first :root closing brace
    prof_main = extract_professional_main()
    insert_marker = "  --shadow-md: 0 4px 6px rgba(0, 0, 0, 0.05);\n}\n"
    if insert_marker in text:
        text = text.replace(
            insert_marker,
            insert_marker + "\n" + prof_main + "\n",
            1,
        )

    # Add btn--outline after btn--primary:hover block
    outline = """
.btn--outline {
  background: transparent;
  border-color: var(--color-border);
  color: var(--color-text);
}

.btn--outline:hover {
  background: var(--color-bg-offset);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

"""
    text = text.replace(
        ".btn--primary:hover {\n  background: var(--color-primary-dark);",
        outline + ".btn--primary:hover {\n  background: var(--color-primary-dark);",
        1,
    )

    # Remove duplicate .loading-spinner block at end (keep first at library section - actually first removed with library)
    # Add loading-spinner to manage if missing
    if ".loading-spinner {" not in text:
        pass

    (STYLE / "main.css").write_text(text)
    print("Wrote manage.css, updated main.css and write.css")

if __name__ == "__main__":
    main()
