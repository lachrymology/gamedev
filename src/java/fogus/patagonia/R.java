package fogus.patagonia;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Event;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Rogue-like 4k game
 *
 * TODO general game play
 *   - Item effects
 *      * use item
 * TODO balance
 * TODO minimize size
 *   - Only do what's needed
 *
 * @author Matt Albrecht
 * @version Sep 2, 2012
 * @since Jun 21, 2012
 * 
 * http://java4k.com/index.php?action=games&method=view&gid=438#source
 */
public class R extends Applet implements Runnable {

    private int key = KEY_NOTHING;

    public void start() {
        new Thread(this).start();
    }

    public void run() {
        // Required for Mac
        while (! isActive()) {
            Thread.yield();
        }

        final BufferedImage image = new BufferedImage(RENDER_WIDTH, RENDER_HEIGHT, BufferedImage.TYPE_INT_RGB);
        final Graphics ogr = image.getGraphics();
        ogr.setFont(Font.decode(FONT_NAME));
        final FontMetrics metrics = ogr.getFontMetrics();
        final int charWidth = metrics.charWidth(DISPLAY_CHAR_PATH); // was 'm'
// DEBUG per-machine character display
/*
System.out.println("textlength="+TEXT_LENGTH);
System.out.println("Font details: " +
        "\n  Name: " + metrics.getFont().getFontName() +
        "\n  'm' width: " + charWidth + " (advance: " + metrics.getMaxAdvance() + ")" +
        "\n  height: " + metrics.getHeight() +
        "\n  leading: " + metrics.getLeading() +
        "\n  ascent: " + metrics.getAscent() + " (max " + metrics.getMaxAscent() + ")" +
        "\n  descent: " + metrics.getDescent() + " (max " + metrics.getMaxDescent() + ")" +
        "\n  max char bounds: " + metrics.getMaxCharBounds(ogr));
System.out.println("Glyph details:");
for (int i = 0; i < DISPLAY_MAP.length; i++) {
    System.out.println("  [" + DISPLAY_MAP[i] + "]: " +
            metrics.getStringBounds(DISPLAY_MAP, i, i+1, ogr)
            );
}
*/

        final int charactersPerLine = RENDER_WIDTH / charWidth;
        final int drawWidth = charactersPerLine * charWidth;
        final int guiLines = CHAR_LINES - 3;

        final char[] renderChar = new char[1];


        final Random random = new Random();

        // --------------------------------------------------------------------
        // GLOBAL CONSTANTS
        int x, y, pos1, pos2, pos3, ax, bx, cx, ay, by, cy, aa, bb, cc, dd, ee;


        final Color[] COLOR_MAP = new Color[COLOR_COUNT];
        COLOR_MAP[COLOR_IDX_WALL] = new Color(COLOR_VALUE_WALL);
        COLOR_MAP[COLOR_IDX_PATH] = new Color(COLOR_VALUE_PATH);
        COLOR_MAP[COLOR_IDX_ROOM] = new Color(COLOR_VALUE_ROOM);
        COLOR_MAP[COLOR_IDX_FEATURE] = new Color(COLOR_VALUE_FEATURE);
        COLOR_MAP[COLOR_IDX_ITEM] = new Color(COLOR_VALUE_ITEM);
        COLOR_MAP[COLOR_IDX_MONSTER] = new Color(COLOR_VALUE_MONSTER);
        COLOR_MAP[COLOR_IDX_PLAYER] = new Color(COLOR_VALUE_PLAYER);
        COLOR_MAP[COLOR_IDX_BACKGROUND] = new Color(COLOR_VALUE_BACKGROUND);


        int level = 0; // marker to indicate nothing has started yet.
        int highestGold = 0;
        int highestLevel = 0;
        StringBuffer message = new StringBuffer();

        startGame: while (true) {
            boolean gameStarted = false;

            StringBuffer status = new StringBuffer();
            a(status, TEXT_IDX_PRESS_ANY_KEY);


            // ================================================================
            // initialize game

            int displayState = DISPLAYSTATE_NEWGAME; // done for optimization on keyAction check



            // ----------------------------------------------------------------
            // setup the monster and item records - these are the constants per
            // game, which the per-instance uses to look up data.

            int[] gameRecords = new int[GAME_LENGTH];

            gameRecords[GAME_MONSTERREC_PLAYER_IDX_DISPLAY] = DISPLAY_CHAR_PLAYER;
            //gameRecords[GAME_MONSTERREC_PLAYER_IDX_COLOR_NAME] = don't care
            gameRecords[GAME_MONSTERREC_PLAYER_IDX_COLOR_IDX] = COLOR_IDX_PLAYER;
            gameRecords[GAME_MONSTERREC_PLAYER_IDX_NAME1] = TEXT_IDX_PLAYERNAME1;
            gameRecords[GAME_MONSTERREC_PLAYER_IDX_NAME2] = TEXT_IDX_PLAYERNAME2;
            //gameRecords[GAME_MONSTERREC_PLAYER_IDX_BASEPOWER] = 0; // don't care
            gameRecords[GAME_MONSTERREC_PLAYER_IDX_AI] = MONSTERREC_AI_PLAYER;

            // Global player records that don't fit in other fields
            int playerFood = PLAYER_STARTING_FOOD;
            int playerMoney = 0;
            int[] playerInventory = new int[PLAYER_INVENTORY_SIZE];

            // The level records should be reused.  Because the location
            // discovery is done through the map, this can be reused, and
            // extra items left over will be ignored.  This will also allow
            // for keeping the player records in the level records without
            // needing to worry about copying them into the new list.
            // BUT the items in the records will need to be cleared out
            // so that there's spaces for the "drop" command to add to the
            // level items.
            // At the start of a new game, this array is also used to store
            // temporary data.
            int[] levelRecords = new int[LEVEL_LENGTH];

            // Add monster records
            // These are relatively increasing in terms of power level.  On
            // each level, the current level is factored into which monsters
            // you may see.

            // First pass: generate by names; they all share a color.
            // After that, duplicate the monsters for different colors, but
            // with increasing power.  Use the item part of the array
            // as temporary storage for data

            // compute color order - shuffle the color indices.
            // Use the item rec space to hold the temporary color values.
            levelRecords[0] = TEXT_IDX_START_COLOR;
            for (aa = 1; aa < COLOR_NAME_COUNT; aa++) {
                bb = random.nextInt(aa + 1);
                levelRecords[aa] = levelRecords[bb];
                levelRecords[bb] = aa + TEXT_IDX_START_COLOR;
            }


            pos1 = GAME_MONSTERREC_FIRSTMONSTER_IDX;
            bb = 0; // name1 rotation
            cc = 0; // name2 rotation
            dd = DISPLAY_UPPERCASE; // display rotation
            for (aa = 0; aa < GAME_MONSTERREC_MONSTERTYPE_COUNT; aa++, pos1 += MONSTERREC_SIZE) {
                gameRecords[pos1 + MR_IDX_COLOR_NAME] = levelRecords[0]; // Fixed at 0, so that the below copy works right
                gameRecords[pos1 + MR_IDX_COLOR_IDX] = COLOR_IDX_MONSTER;
                gameRecords[pos1 + MR_IDX_NAME1] = bb + TEXT_IDX_START_MONSTER;
                gameRecords[pos1 + MR_IDX_NAME2] = TEXT_IDX_START_MONSTER +
                        random.nextInt(TEXT_MONSTER_NAME_COUNT_HALF) + cc;
                gameRecords[pos1 + MR_IDX_DISPLAY] =
                        TEXTBUFFER.charAt(TEXT_POS_MONSTER_NAME_CHARS + bb) + dd;
//System.out.println("Monster rec[" + aa + "] id = " + (int) gameRecords[pos1 + MR_IDX_DISPLAY] + " / " + (char) gameRecords[pos1 + MR_IDX_DISPLAY]);
                gameRecords[pos1 + MR_IDX_AI] = (random.nextInt(MONSTERREC_AI_RANDOMCHANCE) << MONSTERREC_AI_AGGRESSIVE_SHL)
                        | MONSTERREC_AI_SLEEPY_BIT;

                bb++;
                if (bb >= TEXT_MONSTER_NAME_COUNT) {
                    bb = 0;
                    cc = TEXT_MONSTER_NAME_COUNT_HALF;
                    dd = 0;
                }
            }

            // TODO this could be eliminated for space if necessary, but
            // the corresponding counts would need to be adjusted.
            for (aa = 1; aa < COLOR_COUNT; aa++) {
                pos2 = GAME_MONSTERREC_FIRSTMONSTER_IDX;
                // For each color, copy all the monsters defined above, and
                // give them a different color
                for (bb = 0; bb < GAME_MONSTERREC_MONSTERTYPE_COUNT; bb++, pos1 += MONSTERREC_SIZE, pos2 += MONSTERREC_SIZE) {
                    for (cc = 0; cc < MONSTERREC_SIZE; cc++) {
                        gameRecords[pos1 + cc] = gameRecords[pos2 + cc];
                    }
                    gameRecords[pos1 + MR_IDX_COLOR_NAME] = levelRecords[aa];
                }
            }


            // create the item records

            // compute color order - shuffle the color indices.
            // Use the item rec space to hold the temporary color values.
            // These correspond to the item effects.
            // Colors for items are never invisible
            levelRecords[0] = 0;
            for (aa = 1; aa < COLOR_NAME_COUNT; aa++) {
                bb = random.nextInt(aa + 1);
                levelRecords[aa] = levelRecords[bb];
                levelRecords[bb] = aa;
            }

            // item types - The first set of items
            pos1 = GAME_ITEMREC_IDX_MONEY;
            // money is a special type
            gameRecords[pos1 + IR_IDX_DISPLAY] = DISPLAY_CHAR_MONEY;
            //gameRecords[pos1 + IR_IDX_COLOR] = COLOR_IDX_WALL; // = 0
            //gameRecords[pos1 + IR_IDX_NAME] = TEXT_IDX_GOLD_NAME; // doesn't matter
            //gameRecords[pos1 + IR_IDX_TYPE] = ITEMTYPE_MONEY; // doesn't matter
            //gameRecords[pos1 + IR_IDX_IDENTIFIED] = 0; // meaningless
            //gameRecords[pos1 + IR_IDX_ACTION] = 0; // meaningless
            gameRecords[pos1 + IR_IDX_CHANCE] = ITEMCHANCE_GOLD; // 1/5 chance to find it
            pos1 += ITEMREC_SIZE;

            // next chance to find an item - we roll the dice once to see what
            // is found, and if the number is > last one and <= this one, then
            // this is the object we're looking for.
            ee = ITEMCHANCE_GOLD;

            for (aa = 0; aa < ITEMTYPE_COUNT; aa++) {
                for (cc = 0; cc < TEXT_ITEMTYPE_NAME_COUNT; cc++) {
                    for (bb = 0; bb < ITEMACTION_COUNT; bb++, pos1 += ITEMREC_SIZE) {
                        gameRecords[pos1 + IR_IDX_DISPLAY] = TEXTBUFFER.charAt(TEXT_POS_DISPLAY_ITEM_CHARS + aa);
                        gameRecords[pos1 + IR_IDX_COLOR] = levelRecords[bb];
                        gameRecords[pos1 + IR_IDX_NAME] = TEXT_IDX_START_ITEMTYPE +
                                (aa * TEXT_ITEMTYPE_NAME_COUNT) + cc;
                        gameRecords[pos1 + IR_IDX_TYPE] = aa;
                        gameRecords[pos1 + IR_IDX_IDENTIFIED] = 0;
                        gameRecords[pos1 + IR_IDX_ACTION] = bb;
                        ee += ITEMCHANCE_SPECIAL;
                        if (bb == ITEMACTION_FOOD) {
                            ee += ITEMCHANCE_ORDINARY_SPECIAL_DIFF;
                        }
                        gameRecords[pos1 + IR_IDX_CHANCE] = ee;
                    }
                }
            }


            // Add the player to the level records; it's always at the start.

            levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_RECORD] = GAME_MONSTERREC_PLAYER_IDX;
            levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_HEALTH] = PLAYER_STARTING_HEALTH;
            levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_ATTACK] = PLAYER_STARTING_ATTACK;
            levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_ARMOR] = PLAYER_STARTING_ARMOR;
            // These are set per-level
            // levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_X] = GAME_MONSTERREC_PLAYER_IDX;
            // levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_Y] = GAME_MONSTERREC_PLAYER_IDX;
            // levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_MAPPOS] = GAME_MONSTERREC_PLAYER_IDX;

            // These are set with actions
            // levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_ACTION] = ACTION_NONE;

            // These are set once
            // levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_STATE] = AI_STATE_PLAYER; // = 0
            // levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_RETREAT_HEALTH] = 0; // never AI the player to retreat


            startLevel: while (true) {
                // ============================================================
                // initialize level

                int windowX = 0;
                int windowY = 0;

                // Pointer to the item in the inventory the user selected
                // This is defined at the "level" level, because it must be
                // saved across keystrokes.
                int selectedInventoryIndex = PLAYER_INVENTORY_SIZE;

                // initialize the monster / item list space to just 0.
                for (aa = LEVEL_MONSTERINST_FIRSTMONSTER_IDX; aa < LEVEL_ITEMINST_MAXINDEX; aa++) {
                    levelRecords[aa] = 0;
                }

                // ------------------------------------------------------------
                // Generate map, monsters, and items


                // Temporary space used for generation is stored in the
                // level space where the "player inventory drop" allocation
                // is reserved (LEVEL_ITEMINST_START_DROPS_IDX).

// cx = grid size (x)
// cy = grid size (y)
                cx = MAP_CELLHEIGHT_BASE +
                        ((level * MAP_CELLHEIGHT_MULTIPLIER) / MAP_CELLHEIGHT_DIVISOR);
                if (cx > MAP_MAX_X_CELLS) {
                    cx = MAP_MAX_X_CELLS;
                }
                cy = MAP_CELLWIDTH_BASE +
                        ((level * MAP_CELLWIDTH_MULTIPLIER) / MAP_CELLWIDTH_DIVISOR);
                if (cy > MAP_MAX_Y_CELLS) {
                    cy = MAP_MAX_Y_CELLS;
                }
                final int mapWidth = (cx * MAP_CELLSIZE_X) + 2;
                final int mapHeight = (cy * MAP_CELLSIZE_Y) + 2;
// System.out.println("["+cx+","+cy+"] -> ["+mapWidth+","+mapHeight+"]");

                final int[] map = new int[MAP_SIZE];

                // by default, map is filled with '0', which is the empty space
                // character.

                // Use the "rogue" algorithm
                //  - Grid size: (mapWidth/MAP_GRID_DIVX) x (mapHeight/MAP_GRID_DIVY)
                //  - There will be at least (cell count / MAP_CELL_DIV) cells
                //    with rooms.
                //  - Mark the array as having some data:
                //       * room seed position (x,y); absolute on grid coordinates,
                //       but within the cell.
                //       * connected flag (0 or 1)
                //       * has-a-room flag (0 or 1)
                //       * room size.  Ensure that
                //         the seeded room doesn't intersect another room.
                //  ** NOTE: "connect rooms" means draw the path on the map
                //     between the room seed points.
                //  - pick random starting cell, mark it connected.
                //  - While there are unconnected neighbor rooms, connect
                //          to one that isn't connected, mark it as connected,
                //          connect the two rooms, and set as current room.
                //  - While there are unconnected rooms, pick the first one
                //          and connect it to a randomly connected neighbor.
                //          If there are no connected neighbors, skip this room.
                //  - Draw each room.  If a path intersects a wall, turn it
                //          into a door.


                // first pass - generate cells
                pos1 = LEVEL_MAPGEN_START_IDX;
//System.out.println("Map size: (" + mapWidth+","+mapHeight+"); cells: ("+cx+","+cy+")");
                for (ay = 0; ay < cy; ay++) {
                    for (ax = 0; ax < cx; ax++, pos1 += MAP_CELL_SIZE) {
                        // Create the room and its dimensions.  Put if statements
                        // here for the borders of the rooms to eliminate the
                        // need to wiggle them around later.  It makes more
                        // rigid room layouts, but it's simpler code.
//System.out.println("MAPGEN ("+ax+","+ay+")@"+pos1);

                        levelRecords[pos1 + MAP_CELL_IDX_CONNECTED] = 0;

                        // In order to avoid having paths align on the sides of
                        // walls, which creates a long line of doors, we force
                        // the room walls to land on even spaces, and paths on
                        // odd spaces.  We do that by making the seed positions
                        // be odd, and the room dimensions even.

                        // The size and position of the rooms always defines
                        // the interior of the room.  If those are odd, then
                        // the rendering of the walls (which is 1 more/less)
                        // will always be even.  Fortunately, guaranteeing
                        // something is odd is done by ORing in 1.

                        // random number * 2 + 2 is always even
                        // width
                        aa = levelRecords[pos1 + MAP_CELL_IDX_DIM_W] =
                            (random.nextInt(MAP_CELLSIZE_X_SIZE_GEN) + 2) | 1;
                        // height
                        bb = levelRecords[pos1 + MAP_CELL_IDX_DIM_H] =
                            (random.nextInt(MAP_CELLSIZE_Y_SIZE_GEN) + 2) | 1;

                        levelRecords[pos1 + MAP_CELL_IDX_DIM_X] =
                            ((ax * MAP_CELLSIZE_X) + random.nextInt(MAP_CELLSIZE_X_POS_GEN - aa) + 3) | 1;
                        levelRecords[pos1 + MAP_CELL_IDX_DIM_Y] =
                            ((ay * MAP_CELLSIZE_Y) + random.nextInt(MAP_CELLSIZE_Y_POS_GEN - aa) + 3) | 1;

                        // Paths are drawn along straight lines between the seeds.
                        // If the seeds are odd, then the paths will end up on
                        // odd lines.
                        levelRecords[pos1 + MAP_CELL_IDX_SEED_X] =
                            (levelRecords[pos1 + MAP_CELL_IDX_DIM_X] + (aa >> 1)) | 1;
                        levelRecords[pos1 + MAP_CELL_IDX_SEED_Y] =
                            (levelRecords[pos1 + MAP_CELL_IDX_DIM_Y] + (bb >> 1)) | 1;
//System.out.println("Room ("+ax+","+ay+") %("+levelRecords[pos1 + MAP_CELL_IDX_SEED_X]+","+levelRecords[pos1 + MAP_CELL_IDX_SEED_Y]+") -> [("+levelRecords[pos1 + MAP_CELL_IDX_DIM_X]+","+levelRecords[pos1 + MAP_CELL_IDX_DIM_Y]+": "+levelRecords[pos1 + MAP_CELL_IDX_DIM_W]+"x"+levelRecords[pos1 + MAP_CELL_IDX_DIM_H]+"]");
                    }
                }

                // Second pass - connect the rooms
                //   - initialize the loop - room 1 is marked connected.  It's
                //     also where the player starts.

                pos1 = LEVEL_MAPGEN_START_IDX;
                levelRecords[LEVEL_MAPGEN_START_IDX + MAP_CELL_IDX_CONNECTED] = 1;
                levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_X] =
                        levelRecords[LEVEL_MAPGEN_START_IDX + MAP_CELL_IDX_DIM_X] + 1;
                levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_Y] =
                        levelRecords[LEVEL_MAPGEN_START_IDX + MAP_CELL_IDX_DIM_Y] + 1;
                levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_MAPPOS] =
                        (levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_X] +
                        levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_Y]*mapWidth) * MAPREC_SIZE;
                int playerNewX = levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_X];
                int playerNewY = levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_Y];
                // set the player starting map flag
                map[levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_MAPPOS] + MAPREC_IDX_MONSTER_IDX] =
                    LEVEL_MONSTERINST_PLAYER_IDX;

                dd = 1;
                findUnconnectedNeighbor:
                while (dd > 0) {
                    dd = 0;
                    // find an unconnected neighbor
//System.out.println("CONNECT ("+bx+","+by+")");
                    pos2 = LEVEL_MAPGEN_START_IDX;
                    for (aa = 0; aa < (cx*cy); aa++, pos2 += MAP_CELL_SIZE) {
                        if (pos1 != pos2 && levelRecords[pos2 + MAP_CELL_IDX_CONNECTED] == 0) {
                            dd = 1;
                            if (random.nextInt(2) == 0) {
                                // draw line on map of path - L shaped
//System.out.println(" -> ("+ax+","+ay+")");

                                x = levelRecords[pos1 + MAP_CELL_IDX_SEED_X];
                                y = levelRecords[pos2 + MAP_CELL_IDX_SEED_X];
                                if (x > y) {
                                    cc = x;
                                    x = y;
                                    y = cc;
                                }
                                for (; x <= y; x++) {
                                    pos3 = (mapWidth*levelRecords[pos1 + MAP_CELL_IDX_SEED_Y] + x) * MAPREC_SIZE;
                                    map[pos3 + MAPREC_IDX_DISPLAY] = DISPLAY_CHAR_PATH;
                                    map[pos3 + MAPREC_IDX_COLOR] = COLOR_IDX_PATH;
                                    map[pos3 + MAPREC_IDX_PASSABLE] = MAPREC_PASSABLE_YES;
                                    map[pos3 + MAPREC_IDX_LIGHT] = MAPREC_LIGHT_DIM;
                                    //map[pos3 + MAPREC_IDX_SEEN] = MAPREC_SEEN_NO; = 0
                                    //map[pos3 + MAPREC_IDX_MONSTER_IDX] = LEVEL_MONSTERINST_NUL_IDX; = 0
                                    //map[pos3 + MAPREC_IDX_ITEM_IDX] = LEVEL_ITEMINST_NUL_IDX; = 0
                                    //map[pos3 + MAPREC_IDX_ROOM_ID] = MAPREC_ROOM_ID_NUL; = 0
                                    // Make paths always index 1
                                    map[pos3 + MAPREC_IDX_PATH_ID] = 1;
                                }
                                x = levelRecords[pos1 + MAP_CELL_IDX_SEED_Y];
                                y = levelRecords[pos2 + MAP_CELL_IDX_SEED_Y];
                                if (x > y) {
                                    cc = x;
                                    x = y;
                                    y = cc;
                                }
                                for (; x <= y; x++) {
                                    pos3 = (mapWidth*x + levelRecords[pos2 + MAP_CELL_IDX_SEED_X]) * MAPREC_SIZE;
                                    map[pos3 + MAPREC_IDX_DISPLAY] = DISPLAY_CHAR_PATH;
                                    map[pos3 + MAPREC_IDX_COLOR] = COLOR_IDX_PATH;
                                    map[pos3 + MAPREC_IDX_PASSABLE] = MAPREC_PASSABLE_YES;
                                    map[pos3 + MAPREC_IDX_LIGHT] = MAPREC_LIGHT_DIM;
                                    //map[pos3 + MAPREC_IDX_SEEN] = MAPREC_SEEN_NO; = 0
                                    //map[pos3 + MAPREC_IDX_MONSTER_IDX] = LEVEL_MONSTERINST_NUL_IDX; = 0
                                    //map[pos3 + MAPREC_IDX_ITEM_IDX] = LEVEL_ITEMINST_NUL_IDX; = 0
                                    //map[pos3 + MAPREC_IDX_ROOM_ID] = MAPREC_ROOM_ID_NUL; = 0
                                    // Make paths always index 1
                                    map[pos3 + MAPREC_IDX_PATH_ID] = 1;
                                }


                                levelRecords[pos2 + MAP_CELL_IDX_CONNECTED] = 1;
                                pos1 = pos2;

                                continue findUnconnectedNeighbor;
                            }
                        }
//else System.out.println(" -> randomly not connecting");
                    }
                }

                // draw rooms + doors
                int monsterPos = LEVEL_MONSTERINST_FIRSTMONSTER_IDX;
                int itemPos = LEVEL_ITEMINST_IDX + 0 * ITEMINST_SIZE;

                // Find stairs
                pos1 = LEVEL_MAPGEN_START_IDX + ((random.nextInt(cx - 1) + 1) + (random.nextInt(cy - 1) + 1)*cx) * MAP_CELL_SIZE;
                int stairPosX = levelRecords[pos1 + MAP_CELL_IDX_SEED_X];
                int stairPosY = levelRecords[pos1 + MAP_CELL_IDX_SEED_Y];
