# Project-specific R8 rules live here.
# Library consumer rules cover Compose, Firebase, and AndroidX for the current app surface.

# Room opens generated database implementations by class name. Keep this app's
# database contract and generated implementation names stable under R8.
-keep class dev.nihildigit.focuswell.data.db.FocusWellDatabase { *; }
-keep class dev.nihildigit.focuswell.data.db.FocusWellDatabase_Impl { *; }

# Keep Room schema types and DAO signatures readable in release mapping and away
# from accidental constructor/member stripping. This is intentionally scoped to
# the local persistence package, not all domain classes.
-keep class dev.nihildigit.focuswell.data.db.*Entity { *; }
-keep class dev.nihildigit.focuswell.data.db.FocusWellDao { *; }
-keep class dev.nihildigit.focuswell.data.db.FocusWellDao_Impl { *; }
