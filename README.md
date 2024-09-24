# tenkaichi-interaction-editor
Tenkaichi Interaction Editor is a tool that can read and write contents of voice_speaker.dat files.

Said files are found in the character's costume PAK files. They start at byte 84's offset and end at byte 88's offset.

Their contents handle Special Interactions in Budokai Tenkaichi 3.

Samples (of both the DAT and CSV files) have been included in both this repo and the release.

# How to Use
This tool requires Java SE 8 or higher, and is command-line only. Here are all the arguments it can take:

* -r --> Read voice_speaker.dat files and write their contents in CSV files with the same file names.
* -delpre [chara_ID] --> Disable a pre-battle Special Quote against a given character.
* -delpost [chara_ID] --> Disable a post-battle Special Quote against a given character.
* -delpreall --> Disable all pre-battle Special Quotes.
* -delpostall --> Disable all post-battle Special Quotes.
* -delall --> Disable all Special Quotes.
* -wpre [chara_ID] [quote_ID] [1st_or_2nd] --> Assign a pre-battle Special Quote against a given character.
* -wpost [chara_ID] [quote_ID] --> Assign a post-battle Special Quote against a given character.
* -wpreall [quote_ID] --> Assign a pre-battle Special Quote against all characters.
* -wpostall [quote_ID] --> Assign a post-battle Special Quote against all characters.

To clarify, the character ID ranges from 0 to 161, while the quote ID ranges from 0 to 93 (although exceeding 40 is not recommended).

To determine the quote ID, just take the ID (noted in `XX`) of the ADX you wanna play (chara_5`XX`_YY.adx) and subtract 6.