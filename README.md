# AdminToolbox
Effective Minecraft admin tools focused on minimizing access to cheat features, ensuring staff integrity and community trust.

## Commands 

### /admin
Toggles on/off admin mode. In admin mode your inventory is cleared and are put in spectator mode. When admin mode is toggled back off you will be teleported back where you were before you entered admin mode with all your items back.

### /admin [target player]
Same as /admin except you also teleport to the person you are trying to target/watch. Do /admin again to exit, again you will be teleported back to where you were before with your old inventory.

### /admin [x] [y] [z] [optional: world/nether]
Same as /admin except you also teleport to a given xyz location. Do /admin again to exit, again you will be teleported back to where you were before with your old inventory.

### /reveal or /show
When ran while in admin mode, this puts you in adventure mode with where you can't die, or interact with the world much. This simply allows you to reveal your presence to the player when confronting them.

### /back
Teleports you back to your last teleported position (in /admin mode)

3## /yell [player] [message]
Puts a large red message on a given players screen

### /freeze [player]
Stop a player from moving

## API

There is an AdminToolbox API. It can be used in other plugins like so:

```java
public class YourPlugin extends JavaPlugin {
    AdminToolboxApi adminToolboxApi;
    
    @Override
    public void onEnable() {
        ServicesManager services = getServer().getServicesManager();
        adminToolboxApi = services.load(AdminToolboxAPI.class);
        
        if(adminToolboxApi == null) {
            // handle cases where AdminToolbox is outdated, not installed, failed to load, etc.
        }
    }
}
```
