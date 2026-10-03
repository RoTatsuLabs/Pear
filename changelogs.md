# Changelog

All notable changes to Pear are listed here, newest first.

## [Unreleased]

### Changes
* Refresh the library on every launch by default. The setting can still turn it off.
* Speed up the song list by building the visible song list once instead of on every read.
* Stop printing the whole library and the saved queue to the log after each scan and save.
* Sort the song list faster by working out each song's sort key once instead of on every comparison.
* Identify rows in the song list by media ID instead of the whole song entry, and stop logging every row as it redraws.
* Only record the page list for the blurred title bar when the bar blur setting is on.

### Fixes
* Stop the media scan from wiping the saved playlist when audio permission is denied.
* Skip folders with no songs during the library scan instead of crashing on them.
* Skip the queue restore at startup when no saved queue exists, instead of failing silently.
* Save a missing artwork or file address as empty instead of the text "null", which restored as a broken address.
* Stop loading the FFmpeg renderer when the System codec setting is selected.
* Fill in the bitrate from the file when the player reports neither bitrate nor sampling rate.
* Stop the lyric and status-bar loops when the playback service ends, so a restart no longer runs them twice.
* Load the library as soon as audio access is granted, even if notification or Bluetooth access is denied.
