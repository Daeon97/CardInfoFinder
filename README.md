# CardInfoFinder
Card Info Finder makes use of Google cloud vision API through Firebase Machine Learning and the CameraX library to scan and collect a card number.
Thereafter, it looks up the BIN information for the card through the Binlist API endpoint and displays the card brand, card type, bank and country for the card.
Volley was used to implement network request, Facebook's Shimmer was also used to implement loading indicator while the app is retrieving
BIN information from the Binlist API.

### Project parts
/app/src/main contains our concerns. In /app/src/main/java/com/engelsimmanuel/cardinfofinder there is the MainActivity.kt file.
/app/src/main/res also contains some important directories. Layout files are placed in /app/src/main/res/layout, strings are not hardcoded but are placed in
/app/src/main/res/values/strings.xml. /app/src/main/res/values/dimens.xml contains padding and margin values used throughout the app. Feel free to explore other
directories and sub directories.
