MuzeiMusicExtension
===================

A simple music extension for Roman Nurik's Muzei, which displays the artwork of the currently playing song in Muzei.

1. The music extension receives the Android 'meta-changed' intent
2. The artist name, album name and track name are retrieved from the intent
3. The extension searches for the artwork on the device (in the MediaStore and in the folder of the file itself)
4. If no artwork is found on the device, the artwork Uri is retrieved from Last.fm and passed to Muzei

Muzei Music Extension is on the [Play Store](https://play.google.com/store/apps/details?id=com.simplecity.muzei.music)

**Changelog:**

Current Version: 1.0.3

1.0.3
- Fix ClassNotFoundException

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