//System.out.println("Stairs: ("+stairPosX+","+stairPosY+")");

                bb = 0; // room ID
//System.out.println("Max Size: ("+mapWidth+","+mapHeight+")");
                for (aa = 0; aa < (cx * cy); aa++) {
                    bb++;
                    // exterior dimensions
                    pos1 = LEVEL_MAPGEN_START_IDX + aa * MAP_CELL_SIZE;
                    ax = levelRecords[pos1 + MAP_CELL_IDX_DIM_X] - 1;
                    ay = levelRecords[pos1 + MAP_CELL_IDX_DIM_Y] - 1;
                    bx = levelRecords[pos1 + MAP_CELL_IDX_DIM_W] + 1;
                    by = levelRecords[pos1 + MAP_CELL_IDX_DIM_H] + 1;
//System.out.println("DRAW [("+ax+","+ay+"): "+bx+"x"+by+"]@" + pos1);
//System.out.println("  seed: ("+levelRecords[LEVEL_MAPGEN_START_IDX + aa * MAP_CELL_SIZE + MAP_CELL_IDX_SEED_X]+","+levelRecords[LEVEL_MAPGEN_START_IDX + aa * MAP_CELL_SIZE + MAP_CELL_IDX_SEED_Y]+")");

                    // Every room has an item.  This makes the player
                    // really have to decide whether it's worth exploring more
                    // when the stairs are found.
                    int itemChance = random.nextInt(ITEMCHANCE_TOTAL);

                    for (pos1 = GAME_ITEMREC_IDX; pos1 < GAME_ITEMREC_LISTEND_IDX; pos1 += ITEMREC_SIZE) {
                        if (itemChance < gameRecords[pos1 + IR_IDX_CHANCE]) {
//System.out.println("Chance = " + itemChance + " -> " + TEXT[gameRecords[pos1 + IR_IDX_NAME]]);
                            levelRecords[itemPos + II_IDX_RECORD] = pos1;
                            levelRecords[itemPos + II_IDX_CHARGES] = random.nextInt(10) + 1; // TODO game balance
                            levelRecords[itemPos + II_IDX_STRENGTH] = (level * 4 / 3) - random.nextInt((level / 3) + 1) + 2; // TODO game balance
                            if (levelRecords[itemPos + II_IDX_STRENGTH] > 12) { // TODO balance
                                levelRecords[itemPos + II_IDX_STRENGTH] = 12; // TODO balance
                            }
                            if (levelRecords[itemPos + II_IDX_STRENGTH] < 2) {
                                levelRecords[itemPos + II_IDX_STRENGTH] = 2;
                            }
                            if (gameRecords[pos1 + IR_IDX_TYPE] == ITEMTYPE_POTION) {
                                // potion-type objects have only one charge,
                                // but are stronger.
                                levelRecords[itemPos + II_IDX_CHARGES] = 1;
                                levelRecords[itemPos + II_IDX_STRENGTH] <<= 1;
                            }
                            levelRecords[itemPos + II_IDX_WORN] = II_WORN_NO;

                            pos2 = (ax + 1 + random.nextInt(bx - 1) + (ay + 1 + random.nextInt(by - 1))*mapWidth) * MAPREC_SIZE;
                            map[pos2 + MAPREC_IDX_ITEM_IDX] = itemPos;
                            itemPos += ITEMINST_SIZE;
                            break;
                        }
                    }


                    if (monsterPos < LEVEL_MONSTERINST_MAXINDEX && random.nextInt(MONSTERCHANCE_FIND) == 0) {
                        y = (level>>1) + 1;
                        if (y >= GAME_MONSTERREC_COUNT) {
                            y = GAME_MONSTERREC_COUNT - 1;
                        }
                        y = random.nextInt(y) + 1;
                        pos2 = GAME_MONSTERREC_FIRSTMONSTER_IDX + (y * MONSTERREC_SIZE);
//System.out.println("Random monster type " + y + ": " + TEXT[gameRecords[pos2 + MR_IDX_COLOR]] + " " + TEXT[gameRecords[pos2 + MR_IDX_NAME1]] + " " + TEXT[gameRecords[pos2 + MR_IDX_NAME2]]);
                        x = level + 1;
                        y = (level >> 1) + 2;
                        levelRecords[monsterPos + MI_IDX_RECORD] = pos2;
                        levelRecords[monsterPos + MI_IDX_RETREAT_HEALTH] = ((x + random.nextInt(y)) >> 1) + 4;
                        levelRecords[monsterPos + MI_IDX_HEALTH] = levelRecords[monsterPos + MI_IDX_RETREAT_HEALTH] << 1; // game balance
                        if ((gameRecords[pos2 + MR_IDX_AI] >> 1) == MONSTERREC_AI_AGGRESSIVE_SHL) {
                            // never retreat
                            levelRecords[monsterPos + MI_IDX_RETREAT_HEALTH] = 0;
                        }

                        levelRecords[monsterPos +  MI_IDX_ATTACK] = x - (random.nextInt(y)) + 4; // game balance
                        if (levelRecords[monsterPos +  MI_IDX_ATTACK] < 1) {
                            levelRecords[monsterPos +  MI_IDX_ATTACK] = 1;
                        }
                        levelRecords[monsterPos +  MI_IDX_ARMOR] = x - random.nextInt(y); // game balance
                        if (levelRecords[monsterPos +  MI_IDX_ARMOR] < 0) {
                            levelRecords[monsterPos +  MI_IDX_ARMOR] = 0;
                        }
                        x = ax + random.nextInt(bx - 1) + 1;
                        y = ay + random.nextInt(by - 1) + 1;
                        if (x == playerNewX && y == playerNewY) {
                            x++;
                        }
                        pos2 = (x + (y)*mapWidth) * MAPREC_SIZE;
                        levelRecords[monsterPos +  MI_IDX_X] = x;
                        levelRecords[monsterPos +  MI_IDX_Y] = y;
                        levelRecords[monsterPos +  MI_IDX_MAPPOS] = pos2;
                        levelRecords[monsterPos +  MI_IDX_AI_STATE] = AI_STATE_SLEEP;
                        //levelRecords[monsterPos +  MI_IDX_AI_ACTION] = ACTION_NONE; // = 0
                        map[pos2 + MAPREC_IDX_MONSTER_IDX] = monsterPos;
                        monsterPos += MONSTERINST_SIZE;
                    }



                    pos1 = (ax + ay*mapWidth) * MAPREC_SIZE;
                    for (y = 0; y <= by; y++, pos1 += (mapWidth*MAPREC_SIZE)) {
                        pos2 = pos1;
                        for (x = 0; x <= bx; x++, pos2 += MAPREC_SIZE) {
                            dd = DISPLAY_CHAR_ROOM; // display value
                            cc = COLOR_IDX_ROOM;

                            if (x == 0 || x == bx) {
                                // West / East side
                                cc = COLOR_IDX_WALL;
                                dd = DISPLAY_CHAR_BOX_DOUBLE_VERT;

                                // corners are checked in the y if statement,
                                // which is the last setter
                            }
                            if (y == 0) {
                                // North side
                                cc = COLOR_IDX_WALL;
                                dd = DISPLAY_CHAR_BOX_DOUBLE_HORIZ;

                                if (x == 0) {
                                    // NW wall
                                    dd = DISPLAY_CHAR_BOX_DOUBLE_NW;
                                }
                                if (x == bx) {
                                    // NE wall
                                    dd = DISPLAY_CHAR_BOX_DOUBLE_NE;
                                }
                            }
                            if (y == by) {
                                // South side
                                cc = COLOR_IDX_WALL;
                                dd = DISPLAY_CHAR_BOX_DOUBLE_HORIZ;

                                if (x == 0) {
                                    // SW wall
                                    dd = DISPLAY_CHAR_BOX_DOUBLE_SW;
                                }
                                if (x == bx) {
                                    // SE wall
                                    dd = DISPLAY_CHAR_BOX_DOUBLE_SE;
                                }
                            }
                            ee = map[pos2 + MAPREC_IDX_DISPLAY]; // old display setting

                            map[pos2 + MAPREC_IDX_DISPLAY] = dd;
                            map[pos2 + MAPREC_IDX_COLOR] = cc;
                            //map[pos2 + MAPREC_IDX_SEEN] = MAPREC_SEEN_NO; //= 0
                            //map[pos2 + MAPREC_IDX_MONSTER_IDX] = LEVEL_MONSTERINST_NUL_IDX; //= 0
                            //map[pos2 + MAPREC_IDX_ITEM_IDX] = LEVEL_ITEMINST_NUL_IDX; //= 0
                            map[pos2 + MAPREC_IDX_ROOM_ID] = bb;
                            //map[pos2 + MAPREC_IDX_PATH_ID] = MAPREC_PATH_ID_NUL; //= 0

                            if (dd == DISPLAY_CHAR_ROOM) {
                                map[pos2 + MAPREC_IDX_PASSABLE] = MAPREC_PASSABLE_YES;
                                map[pos2 + MAPREC_IDX_PATH_ID] = MAPREC_PATH_ID_NUL;

                                map[pos2 + MAPREC_IDX_LIGHT] = MAPREC_LIGHT_LIT; // = 0, but we must set it because the path may have not

                                // stairs
                                if ((y + ay) == stairPosY && (x + ax) == stairPosX) {
                                    map[pos2 + MAPREC_IDX_DISPLAY] = DISPLAY_CHAR_DOWNSTAIR;
                                    map[pos2 + MAPREC_IDX_COLOR] = COLOR_IDX_FEATURE;

                                    // Don't let an item or monster disguise where the stairs are
                                    map[pos2 + MAPREC_IDX_MONSTER_IDX] = LEVEL_MONSTERINST_NUL_IDX;
                                    map[pos2 + MAPREC_IDX_ITEM_IDX] = LEVEL_ITEMINST_NUL_IDX;

//System.out.println("stairs: ("+x+","+y+")");
                                }
                            } else {
                                // Edge
                                if (ee == DISPLAY_CHAR_PATH) {
                                    // Door
                                    map[pos2 + MAPREC_IDX_DISPLAY] = DISPLAY_CHAR_DOOR;
                                    map[pos2 + MAPREC_IDX_COLOR] = COLOR_IDX_FEATURE;
                                    //map[pos2 + MAPREC_IDX_LIGHT] = MAPREC_LIGHT_DIM; // already set by path
                                    //map[pos2 + MAPREC_IDX_PASSABLE] = MAPREC_PASSABLE_YES; // already set by path
                                }
                                // else it was all set correctly:
                                    // Wall
                                    // map[pos2 + MAPREC_IDX_LIGHT] = MAPREC_LIGHT_LIT; // = 0
                                    // map[pos2 + MAPREC_IDX_PASSABLE] = MAPREC_PASSABLE_NO; // = 0
                            }
                        }
                    }
                }

                // starting window-over-the-map position
                /* This isn't necessary, because the window is always near 0
                 * at the start of a level.
                windowX = levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_X] - (charactersPerLine / 2);
                if (windowX < 0) {
                    windowX = 0;
                }
                windowY = levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_Y] - (guiLines  / 2);
                if (windowY < 0) {
                    windowY = 0;
                }
                 */


                // DEBUG see the whole map
                /*
                pos1 = 0;
                for (x = 0; x < mapWidth; x++) {
                    for (y = 0; y < mapHeight; y++) {
                        if (map[pos1 + MAPREC_IDX_DISPLAY] != 0) {
                            map[pos1 + MAPREC_IDX_SEEN] = MAPREC_SEEN_ALWAYS;
                        }
                        pos1 += MAPREC_SIZE;
                    }
                }
                */


                // ===========================================================
                // GAME LOOP
                long lastTime = System.nanoTime();


                Graphics sg = getGraphics();
                loopGame: while (true) {
                    int actionId = ACTION_NONE;
                    // ========================================================
                    // Shared processing between started and not started games

                    // --------------------------------------------------------
                    // Initialize the screen
                    // background
                    ogr.setColor(COLOR_MAP[COLOR_IDX_BACKGROUND]);
                    ogr.fillRect(0, 0, RENDER_WIDTH, RENDER_HEIGHT);

                    int playerDirection = 0;
                    boolean playerMoved = false;

                    if (this.key != KEY_NOTHING) {
                        // ================================================
                        // Process the key press

                        // gameStarted == false means displayState == DISPLAYSTATE_STATS
                        displayStateSwitch: switch (displayState) {
                            case DISPLAYSTATE_START:
                                displayState = DISPLAYSTATE_MAP;
                                playerMoved = true;
                                a(message, TEXT_IDX_DOWN);
                                status = new StringBuffer();
                                break;
                            case DISPLAYSTATE_MAP:
                                // descend into the depths
                                if (this.key == KEY_DOWN1
                                        && map[levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_MAPPOS] + MAPREC_IDX_DISPLAY] == DISPLAY_CHAR_DOWNSTAIR
                                        ) {
                                    displayState = DISPLAYSTATE_START;
                                    this.key = KEY_INITIALIZE;
                                    level++;
                                    // status update?
                                    continue startLevel;
                                }

                                if (this.key == KEY_TAKE1) {
                                        // Even if there is nothing on the
                                        // floor, this will count as an action.
                                        actionId = ACTION_TAKE;
                                        break displayStateSwitch;
                                }
                                if (this.key == KEY_DROP1) {
                                        if (map[levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_MAPPOS] + MAPREC_IDX_ITEM_IDX] != LEVEL_ITEMINST_NUL_IDX) {
                                            // message: already something here
                                            a(message, TEXT_IDX_CANT_DROP);
                                            break displayStateSwitch;
                                        }

                                        // this is not an action, but the
                                        // beginning of an action.
                                        displayState = DISPLAYSTATE_DROP_INV;
                                        break displayStateSwitch;
                                }
                                if (this.key == KEY_USE1) {
                                        // this is not an action, but the
                                        // beginning of an action.
                                        displayState = DISPLAYSTATE_USE_INV;
                                        break displayStateSwitch;
                                }
                                if (this.key == KEY_WEAR1) {
                                        // this is not an action, but the
                                        // beginning of an action.
                                        displayState = DISPLAYSTATE_WEAR_INV;
                                        break displayStateSwitch;
                                }

                                // the only other action to check is movement,
                                // so fall through
                                actionId = ACTION_MOVE;
                            case DISPLAYSTATE_PICKDIR:
                                if (this.key == KEY_SELF1) {
                                    playerDirection = KEY_SELF1;
                                } else {
                                    playerDirection = this.key & KEY_DIR_MASK;
                                }

                                if (playerDirection == KEY_NOTHING) {
                                    actionId = ACTION_NONE;
                                    displayState = DISPLAYSTATE_MAP;
                                    status = new StringBuffer();
                                    a(status, TEXT_IDX_CANCELED);
                                } else
                                if (actionId != ACTION_MOVE) {
                                    actionId = ACTION_USEDIR;
                                }

                                break;
                            case DISPLAYSTATE_DROP_INV:
                            case DISPLAYSTATE_USE_INV:
                            case DISPLAYSTATE_WEAR_INV:
                                // pick an item from the inventory
                                selectedInventoryIndex = (this.key - 'a') * ITEMINST_SIZE;
                                if (selectedInventoryIndex < 0 ||
                                        selectedInventoryIndex >= PLAYER_INVENTORY_SIZE ||
                                        playerInventory[selectedInventoryIndex + II_IDX_RECORD] == GAME_ITEM_NUL_IDX) {
                                    selectedInventoryIndex = PLAYER_INVENTORY_SIZE;
                                }


                                // If no valid item picked, report cancel and
                                // return to map; no action performed.
                                if (selectedInventoryIndex == PLAYER_INVENTORY_SIZE) {
                                    displayState = DISPLAYSTATE_MAP;
                                    status = new StringBuffer();
                                    a(status, TEXT_IDX_CANCELED);
                                    break;
                                }

                                if (displayState == DISPLAYSTATE_USE_INV) {
                                    // switch state to select direction
                                    displayState = DISPLAYSTATE_MAP;
                                    displayState = DISPLAYSTATE_PICKDIR;

                                    // message about choosing direction
                                    // - need to clear the previous message.
                                    message = new StringBuffer();
                                    a(message, TEXT_IDX_PICKDIR);
                                } else {
                                    actionId =
                                            (displayState == DISPLAYSTATE_DROP_INV)
                                                ? ACTION_DROP : ACTION_WEAR;
                                }

                                break;
                            //case DISPLAYSTATE_NEWGAME: - use default instead
                            default:
                                // any key to continue
                                if (! gameStarted) {
                                    // --------------------------------------------
                                    // Start a new game
                                    gameStarted = true;

                                    level = 1;

                                    displayState = DISPLAYSTATE_START;
                                    this.key = KEY_INITIALIZE;
                                    message = new StringBuffer();
                                    continue loopGame;
                                }
                                displayState = DISPLAYSTATE_MAP;
                        }
                        this.key = KEY_NOTHING;
                    }

                    if (gameStarted) {
                        // We only perform an update if the user performs an
                        // action.
                        levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_ACTION] = ACTION_NONE;

                        if (actionId != ACTION_NONE) {
                            message = new StringBuffer();
                            playerFood--;
                            if (playerFood < 0) {
                                playerFood = 0;
                                levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_HEALTH]--;
                                a(message, TEXT_IDX_STARVING);
                            }

                            // ================================================
                            // process next step in the game

                            // ------------------------------------------------
                            // Update Player
                            updatePlayer:
                            switch (actionId) {
                                case ACTION_MOVE:
                                    aa = ACTION_NONE;
                                    playerNewX = levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_X];
                                    playerNewY = levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_Y];
                                    if ((playerDirection & KEY_DIR_N) != 0) {
                                        playerNewY--;
                                        aa = ACTION_MOVE;
                                    } else
                                    if ((playerDirection & KEY_DIR_S) != 0) {
                                        playerNewY++;
                                        aa = ACTION_MOVE;
                                    }
                                    if ((playerDirection & KEY_DIR_E) != 0) {
                                        playerNewX++;
                                        aa = ACTION_MOVE;
                                    } else
                                    if ((playerDirection & KEY_DIR_W) != 0) {
                                        playerNewX--;
                                        aa = ACTION_MOVE;
                                    }
                                    // if playerSelf, do nothing

                                    levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_ACTION] = aa;

                                    break;
                                case ACTION_USEDIR:
                                    pos2 = playerInventory[selectedInventoryIndex + II_IDX_RECORD];
                                    aa = playerInventory[selectedInventoryIndex + II_IDX_STRENGTH];

                                    // identify the game record item
                                    gameRecords[pos2 + IR_IDX_IDENTIFIED] = 1;

                                    pos1 = LEVEL_MONSTERINST_NUL_IDX;
                                    if ((playerDirection & KEY_SELF1) != 0) {
                                        pos1 = LEVEL_MONSTERINST_PLAYER_IDX;
                                    } else {
                                        ax = levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_X];
                                        ay = levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_Y];
                                        pos3 = (ax + ay * mapWidth) * MAPREC_SIZE;
                                        bb = 0;
                                        if ((playerDirection & KEY_DIR_N) != 0) {
                                            bb -= mapWidth;
                                        } else
                                        if ((playerDirection & KEY_DIR_S) != 0) {
                                            bb += mapWidth;
                                        }
                                        if ((playerDirection & KEY_DIR_E) != 0) {
                                            bb++;
                                        } else
                                        if ((playerDirection & KEY_DIR_W) != 0) {
                                            bb--;
                                        }
                                        bb *= MAPREC_SIZE;

                                        // Don't perform boundary checks -
                                        // by checking passable, we know that the direction is inside the map.
                                        do {
                                            pos3 += bb;
                                            pos1 = map[pos3 + MAPREC_IDX_MONSTER_IDX];
                                        } while (map[pos3 + MAPREC_IDX_PASSABLE] == MAPREC_PASSABLE_YES &&
                                                pos1 == LEVEL_MONSTERINST_NUL_IDX);
                                    }

                                    a(message, gameRecords[pos2 + IR_IDX_NAME]);
                                    a(message, TEXT_IDX_OF);
                                    a(message, gameRecords[pos2 + IR_IDX_ACTION] + TEXT_IDX_START_ITEMACTION_NAME);
                                    if (pos1 == LEVEL_MONSTERINST_NUL_IDX) {
                                        a(message, TEXT_IDX_ITEMACTION_VERB_NOTHING);
                                    } else {
                                        // effect
                                        dd = 0; // report effect on creature?
                                        pos3 = levelRecords[pos1 + MI_IDX_RECORD];
                                        a(message, gameRecords[pos2 + IR_IDX_ACTION] + TEXT_IDX_START_ITEMACTION_VERB);

                                        switch (gameRecords[pos2 + IR_IDX_ACTION]) {
                                            case ITEMACTION_FOOD:
                                                if (pos1 == LEVEL_MONSTERINST_PLAYER_IDX) {
                                                    playerFood += (aa * ITEM_HUNGER_MULT);
                                                    if (playerFood > PLAYER_MAXIMUM_FOOD) {
                                                        playerFood = PLAYER_MAXIMUM_FOOD;
                                                    }
                                                }
                                                dd = 1;
                                                break;
                                            // ITEMACTION_NONE - do nothing
                                            case ITEMACTION_DAMAGE:
                                                aa = -aa;
                                                // fall through and damage health
                                            case ITEMACTION_HEAL:
                                                dd = 1;
                                                cc = levelRecords[pos1 + MI_IDX_HEALTH] + aa;
                                                if (pos1 == LEVEL_MONSTERINST_PLAYER_IDX &&
                                                        cc > PLAYER_MAXIMUM_HEALTH) {
                                                    cc = PLAYER_MAXIMUM_HEALTH;
                                                }
                                                levelRecords[pos1 + MI_IDX_HEALTH] = cc;
                                                // monster death is checked
                                                // in the monster loop

                                                break;
                                        }
                                        if (dd == 1) {
                                            a(message, gameRecords[pos3 + MR_IDX_COLOR_NAME]);
                                            a(message, gameRecords[pos3 + MR_IDX_NAME1]);
                                            a(message, gameRecords[pos3 + MR_IDX_NAME2]);
                                        }
                                    }

                                    // remove a charge
                                    cc = TEXT_IDX_EOS;
                                    if (--playerInventory[selectedInventoryIndex + II_IDX_CHARGES] <= 0) {
                                        playerInventory[selectedInventoryIndex + II_IDX_RECORD] = GAME_ITEM_NUL_IDX;
                                        cc = TEXT_IDX_ITEM_DISAPPEARS;
                                    }
                                    a(message, cc);


                                    displayState = DISPLAYSTATE_MAP;
                                    break;
                                case ACTION_TAKE:
                                    pos1 = MAPREC_IDX_ITEM_IDX + levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_MAPPOS];
                                    dd = map[pos1];
                                    if (dd == LEVEL_ITEMINST_NUL_IDX) {
                                        // message: nothing to pick up
                                        a(message, TEXT_IDX_NOTAKE);
                                    } else {
                                        cc = levelRecords[dd + II_IDX_RECORD];
                                        if (cc == GAME_ITEMREC_IDX_MONEY) {
                                            cc = levelRecords[dd + II_IDX_STRENGTH];
                                            playerMoney += cc;
                                            map[pos1] = LEVEL_ITEMINST_NUL_IDX;
                                            message.append(cc);
                                            a(message, TEXT_IDX_GOLD_NAME);
                                            a(message, TEXT_IDX_TAKEN);
                                            break;
                                        }
                                        // pick up the item

                                        for (aa = 0; aa < PLAYER_INVENTORY_SIZE; aa += ITEMINST_SIZE) {
                                            if (playerInventory[aa + II_IDX_RECORD] == GAME_ITEM_NUL_IDX) {
                                                for (bb = 0; bb < ITEMINST_SIZE; bb++) {
                                                    playerInventory[aa + bb] = levelRecords[dd + bb];
                                                }
                                                map[pos1] = LEVEL_ITEMINST_NUL_IDX;

                                                // message: took the item
                                                if (gameRecords[cc + IR_IDX_IDENTIFIED] == 0) {
                                                    a(message, gameRecords[cc + IR_IDX_COLOR] + TEXT_IDX_START_COLOR);
                                                    a(message, gameRecords[cc + IR_IDX_NAME]);
                                                } else {
                                                    a(message, gameRecords[cc + IR_IDX_NAME]);
                                                    a(message, TEXT_IDX_OF);
                                                    a(message, gameRecords[cc + IR_IDX_ACTION] + TEXT_IDX_START_ITEMACTION_NAME);
                                                }
                                                a(message, TEXT_IDX_TAKEN);
                                                break updatePlayer;
                                            }
                                        }
                                        // message: you're holding too much
                                        a(message, TEXT_IDX_OVERBURDENED);
                                    }
                                    break;
                                case ACTION_DROP:
                                    pos1 = levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_MAPPOS];
                                    // is the item worn?  If so, remove it and its effects
                                    if (playerInventory[selectedInventoryIndex + II_IDX_WORN] == II_WORN_YES) {
                                        dd = gameRecords[playerInventory[selectedInventoryIndex + II_IDX_RECORD] + IR_IDX_TYPE];
                                        if (dd == ITEMTYPE_ARMOR) {
                                            levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_ARMOR] -= playerInventory[selectedInventoryIndex + II_IDX_STRENGTH];
                                        } else
                                        if (dd == ITEMTYPE_WEAPON) {
                                            levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_ATTACK] -= playerInventory[selectedInventoryIndex + II_IDX_STRENGTH];
                                        }
                                        playerInventory[selectedInventoryIndex + II_IDX_WORN] = II_WORN_NO;
                                    }

                                    // Find a position to drop the item in the level list
                                    for (aa = LEVEL_ITEMINST_IDX; aa < LEVEL_ITEMINST_MAXINDEX; aa += ITEMINST_SIZE) {
                                        if (levelRecords[aa + II_IDX_RECORD] == GAME_ITEM_NUL_IDX) {
                                            // copy the instance to the level list
                                            for (bb = 0; bb < ITEMINST_SIZE; bb++) {
                                                levelRecords[aa + bb] = playerInventory[selectedInventoryIndex + bb];
                                            }
                                            cc = levelRecords[aa + II_IDX_RECORD];

                                            // remove it from the player's inventory
                                            playerInventory[selectedInventoryIndex + II_IDX_RECORD] = GAME_ITEM_NUL_IDX;

                                            // put the item on the map
                                            map[pos1 + MAPREC_IDX_ITEM_IDX] = aa;

                                            // message: dropped item
                                            if (gameRecords[cc + IR_IDX_IDENTIFIED] == 0) {
                                                a(message, gameRecords[cc + IR_IDX_COLOR] + TEXT_IDX_START_COLOR);
                                                a(message, gameRecords[cc + IR_IDX_NAME]);
                                            } else {
                                                a(message, gameRecords[cc + IR_IDX_NAME]);
                                                a(message, TEXT_IDX_OF);
                                                a(message, gameRecords[cc + IR_IDX_ACTION] + TEXT_IDX_START_ITEMACTION_NAME);
                                            }
                                            a(message, TEXT_IDX_DROPPED);
                                            break;
                                        }
                                    }
                                    displayState = DISPLAYSTATE_MAP;
                                    break;
                                case ACTION_WEAR:
                                    displayState = DISPLAYSTATE_MAP;

                                    if (playerInventory[selectedInventoryIndex + II_IDX_WORN] == II_WORN_YES) {
                                        // already worn
                                        a(message, TEXT_IDX_CANCELED);
                                        break;
                                    }

                                    // Test if the item is wear / wield -able
                                    cc = gameRecords[playerInventory[selectedInventoryIndex + II_IDX_RECORD] + IR_IDX_TYPE];
                                    if (cc == ITEMTYPE_ARMOR) {
                                        levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_ARMOR] += playerInventory[selectedInventoryIndex + II_IDX_STRENGTH];
                                    } else
                                    if (cc == ITEMTYPE_WEAPON) {
                                        levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_ATTACK] += playerInventory[selectedInventoryIndex + II_IDX_STRENGTH];
                                    } else {
                                        // not wear-able
                                        a(message, TEXT_IDX_CANCELED);
                                        break;
                                    }

                                    // Loop over current equipment and unwear it
                                    for (pos1 = 0; pos1 < PLAYER_INVENTORY_SIZE; pos1 += ITEMINST_SIZE) {
                                        dd = gameRecords[playerInventory[pos1 + II_IDX_RECORD] + IR_IDX_TYPE];
                                        if (playerInventory[pos1 + II_IDX_RECORD] != GAME_ITEM_NUL_IDX &&
                                                playerInventory[pos1 + II_IDX_WORN] == II_WORN_YES &&
                                                dd == cc) {
                                            if (dd == ITEMTYPE_ARMOR) {
                                                levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_ARMOR] -= playerInventory[pos1 + II_IDX_STRENGTH];
                                            } else
                                            if (dd == ITEMTYPE_WEAPON) {
                                                levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_ATTACK] -= playerInventory[pos1 + II_IDX_STRENGTH];
                                            }
                                            playerInventory[pos1 + II_IDX_WORN] = II_WORN_NO;
                                        }
                                    }

                                    playerInventory[selectedInventoryIndex + II_IDX_WORN] = II_WORN_YES;
                                    break;
                            }


                            // ------------------------------------------------
                            // Update Monsters

                            // loop over monsters

                            // NOTE player is always the first "monster" looked at
                            for (pos1 = LEVEL_MONSTERINST_PLAYER_IDX; pos1 < LEVEL_MONSTERINST_MAXINDEX; pos1 += MONSTERINST_SIZE) {
                                pos3 = levelRecords[pos1 + MI_IDX_RECORD];
                                if (pos3 != LEVEL_MONSTERINST_NUL_IDX) {
                                    // Handle monster actions
                                    x = levelRecords[pos1 + MI_IDX_X];
                                    bx = ax = (x - levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_X]);
                                    if (bx < 0) {
                                        bx = 0 - bx;
                                    }
                                    y = levelRecords[pos1 + MI_IDX_Y];
                                    by = ay = (y - levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_Y]);
                                    if (by < 0) {
                                        by = 0 - by;
                                    }
                                    cc = map[levelRecords[pos1 + MI_IDX_MAPPOS] + MAPREC_IDX_ROOM_ID];
                                    dd = map[levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_MAPPOS] + MAPREC_IDX_ROOM_ID];

                                    // If the monster is injured, not aggressive, and not a player, then they
                                    // switch to retreat mode.
                                    ee = gameRecords[pos3 + MR_IDX_AI];
                                    if (levelRecords[pos1 + MI_IDX_HEALTH] < levelRecords[pos1 + MI_IDX_RETREAT_HEALTH]) {
                                        levelRecords[pos1 + MI_IDX_AI_STATE] = AI_STATE_RETREAT;
                                    }
                                    if (levelRecords[pos1 + MI_IDX_HEALTH] < 0) {
                                        // Momentary state change to ensure
                                        // dead monsters don't do things.
                                        // This actually helps, because it means if
                                        // multiple monsters are moving, nothing will
                                        // step on a corpse when the corpse dies this turn.
                                        // Actual death check is below.
                                        levelRecords[pos1 + MI_IDX_AI_STATE] = AI_STATE_DEAD;
                                        levelRecords[pos1 + MI_IDX_AI_ACTION] = ACTION_NONE;
                                    }

                                    // used for delta direction
                                    aa = 1;
                                    switch (levelRecords[pos1 + MI_IDX_AI_STATE]) {
                                        case AI_STATE_PLAYER:
                                            x = playerNewX;
                                            y = playerNewY;
                                            break;
                                        case AI_STATE_SLEEP:
                                            if (cc == dd) {
                                                if (((ee & MONSTERREC_AI_SLEEPY_BIT) != 0 && (bx + by) < MONSTER_AI_DETECT_SLEEPY) ||
                                                        (bx + by) < MONSTER_AI_DETECT_ALERT) {
                                                    // wake up the monster
                                                    aa =  AI_STATE_CHARGE;
                                                    if (ee == MONSTERREC_AI_AFRAID) {
                                                        aa = AI_STATE_RETREAT;
                                                    }
                                                    if (ee == MONSTERREC_AI_RANDOM) {
                                                        aa = AI_STATE_RANDOM;
                                                    }
                                                    levelRecords[pos1 + MI_IDX_AI_STATE] = aa;
                                                }
                                                levelRecords[pos1 + MI_IDX_AI_ACTION] = ACTION_NONE;
                                            }
                                            break;
                                        case AI_STATE_RANDOM:
                                            // -1 to 1
                                            x += random.nextInt(3) - 2;
                                            y += random.nextInt(3) - 2;
                                            levelRecords[pos1 + MI_IDX_AI_ACTION] = ACTION_MOVE;
                                            break;
                                        case AI_STATE_RETREAT:
                                            // if the player is 1 space away,
                                            // attack back, otherwise retreat.
                                            // This extra logic seems to mess up the
                                            // movement.  Not only that, it's
                                            // kind of fun to attack a monster without
                                            // repercussions.
                                            //if ((bx + by) > 2) {
                                            aa = 0 - aa;
                                            //}
                                            // fall through with the correct
                                            // direction
                                        case AI_STATE_CHARGE:
                                            if (ax > 0) {
                                                // move closer by subtracting
                                                x -= aa;
                                            }
                                            if (ax < 0) {
                                                x += aa;
                                            }
                                            if (ay > 0) {
                                                // move closer by subtracting
                                                y -= aa;
                                            }
                                            if (ay < 0) {
                                                y += aa;
                                            }
                                            levelRecords[pos1 + MI_IDX_AI_ACTION] = ACTION_MOVE;
                                            break;
                                        // death: do nothing
                                    }
                                    pos2 = (x + y * mapWidth) * MAPREC_SIZE;

                                    // Don't need boundary checks for x & y -
                                    // those will always be a delta of 1 from original
                                    // position, the original position is in a passable
                                    // location, and that's bounded by a valid, impassible location.
                                    if (levelRecords[pos1 + MI_IDX_AI_ACTION] == ACTION_MOVE &&
                                            map[pos2 + MAPREC_IDX_PASSABLE] == MAPREC_PASSABLE_YES) {
                                        aa = map[pos2 + MAPREC_IDX_MONSTER_IDX];
                                        if (aa != LEVEL_MONSTERINST_NUL_IDX) {
                                            // pos1 attacks
                                            cc = 0; // text to print
                                            if (levelRecords[aa + MI_IDX_ARMOR] < levelRecords[pos1 + MI_IDX_ATTACK] + random.nextInt(levelRecords[pos1 + MI_IDX_ATTACK])) {
                                                bb = random.nextInt(levelRecords[pos1 + MI_IDX_ATTACK]) - levelRecords[aa + MI_IDX_ARMOR];
                                                if (bb < 0) {
                                                    bb = 0;
                                                }
                                                levelRecords[aa + MI_IDX_HEALTH] -= bb + 1;
                                                if (levelRecords[aa + MI_IDX_HEALTH] > 0) {
                                                    cc = TEXT_IDX_WAS_HIT;
                                                    // A bit extra space, but
                                                    // worth it for correct grammar
                                                    if (aa == LEVEL_MONSTERINST_PLAYER_IDX) {
                                                        cc = TEXT_IDX_WERE_HIT;
                                                    }
                                                } // else Death of aa, checked below
                                            } else {
                                                cc = TEXT_IDX_WAS_MISSED;
                                            }
                                            if (cc > 0) {
                                                bb = levelRecords[aa + MI_IDX_RECORD];
                                                a(message, gameRecords[bb + MR_IDX_COLOR_NAME]);
                                                a(message, gameRecords[bb + MR_IDX_NAME1]);
                                                a(message, gameRecords[bb + MR_IDX_NAME2]);
                                                a(message, cc);
                                            }
                                        } else {

                                            // No fight, so move
                                            map[levelRecords[pos1 + MI_IDX_MAPPOS] + MAPREC_IDX_MONSTER_IDX] = LEVEL_MONSTERINST_NUL_IDX;
                                            levelRecords[pos1 + MI_IDX_X] = x;
                                            levelRecords[pos1 + MI_IDX_Y] = y;
                                            levelRecords[pos1 + MI_IDX_MAPPOS] = pos2;
                                            map[pos2 + MAPREC_IDX_MONSTER_IDX] = pos1;

                                            if (pos1 == LEVEL_MONSTERINST_PLAYER_IDX) {
                                                playerMoved = true;
                                                // Update the window view
                                                windowX = x - (charactersPerLine / 2);
                                                if (windowX < 0) {
                                                    windowX = 0;
                                                }
                                                windowY = y - (guiLines  / 2);
                                                if (windowY < 0) {
                                                    windowY = 0;
                                                }
//System.out.println("Window: ("+windowX+","+windowY+")");
                                            }
                                        }
                                    }
                                }
                            }

                            // monster death loop
                            for (pos1 = LEVEL_MONSTERINST_FIRSTMONSTER_IDX; pos1 < LEVEL_MONSTERINST_MAXINDEX; pos1 += MONSTERINST_SIZE) {
                                if (levelRecords[pos1 + MI_IDX_RECORD] != LEVEL_MONSTERINST_NUL_IDX &&
                                        levelRecords[pos1 + MI_IDX_HEALTH] <= 0) {
                                    bb = levelRecords[pos1 + MI_IDX_RECORD];
                                    a(message, gameRecords[bb + MR_IDX_COLOR_NAME]);
                                    a(message, gameRecords[bb + MR_IDX_NAME1]);
                                    a(message, gameRecords[bb + MR_IDX_NAME2]);
                                    a(message, TEXT_IDX_WAS_KILLED);
                                    pos2 = (levelRecords[pos1 + MI_IDX_X] + (levelRecords[pos1 + MI_IDX_Y] * mapWidth)) * MAPREC_SIZE;
                                    map[pos2 + MAPREC_IDX_MONSTER_IDX] = LEVEL_MONSTERINST_NUL_IDX;
                                    levelRecords[pos1 + MI_IDX_RECORD] = LEVEL_MONSTERINST_NUL_IDX;

                                    if (map[pos2 + MAPREC_IDX_ITEM_IDX] == LEVEL_ITEMINST_NUL_IDX) {
                                        // drop money
                                        for (aa = LEVEL_ITEMINST_IDX; aa < LEVEL_ITEMINST_MAXINDEX; aa += ITEMINST_SIZE) {
                                            if (levelRecords[aa + II_IDX_RECORD] == GAME_ITEM_NUL_IDX) {
                                                levelRecords[aa + II_IDX_RECORD] = GAME_ITEMREC_IDX_MONEY;
                                                levelRecords[aa + II_IDX_STRENGTH] = levelRecords[pos1 + MI_IDX_ARMOR] + 1;
                                                map[pos2 + MAPREC_IDX_ITEM_IDX] = aa;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }


                            // ------------------------------------------------
                            // Update Status
                            status = new StringBuffer();
                            a(status, TEXT_IDX_HEALTH);
                            status.append(levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_HEALTH]);
                            a(status, TEXT_IDX_HUNGER);
                            status.append((playerFood * STATUS_HUNGER_MULT) / PLAYER_MAXIMUM_FOOD);
                            a(status, TEXT_IDX_GOLD_VAL);
                            status.append(playerMoney);
                            a(status, TEXT_IDX_ATTACK_VAL);
                            status.append(levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_ATTACK]);
                            a(status, TEXT_IDX_ARMOR_VAL);
                            status.append(levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_ARMOR]);
                        }


                        // --------------------------------------------------
                        // Update the visible rooms.  This should be done only
                        // when the player moves (for performance reasons),
                        // but it's done here in order
                        // to allow the game-start state to perform this
                        // logic, too, and put this logic in only one place.

                        // If the player is standing on a door or a path, the
                        // location is visible, but the location beside you
                        // may be invisible.  Need to check this whenever the
                        // player moves

                        if (playerMoved) {
                            pos1 = levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_MAPPOS];

                            // we haven't seen this place.  Update the seen
                            // values, depending on the dark and room ID.
                            bb = map[pos1 + MAPREC_IDX_ROOM_ID];
                            cc = map[pos1 + MAPREC_IDX_PATH_ID];

                            // search the whole map for rooms matching this

                            for (ay = 0; ay < mapHeight; ay++) {
                                for (ax = 0; ax < mapWidth; ax++) {
                                    pos2 = (ax + ay*mapWidth) * MAPREC_SIZE;
                                    if ((map[pos2 + MAPREC_IDX_ROOM_ID] == bb && bb != 0) ||
                                            (map[pos2 + MAPREC_IDX_PATH_ID] == cc && cc != 0)) {
                                        if (map[pos2 + MAPREC_IDX_LIGHT] == MAPREC_LIGHT_LIT ||
                                                (ax >= levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_X] - 1 && ax <= levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_X] + 1 &&
                                                ay >= levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_Y] - 1 && ay <= levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_Y] + 1)) {
                                            map[pos2 + MAPREC_IDX_SEEN] = MAPREC_SEEN_ALWAYS;
                                        }
                                    }
                                }
                            }
                        }

                        // -----------------------------------------------
                        // Test player death
                        if (levelRecords[LEVEL_MONSTERINST_PLAYER_IDX_HEALTH] <= 0) {
                            // Death
                            if (playerMoney > highestGold) {
                                highestGold = playerMoney;
                            }
                            if (level > highestLevel) {
                                highestLevel = level;
                            }
                            message = new StringBuffer();
                            a(message, TEXT_IDX_REACHED_MONEY);
                            message.append(playerMoney);
                            a(message, TEXT_IDX_REACHED_LEVEL);
                            message.append(level);
                            a(message, TEXT_IDX_HIGHEST_MONEY);
                            message.append(highestGold);
                            a(message, TEXT_IDX_HIGHEST_LEVEL);
                            message.append(highestLevel);
                            gameStarted = false;
                            continue startGame;
                        }


                        // ------------------------------------------------
                        // Draw the screen

                        if (displayState == DISPLAYSTATE_MAP || displayState == DISPLAYSTATE_PICKDIR) {
                                // current map
                                for (y = windowY; (y < windowY + guiLines) && (y < mapHeight); y++) {
                                    pos1 = (y * mapWidth + windowX) * MAPREC_SIZE;
                                    for (x = windowX; (x < windowX + drawWidth) && (x < mapWidth); x++, pos1 += MAPREC_SIZE) {
                                        // cc = color index
                                        // dd = display character
                                        if (map[pos1 + MAPREC_IDX_SEEN] != MAPREC_SEEN_NO) {
                                            aa = map[pos1 + MAPREC_IDX_MONSTER_IDX];
                                            if (aa != LEVEL_MONSTERINST_NUL_IDX) {
                                                // monster
                                                bb = levelRecords[aa + MI_IDX_RECORD];
                                                cc = gameRecords[bb + MR_IDX_COLOR_IDX];
                                                dd = gameRecords[bb + MR_IDX_DISPLAY];
                                            } else {
                                                aa = map[pos1 + MAPREC_IDX_ITEM_IDX];
                                                if (aa != LEVEL_ITEMINST_NUL_IDX) {
                                                    // item
                                                    bb = levelRecords[aa + II_IDX_RECORD];
                                                    cc = COLOR_IDX_ITEM; // gameRecords[bb + IR_IDX_COLOR];
                                                    dd = gameRecords[bb + IR_IDX_DISPLAY];
                                                } else {
                                                    // map
                                                    cc = map[pos1 + MAPREC_IDX_COLOR];
                                                    dd = map[pos1 + MAPREC_IDX_DISPLAY];
                                                    if (dd == 0) {
                                                        dd = DISPLAY_CHAR_BLANK;
                                                    }
                                                }
                                            }
//if (cc > COLOR_MAP.length) {
//    System.out.println("Error!");
//}
                                            renderChar[0] = (char) dd;
                                            ogr.setColor(COLOR_MAP[cc]);
                                            ogr.drawChars(renderChar, 0, 1,
                                                (x - windowX) * charWidth + BORDER_X,
                                                (y - windowY) * FONT_SIZE + BORDER_Y);
                                        }
                                    }
                                }

                        }
                        if (displayState == DISPLAYSTATE_DROP_INV ||
                                displayState == DISPLAYSTATE_USE_INV ||
                                displayState == DISPLAYSTATE_WEAR_INV) {
                                // Draw the inventory
                                aa = BORDER_Y + FONT_SIZE;
                                ogr.setColor(COLOR_MAP[COLOR_IDX_INV_TITLE]);
                                message = new StringBuffer();
                                a(message, TEXT_IDX_INV_TITLE);
                                ogr.drawString(message.toString(),
                                        BORDER_X, BORDER_Y + FONT_SIZE);

                                aa += FONT_SIZE * 2;
                                dd = 'a';
                                for (bb = 0; bb < PLAYER_INVENTORY_SIZE; bb += ITEMINST_SIZE) {
                                    cc = playerInventory[bb + II_IDX_RECORD];
                                    if (cc != GAME_ITEM_NUL_IDX) {
                                        ogr.setColor(COLOR_MAP[COLOR_IDX_INV_ITEMKEY]);
                                        renderChar[0] = (char) dd;
                                        ogr.drawChars(renderChar, 0, 1, BORDER_X, aa);
                                        message = new StringBuffer();
                                        if (gameRecords[cc + IR_IDX_IDENTIFIED] == 0) {
                                            a(message, gameRecords[cc + IR_IDX_COLOR] + TEXT_IDX_START_COLOR);
                                            a(message, gameRecords[cc + IR_IDX_NAME]);
                                        } else {
                                            a(message, gameRecords[cc + IR_IDX_NAME]);
                                            a(message, TEXT_IDX_OF);
                                            a(message, gameRecords[cc + IR_IDX_ACTION] + TEXT_IDX_START_ITEMACTION_NAME);
                                            a(message, TEXT_IDX_ITEM_STRENGTH);
                                            message.append(playerInventory[bb + II_IDX_STRENGTH]);
                                            a(message, TEXT_IDX_SPACE);
                                            message.append(playerInventory[bb + II_IDX_CHARGES]);
                                            a(message, TEXT_IDX_ITEM_CHARGES);
                                        }

                                        if (playerInventory[bb + II_IDX_WORN] == II_WORN_YES) {
                                            a(message, TEXT_IDX_INV_WORN);
                                        }
                                        ogr.setColor(COLOR_MAP[COLOR_IDX_INV_ITEMTEXT]);
                                        ogr.drawString(message.toString(),
                                                BORDER_X + charWidth * 5,
                                                aa);

                                        aa += FONT_SIZE;
                                    }
                                    // keep the letter assignment constant
                                    // by always increasing the letter, even
                                    // over blank spots in the inventory
                                    dd++;
                                }

                                message = new StringBuffer();
                                a(message, TEXT_IDX_INV_PICK);
                                a(message, TEXT_IDX_START_INV_DISPLAYSTATE +
                                                    displayState - DISPLAYSTATE_DROP_INV);
                                status = new StringBuffer();
                                a(status, TEXT_IDX_INV_PICK_POST);
                        }
                        //if displayState == DISPLAYSTATE_NEWGAME:
                        //    // do nothing
                        //    break;
                    }
                    // else game not started

                    // Draw the message & status
                    ogr.setColor(COLOR_MAP[COLOR_IDX_MESSAGE + (playerFood & 1)]);
                    ogr.drawString(message.toString(), BORDER_X, (guiLines + 1) * FONT_SIZE);
                    ogr.setColor(COLOR_MAP[COLOR_IDX_MESSAGE]);
                    ogr.drawString(status.toString(), BORDER_X, (guiLines + 2) * FONT_SIZE);

                    // ========================================================
                    // Render the frame and wait for the next frame



                    sg.drawImage(image,
                            0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT, // destination
                            0, 0, RENDER_WIDTH, RENDER_HEIGHT, // source
                            null);
                    do {
                        Thread.yield();
                    } while (System.nanoTime() - lastTime < 0);
                    if (!isActive()) return;

                    lastTime += NANO_WAIT_TIME;
                }
            }
        }
    }


    public boolean handleEvent(Event e) {
        // Process the key and mouse events.

//System.out.println(e.id + ": " + e.key + "; " + e.modifiers);
        if (e.id == Event.KEY_PRESS || e.id == Event.KEY_ACTION) {
            switch (e.key) {
                // directions
                case KEY_NW1:
                case KEY_NW2:
                    this.key |= KEY_DIR_NW;
                    break;
                case KEY_N1:
                case KEY_N2:
                    this.key |= KEY_DIR_N;
                    break;
                case KEY_NE1:
                case KEY_NE2:
                    this.key |= KEY_DIR_NE;
                    break;
                case KEY_W1:
                case KEY_W2:
                    this.key |= KEY_DIR_W;
                    break;
                case KEY_E1:
                case KEY_E2:
                    this.key |= KEY_DIR_E;
                    break;
                case KEY_SW1:
                case KEY_SW2:
                    this.key |= KEY_DIR_SW;
                    break;
                case KEY_S1:
                case KEY_S2:
                    this.key |= KEY_DIR_S;
                    break;
                case KEY_SE1:
                case KEY_SE2:
                    this.key |= KEY_DIR_SE;
                    break;

                // alternates
                case KEY_SELF2:
                    this.key = KEY_SELF1;
                    break;

                default:
                    //System.out.println(e.key);
                    // NOTE this clears out the direction flag!
                    this.key = e.key;
                    if (this.key >= 'A' && this.key <= 'Z') {
                        this.key = this.key - 'A' + 'a';
                    }
            }
        }
        return false;
    }



    private static final void a(StringBuffer sb, int index) {
        int x = 0;
        while (--index >= 0) {
            while (TEXTBUFFER.charAt(x++) != TEXT_SEPARATOR) {
            }
        }
        while (TEXTBUFFER.charAt(x) != TEXT_SEPARATOR) {
            sb.append(TEXTBUFFER.charAt(x++));
        }
    }

    private static final char TEXT_SEPARATOR = '\r';



    // -----------------------------------------------------------------------
    // Display Constants

    private static final int FONT_SIZE = 12;

    private static final int DISPLAY_WIDTH = 48 * FONT_SIZE;
    private static final int DISPLAY_HEIGHT = 48 * FONT_SIZE;
    private static final int RENDER_SCALING_FACTOR = 1;
    //private static final int RENDER_SHR_FACTOR = 0;
    private static final int RENDER_WIDTH = DISPLAY_WIDTH / RENDER_SCALING_FACTOR;
    private static final int RENDER_HEIGHT = DISPLAY_HEIGHT / RENDER_SCALING_FACTOR;
    private static final int RENDER_PIXEL_COUNT = RENDER_WIDTH * RENDER_HEIGHT;
    private static final int FRAMES_PER_SECOND = 5;
    private static final int NANO_WAIT_TIME = (1000000000 / FRAMES_PER_SECOND);

    // This may need to be merged in with a large single string, to reduce the
    // 6 (or so) extra bytes used to allocate the variable in the constant
    // pool, but the code to extract this string from the constant pool may be
    // more.
    private static final String FONT_NAME = "monospaced-plain-" + FONT_SIZE;

    private static final int BORDER_X = 2;
    private static final int BORDER_Y = FONT_SIZE / 2;
    private static final int CHAR_LINES = RENDER_HEIGHT / FONT_SIZE;

    // Display states: what the screen is showing
    private static final int DISPLAYSTATE_MAP = 0;
    private static final int DISPLAYSTATE_PICKDIR = 1;

        // the order of drop / use / wear must match the text inventory order
    private static final int DISPLAYSTATE_DROP_INV = 2;
    private static final int DISPLAYSTATE_USE_INV = 3;
    private static final int DISPLAYSTATE_WEAR_INV = 4;
    private static final int DISPLAYSTATE_START = 5;
    private static final int DISPLAYSTATE_NEWGAME = 6;


    // ------------------------------------------------------------------------



    // ------------------------------------------------------------------------
    // Map Display Constants

    // MAP DATA
    //   The "map" array stores 32-bits of information for each location.
    //
    //   Layout 0:
    //      bits 0-5: character code for the map location
    //      bits 6-7: map location color code
    //      bits 8-15: map location flags
    //      bits 16-23: room ID for location
    //      bits 24-31: path ID for location


    //   Layout 1:
    //   This new layout allows for a single pass on screen rendering, without needing
    //   to black out a rectangle after drawing.
    //      bits 0-3: character code for the map location (16 location values)
    //          (the Color lookup array will have values 0-15 (or whatever the max
    //          is) as the color value for the map character code.
    //      bit  4: flag: is the location passable?
    //      bit  5: flag: is the location dark?
    //      bit  6: flag: has the player seen this location?
    //      bits 7-12: monster ID lookup (maximum 62 monsters per level + player)
    //      bits 13-18: treasure ID lookup (maximum 63 treasures per level)
    //      bits 19-24: room ID (maximum 63 rooms per level)
    //      bits 25-30: path ID (maximum 63 paths per level)

    //      Notes on layout 1: that may be too big for room and path maximum,
    //      so reducing those down by half would free up 2 more flags.


    // Layout 2:
    // A problem with layout 1 is the amount of code used to shift and mask.  By
    // moving the map to a structure, we increase the code slightly by using either
    // +X or shl / shr on the map position calculation, but that only affects the
    // loops on the map, and finding the update position for the player and monsters.
    // It also opens up the number of values possible.

    //   index  meaning
    //      0   index into map display character
    //      1   color index
    //      2   flag: is passable? (0 or 1)
    //      3   flag: is the location dark?  This can have multiple values, where
    //              "dark" is wall/path-dark or room dark.
    //      4   flag: has the player seen this location?  Again, can have multiple
    //              values for the "dark" meaning.  Such as, 0 = not seen,
    //              1 = can see forever, 2 = seen but will be reset if the player
    //              moves away.
    //      5   Monster ID lookup.  0 = the NUL monster, 1 = player, 2+ = monster.
    //              Because this is an integer value now, this can point directly
    //              into the monster structure list, rather than an index that must
    //              be recalculated on each use.
    //      6   Treasure ID lookup. 0 = the NUL treasure, 1+ = treasure.
    //              This can work just like the Monster ID lookup.
    //      7   Room ID
    //      8   Path ID

    private static final int MAPREC_IDX_DISPLAY = 0;
    private static final int MAPREC_IDX_COLOR = 1;
    private static final int MAPREC_IDX_PASSABLE = 2;
    private static final int MAPREC_IDX_LIGHT = 3;
    private static final int MAPREC_IDX_SEEN = 4;
    private static final int MAPREC_IDX_MONSTER_IDX = 5;
    private static final int MAPREC_IDX_ITEM_IDX = 6;
    private static final int MAPREC_IDX_ROOM_ID = 7;
    private static final int MAPREC_IDX_PATH_ID = 8;
    private static final int MAPREC_SIZE = 9;

    private static final int MAPREC_PASSABLE_NO = 0;
    private static final int MAPREC_PASSABLE_YES = 1;

    private static final int MAPREC_LIGHT_LIT = 0;
    private static final int MAPREC_LIGHT_DIM = 1;

    private static final int MAPREC_SEEN_NO = 0;
    private static final int MAPREC_SEEN_ALWAYS = 1;

    private static final int MAPREC_ROOM_ID_NUL = 0;
    private static final int MAPREC_PATH_ID_NUL = 0;



    // -----------------------------------------------------------------------
    // Color lookups
    private static final int COLOR_VALUE_WALL = 0xc0c0c0; // wall
    private static final int COLOR_VALUE_PATH = 0x993300; // path
    private static final int COLOR_VALUE_ROOM = 0x404040; // room
    private static final int COLOR_VALUE_FEATURE = 0xffffff; // feature
    private static final int COLOR_VALUE_PLAYER = 0x3399ff; // player
    private static final int COLOR_VALUE_ITEM = 0x00ff00; // item
    private static final int COLOR_VALUE_MONSTER = 0xff0000; // monster
    private static final int COLOR_VALUE_BACKGROUND = 0; // background


    private static final int COLOR_IDX_WALL = 0;
    private static final int COLOR_IDX_INV_ITEMTEXT = 0;
    private static final int COLOR_IDX_PATH = 1;
    private static final int COLOR_IDX_ROOM = 2;
    private static final int COLOR_IDX_FEATURE = 3;
    private static final int COLOR_IDX_MESSAGE = 3;
    private static final int COLOR_IDX_PLAYER = 4;
    private static final int COLOR_IDX_INV_TITLE = 4;
    private static final int COLOR_IDX_MONSTER = 5;
    private static final int COLOR_IDX_ITEM = 6;
    private static final int COLOR_IDX_INV_ITEMKEY = 6;
    private static final int COLOR_IDX_BACKGROUND = 7;
    private static final int COLOR_COUNT = 8;
    private static final int COLOR_NAME_COUNT = 7; // exclude invisible / background


    private static final int DISPLAY_CHAR_BLANK = ' ';
    private static final int DISPLAY_CHAR_ROOM = '.';
    private static final int DISPLAY_CHAR_PATH = '\u2592';
    private static final int DISPLAY_CHAR_DOOR = '+';
        // no locked doors

    private static final int DISPLAY_CHAR_BOX_DOUBLE_HORIZ = '\u2550';
    private static final int DISPLAY_CHAR_BOX_DOUBLE_VERT = '\u2551';
    private static final int DISPLAY_CHAR_BOX_DOUBLE_NW = '\u2554';
    private static final int DISPLAY_CHAR_BOX_DOUBLE_NE = '\u2557';
    private static final int DISPLAY_CHAR_BOX_DOUBLE_SW = '\u255a';
    private static final int DISPLAY_CHAR_BOX_DOUBLE_SE = '\u255d';

    private static final int DISPLAY_CHAR_DOWNSTAIR = '>';
    private static final int DISPLAY_CHAR_MONEY = '$';

    //private static final int DISPLAY_CHAR_ITEMTYPE_START = 12;

    //private static final int DISPLAY_CHAR_ARMOR = '[';
    //private static final int DISPLAY_CHAR_WEAPON = '/';
    //private static final int DISPLAY_CHAR_WAND = '*';
    //private static final int DISPLAY_CHAR_POTION = '!';


    private static final int DISPLAY_CHAR_PLAYER = '@';


    // -----------------------------------------------------------------------
    // Status display constants
    private static final int STATUS_HUNGER_MULT = 100;
    private static final int ITEM_HUNGER_MULT = 20; // TODO balance



    // -----------------------------------------------------------------------
    // Player Condition Values

    private static final int PLAYER_STARTING_HEALTH = 100;
    private static final int PLAYER_MAXIMUM_HEALTH = 100;
    private static final int PLAYER_MAXIMUM_FOOD = 1000;
    private static final int PLAYER_STARTING_FOOD = 500;
    private static final int PLAYER_STARTING_ARMOR = 0;
    private static final int PLAYER_INVENTORY_COUNT = 26; // RENDER_HEIGHT / 12 must be more than letters in alphabet
    private static final int PLAYER_STARTING_ATTACK = 4;


    // -----------------------------------------------------------------------
    // Map Generation Values

    private static final int MAP_CELLSIZE_X = 16;
    private static final int MAP_CELLSIZE_Y = 16;

    private static final int MAP_CELLSIZE_DELTA = 9;

    private static final int MAP_CELLSIZE_X_SIZE_GEN = MAP_CELLSIZE_X - MAP_CELLSIZE_DELTA;
    private static final int MAP_CELLSIZE_Y_SIZE_GEN = MAP_CELLSIZE_Y - MAP_CELLSIZE_DELTA;

    private static final int MAP_CELLSIZE_X_POS_GEN = MAP_CELLSIZE_X - MAP_CELLSIZE_DELTA + 3;
    private static final int MAP_CELLSIZE_Y_POS_GEN = MAP_CELLSIZE_Y - MAP_CELLSIZE_DELTA + 3;

    private static final int MAP_MAX_X_CELLS = 20;
    private static final int MAP_MAX_Y_CELLS = 20;
    private static final int MAP_MAX_CELLS = MAP_MAX_X_CELLS * MAP_MAX_Y_CELLS;

    private static final int MAP_CELL_IDX_CONNECTED = 0;
    private static final int MAP_CELL_IDX_SEED_X = 1;
    private static final int MAP_CELL_IDX_SEED_Y = 2;
    private static final int MAP_CELL_IDX_DIM_X = 3;
    private static final int MAP_CELL_IDX_DIM_Y = 4;
    private static final int MAP_CELL_IDX_DIM_W = 5;
    private static final int MAP_CELL_IDX_DIM_H = 6;

    private static final int MAP_CELL_SIZE = 7;

    private static final int MAP_CELL_CONNECTION_DIV = 3;

    // game balance
    // this has a HUGE impact on how fun the game is.
    private static final int MAP_CELLHEIGHT_BASE = 2;
    private static final int MAP_CELLHEIGHT_MULTIPLIER = 2;
    private static final int MAP_CELLHEIGHT_DIVISOR = 3;

    private static final int MAP_CELLWIDTH_BASE = 2;
    private static final int MAP_CELLWIDTH_MULTIPLIER = 1;
    private static final int MAP_CELLWIDTH_DIVISOR = 2;

    // It's supposed to be +2, but give it some extra freedom to eliminate
    // the nagging ArrayIndexOutOfRange error.
    private static final int MAP_SIZE = (
            ((MAP_MAX_X_CELLS * MAP_CELLSIZE_X) + 10) *
            ((MAP_MAX_Y_CELLS * MAP_CELLSIZE_Y) + 10)) * MAPREC_SIZE;


    // -----------------------------------------------------------------------
    // Monster Record Struct

    private static final int MR_IDX_DISPLAY = 0; // display character lookup
    private static final int MR_IDX_COLOR_IDX = 1; // lookup for the display color
    private static final int MR_IDX_COLOR_NAME = 2; // lookup for the name color
    private static final int MR_IDX_NAME1 = 3; // lookup for the name part 1
    private static final int MR_IDX_NAME2 = 4; // lookup for the name part 2
    private static final int MR_IDX_AI = 5; // AI type

    private static final int MONSTERREC_SIZE = 7;

    private static final int MONSTERREC_AI_PLAYER = 0;
    private static final int MONSTERREC_AI_AGGRESSIVE = 2;
    private static final int MONSTERREC_AI_AGGRESSIVE_SLEEPY = 3;
    private static final int MONSTERREC_AI_RANDOM = 4;
    private static final int MONSTERREC_AI_RANDOM_SLEEPY = 5;
    private static final int MONSTERREC_AI_AFRAID = 6;
    private static final int MONSTERREC_AI_AFRAID_SLEEPY = 7;
    private static final int MONSTERREC_AI_COUNT = 8;

    private static final int MONSTERREC_AI_RANDOMCHANCE = (MONSTERREC_AI_COUNT / 2) - 1;

    private static final int MONSTERREC_AI_SLEEPY_BIT = 1;
    private static final int MONSTERREC_AI_AGGRESSIVE_SHL = 1;


    private static final int MONSTER_AI_DETECT_SLEEPY = 6;
    private static final int MONSTER_AI_DETECT_ALERT = 12;





    // -----------------------------------------------------------------------
    // Monster Instance Struct

    private static final int MI_IDX_RECORD = 0; // pointer into the monster record struct
    private static final int MI_IDX_HEALTH = 1;
    private static final int MI_IDX_RETREAT_HEALTH = 2;
    private static final int MI_IDX_ATTACK = 3;
    private static final int MI_IDX_ARMOR = 4;
    private static final int MI_IDX_X = 5;
    private static final int MI_IDX_Y = 6;
    private static final int MI_IDX_MAPPOS = 7;
    private static final int MI_IDX_AI_STATE = 8;
    private static final int MI_IDX_AI_ACTION = 9; // see ACTION_x below (player's actions)
    private static final int MONSTERINST_SIZE = 10;


    private static final int AI_STATE_PLAYER = 0;
    private static final int AI_STATE_SLEEP = 1;
    private static final int AI_STATE_RANDOM = 2;
    private static final int AI_STATE_RETREAT = 3;
    private static final int AI_STATE_CHARGE = 4;
    private static final int AI_STATE_DEAD = 5;



    // -----------------------------------------------------------------------
    // Item / Treasure Record Struct

    private static final int IR_IDX_DISPLAY = 0; // display character lookup
    private static final int IR_IDX_COLOR = 1; // lookup for the display and name color.  Name color is displayed when the item is not identified.
    private static final int IR_IDX_NAME = 2; // lookup for the name
    private static final int IR_IDX_TYPE = 3; // armor, weapon, wand, potion, scroll
    private static final int IR_IDX_IDENTIFIED = 4; // was this item identified?  this is global for all the types
    private static final int IR_IDX_ACTION = 5; // the kind of action it performs when "used"
    private static final int IR_IDX_CHANCE = 6; // chance to find this item in a room, X / 100.
    private static final int ITEMREC_SIZE = 7;


    private static final int ITEMTYPE_ARMOR = 0;
    private static final int ITEMTYPE_WEAPON = 1;
    private static final int ITEMTYPE_WAND = 2;
    private static final int ITEMTYPE_POTION = 3;
    private static final int ITEMTYPE_COUNT = 4;
    private static final int ITEMTYPE_MONEY = 5; // special type


    private static final int ITEMACTION_FOOD = 0;
    private static final int ITEMACTION_NONE = 1;
    private static final int ITEMACTION_DAMAGE = 2;
    private static final int ITEMACTION_HEAL = 3;
    private static final int ITEMACTION_COUNT = 4;



    // -----------------------------------------------------------------------
    // Item / Treasure Instance Struct

    private static final int II_IDX_RECORD = 0; // pointer into the item record struct
    private static final int II_IDX_CHARGES = 1; // number of charges for the item.  Scrolls are always 1 for this.
    private static final int II_IDX_STRENGTH = 2; // how strong the item is - +armor for armor type, +attack for weapon, +damage for wand, +heal for potion or scroll, etc
    private static final int II_IDX_WORN = 3;
    private static final int ITEMINST_SIZE = 4;

    private static final int PLAYER_INVENTORY_SIZE = PLAYER_INVENTORY_COUNT * ITEMINST_SIZE;

    private static final int II_WORN_NO = 0;
    private static final int II_WORN_YES = 1;


    // -----------------------------------------------------------------------
    // Per-Game Data Array

    private static final int GAME_MONSTERREC_IDX = 0;
    private static final int GAME_MONSTERREC_PLAYER_IDX = GAME_MONSTERREC_IDX + 1;
    private static final int GAME_MONSTERREC_FIRSTMONSTER_IDX = MONSTERREC_SIZE;
    private static final int GAME_MONSTERREC_MONSTERTYPE_COUNT = 10;
    private static final int GAME_MONSTERREC_COUNT = GAME_MONSTERREC_MONSTERTYPE_COUNT * COLOR_COUNT;
    private static final int GAME_MONSTERREC_LISTEND_IDX = GAME_MONSTERREC_IDX +
            MONSTERREC_SIZE * (GAME_MONSTERREC_COUNT + 1);
    private static final int GAME_ITEMREC_IDX = GAME_MONSTERREC_LISTEND_IDX;
    private static final int GAME_ITEMREC_IDX_MONEY = GAME_ITEMREC_IDX;
    private static final int TEXT_ITEMTYPE_NAME_COUNT = 4; // out of place, but required here

    private static final int GAME_ITEMREC_COUNT = 1 + ITEMTYPE_COUNT * ITEMACTION_COUNT * TEXT_ITEMTYPE_NAME_COUNT;
    private static final int GAME_ITEMREC_LISTEND_IDX = GAME_ITEMREC_IDX +
            GAME_ITEMREC_COUNT * ITEMREC_SIZE;
    private static final int GAME_LENGTH = GAME_ITEMREC_LISTEND_IDX;

    private static final int GAME_ITEM_NUL_IDX = 0;

    private static final int GAME_MONSTERREC_PLAYER_IDX_DISPLAY =
            GAME_MONSTERREC_PLAYER_IDX + MR_IDX_DISPLAY;
    private static final int GAME_MONSTERREC_PLAYER_IDX_COLOR_IDX =
            GAME_MONSTERREC_PLAYER_IDX + MR_IDX_COLOR_IDX;
    private static final int GAME_MONSTERREC_PLAYER_IDX_NAME1 =
            GAME_MONSTERREC_PLAYER_IDX + MR_IDX_NAME1;
    private static final int GAME_MONSTERREC_PLAYER_IDX_NAME2 =
            GAME_MONSTERREC_PLAYER_IDX + MR_IDX_NAME2;
    private static final int GAME_MONSTERREC_PLAYER_IDX_AI =
            GAME_MONSTERREC_PLAYER_IDX + MR_IDX_AI;



    // We roll the dice once to see what is found, and if the number is > last
    // one and <= this one, then this is the object we're looking for.
    // We want a greater chance to find ordinary items.
    // There are GAME_ITEMREC_COUNT total items.
    // ITEMTYPE_COUNT * TEXT_ITEMTYPE_NAME_COUNT for ordinary
    // (GAME_ITEMREC_COUNT - ITEMTYPE_COUNT * TEXT_ITEMTYPE_NAME_COUNT - 1)
    //   for rest.
    // The total should be ITEMCHANCE_DESIRED_TOTAL, but ends up being
    // ITEMCHANCE_TOTAL
    private static final int ITEMCHANCE_DESIRED_TOTAL = 300;

    private static final int ITEMCHANCE_GOLD = 100; // 1/3

    // (ITEMTYPE_COUNT * TEXT_ITEMTYPE_NAME_COUNT) - 30% chance to find one.
    private static final int ITEMCHANCE_ORDINARY_COUNT =
            ITEMTYPE_COUNT * TEXT_ITEMTYPE_NAME_COUNT;
    private static final int ITEMCHANCE_ORDINARY =
            (ITEMCHANCE_DESIRED_TOTAL / 3) // 30% chance to find one
            / ITEMCHANCE_ORDINARY_COUNT;
    private static final int ITEMCHANCE_ORDINARY_TOTAL =
            ITEMCHANCE_ORDINARY_COUNT * ITEMCHANCE_ORDINARY;
    private static final int ITEMCHANCE_SPECIAL_COUNT =
            GAME_ITEMREC_COUNT - ITEMCHANCE_ORDINARY_COUNT - 1;
    private static final int ITEMCHANCE_SPECIAL =
            (ITEMCHANCE_DESIRED_TOTAL - ITEMCHANCE_GOLD - ITEMCHANCE_ORDINARY_TOTAL) /
            ITEMCHANCE_SPECIAL_COUNT;
    private static final int ITEMCHANCE_ORDINARY_SPECIAL_DIFF =
            ITEMCHANCE_ORDINARY - ITEMCHANCE_SPECIAL;

    private static final int ITEMCHANCE_TOTAL =
            ITEMCHANCE_GOLD +
            ITEMCHANCE_ORDINARY_TOTAL +
            (ITEMCHANCE_SPECIAL * ITEMCHANCE_SPECIAL_COUNT);



    private static final int MONSTERCHANCE_FIND = 2;



    // -----------------------------------------------------------------------
    // Per-Level Data Array

    private static final int LEVEL_MONSTERINST_IDX = 0;
    private static final int LEVEL_MONSTERINST_NUL_IDX = 0;
    private static final int LEVEL_MONSTERINST_PLAYER_IDX = LEVEL_MONSTERINST_IDX +
            MONSTERINST_SIZE;
    private static final int LEVEL_MONSTERINST_FIRSTMONSTER_IDX = LEVEL_MONSTERINST_IDX +
            MONSTERINST_SIZE * 2;
    private static final int LEVEL_MONSTERINST_COUNT = MAP_MAX_CELLS; // maximum # of monsters per level
    private static final int LEVEL_MONSTERINST_MAXINDEX = MONSTERINST_SIZE * LEVEL_MONSTERINST_COUNT;
    private static final int LEVEL_ITEMINST_IDX = LEVEL_MONSTERINST_MAXINDEX;

    private static final int LEVEL_ITEMINST_NUL_IDX = 0;
    private static final int LEVEL_ITEMINST_COUNT = 255 + PLAYER_INVENTORY_COUNT + MAP_MAX_CELLS; // maximum # of treasure per level + space for the player to drop things + gold per monster
    private static final int LEVEL_ITEMINST_MAXINDEX = LEVEL_ITEMINST_IDX +
            LEVEL_ITEMINST_COUNT * ITEMREC_SIZE;

    private static final int LEVEL_MAPGEN_START_IDX =
        LEVEL_ITEMINST_MAXINDEX;
    // + 2: this gives us a buffer around the border to make the "if" checks
    // simpler.
    private static final int LEVEL_MAPGEN_TEMPSPACE =
            ((MAP_MAX_X_CELLS + 2) * (MAP_MAX_Y_CELLS + 2)) * MAP_CELL_SIZE;
    private static final int LEVEL_MAPGEN_MAXINDEX =
            LEVEL_MAPGEN_START_IDX + LEVEL_MAPGEN_TEMPSPACE;

    private static final int LEVEL_LENGTH = LEVEL_MAPGEN_MAXINDEX;

    private static final int LEVEL_MONSTERINST_PLAYER_IDX_RECORD =
            LEVEL_MONSTERINST_PLAYER_IDX + MI_IDX_RECORD;
    private static final int LEVEL_MONSTERINST_PLAYER_IDX_HEALTH =
            LEVEL_MONSTERINST_PLAYER_IDX + MI_IDX_HEALTH;
    private static final int LEVEL_MONSTERINST_PLAYER_IDX_RETREAT_HEALTH =
        LEVEL_MONSTERINST_PLAYER_IDX + MI_IDX_RETREAT_HEALTH;
    private static final int LEVEL_MONSTERINST_PLAYER_IDX_ATTACK =
            LEVEL_MONSTERINST_PLAYER_IDX + MI_IDX_ATTACK;
    private static final int LEVEL_MONSTERINST_PLAYER_IDX_ARMOR =
            LEVEL_MONSTERINST_PLAYER_IDX + MI_IDX_ARMOR;
    private static final int LEVEL_MONSTERINST_PLAYER_IDX_X =
            LEVEL_MONSTERINST_PLAYER_IDX + MI_IDX_X;
    private static final int LEVEL_MONSTERINST_PLAYER_IDX_Y =
            LEVEL_MONSTERINST_PLAYER_IDX + MI_IDX_Y;
    private static final int LEVEL_MONSTERINST_PLAYER_IDX_MAPPOS =
            LEVEL_MONSTERINST_PLAYER_IDX + MI_IDX_MAPPOS;
    private static final int LEVEL_MONSTERINST_PLAYER_IDX_STATE =
            LEVEL_MONSTERINST_PLAYER_IDX + MI_IDX_AI_STATE;
    private static final int LEVEL_MONSTERINST_PLAYER_IDX_ACTION =
            LEVEL_MONSTERINST_PLAYER_IDX + MI_IDX_AI_ACTION;


    // -----------------------------------------------------------------------
    // Player Action Types

    private static final int ACTION_NONE = 0;
    private static final int ACTION_MOVE = 1;
    private static final int ACTION_USEDIR = 2;
    private static final int ACTION_TAKE = 3;
    private static final int ACTION_DROP = 4;
    private static final int ACTION_WEAR = 5;


    // -----------------------------------------------------------------------
    // Text Struct

    // This may not be necessary?

    private static final int T_IDX_POS = 0; // position in the string of the text
    private static final int T_IDX_LENGTH = 1; // length of the string

    // FIXME temporary place for the strings
    private static final String TEXTBUFFER =
        " " + TEXT_SEPARATOR +                               // TEXT_IDX_SPACE
        "Press Any Key To Start" + TEXT_SEPARATOR +          // TEXT_IDX_PRESS_ANY_KEY
        "*** You died with " + TEXT_SEPARATOR +              // TEXT_IDX_REACHED_MONEY
        " gold on level " + TEXT_SEPARATOR +                 // TEXT_IDX_REACHED_LEVEL
        "You" + TEXT_SEPARATOR +                             // TEXT_IDX_PLAYERNAME1
        "You descend down the stairs, and the passage closes behind you. " + TEXT_SEPARATOR + // TEXT_IDX_DOWN
        " of " + TEXT_SEPARATOR +                            // TEXT_IDX_OF
        "Health: " + TEXT_SEPARATOR +                        // TEXT_IDX_HEALTH
        "%    Food: " + TEXT_SEPARATOR +                     // TEXT_IDX_HUNGER
        "You are starving. " + TEXT_SEPARATOR +              // TEXT_IDX_STARVING
        "You are carrying too much. " + TEXT_SEPARATOR +     // TEXT_IDX_OVERBURDENED
        "Nothing to take. " + TEXT_SEPARATOR +               // TEXT_IDX_NOTAKE
        "You are carrying:" + TEXT_SEPARATOR +               // TEXT_IDX_INV_TITLE
        " (worn)" + TEXT_SEPARATOR +                         // TEXT_IDX_INV_WORN
        "Pick the letter for the item to " + TEXT_SEPARATOR + // TEXT_IDX_INV_PICK
        "(any other key to cancel)" + TEXT_SEPARATOR +       // TEXT_IDX_INV_PICK_POST
        " taken. " + TEXT_SEPARATOR +                        // TEXT_IDX_TAKEN
        "Canceled. " + TEXT_SEPARATOR +                      // TEXT_IDX_CANCELED
        "There is no place on the ground to put that. " + TEXT_SEPARATOR + // TEXT_IDX_CANT_DROP
        " dropped. " + TEXT_SEPARATOR +                      // TEXT_IDX_DROPPED
        "%    Gold: " + TEXT_SEPARATOR +                     // TEXT_IDX_GOLD_VAL
        "    Attack: " + TEXT_SEPARATOR +                    // TEXT_IDX_ATTACK_VAL
        "    Armor: " + TEXT_SEPARATOR +                     // TEXT_IDX_ARMOR_VAL
        "was hit. " + TEXT_SEPARATOR +                       // TEXT_IDX_WAS_HIT
        "died. " + TEXT_SEPARATOR +                          // TEXT_IDX_WAS_KILLED
        "dodged. " + TEXT_SEPARATOR +                        // TEXT_IDX_WAS_MISSED
        " +" + TEXT_SEPARATOR +                              // TEXT_IDX_ITEM_STRENGTH
        " charges" + TEXT_SEPARATOR +                        // TEXT_IDX_ITEM_CHARGES
        " and it vanishes. " + TEXT_SEPARATOR +              // TEXT_IDX_ITEM_DISAPPEARS
        " Gold" + TEXT_SEPARATOR +                           // TEXT_IDX_GOLD_NAME
        "Pick a direction. " + TEXT_SEPARATOR +              // TEXT_IDX_PICKDIR
        ". " + TEXT_SEPARATOR +                              // TEXT_IDX_EOS
        "were hit. " + TEXT_SEPARATOR +                      // TEXT_IDX_WERE_HIT
        ". Record Gold: " + TEXT_SEPARATOR +                 // TEXT_IDX_HIGHEST_MONEY
        ". Record Level: " + TEXT_SEPARATOR +                // TEXT_IDX_HIGHEST_LEVEL

        // TEXT_IDX_START_INV_DISPLAYSTATE
        "drop" + TEXT_SEPARATOR +
        "use" + TEXT_SEPARATOR +
        "wear" + TEXT_SEPARATOR +

        // TEXT_IDX_START_COLOR
        "Silver " + TEXT_SEPARATOR +                          // COLOR_IDX_WALL = 0;
        "Brown " + TEXT_SEPARATOR +                           // COLOR_IDX_PATH = 1;
        "Grim " + TEXT_SEPARATOR +                            // COLOR_IDX_ROOM = 2;
        "Glistening " + TEXT_SEPARATOR +                      // COLOR_IDX_FEATURE = 3;
        "Putrid " + TEXT_SEPARATOR +                          // COLOR_IDX_ITEM = 4;
        "Bloody " + TEXT_SEPARATOR +                          // COLOR_IDX_MONSTER = 5;
        "Turquoise " + TEXT_SEPARATOR +                       // COLOR_IDX_PLAYER = 6;

        // TEXT_IDX_START_MONSTER - none should start with the same letter
        "Badger " + TEXT_SEPARATOR +
        "Hog " + TEXT_SEPARATOR +
        "Gopher " + TEXT_SEPARATOR +
        "Snake " + TEXT_SEPARATOR +
        "Wolf " + TEXT_SEPARATOR +
        "Quail " + TEXT_SEPARATOR +


        // TEXT_IDX_START_ITEMTYPE
            // ITEMTYPE_ARMOR = 0;
        "Dou" + TEXT_SEPARATOR +
        "Cuirass" + TEXT_SEPARATOR +
        "Breastplate" + TEXT_SEPARATOR +
        "Linothorax" + TEXT_SEPARATOR +

            // ITEMTYPE_WEAPON = 1;
        "Dagger" + TEXT_SEPARATOR +
        "Mace" + TEXT_SEPARATOR +
        "Axe" + TEXT_SEPARATOR +
        "Sword" + TEXT_SEPARATOR +

            // ITEMTYPE_WAND = 2;
        "Talisman" + TEXT_SEPARATOR +
        "Wand" + TEXT_SEPARATOR +
        "Holy Symbol" + TEXT_SEPARATOR +
        "Staff" + TEXT_SEPARATOR +

            // ITEMTYPE_POTION = 3;
        "Muffin" + TEXT_SEPARATOR +
        "Scroll" + TEXT_SEPARATOR +
        "Potion" + TEXT_SEPARATOR +
        "Pizza" + TEXT_SEPARATOR +

        // TEXT_IDX_START_ITEMACTION_NAME

            // ITEMACTION_FOOD = 0;
        "Filling" + TEXT_SEPARATOR +

            // ITEMACTION_NONE = 1;
        "Normality" + TEXT_SEPARATOR +

            // ITEMACTION_DAMAGE = 2;
        "Injury" + TEXT_SEPARATOR +

            // ITEMACTION_HEAL = 3;
        "Healing" + TEXT_SEPARATOR +

        // TEXT_IDX_START_ITEMACTION_VERB

            // ITEMACTION_FOOD = 0;
        " is eaten by " + TEXT_SEPARATOR +

            // ITEMACTION_NONE = 1;
        " fizzles" + TEXT_SEPARATOR +

            // ITEMACTION_DAMAGE = 2;
        " hurts " + TEXT_SEPARATOR +

            // ITEMACTION_HEAL = 3;
        " heals " + TEXT_SEPARATOR +

            // TEXT_POS_MONSTER_NAME_CHARS
        "BHGSWQ" +

            // TEXT_POS_DISPLAY_ITEM_CHARS
        "[/*!";


    private static final int TEXT_IDX_SPACE = 0;
    private static final int TEXT_IDX_PRESS_ANY_KEY = 1;
    private static final int TEXT_IDX_REACHED_MONEY = 2;
    private static final int TEXT_IDX_REACHED_LEVEL = 3;
    private static final int TEXT_IDX_PLAYERNAME1 = 4;
    private static final int TEXT_IDX_PLAYERNAME2 = 0; // EMPTY
    private static final int TEXT_IDX_DOWN = 5;
    private static final int TEXT_IDX_OF = 6;
    private static final int TEXT_IDX_HEALTH = 7;
    private static final int TEXT_IDX_HUNGER = 8;
    private static final int TEXT_IDX_STARVING = 9;
    private static final int TEXT_IDX_OVERBURDENED = 10;
    private static final int TEXT_IDX_NOTAKE = 11;
    private static final int TEXT_IDX_INV_TITLE = 12;
    private static final int TEXT_IDX_INV_WORN = 13;
    private static final int TEXT_IDX_INV_PICK = 14;
    private static final int TEXT_IDX_INV_PICK_POST = 15;
    private static final int TEXT_IDX_TAKEN = 16;
    private static final int TEXT_IDX_CANCELED = 17;
    private static final int TEXT_IDX_CANT_DROP = 18;
    private static final int TEXT_IDX_DROPPED = 19;
    private static final int TEXT_IDX_GOLD_VAL = 20;
    private static final int TEXT_IDX_ATTACK_VAL = 21;
    private static final int TEXT_IDX_ARMOR_VAL = 22;
    private static final int TEXT_IDX_WAS_HIT = 23;
    private static final int TEXT_IDX_WAS_KILLED = 24;
    private static final int TEXT_IDX_WAS_MISSED = 25;
    private static final int TEXT_IDX_ITEM_STRENGTH = 26;
    private static final int TEXT_IDX_ITEM_CHARGES = 27;
    private static final int TEXT_IDX_ITEM_DISAPPEARS = 28;
    private static final int TEXT_IDX_GOLD_NAME = 29;
    private static final int TEXT_IDX_PICKDIR = 30;
    private static final int TEXT_IDX_EOS = 31;
    private static final int TEXT_IDX_WERE_HIT = 32;
    private static final int TEXT_IDX_HIGHEST_MONEY = 33;
    private static final int TEXT_IDX_HIGHEST_LEVEL = 34;
    private static final int TEXT_IDX_START_INV_DISPLAYSTATE = 35;

    private static final int TEXT_IDX_START_COLOR = TEXT_IDX_START_INV_DISPLAYSTATE + 3;
    private static final int TEXT_IDX_START_MONSTER = TEXT_IDX_START_COLOR + COLOR_NAME_COUNT;
    private static final int TEXT_MONSTER_NAME_COUNT = 6;
    private static final int TEXT_MONSTER_NAME_COUNT_HALF = TEXT_MONSTER_NAME_COUNT / 2;
    private static final int TEXT_IDX_START_ITEMTYPE = TEXT_IDX_START_MONSTER + TEXT_MONSTER_NAME_COUNT;
    //private static final int TEXT_ITEMTYPE_NAME_COUNT = 4; // number of names per item type - defined above
    private static final int TEXT_IDX_START_ITEMACTION_NAME = TEXT_IDX_START_ITEMTYPE +
        (TEXT_ITEMTYPE_NAME_COUNT * ITEMTYPE_COUNT);
    private static final int TEXT_IDX_START_ITEMACTION_VERB = TEXT_IDX_START_ITEMACTION_NAME + ITEMACTION_COUNT;
    private static final int TEXT_IDX_ITEMACTION_VERB_NOTHING = TEXT_IDX_START_ITEMACTION_VERB + ITEMACTION_NONE;

    // NOTE if the text ever changes, this length must be recomputed
    private static final int TEXT_LENGTH = 861; // TEXTBUFFER.length();
    private static final int TEXT_POS_DISPLAY_ITEM_CHARS = TEXT_LENGTH - ITEMTYPE_COUNT;
    private static final int TEXT_POS_MONSTER_NAME_CHARS = TEXT_POS_DISPLAY_ITEM_CHARS - TEXT_MONSTER_NAME_COUNT;


    // -----------------------------------------------------------------------
    // Key Commands

    //      nothing was pushed
    private static final int KEY_NOTHING = 0;
    private static final int KEY_INITIALIZE = 1; // special internal key - make a move, but do nothing

    //   Directions are bits on the upper (never used) parts of the key value
    //   This allows for a user to push two keys to simulate a diagonal movement.
    private static final int KEY_NODIR_MASK = 0xffff;
    private static final int KEY_DIR_N = 0x10000;
    private static final int KEY_DIR_E = 0x20000;
    private static final int KEY_DIR_S = 0x40000;
    private static final int KEY_DIR_W = 0x80000;

    private static final int KEY_DIR_NE = KEY_DIR_N | KEY_DIR_E;
    private static final int KEY_DIR_NW = KEY_DIR_N | KEY_DIR_W;
    private static final int KEY_DIR_SE = KEY_DIR_S | KEY_DIR_E;
    private static final int KEY_DIR_SW = KEY_DIR_S | KEY_DIR_W;
    private static final int KEY_DIR_MASK = KEY_DIR_N | KEY_DIR_E | KEY_DIR_W | KEY_DIR_S;

    private static final int KEY_NW1 = 1000; // home
    private static final int KEY_NW2 = 55; // '7'

    private static final int KEY_N1 = 1004; // up arrow
    private static final int KEY_N2 = 56; // '8'

    private static final int KEY_NE1 = 1002; // PgUp
    private static final int KEY_NE2 = 57; // '9'

    private static final int KEY_W1 = 1006; // left arrow
    private static final int KEY_W2 = 52; // '4'

    private static final int KEY_SELF1 = 46; // '.'
    private static final int KEY_SELF2 = 53; // '5'

    private static final int KEY_E1 = 1007; // right arrow
    private static final int KEY_E2 = 54; // '6'

    private static final int KEY_SW1 = 1001; // End
    private static final int KEY_SW2 = 49; // '1'

    private static final int KEY_S1 = 1005; // down arrow
    private static final int KEY_S2 = 50; // '2'

    private static final int KEY_SE1 = 1003; // PgDn
    private static final int KEY_SE2 = 51; // '3'

    //      Non-directional keys

    private static final int KEY_USE1 = 'u';

    private static final int KEY_DOWN1 = '>';

    private static final int KEY_TAKE1 = 'g';

    private static final int KEY_DROP1 = 'd';

    private static final int KEY_WEAR1 = 'w';

    private static final int DISPLAY_UPPERCASE = 'a' - 'A';
}
