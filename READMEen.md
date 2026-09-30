# Search Stronghold

The mod is made by DeepSeek.   [Prompt](https://github.com/yinlin12121/search-stronghold/edit/main/tishici.txt)

A **pure client-side** Fabric mod: throw two eyes of ender and automatically calculate the coordinates of the stronghold.

- Minecraft: **26.1.2/26.2/26.3** (Mojang official mappings, no obfuscation)
- Fabric Loader: 0.19.5+
- Java: 25
- **After installing the mod, enter "@language English" in the chat bar to switch the mod to English.(Version v1.0 does not have this feature.)**
- 
## Usage

1. Enter `@start` in the chat bar (this message is only intercepted locally and **will not be sent to the server**).
2. The chat displays `Throw the first eye of ender`; at this point, throw an eye of ender.
3. The chat displays `Change position and throw the second eye of ender`; walk at least 32 blocks away, then throw another one.
4. After the calculation is complete, the chat outputs:

```
Calculated
Eye of Ender 1   z=0.3x+125.4
Eye of Ender 2   z=-0.9x+487.1
Stronghold coordinates (1234, -567)
Stronghold coordinates in the nether (x,z)
```

After the calculation ends, the mod automatically returns to the dormant state and clears all data; during the calculation, you can enter `@stop` at any time to forcibly end it.

## Language switching

The module uses Chinese by default. You can switch the prompt language by entering the following instructions in the chat bar (this message is also only intercepted locally and * * will not be sent to the server * *):

| Instruction | Prompt after switching |
| --- | --- |
| `@language Chinese` | 语言已切换为中文 |
| `@language English` | Language has been switched to English. |

Language names are case-insensitive, and ` @ languageenglish` and ` @ languagechinese` are acceptable. Switching languages only affects the newly generated prompts, and the output prompts will not be rewritten; Calculation status and recorded coordinates are not affected.

As long as the message appears to be a ` @ language` instruction, it will be intercepted locally, and * * will not be sent to the server * *: If the language name is misspelled (for example, `@ languageFrench`) or the language name is not written, the message will be silently discarded, and the language will remain unchanged without any prompt. The text like ` @ languageenglish` without spaces is not an instruction, and it will be sent as normal chat.

After switching to English, all the prompts of the module are as follows:

| Situation | Prompt |
| --- | --- |
| Enter `@ start`| Throw the first eye of ender |
| The first record is completed | Change position and throw the second eye of ender |
| There are other moving eye of the last shadow in the radius of 16 squares | Failed to get coordinates; there are other eyes of ender nearby |
| The eye of the last shadow flies down | The stronghold is already nearby |
| Two throwing positions are less than 32 squares | Need to go farther away (beyond 32 blocks) |
| Two straight lines are parallel, or the intersection distance exceeds 20000 grids | Calculation failed; the stronghold is too far away |
| Other circumstances where coordinates cannot be obtained/calculated | Calculation failed |
| Enter ` @stop` | End calculation |

The output of calculation results in English mode is:

```
Calculated
Eye of Ender 1 z=0.3x+125.4
Eye of Ender 2 z=-0.9x+487.1
Stronghold coordinates (1234, -567)
Stronghold coordinates in the nether (154, -71)

## How It Works

After an eye of ender is thrown, it drifts horizontally toward the nearest stronghold, and its trajectory in the xz plane is a line segment. Record the `(x, z)` coordinates of the same eye of ender at two different times to obtain the linear function expression `z = kx + b` for the line on which that eye of ender lies. The player moves to another position and throws another one; the intersection of the two lines is the stronghold coordinates.

## States and Messages

The mod has only two states: **dormant** (does not collect any coordinates or participate in any game behavior) and **calculating** (after entering `@start`). The following situations produce messages in the chat:

| Situation | Message | Whether to exit the calculation state |
| --- | --- | --- |
| Entering `@start` | Throw the first eye of ender | No |
| First eye recorded | Change position and throw the second eye of ender | No |
| There is another moving eye of ender within a 16-block radius | Failed to get coordinates; there are other eyes of ender nearby | No (will prompt again after the interference disappears) |
| The eye of ender flies downward (y decreases, meaning you are already at the stronghold) | The stronghold is already nearby | No |
| The two throw positions are less than 32 blocks apart | Need to go farther away (beyond 32 blocks) | No (continues automatically after you move away) |
| The two lines are parallel, or the intersection is more than 20,000 blocks away | Calculation failed; the stronghold is too far away | Yes |
| Other cases where coordinates cannot be obtained/calculation cannot be performed | Calculation failed | Yes |
| Entering `@stop` | End calculation | Yes |
| Leaving the world/quitting the game | (no message) | Yes, and no data is saved |

Entering `@start` again while in the calculation state, or entering `@stop` while dormant, is ignored with no message.

## Accuracy

- Eye of ender coordinates are recorded at full `double` precision; internal calculations do not round.
- On output, the two lines are kept to 1 decimal place, and the intersection coordinates are rounded to integers.
- It verifies whether the sample points fall on the same line to avoid mixing in data from other eyes of ender.

## Multiplayer

Chat input is cancelled at the `HEAD` of `ChatScreen.handleChatInput`, so no chat packet is generated; all messages are displayed locally through `ChatHud.addClientSystemMessage`. The mod does not register any network channels, items, or blocks, so it can be used directly on multiplayer servers.

## Main Files

```
src/client/java/com/example/client/
├── SearchStrongholdClient.java        Client entry point
├── StrongholdSearcher.java            State machine, intersection calculation, chat messages
├── EyeTracker.java                    Coordinate sampling and line fitting for a single eye of ender
├── Trajectory.java                    Data and formatting for the line z = kx + b
└── mixin/
    ├── ChatScreenMixin.java           Intercepts @start / @stop (does not send to server)
    └── ClientLevelMixin.java          Per-client-tick detection + clears data when leaving the world
```
