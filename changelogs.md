# Changelog

All notable changes to Pear are listed here, newest first.

## [Unreleased]

### Changes
* Rework the README with a centered header, a short feature list and the build steps.
* Rename the app to Pear. The launcher label, the settings text and the media notification channel no longer say Flamingo.
* Show lyrics stored inside the song's tags when there is no TTML or LRC file next to it. LRC and TTML text in the lyrics tag of MP3, M4A, FLAC and Ogg files is read, and a file next to the song still wins.
* Change the app ID and package name to rotatsu.yos.music.player. Android treats this as a new app, so it installs next to the old one and starts with empty settings and library data.
* Keep the full-width album cover attached to the top of the album page when you pull the list past its start, instead of leaving a black gap above it.
* Use a translate icon for the lyrics translation button, both when it is off and when it is on.
* Move between the playback page and the lyrics with one connected transition. The cover shrinks into the small cover, the title and buttons slide up with it, and the backdrop fades. It works in both directions, with Static Full-Screen Album Art on or off.
* Hide the small drag handle on the playback page while Static Full-Screen Album Art is on. It still shows on the lyrics page.
* Add a Static Full-Screen Album Art switch in the interface settings. When it is on, the playback page shows the album cover edge to edge at the top and fades it into a dark gradient taken from the cover, and the album page opens with a full-width cover. It is off by default.
* Blur the lyric lines more the further they are from the sung one, up to 8 dp like Apple Music, and turn the lyric blur on by default on Android 12 and newer. Anyone who switched it off keeps it off.
* Rework the lyric view after BitChord. Lines around the sung one fade and blur in steps, lines settle on one curve, the scroll starts a little before the next line and the lines below follow one after another. Scrolling by hand flattens the dimming, a pressed line dips slightly, and an instrumental break shows three dots that fill in turn.
* Give held notes a soft glow that moves along the sung characters. Only words held for about a second or more glow, and a longer hold glows more. It needs Android 12 or newer.
* Dim every line that is not being sung, including lines without word timing, and fade a line back to grey once it has been sung.
* Show the cover blurred and still behind the lyrics, with a dark fade at the top and a scrim that gets stronger for light covers so the lyrics stay readable.
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
* Show Arabic lyrics with word timing in full while they play. The letters used to fall apart and land out of order, and the line was cut down to its last two letters. Each word now keeps its joined letters and the highlight sweeps from right to left.
* Lay out Arabic and Hebrew lyrics right to left. The lines sit against the right edge, the transliteration and translation line up with them, and the transliteration follows the words from right to left.
* Keep Arabic and Hebrew lyric lines against the right edge when they fit on one row. They used to sit at the left, and only a wrapped second row lined up on the right.
* Stop joined letters from showing lighter spots where they overlap in Arabic lyrics that are not the current line.
* Stop the media scan from wiping the saved playlist when audio permission is denied.
* Skip folders with no songs during the library scan instead of crashing on them.
* Skip the queue restore at startup when no saved queue exists, instead of failing silently.
* Save a missing artwork or file address as empty instead of the text "null", which restored as a broken address.
* Stop loading the FFmpeg renderer when the System codec setting is selected.
* Fill in the bitrate from the file when the player reports neither bitrate nor sampling rate.
* Stop the lyric and status-bar loops when the playback service ends, so a restart no longer runs them twice.
* Load the library as soon as audio access is granted, even if notification or Bluetooth access is denied.
