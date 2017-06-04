# SantaScramble
A graphical user interface (GUI) for the `filter` module of my
[BldSuite](https://github.com/suushiemaniac/BLDSuite).
Requires Java 8u41 or later.

## Usage
Usage is (hopefully) pretty straightforward. Just specify your desired scramble properties
and hit "OK" to start a search.

## Warning
This program uses a brute-force approach at finding scrambles. This means it basically spams TNoodle, and then checks
if your requirements are met for a given scramble. If not, the next TNoodle scramble is used.

Depending on your processing power, this can take a very, *very*, **very**, ***very*** long time.
I'm not saying your machine is bad, but Java is. Yet since TNoodle is written in Java, I have no other choice.

For a modern 4-core Intel processor, 6 scrambles/second is a good estimate. The more specific your wishes are,
the longer it will take until one of the brute-forced scrambles actually matches them.

If you even want *several* scrambles that match certain conditions, plan for ~5 to 10 minutes of search time
(and a lot of battery drain if you're on a laptop that's not plugged in!!)

## Configuration
Since everybody uses slightly different BLD systems, this program creates a configuration file
named `santaScramble.json` in your home directory (`C:\Users\JohnDoe` for Windows,
`/home/users/JohnDoe` on UNIX-based systems).

The basic configuration is set to [Speffz](https://www.speedsolving.com/wiki/index.php/Speffz) schemes and M2/OldPochmann buffers with white on top and green in front.
You can edit the configuration to your desires, but the program needs to be restarted for the changes to take effect.

The file looks something like this:
```json
{
	"buffer": {
		"C": 0,
		"E": 20,
		"T": 20,
		"W": 20,
		"X": 0
	},
	"lettering": {
		"C": "ABCDEFGHIJKLMNOPQRSTUVWX",
		"E": "ABCDEFGHIJKLMNOPQRSTUVWX",
		"T": "ABCDEFGHIJKLMNOPQRSTUVWX",
		"W": "ABCDEFGHIJKLMNOPQRSTUVWX",
		"X": "ABCDEFGHIJKLMNOPQRSTUVWX"
	},
	"orientation": {
		"front": 2,
		"top": 0
	}
}
```
where the letters denote pieces.
- `C` = Corner
- `E` = Edge
- `Wi` = Wing
- `XCe` = XCenter
- `TCe` = TCenter

Puzzles larger than 5x5 are currently not supported.

### Buffers
For every piece type in the buffer list, there is an integer which represents
the **0-based position of your buffer sticker in Speffz order**.

If your edge buffer is UF, you would replace the line `"E": 20` by `"E": 2`
because UF is the third sticker in Speffz labelling order

### Cube orientation
The solving orientation is encoded by specifying the top and front color in standard western/BOY schemes.
Face numbers again follow **0-based Speffz labelling order**
- White: `0`
- Orange: `1`
- Green: `2`
- Red: `3`
- Blue: `4`
- Yellow: `5`

If you hold your cube with orange on top and blue in front, you would write
`"top": 1` and `"front": 4` in the configuration file, respectively.

If you use fancy stickering schemes with gold, cyan and pink, just define the top/front colors you scramble with as white/green
and enumerate your scheme based on that assumption.

### Lettering
For each piece type, the lettering scheme is represented by a **joined string of sticker labels in Speffz order**.
No delimiter between letters!

## Help
If you need a hand at configuring or have any other feedback, shoot me an [email](mailto:suushiemaniac@gmail.com)