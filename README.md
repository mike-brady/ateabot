**Current Version:** 0.1

Hello! I am ateabot. I help redditors learn what abbreviations stand for. You can get my help on Reddit by making a comment using the following command format.

# Command Format

u/ateabot **command** : **input text**

There are 4 parts to the command.

|1|2|3|4|
|:-|:-|:-|:-|
|u/ateabot|command|:|input text|

1. **Username Tag** - Required. Tag me by username. This is how I get notified of your request.
2. **Command** - Optional. The command you would like me to perform. If omitted, I will perform the **List** command by default. I can perform the following commands:
   * **List** - Respond with abbreviations found in the input text and list them with their expansions in table format.
   * **Explain** - Respond with the input text, putting the expansion of each abbreviation in parenthesis.
   * **Expand** - Respond with the input text, replacing the abbreviations with their expansions.
3. **Colon** - Only required if input text is provided.
4. **Input Text** - Optional. The text you want me to find abbreviations in. If omitted, I will use the parent comment or post that you replied to as the input text.

# Example Commands

|**Command**|**Action**|
|:-|:-|
|u/ateabot|I will **List** all abbreviations I find in the comment or post you are replied to.|
|u/ateabot explain|I will **Explain** the text from the comment or post you replied to.|
|u/ateabot expand|I will **Expand** the text from the comment or post you replied to.|
|u/ateabot : ITT|I will **List** all abbreviations I find in the input text \"ITT\" you provided.|
|u/ateabot explain this : This is an example sentence with an abbr.|I will **Explain** the input text \"This is an example sentence with an abbr.\" you provided.|

*Note the word \"this\" after \"explain\" in the last command. You can add any other words you want along side the command. As long as the command is somewhere between the username mention and the colon (if included)* *I will understand what you are asking!*