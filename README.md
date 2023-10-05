# csgroupchat
A simple Java-based group chat program with support for multiplayer text-based games, private messaging, and server-side logging.<br>
As of initial version, this is a simple NetBeans project folder that allows for demonstration inside of an IDE or using the <i>java</i> command locally.  Future versions will feature distributable standalone packages for easy commandline execution.<br>
# Server
As of initial version, this supports uncapped network socket connections on a single passed port address.  Client user messages and commands are logged locally on stdout.  All client user input is passed to the server for processing and execution, allowing for easy expansion of features and commands.  Future versions will support logging to file, local user input for server administration, as well as message encryption.
# Client
As of initial release, requires user to enter a username before being allowed to read and send messages.  Once a username has been given, a user can chat freely with other users that are also connected.  Future versions will support message encryption.
# Commands
This list will be expanded on as features are added:<br>
* /whisper client message -- This allows a client to send a private message to a specified username.
* AllClients -- Returns a list of all connected clients.  Will return a value of <i>null</i> for clients connected who have not yet chosen a username.
* Bye -- The client sends the message "Bye" to everyone and then disconnects.  This will be changed for future versions to use slash commands.
* /poker join -- Client joins the running game of texas hold 'em poker, starting an instance if it is not already running.
* /poker start nobets --Begins the game.  Since this version has no betting cycles, the game plays as if all players go all in, showing all hands and all 5 table cards.  The game will determine a winner and print a message to all players.
* /poker leave -- Client leaves the game. When all players have left, the game instance is shut down.
# Known Issues
* The hand evaluator for the poker game is not perfect, and will sometimes assign the winner to not the best hand.
* Input errors are poorly handled and need catching.
* Proper betting cycles for a proper game of poker causes server hang waiting for the next player to place a bet.  This is why nobets version is currently used. Probably needs additional threading.
* Username collisions are currently unhandled. Whisper messages may reach multiple clients and a disconnect command may propagate between all users with same name.
# TODO
* Add support for subgroups.
* Add support for server-side kicks and moderation.
* Add support for client-side logging.
* Possibly add support for Message class, allowing for message moderation, deletion, and edits that propagates between all connected clients.
