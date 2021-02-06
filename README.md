# CardInfoFinder
Card Info Finder makes use of Google cloud vision API through Firebase Machine Learning and the CameraX library to scan and collect a card number.
Thereafter, it looks up the BIN information for the card through the Binlist API endpoint and displays the card brand, card type, bank and country for the card.
Volley was used to implement network request, Facebook's Shimmer was also used to implement loading indicator while the app is retrieving
BIN information from the Binlist API.

### Project parts
/app/src/main contains our concerns. In /app/src/main/java/com/engelsimmanuel/cardinfofinder there is the MainActivity.kt file which contains our app main logic. There is also the Constants.kt file
which contains constants used in the app and finally is the Commons file which constains a bunch of functions for UI decoration
/app/src/main/res also contains some important directories. Layout files are placed in /app/src/main/res/layout, strings are not hardcoded but are placed in
/app/src/main/res/values/strings.xml. /app/src/main/res/values/dimens.xml contains padding and margin values used throughout the app. Feel free to explore other
directories and sub directories.

### Built APK
Kindly check the /app/build/outputs/apk/debug directory as it contains the built apk, app-debug.apk for installation purposes. Note that the app only supports Android 5.0 and later devices
