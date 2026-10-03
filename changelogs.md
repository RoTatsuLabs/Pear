# Changelog

All notable changes to Pear are listed here, newest first.

## [Unreleased]

### Changes
* Light up the lyric being sung with a soft glow that follows the highlight, holds while a word is held and fades out after it. A line that has been sung now fades from white back to grey instead of staying white.
* Show a transliteration in Latin letters above the lyrics when the song has one. A TTML file can bring its own, and other lyrics in Chinese, Japanese, Korean, Cyrillic, Greek and more get one automatically. Word timed lines show it above each word and light it up with the word.
* Show the credits from a TTML file, such as the songwriters, below the last lyric line.
* Read lyrics from a TTML file next to the song, in addition to LRC. Word timing, duet sides and translations come from the file, and songwriter credits are kept for the lyric view.
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
