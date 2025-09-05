# Teams Plugin
## Table of Contents
[Summary](#Summary)  
[Installation](#Installation)  
[Commands](#Commands)  
[Configuration](#Configuration)  
[Permissions](#permissions)  

## Summary
A custom treasure hunting plugin for Paper 1.21+ that lets administrators place treasure blocks anywhere in the world. When a player right-clicks a treasure block, a predefined console command runs for that player. Each treasure can be claimed only once per player, and claims are synced across multiple servers using a shared MySQL database. Includes an in-game GUI for viewing and deleting treasures.
## Installation
1. Build the plugin
```
git clone https://github.com/extstudios/TreasureHunt.git
cd TreasureHunt
mvn clean package
```
The project is a Maven build targeting Java 21 and depends on paper-api:1.21.8-R0.1-SNAPSHOT (scope provided) with HikariCP 7.0.2 for connection pooling.
2. Drop into your server
- Place the built JAR in your server’s plugins/ folder.
- The plugin declares api-version: '1.21.8'.
3. Configure config.yml with your MySQL connection details.
4. Restart the server.

## Commands
All commands are under the base command /ms (alias: /mobsoccer). Usage and behavior below

 Command                                 | What it does                                                                 
-----------------------------------------|------------------------------------------------------------------------------
 /treasure create {id} {command}         | Starts block selection for creating a treasure with the given ID and console command (%player% is replaced with the player’s name on claim). Right-click a block to bind it.                                    
 /treasure delete {id}                   | Deletes the specified treasure from the database and removes it from all server caches.        
 /treasure completed {id}                | Lists all players who have claimed the specified treasure.
 /treasure list                          | Lists all treasures in chat.
 /treasure gui                           | Opens the GUI showing treasures with pagination and the option to delete them.
 /treasure reload                        | reloads db

## Configuration

### config.yml
```mysql:
host: localhost
port: 3306
database: minecraft
username: root
password: secret
useSSL: false
pool:
maximumPoolSize: 10
minimumIdle: 2
connectionTimeoutMs: 10000
idleTimeoutMs: 600000
maxLifetimeMs: 1800000

# Interval in seconds for automatically refreshing treasures from the database.
# Set to 0 to disable auto-refresh.
refresh-interval-seconds: 15
```
## Permissions
Declared in plugin.yml (all default to op). There’s also a wildcard node with children.

Permission                               | Description                                                                 
-----------------------------------------|------------------------------------------------------------------------------
 treasurehunt.commands                   | Base permission for using /treasure commands.                                    
 treasurehunt.create                     | Allows creating new treasures.       
 treasurehunt.delete                     | Allows deleting treasures.
 treasurehunt.completed                  | Allows viewing players who have claimed a treasure.
 treasurehunt.list                       | Allows listing treasures.
 treasurehunt.gui                        | Allows opening the GUI view.
treasurehunt.commands.*                  | Wildcard node including all the above.                                                                            |