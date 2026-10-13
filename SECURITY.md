# Security and privacy

This experimental, unmaintained source edition is an offline demonstration.
There is no hosted service, school login, remote command API or support promise.
Do not add account details to fixtures, screenshots, issues or contributions.

The browser calls only its in-process adapter; CSP blocks connections. The
Android manifest has no Internet permission and no HTTP client is distributed.
Tests guard network calls in representative browser/Python workflows. This is
not a comprehensive security audit, and arbitrary forks are outside this claim.

Notes, images and completion state use ordinary local storage/preferences. They
are not encrypted; browser/app removal clears them. Data belongs to the local
user. Do not treat the demonstration as a safe store for sensitive school records.

The legacy gateway, privileged build/deploy tools, credentials and live account
integration are excluded. No instructions for exposing them are provided.
