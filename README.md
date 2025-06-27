# Roster Capture

Roster Capture helps teachers of large classes learn students' names and other important
information. Teachers specify what information they want to collect and then pass the phone
around the classroom, letting each student take a selfie and provide relevant information about
themselves (such as their preferred name and how it is pronounced).

The collected information is stored in the popular flashcard app,
[AnkiDroid](https://play.google.com/store/apps/details?id=com.ichi2.anki), where teachers can
study the information and quiz themselves. They can also sync the data from AnkiDroid to
to [AnkiWeb](https://ankiweb.net/about), so it can be accessed online or through Anki's
desktop or iOS apps. All Anki apps are free and open source.

[![Configure Student Fields](images/configure-student-fields-thumbnail.png)](images/configure-student-fields.png)
[![Select Course](images/select-course-thumbnail.png)](images/select-course.png)

[![Student Information 1](images/student-information1-thumbnail.png)](images/student-information1.png)
[![Student Information 2](images/student-information2-thumbnail.png)](images/student-information2.png)

## FAQ
### What information can be collected?

These are the required fields:
* First name
* Last name
* Selfie

The teacher can decide whether to collect the following types of information and whether
they should be required or optional:
* ID
* Preferred name
* Audio recording of student's name
* Pronouns
   * She/her/hers
   * He/him/his
   * They/them/theirs
   * Other (specified by user)

The prompt for any field can be changed. For example, I use "NUID" [Northeastern ID]
for ID information for my students.

### Is it safe to give students my phone?

The app uses [Android pinning](https://support.google.com/android/answer/9455138?hl=en)
so students can't leave the Add Student screen.

Students may be able to view incoming notifications. You can protect yourself by:
* [Disabling lock screen notifications](https://support.google.com/android/answer/9079661)
* [Enabling Do Not Disturb mode](https://support.google.com/android/answer/9069335)

For more security, use a burner phone or [Guest Mode](https://support.google.com/pixelphone/answer/6115141).

### What permissions are required?

* CAMERA, so students can take selfies
* RECORD_AUDIO, so students can record their names
* USE_BIOMETRIC, so students can't do anything but add their information

### What version of Android is required?

Android 14 (API 34)

### What if I have questions or suggestions?

Create an issue, or otherwise contact me. I'd be happy to hear from you.

## Credits

I got the idea of passing a phone around the classroom to get selfies from
[Name That Student](http://www.alexandramarin.ca/namethatstudent.html)
by [Alexandra Marin](http://www.alexandramarin.ca/).

This app uses AnkiDroid's
[Instant-Add API](https://github.com/ankidroid/Anki-Android/wiki/AnkiDroid-API).

### Image sources
The app icons were created by Aisha Asgha.

* [Pictogrammers](https://pictogrammers.com/) ([Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0))
  * camera_account.xml
  * camera_retake_outline.xml
  * lock.xml
  * microphone_outline.xml
  * stop_circle_outline.xml
* Material Design icons by Google ([Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0))
  * ic_add_24.xml
  * ic_edit_24.xml
  * is_settings_24.xml
  * outline_app_blocking_24.xml
  * outline_error_24.xml
* Claude.ai
  * dashed_border.xml
  * solid_border.xml