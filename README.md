## MuzeiMusicExtension

A simple music extension for [Muzei](http://muzei.co/), which displays the artwork of the currently playing song in Muzei.

1. The music extension receives `MediaController Metadata/Playstate` changes (via the `NotificationListenerService`)
2. The artist name, album name and track name are retrieved from the `Metadata`
3. The extension retrieves artwork from the internet and passes it to Muzei.

Muzei Music Extension is on the [Play Store](https://play.google.com/store/apps/details?id=com.simplecity.muzei.music)

### Changelog:

Current Version: 2.1.0

#### 2.1.0
- Google have nerfed manifest declared broadcast receivers, which means we can no longer listen to `metachanged` events from various apps, since they're not targeted directly at Muzei. So, we now rely exclusively on the NotificationListenerService for metadata & playstate changes.
- Use Shuttle's artwork API, since Last.FM is unreliable. Since the API redirects to the url of the actual image, we no longer need to parse any JSON, so Retrofit/OkHttp are no longer required.
- Since Muzei performs its own caching, and Google prevent us from discovering artwork on disk, we now just fetch artwork exclusively from online sources. We no longer need access to the MediaStore.
- The option to revert to default artwork when music isn't playing has been removed. Since artwork providers could be local or remote, and it's difficult to determine the actual play state, we now just display the most recent artwork.

#### 2.0.1
- Crash fixes

#### 2.0
- Migrated to `Kotlin`
- Migrated from `Volley` to `Retrofit`
- Use `Dagger `for injecting network related modules
- Use regex when performing folder search, covers more artwork naming possibilities
- Fixed an issue where Muzei didn't have permission to access artwork `Uris` (using `StreamProvider`)
- Added `SetupActivity`, which requests for necessary permissions (write external storage)
- Bump minSDK to 21 (Lollipop)
- Added onboarding screen for requesting permissions
- Added a `Track` model to encapsulate track data
- Don’t attempt to store non-local artwork on disk or in the `MediaStore`. Just pass the url to Muzei and let it do its own caching.
- Update to `AppCompat` & use `SwitchPreference` in `Settings` screen
- Disable custom `BroadcastReceiver` if `NotificationListener` is running - no need for both, and `NotificationListener` is going to be much more reliable.
- Remove any sort of gleaning of track information from the notification in the `NotificationListenerService`, and instead just look at the `MediaController Metadata`

#### 1.0.6
- Fixed a crash occurring when toggling notification settings
- Fixed an issue where default artwork wasn't working
- Possible support for Apple music (untested)
- Various minor changes

#### 1.0.5
- Remove 'api' folder & reference maven repo for API dependency.
- Use 'MediaController' on Android 5.0 for retrieving metadata from NL Service
- Update gradle build version
- Minor tidy up
- Add untested support for SoundCloud (assuming they use a fairly standard intent for media updates)
- Version bump

#### 1.0.4
- Added support for new Spotify app
- Added support for jetAudio Music Player & jetAudio Music Player Plus (both untested)
- Various minor bug fixes
- Fix issue where artwork wouldn't download if a MediaStore album art entry was present in the MediaStore (even if that entry pointed to a file that didn't exist)
- Add more paths to search from on device (*.png locations, cover.* & album.*)

#### 1.0.3
- Fixed ClassNotFoundException

#### 1.0.2
- Added option to choose default artwork (via settings)
- Added support for Pandora (requires Android 4.4 & notification access)
- Any music player with lockscreen controls is now supported on Android 4.4, but requires notification access

#### 1.0.1
- Add support for many more music players, including:
- Spotify (requires Android 4.3 & notification access)
- Rdio
- PowerAMP
- PlayerPro
- Rocket Player
- Doubletwist
- Android Music Player
- Untested but hopefully now working:
- MIUI
- Rhapsody
- Samsung Music Player
- HTC Music Player
- Clicking track info now opens music player chooser
- Some potential performance & bug fixes

#### 1.0.0

 * Initial release

#### Developed By

 * Tim Malseed - <t.malseed@gmail.com>


#### Credits

 * [Roman Nurik](https://medium.com/@romannurik/) - Author of Muzei
 * [Ian Lake](https://medium.com/@ianhlake/) - Author of Muzei


#### License

    Copyright 2019 Tim Malseed

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
