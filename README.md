# Console Commands #
This is an unofficial developer's console for Fractal Softworks' indie space combat game Starfarer. You can find the official forum thread for this mod here: http://fractalsoftworks.com/forum/index.php?topic=4106.0

### Instructions ###
This is installed just like any regular mod. Put it in the mods folder and make sure it's tagged in Starsector's launcher. Once in the game, you can summon the console with **control+backspace** and enter your commands. While the commands themselves aren't case sensitive, arguments are. For a full list of supported commands enter 'help' in the console. For more information on a specific command use 'help <command>'.

You can enter multiple commands by separating them with a semicolon. For example, "god;nocooldown;reveal;infiniteflux;infiniteammo" would run all of these combat cheats in a row. RunCode is an exception and can't be used with the command separator.

If you want to change the key that summons the console or the command separator, you can change these and several other console settings in _data/console/console_settings.json_.


### Current features ###
 * Input commands through a popup window (currently requires you to run the game in windowed or undecorated mode)
 * A large list of included commands, and the ability to add your own custom commands to the console
 * Write, compile and run code in-game using the Janino library
 * Doesn't require a new game, and can be safely untagged from a running game without issues
 * Should be compatible with all mods, even total conversions


### Troubleshooting ###
* The input popup never appears when I press the console button in combat:
  * Unfortunately, due to the way the combat pop-up is implemented you must either run the game windowed or in borderless windowed mode to use the console in battle (see instructions below). This will be fixed with the 3.0 release.


### Borderless window instructions ###
To enter commands in combat while playing fullscreen you will have to modify your settings.json to enable borderless mode, but you will only have to do this once per Starsector update.

To run your game borderless, open _starsector-core/data/config/settings.json_ and change _"undecoratedWindow"_ to _true_ and uncomment (remove the # in front of) the _"windowLocationX":0_ and _"windowLocationY":0_ lines. Next, untag fullscreen in the game launcher and make sure the game's resolution is set to your native desktop resolution. This should give you a fullscreen borderless window and allow the console popup to function correctly.

Note that you might have vsync issues if you are on Windows 7 and don't have the Aero theme enabled.


### Limitations ###
The console only functions properly in combat when running windowed or undecorated mode. This will be fixed in the upcoming 3.0 update.


### Upcoming/planned features ###
 * A proper custom UI rather than hacked together campaign dialogs and Java input popups.
 * More commands! If you have any requests let me know in the forum thread and I'll try to add them to the next version.