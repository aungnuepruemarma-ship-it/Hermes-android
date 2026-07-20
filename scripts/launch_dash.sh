#!/bin/bash
# Reference helper: how HermesLauncherService injects the session token.
#
# The Android app spawns `hermes dashboard` with HERMES_DASHBOARD_SESSION_TOKEN
# set in the child environment (see service/HermesLauncherService.kt). This makes
# the token deterministic so the app authenticates without scraping stdout.
#
# web_server.py (hermes-agent 0.16.0) line 139:
#   _SESSION_TOKEN = os.environ.get("HERMES_DASHBOARD_SESSION_TOKEN") or secrets.token_urlsafe(32)
#
# Replace the token value below with your own 32+ char random string.
export HERMES_DASHBOARD_SESSION_TOKEN="REPLACE_WITH_32PLUS_CHAR_RANDOM_TOKEN"
exec hermes dashboard --host 127.0.0.1 --port 9119 --no-open
