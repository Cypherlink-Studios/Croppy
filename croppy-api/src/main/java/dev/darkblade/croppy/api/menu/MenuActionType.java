package dev.darkblade.croppy.api.menu;

/**
 * Types of executable operations triggered by menu button clicks.
 */
public enum MenuActionType {

    /**
     * Domain station operation (e.g., START_PROCESS, CANCEL_PROCESS).
     */
    STATION_ACTION,

    /**
     * Command executed by the server console.
     */
    CONSOLE_COMMAND,

    /**
     * Command executed as the clicking player.
     */
    PLAYER_COMMAND,

    /**
     * Navigation opening another registered menu definition.
     */
    OPEN_MENU,

    /**
     * Closes the active menu for the player.
     */
    CLOSE,

    /**
     * Plays an audio sound effect for the clicking player.
     */
    SOUND,

    /**
     * Sends a MiniMessage-formatted message or action bar to the player.
     */
    MESSAGE
}
