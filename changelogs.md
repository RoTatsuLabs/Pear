# Changelog

All notable changes to Pear are listed here, newest first.

## [Unreleased]

### Changes
* Rework the lyric view after BitChord. Lines around the sung one fade and blur in steps, lines settle on one curve, the scroll starts a little before the next line and the lines below follow one after another. Scrolling by hand flattens the dimming, a pressed line dips slightly, and an instrumental break shows three dots that fill in turn.
* Give held notes a soft glow that moves along the sung characters. Only words held for about a second or more glow, and a longer hold glows more. It needs Android 12 or newer.
* Dim every line that is not being sung, including lines without word timing, and fade a line back to grey once it has been sung.
* Show the cover blurred and still behind the lyrics, under a dark scrim that gets stronger for light covers so the lyrics stay readable.
* Show a transliteration in Latin letters above the lyrics when the song has one. A TTML file can bring its own, and other lyrics in Chinese, Japanese, Korean, Cyrillic, Greek and more get one automatically. Each syllable sits above its own character, also when the line wraps, and lights up from left to right with that character.
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
