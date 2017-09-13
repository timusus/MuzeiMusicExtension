MuzeiMusicExtension
===================

A simple music extension for Roman Nurik's Muzei, which displays the artwork of the currently playing song in Muzei.

1. The music extension receives the Android 'meta-changed' intent
2. The artist name, album name and track name are retrieved from the intent
3. The extension searches for the artwork on the device (in the MediaStore and in the folder of the file itself)
4. If no artwork is found on the device, the artwork Uri is retrieved from Last.fm and passed to Muzei

Muzei Music Extension is on the [Play Store](https://play.google.com/store/apps/details?id=com.simplecity.muzei.music)

**Changelog:**

Current Version: 2.0

2.0
- Migrated to Kotlin
- Migrated from Volley to Retrofit
- Use Dagger for injecting network related modules
- Use regex when performing folder search, covers more artwork naming possibilities
- Fixed an issue where Muzei didn't have permission to access artwork Uris (using StreamProvider)
- Added SetupActivity, which requests for necessary permissions (write external storage)

1.0.6
- Fixed a crash occurring when toggling notification settings
- Fixed an issue where default artwork wasn't working
- Possible support for Apple music (untested)
- Various minor changes

1.0.5
- Remove 'api' folder & reference maven repo for API dependency.
- Use 'MediaController' on Android 5.0 for retrieving metadata from NL Service
- Update gradle build version
- Minor tidy up
- Add untested support for SoundCloud (assuming they use a fairly standard intent for media updates)
- Version bump

1.0.4
- Added support for new Spotify app
- Added support for jetAudio Music Player & jetAudio Music Player Plus (both untested)
- Various minor bug fixes
- Fix issue where artwork wouldn't download if a MediaStore album art entry was present in the MediaStore (even if that entry pointed to a file that didn't exist)
- Add more paths to search from on device (*.png locations, cover.* & album.*)

1.0.3
- Fixed ClassNotFoundException

1.0.2
- Added option to choose default artwork (via settings)
- Added support for Pandora (requires Android 4.4 & notification access)
- Any music player with lockscreen controls is now supported on Android 4.4, but requires notification access

1.0.1
 * Add support for many more music players, including:
- Spotify (requires Android 4.3 & notification access)
- Rdio
- PowerAMP
- PlayerPro
- Rocket Player
- Doubletwist
- Android Music Player

 * Untested but hopefully now working:
- MIUI
- Rhapsody
- Samsung Music Player
- HTC Music Player

 * Clicking track info now opens music player chooser

 * Some potential performance & bug fixes

1.0.0

 * Initial release

**Developed By**

 * Tim Malseed - <t.malseed@gmail.com>


**Credits**

 * [Roman Nurik](https://plus.google.com/+RomanNurik/) - Author of Muzei


**License**

    Copyright 2013 Tim Malseed

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
