function option(name: string, type: string, description: string, defaultValue: string | number | boolean, allowedValues: (string|number)[] | undefined = undefined, availableSince: string = "") {
  return { name, type, description, defaultValue, availableSince, allowedValues };
}

export type GameOption = ReturnType<typeof option>
export type GameOptionCategory = {
  name: string;
  key: string;
  description: string;
  options: GameOption[];
}

export const options: GameOptionCategory[]  = [
  {
    name: "Sound",
    key: "sound_volumes",
    description: "Sound settings",
    options: [
      // TODO: Key override "sound"
      option("soundDevice", "string", "Sound device to be used", "", undefined, "1.18"),
      option("soundCategory_master", "number", "The volume of all sounds", 1.0), // range 0-1
      option("soundCategory_music", "number", "The volume of gameplay music", 1.0), // range 0-1
      option("soundCategory_record", "number", "The volume of music/sounds from Jukeboxes and Note Blocks", 1.0), // range 0-1
      option("soundCategory_weather", "number", "The volume of rain and thunder", 1.0), // range 0-1
      option("soundCategory_block", "number", "The volume of blocks", 1.0), // range 0-1
      option("soundCategory_hostile", "number", "The volume of hostile and neutral mobs", 1.0), // range 0-1
      option("soundCategory_neutral", "number", "The volume of passive mobs", 1.0), // range 0-1
      option("soundCategory_player", "number", "The volume of players", 1.0), // range 0-1
      option("soundCategory_ambient", "number", "The volume of cave sounds and fireworks", 1.0), // range 0-1
      option("soundCategory_voice", "number", "The volume of voices", 1.0), // range 0-1
    ]
  },
  {
    name: "Gameplay",
    key: "gameplay",
    description: "Gameplay settings",
    options: [
      // TODO: Key override "movement"
      option("autoJump", "boolean", "Whether auto-jump is enabled", true),
      // TODO: Key override "movement"
      option("toggleCrouch", "boolean", "Whether the sneak key must be pressed or held to activate sneaking", false),
      // TODO: Key override "movement"
      option("toggleSprint", "boolean", "Whether the sprint key must be pressed or held to activate sprinting", false),
      option("fov", "number", "How large the field of view is (floating-point). The in-game value is counted in degrees, however, the options.txt isn't. The value is converted into degrees with the following formula: degrees = 40 * value + 70", 0.0), // TODO: min and max; // TODO: add formula support to the UI
      option("attackIndicator", "number", "When hitting, how the attack indicator is shown on screen", 1), // range 0-2
      option("lang", "string", "Language to be used", "en_us"),
      option("hideBundleTutorial", "boolean", "Whether the player has seen the bundle tutorial hint when trying to use a bundle.", false),
      option("entityShadows", "boolean", "Whether to display entity shadows", true),
      option("forceUnicodeFont", "boolean", "Whether Unicode font should be used", false),
      option("touchscreen", "boolean", "Whether touchscreen controls are used", false),
    ]
  },
  {
    name: "Video",
    key: "video",
    description: "Video settings",
    options: [
      option("enableVsync", "boolean", "Whether v-sync (vertical synchronization) is enabled", true),
      option("bobView", "boolean", "Whether or not the camera bobs up and down as the player walks", true),
      option("gamma", "number", "Brightness", 0.5), // range 0-1
      option("renderDistance", "number", "The render distance in the chunk radius from the player", 12, undefined), // range 2-32
      option("simulationDistance", "number", "The simulation distance in the chunk radius from the player", 12, undefined, "1.18"), // range 5-32
      option("entityDistanceScaling", "number", "The maximum distance from the player that entities render", 1.0), // range 0.5-5.0
      option("guiScale", "number", "Size of interfaces", 0), // range 0 (Auto) or 1+ for size. Upper limit based on window resolution.
      option("particles", "number", "Amount of particles (such as rain, potion effects, etc.)", 0), // range 0-2 (All, Decreased, Minimal) // TODO: Selector?
      option("maxFps", "number", "The maximum framerate", 120), // range 10-260
      option("graphicsMode", "number", "Whether Fast (less detailed), Fancy (more detailed), or Fabulous! (most detailed) graphics are turned on", 1, [0, 1, 2]), // range 0-2
      option("ao", "boolean", "Smooth lighting", true),
      option("biomeBlendRadius", "number", "Radius for which biome blending should happen", 2, undefined, "1.13"), // range 0-7
      option("renderClouds", "string", "Whether to display clouds", "true", ["true", "false", "fast"]), // TODO: Fix this one
      option("mipmapLevels", "number", "Amount by which distant textures are smoothed", 4), // range 0-4
    ]
  },
  {
    name: "Chat",
    key: "chat",
    description: "Chat settings",
    options: [
      option("autoSuggestions", "boolean", "True if brigadier's command suggestion UI should always be shown, instead of just when pressing tab", true, undefined, "1.13"),
      option("chatColors", "boolean", "Whether colored chat is allowed", true),
      option("chatLinks", "boolean", "Whether links show as links or just text in the chat (true/false [links/plaintext])", true),
      option("chatLinksPrompt", "boolean", "Whether clicking on links in chat needs confirmation before opening them", true),
      option("chatVisibility", "number", "What is seen in chat", 0, [0, 1, 2]), // range 0-2
      option("chatOpacity", "number", "Opacity of the chat", 1.0), // range 0-1
      option("chatLineSpacing", "number", "Spacing between text in chat", 0.0), // range 0-1
      option("textBackgroundOpacity", "number", "Opacity of text background", 0.5), // range 0-1
      option("backgroundForChatOnly", "boolean", "Toggles if the background is only in chat or if it's everywhere", true),
      option("chatHeightFocused", "number", "How tall the chat span is", 1.0), // range 0-1
      option("chatDelay", "number", "How much delay there is between text", 0.0), // range 0-6
      option("chatHeightUnfocused", "number", "How tall the maximum chat span is, when the chat button is not pressed", 0.4375), // range 0-1
      option("chatScale", "number", "The scale/size of the text in the chat", 1.0), // range 0-1
      option("chatWidth", "number", "The span width of the chat", 1.0), // range 0-1
    ]
  },
  {
    name: "Accessibility",
    key: "accessibility",
    description: "Accessibility settings",
    options: [
      option("showSubtitles", "boolean", "If subtitles are shown", false),
      option("darkMojangStudiosBackground", "boolean", "Whether the Mojang Studios loading screen will appear monochrome", false, undefined, "1.17"),
      option("hideLightningFlashes", "boolean", "Hide lightning flashes (visual effect)", false, undefined, "1.18"),
      option("screenEffectScale", "number", "Distortion Effects (how intense the effects of Nausea and nether portals are)", 1.0), // range 0-1
      option("fovEffectScale", "number", "FOV Effects (how much the field of view changes when sprinting, having Speed or Slowness etc.)", 1.0), // range 0-1
      option("darknessEffectScale", "number", "Darkness Pulsing (how much the Darkness effect pulses)", 1.0, undefined, "1.19"), // range 0-1
      option("mainHand", "string", "Whether the main hand appears as left or right", "right"),
      option("narrator", "number", "Setting of the Narrator", 0, [0, 1, 2, 3], "1.12"),
      option("narratorHotKey", "boolean", "Pressing Ctrl + B will not open narrator if it is turned off.", true, undefined, "1.20.2"),
      option("tutorialStep", "string", "Next stage of tutorial hints to display", "movement", ["movement", "find_tree", "punch_tree", "open_inventory", "craft_planks", "none"], "1.12"),
    ]
  },
  {
    name: "System",
    key: "system",
    description: "System settings",
    options: [
      option("showAutosaveIndicator", "boolean", "Whether to show autosave indicator on the right-bottom of the screen", true, undefined, "1.18"),
      option("allowServerListing", "boolean", "Whether to allow player's ID to be shown in the player list shown on the multiplayer screen", true, undefined, "1.18"),
      option("hideServerAddress", "boolean", "Has no effect in modern versions", false),
      option("joinedFirstServer", "boolean", "Whether the player has joined a server before. If false, the Social Interactions tutorial hint will appear when joining a server.", false, undefined, "1.16.4"),
      option("skipMultiplayerWarning", "boolean", "Whether to skip the legal disclaimer when entering the multiplayer screen", false, undefined, "1.15.2"),
      option("skipRealms32bitWarning", "boolean", "Whether to skip the 32-bit environment warning when entering the Realms screen", false, undefined, "1.18.2"),
      option("realmsNotifications", "boolean", "Whether Realms invites are alerted on the main menu", true),
      option("snooperEnabled", "boolean", "Whether snooper is enabled", true, undefined, "1.19"),
      option("useNativeTransport", "boolean", "Whether to use a Netty EpollSocketChannel for connections to servers instead of a NioSocketChannel (only applies if EPOLL is available on the user's system)", true),
      option("prioritizeChunkUpdates", "number", "Chunk section update strategy", 0, [0, 1, 2], "1.18"), // range 0-2
      option("hideMatchedNames", "boolean", "Some servers send chat messages in non-standard formats. With this option on, the game will attempt to apply chat hiding anyway by matching the text in messages.", true, undefined, "1.16.4"),
      option("syncChunkWrites", "boolean", "Whether to open region files in synchronous mode", true, undefined, "1.16.4"),
    ]
  },
  {
    name: "Mouse",
    key: "mouse",
    description: "Mouse settings",
    options: [
      option("mouseSensitivity", "number", "How much a mouse movement changes the position of the camera", 0.5), // TODO: min and max
      option("discrete_mouse_scroll", "boolean", "Ignores scrolling set by operating system", false),
      option("invertYMouse", "boolean", "Whether mouse is inverted or not", false),
      option("mouseWheelSensitivity", "number", "Allows making the mouse wheel more sensitive", 1.0, undefined, "1.13"),
      option("rawMouseInput", "boolean", "Ignores acceleration set by the operating system", true),
    ]
  },
  {
    name: "Inputs",
    key: "inputs",
    description: "Input settings",
    options: [
      option("key_key.attack", "keycode", "Attack control", "key.mouse.left"),
      option("key_key.use", "keycode", "Use Item control", "key.mouse.right"),
      option("key_key.forward", "keycode", "Forward control", "key.keyboard.w"),
      option("key_key.left", "keycode", "Left control", "key.keyboard.a"),
      option("key_key.back", "keycode", "Back control", "key.keyboard.s"),
      option("key_key.right", "keycode", "Right control", "key.keyboard.d"),
      option("key_key.jump", "keycode", "Jump control", "key.keyboard.space"),
      option("key_key.sneak", "keycode", "Sneak control", "key.keyboard.left.shift"),
      option("key_key.sprint", "keycode", "Sprint control", "key.keyboard.left.control"),
      option("key_key.drop", "keycode", "Drop control", "key.keyboard.q"),
      option("key_key.inventory", "keycode", "Inventory control", "key.keyboard.e"),
      option("key_key.chat", "keycode", "Chat control", "key.keyboard.t"),
      option("key_key.playerlist", "keycode", "List Players control", "key.keyboard.tab"),
      option("key_key.pickItem", "keycode", "Pick Block control", "key.mouse.middle"),
      option("key_key.command", "keycode", "Command control", "key.keyboard.slash"),
      option("key_key.socialInteractions", "keycode", "Social Interaction control", "key.keyboard.p"),
      option("key_key.screenshot", "keycode", "Screenshot control", "key.keyboard.f2"),
      option("key_key.togglePerspective", "keycode", "Perspective control", "key.keyboard.f5"),
      option("key_key.smoothCamera", "keycode", "Mouse Smoothing control", "key.keyboard.unknown"),
      option("key_key.fullscreen", "keycode", "Fullscreen control", "key.keyboard.f11"),
      option("key_key.spectatorOutlines", "keycode", "Visibility of player outlines in Spectator Mode control", "key.keyboard.unknown"),
      option("key_key.swapOffhand", "keycode", "Swapping of items between both hands control", "key.keyboard.f"),
      option("key_key.saveToolbarActivator", "keycode", "Save current toolbar to a slot (in Creative Mode)", "key.keyboard.c", undefined, "1.12"),
      option("key_key.loadToolbarActivator", "keycode", "Load toolbar from a slot (in Creative Mode)", "key.keyboard.x", undefined, "1.12"),
      option("key_key.advancements", "keycode", "Open the Advancements screen", "key.keyboard.l", undefined, "1.12"),
      option("key_key.hotbar.1", "keycode", "Hotbar Slot 1 control", "key.keyboard.1"),
      option("key_key.hotbar.2", "keycode", "Hotbar Slot 2 control", "key.keyboard.2"),
      option("key_key.hotbar.3", "keycode", "Hotbar Slot 3 control", "key.keyboard.3"),
      option("key_key.hotbar.4", "keycode", "Hotbar Slot 4 control", "key.keyboard.4"),
      option("key_key.hotbar.5", "keycode", "Hotbar Slot 5 control", "key.keyboard.5"),
      option("key_key.hotbar.6", "keycode", "Hotbar Slot 6 control", "key.keyboard.6"),
      option("key_key.hotbar.7", "keycode", "Hotbar Slot 7 control", "key.keyboard.7"),
      option("key_key.hotbar.8", "keycode", "Hotbar Slot 8 control", "key.keyboard.8"),
      option("key_key.hotbar.9", "keycode", "Hotbar Slot 9 control", "key.keyboard.9"),
    ]
  },
  {
    name: "Models",
    key: "model",
    description: "Models settings",
    options: [
      option("modelPart_cape", "boolean", "Whether the cape is shown", true),
      option("modelPart_jacket", "boolean", "Whether the 'Jacket' skin layer is shown", true),
      option("modelPart_left_sleeve", "boolean", "Whether the 'Left Sleeve' skin layer is shown", true),
      option("modelPart_right_sleeve", "boolean", "Whether the 'Right Sleeve' skin layer is shown", true),
      option("modelPart_left_pants_leg", "boolean", "Whether the 'Left Pants Leg' skin layer is shown", true),
      option("modelPart_right_pants_leg", "boolean", "Whether the 'Right Pants Leg' skin layer is shown", true),
      option("modelPart_hat", "boolean", "Whether the 'Hat' skin layer is shown", true),
    ]
  },
  {
    name: "Debug",
    key: "debug",
    description: "Debug settings",
    options: [
      option("reducedDebugInfo", "boolean", "Whether to show reduced information on the Debug screen", false),
      option("advancedItemTooltips", "boolean", "Whether hovering over items in the inventory shows its ID and durability; toggled by pressing F3+H", false),
      option("pauseOnLostFocus", "boolean", "Whether switching out of Minecraft without pressing Esc or opening an in-game interface automatically pauses the game; toggled by pressing F3+P", true),
      option("glDebugVerbosity", "number", "LWJGL log info level (only on some machines)", 1, [0, 1, 2, 3, 4], "1.13"), //(none, HIGH, MEDIUM, LOW, NOTIFICATION)
    ]
  },
]
