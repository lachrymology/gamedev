package fogus.patagonia.doodads;

import java.awt.Event;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Random;

// http://java4k.com/index.php?action=games&method=view&gid=400

public class General
    extends java.applet.Applet
{
    private final static byte HEX_HEIGHT = 46;
    private final static byte HEX_WIDTH = 60;
    private final static byte MAP_COLUMNS = 17;
    private final static byte MAP_ROWS = 10;
    private final static int MAP_WIDTH = (1 + (MAP_COLUMNS) * 3 / 4)
 * HEX_WIDTH;
    private final static int MAP_HEIGHT = MAP_ROWS * HEX_HEIGHT
 + HEX_HEIGHT;
    private final static int MAP_FIRST_COLUMN_CENTER_X = HEX_WIDTH / 2;
    private final static int MAP_COLUMN_DISTANCE = HEX_WIDTH * 3 / 4;
    private final static int MAP_EVEN_ROW_LAYER = HEX_HEIGHT / 2;
    private final static int MAP_HEXES = MAP_COLUMNS * MAP_ROWS;
    private final static byte NO_UNIT = 0;
    private final static byte INFANTRY = 1;
    private final static byte TANK = 2;
    private final static byte ARTILLERY = 3;
    private final static byte ENGINEER = 4;
    private final static byte NR_UNIT_TYPES = ENGINEER;
    private final static int BITMAP_DATA_WIDTH = 8;
    private final static int BITMAP_DATA_HEIGHT = 8;
    private final static int BITMAP_DATA_SIZE
        = BITMAP_DATA_WIDTH * BITMAP_DATA_HEIGHT;
    private final static int BITMAP_SCALE = 3;
    private final static int BITMAP_WIDTH = BITMAP_DATA_WIDTH * BITMAP_SCALE;
    private final static int BITMAP_HEIGHT = BITMAP_DATA_HEIGHT * BITMAP_SCALE;
    private final static int BITMAP_SELECTED = 0;
    private final static int BITMAP_LAST_UNIT = NR_UNIT_TYPES;
    private final static int BITMAP_MOVE_TARGET = BITMAP_LAST_UNIT + 1;
    private final static int BITMAP_ATTACK_TARGET = BITMAP_LAST_UNIT + 2;
    private final static int BITMAP_FUTURE_TURN = BITMAP_LAST_UNIT + 3;
    private final static int BITMAP_TURN = BITMAP_LAST_UNIT + 4;
    private final static int NR_BITMAPS = BITMAP_TURN + 1;
    private final static String BITMAP_DATA =
   "xx....xx"
   +"x......x"
   +"........"
   +"........"
   +"........"
   +"........"
   +"x......x"
   +"xx....xx"
   +"........"
   +"........"
   +"...xx..."
   +".xxxx..."
   +"...xx..."
   +"...xx..."
   +"........"
   +"........"
   +"........"
   +"........"
   +"........"
   +"....xxx."
   +"xxxxxxxx"
   +"...xxxxx"
   +"...xxxxx"
   +"........"
   +"x......."
   +"xx......"
   +".xx....."
   +"..xx...."
   +"...xxx.."
   +"..xxxx.."
   +"..x.xxx."
   +"...xx.xx"
   +"........"
   +"......xx"
   +"....xxxx"
   +"......xx"
   +"xxxxx.xx"
   +"x.x.x..."
   +"x.x.x..."
   +"........"
   +"........"
   +"........"
   +"..xxxx.."
   +"..xx.x.."
   +"..x.xx.."
   +"..xxxx.."
   +"........"
   +"........"
   +".x....x."
   +"x......x"
   +"........"
   +"........"
   +"........"
   +"........"
   +"x......x"
   +".x....x."
        ;
    private byte turn;
    private byte state;
    private final static int WOODS_BASE = 10;
    private final static int WOODS_SPREAD = 30;
    private final static int MAX_NR_RIVERS = 3;
    private final static int FIRST_POSSIBLE_RIVER_COLUMN = 3;
    private final static int RIVER_LINE_GARRISON = 50;
    private final static int BRIDGE_PERCENT = 5;
    private final static int CAMPAIGN_FRIENDLY_COLUMNS
        = FIRST_POSSIBLE_RIVER_COLUMN;
    private final static int CAMPAIGN_MIN_NML = 3;
    private final static int CAMPAIGN_FRIENDLY_INFANTRY = 17;
    private final static int CAMPAIGN_FRIENDLY_TANK = 24;
    private final static int CAMPAIGN_FRIENDLY_ARTILLERY = 30;
    private final static int CAMPAIGN_FRIENDLY_ENGINEER = 35;
    private final static int CAMPAIGN_ENEMY_INFANTRY = 35;
    private final static int CAMPAIGN_ENEMY_TANK = 45;
    private final static int CAMPAIGN_ENEMY_ARTILLERY = 50;
    private final static byte[] campaign = new byte[] {
        0x29, 0x33, 0x39,
        0x30, 0x1f, 0x22,
        0x2b, 0x22, 0x22,
        0x34, 0x7a, 0x74,
        0x35, 0x42, 0x71,
        0x45, 0x52, 0x01,
        0x55, 0x62, 0x11,
        0x65, 0xffffff90, 0x32,
        0x0a, 0x15, 0x31,
        0x19, 0x74, 0x4a,
        0x53, 0x54, 0xffffff9f,
        0x55, 0x57, 0xffffffa4,
    };
    private final static int LAST_CAMPAIGN_SCENARIO = 11;
    private final static int COLOR_FRIENDLY_BACKGROUND = 0x386bb9;
    private final static int COLOR_FRIENDLY_BORDER = 0x0d38b9;
    private final static int COLOR_ENEMY_BACKGROUND = 0xe5a047;
    private final static int COLOR_ENEMY_BORDER = 0xb93838;
    private final static int COLOR_MOVED_BORDER = 0x878787;
    private final static int COLOR_OPEN = 0xffffed;
    private final static int COLOR_WOODS_1 = 0x3a691d;
    private final static int COLOR_WOODS_2 = 0x62875b;
    private final static int COLOR_WATER_1 = 0x62a7a7;
    private final static int COLOR_WATER_2 = 0x404aa7;
    private final static int COLOR_CITY_1 = 0x624848;
    private final static int COLOR_CITY_2 = 0xa78787;
    private final static int COLOR_RED_1 = 0xff0037;
    private final static int COLOR_RED_2 = 0xff4037;
    private final static int COLOR_GREEN_1 = 0x62ff00;
    private final static int COLOR_GREEN_2 = 0x3aa700;
    private final static int TERRAIN_DRAW_COLOR_1 = 0;
    private final static int TERRAIN_DRAW_COLOR_2 = 1;
    private final static int TERRAIN_DRAW_X_SPREAD_OFFSET = 2;
    private final static int TERRAIN_DRAW_WIDTH_OFFSET = 3;
    private final static int TERRAIN_DRAW_HEIGHT_OFFSET = 4;
    private final static int TERRAIN_DRAW_AMOUNT_OFFSET = 5;
    private final static int TERRAIN_DEFENSE_OFFSET = 6;
    private final static int TERRAIN_MOVE_COST_OFFSET = 7;
    private final static int TERRAIN_DATA_SIZE
        = TERRAIN_MOVE_COST_OFFSET + 1;
    private final static int[] terrainData
        = new int[] {
        0, 0, 0, 0, 0, 0, 9, 1,
        COLOR_WOODS_1, COLOR_WOODS_2, HEX_WIDTH * 7 / 10, 4, 4, 0x87, 6, 2,
        COLOR_CITY_1, COLOR_CITY_2, HEX_WIDTH * 4 / 5, 7, 7, 45, 5, 1,
        COLOR_WATER_1, COLOR_WATER_2, HEX_WIDTH / 4, 3, HEX_HEIGHT / 3, 30, 9,
        13
    };
    private final static int SCREEN_MAP_Y = BITMAP_HEIGHT;
    private final static int SCREEN_WIDTH = MAP_WIDTH;
    private final static int SCREEN_HEIGHT
        = MAP_HEIGHT + SCREEN_MAP_Y + 2;
    private final static int SCREEN_STATUS_BAR_TEXT_Y = BITMAP_HEIGHT - 3;
    private final static int SCREEN_TURN_LABEL_X = 10;
    private final static int SCREEN_TURN_NR_X = 50;
    private final static int SCREEN_TURN_SLASH_X = 65;
    private final static int SCREEN_TURN_TOTAL_X = 70;
    private final static int SCREEN_LEVEL_LABEL_X = 140;
    private final static int SCREEN_LEVEL_NR_X = 180;
    private final static int SCREEN_BUTTON_X = SCREEN_WIDTH - 100;
    private final static int SCREEN_BUTTON_TEXT_X = SCREEN_WIDTH - 80;
    private final static int SCREEN_BUTTON_HEIGHT = BITMAP_HEIGHT;
    private final static int SCREEN_MESSAGE_X = 300;
    private final static int UNIT_BORDER_WIDTH = 2;
    private final static byte CLEAR = 0;
    private final static byte WOODS = 1;
    private final static byte CITY = 2;
    private final static byte WATER = 3;
    private final static int NR_TERRAIN_TYPES = 4;
    private final static int CITY_RARITY = 40;
    private final static int NR_TURNS = 15;
    private final static byte SEARCH_STATE_BIT = 64;
    private final static byte NEW_LEVEL = 1;
    private final static byte NEW_TURN = 2 | SEARCH_STATE_BIT;
    private final static byte SELECT_UNIT_TO_MOVE = 3;
    private final static byte MARK_UNIT_TO_MOVE = 4;
    private final static byte FIND_MOVE_TARGETS = 5 | SEARCH_STATE_BIT;
    private final static byte FIND_ATTACK_TARGETS = 6 | SEARCH_STATE_BIT;
    private final static byte SHOW_TARGETS = 7;
    private final static byte FIND_ENEMY_MOVE_TARGETS = 8 | SEARCH_STATE_BIT;
    private final static byte FIND_ENEMY_ZOC = 9 | SEARCH_STATE_BIT;
    private final static byte FIND_FRIENDLY_ZOC = 10 | SEARCH_STATE_BIT;
    private final static byte WON_CAMPAIGN = 11;
    private final static byte WON_LEVEL = 12;
    private final static byte FAIL_LEVEL = 13;
    private final static byte FIND_FRIENDLY_FOV = 14 | SEARCH_STATE_BIT;
    private final static byte SELECT_ENEMY_UNIT_TO_MOVE = 15
        | SEARCH_STATE_BIT;
    private final static byte FIND_ENEMY_ATTACK_TARGETS = 16
        | SEARCH_STATE_BIT;
    private final static byte FIND_FRIENDLY_THREAT = 17
        | SEARCH_STATE_BIT;
    private final static byte FIND_ENEMY_THREAT = 18
        | SEARCH_STATE_BIT;
    private final static byte FIND_ENEMY_VALUE = 19
        | SEARCH_STATE_BIT;
    private final static byte MARK_NONE = -1;
    private final static byte MARK_SELECTED = BITMAP_SELECTED;
    private final static byte MARK_MOVE_TARGET = BITMAP_MOVE_TARGET;
    private final static byte MARK_ATTACK_TARGET = BITMAP_ATTACK_TARGET;
    private int level = 0;
    private final static byte MAP_TERRAIN_LAYER = 0;
    private final static byte MAP_FRIENDLY_UNIT_LAYER = 1;
    private final static byte MAP_MARK_LAYER = 2;
    private final static byte MAP_FRIENDLY_MOVED_LAYER = 3;
    private final static byte MAP_FRIENDLY_FOV_LAYER = 4;
    private final static byte MAP_ENEMY_UNIT_LAYER = 5;
    private final static byte MAP_ZOC_LAYER = 6;
    private final static byte MAP_MOVE_COST_LAYER = 7;
    private final static byte MAP_RANGE_LAYER = 8;
    private final static byte MAP_TMP_LAYER = 9;
    private final static byte MAP_FRIENDLY_THREAT_LAYER = 10;
    private final static byte MAP_ENEMY_THREAT_LAYER = 11;
    private final static byte MAP_ENEMY_VALUE_LAYER = 12;
    private final static byte MAP_ENEMY_MOVED_LAYER = 13;
    private final static byte MAP_ATTACKED_LAYER = 14;
    private final static int MAP_DATA_SIZE = 15;
    private final static int MAP_DATA_TOTAL_SIZE
        = MAP_COLUMNS * MAP_ROWS * MAP_DATA_SIZE;
    private final byte[] map = new byte[MAP_DATA_TOTAL_SIZE];
    private final static byte FOV_RANGE =
    6;
    private final static int DEFENDER_FIRE_BACK = 6;
    private final static byte DEFAULT_ZOC_VALUE = 10;
    private final static byte VALUE_SEARCH_ITERATIONS = 10;
    private final static byte THREAT_SEARCH_ITERATIONS = 10;
    private final static byte CITY_VALUE = 30;
    private final static byte ARTILLERY_DEFEND_VALUE = 5;
    private final static int UNIT_THREAT = 5;
    private final static int UNIT_RANGE_OFFSET = 0;
    private final static int UNIT_MOVE_OFFSET = 1;
    private final static int UNIT_SIZE
        = UNIT_MOVE_OFFSET + 1;
    private final byte[] unitData = new byte[]
        {
            2, 5,
            2, 7,
            4, 3,
            2, 5
        };
    public void start() {
        state = NEW_LEVEL;
        turn = 1;
        Random mapRnd = new Random(campaign[level * 3]);
        Random funitsRnd = new Random(campaign[level * 3 + 1]);
        Random eunitsRnd = new Random(campaign[level * 3 + 2]);
        for (int i = 0; i < MAP_DATA_TOTAL_SIZE; i += MAP_DATA_SIZE) {
            byte initTerrain = CLEAR;
            if (mapRnd.nextInt(CITY_RARITY) == 0) {
                initTerrain = CITY;
            }
            map[i + MAP_TERRAIN_LAYER] = initTerrain;
            map[i + MAP_FRIENDLY_UNIT_LAYER] = NO_UNIT;
            map[i + MAP_ENEMY_UNIT_LAYER] = NO_UNIT;
            map[i + MAP_MARK_LAYER] = MARK_NONE;
        }
        int i = 0;
        int nrRivers = mapRnd.nextInt(MAX_NR_RIVERS);
        for (int n = 0; n < nrRivers; n++) {
           int c = FIRST_POSSIBLE_RIVER_COLUMN
               + mapRnd.nextInt(MAP_COLUMNS - FIRST_POSSIBLE_RIVER_COLUMN);
           for (int r = 0; r < MAP_ROWS; r++) {
               map[(c * MAP_ROWS + r) * MAP_DATA_SIZE + MAP_TERRAIN_LAYER]
                   = WATER;
           }
        }
        int firstEnemyColumn = CAMPAIGN_FRIENDLY_COLUMNS
            + CAMPAIGN_MIN_NML
            + funitsRnd.nextInt(MAP_COLUMNS / 4);
        i = 0;
        for (int c = 0; c < MAP_COLUMNS - 1; c++) {
            for (int r = 0; r < MAP_ROWS; r++) {
                byte terrain = map[i + MAP_TERRAIN_LAYER];
                if (terrain != WATER
                    && r > 0
                    && c > 0) {
                    int woods_percentage = WOODS_BASE;
                    if (map[i - MAP_DATA_SIZE * MAP_ROWS + MAP_TERRAIN_LAYER]
                        == WOODS) {
                        woods_percentage += WOODS_SPREAD;
                    }
                    if (map[i - MAP_DATA_SIZE + MAP_TERRAIN_LAYER] == WOODS) {
                        woods_percentage += WOODS_SPREAD;
                    }
                    if (mapRnd.nextInt(100) < woods_percentage) {
                        terrain = WOODS;
                    }
                }
                map[i + MAP_TERRAIN_LAYER] = terrain;
                byte unit = NO_UNIT;
                if ((c < CAMPAIGN_FRIENDLY_COLUMNS
                     || level == 3 && c > (MAP_COLUMNS
                                           - CAMPAIGN_FRIENDLY_COLUMNS))
                    && r > 0) {
                    int u = funitsRnd.nextInt(100);
                    if (u < CAMPAIGN_FRIENDLY_ENGINEER) {
                        unit = ENGINEER;
                    }
                    if (u < CAMPAIGN_FRIENDLY_ARTILLERY) {
                        unit = ARTILLERY;
                    }
                    if (u < CAMPAIGN_FRIENDLY_TANK) {
                        unit = TANK;
                    }
                    if (terrain == CITY || u < CAMPAIGN_FRIENDLY_INFANTRY) {
                        unit = INFANTRY;
                    }
                    if (terrain != WATER) {
                        map[i + MAP_FRIENDLY_UNIT_LAYER] = unit;
                        map[i + MAP_FRIENDLY_FOV_LAYER] = 1;
                    }
                } else if (c >= firstEnemyColumn) {
                    int u = eunitsRnd.nextInt(1000);
                    if (terrain == CITY || u < CAMPAIGN_ENEMY_INFANTRY) {
                        if (map[i + MAP_ROWS * MAP_DATA_SIZE
                                + MAP_TERRAIN_LAYER] != WATER) {
                            if (eunitsRnd.nextInt(2) == 0
                                && terrain == CITY) {
                                map[i + MAP_ROWS * MAP_DATA_SIZE
                                    + MAP_ENEMY_UNIT_LAYER] = ARTILLERY;
                            }
                        }
                        unit = INFANTRY;
                    } else if (u < CAMPAIGN_ENEMY_TANK) {
                        unit = TANK;
                    } else if (u < CAMPAIGN_ENEMY_ARTILLERY) {
                        unit = ARTILLERY;
                        if (map[i - MAP_ROWS * MAP_DATA_SIZE
                                + MAP_TERRAIN_LAYER] != WATER) {
                            map[i - MAP_ROWS * MAP_DATA_SIZE
                                + MAP_ENEMY_UNIT_LAYER] = INFANTRY;
                        }
                    }
                    if (terrain == WATER) {
                        if (map[i + MAP_ROWS * MAP_DATA_SIZE
                                + MAP_TERRAIN_LAYER] != WATER) {
                            if (eunitsRnd.nextInt(100) < RIVER_LINE_GARRISON) {
                                unit = INFANTRY;
                            }
                            map[i + MAP_ROWS * MAP_DATA_SIZE
                                + MAP_ENEMY_UNIT_LAYER] = unit;
                        }
                    } else if (unit != NO_UNIT && terrain != WATER) {
                        map[i + MAP_ENEMY_UNIT_LAYER] = unit;
                    }
                }
                i += MAP_DATA_SIZE;
            }
        }
        repaint();
    }
    public void update(Graphics g) {
        paint(g);
    }
    public void paint(Graphics pg) {
        Image bufferImage = createImage(SCREEN_WIDTH, SCREEN_HEIGHT);
        Graphics g = bufferImage.getGraphics();
        Random drawRnd = new Random(0);
        Color black = new Color(0);
 g.setColor(black);
        g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        g.setColor(new Color(0xffffff));
        g.drawString("TURN      / 15",
                     SCREEN_TURN_LABEL_X, SCREEN_STATUS_BAR_TEXT_Y);
        g.drawString(Integer.toString(turn),
                     SCREEN_TURN_NR_X, SCREEN_STATUS_BAR_TEXT_Y);
        g.drawString("MAP", SCREEN_LEVEL_LABEL_X, SCREEN_STATUS_BAR_TEXT_Y);
        g.drawString(Integer.toString(level + 1),
                     SCREEN_LEVEL_NR_X, SCREEN_STATUS_BAR_TEXT_Y);
        if (state == FAIL_LEVEL) {
            g.drawString("FAIL", SCREEN_MESSAGE_X, SCREEN_STATUS_BAR_TEXT_Y);
        }
        if (state == WON_LEVEL) {
            g.drawString("WON", SCREEN_MESSAGE_X, SCREEN_STATUS_BAR_TEXT_Y);
        }
        if (state == WON_CAMPAIGN) {
            g.drawString("WON ALL",
                         SCREEN_MESSAGE_X, SCREEN_STATUS_BAR_TEXT_Y);
        }
        g.setColor(new Color(COLOR_FRIENDLY_BORDER));
        g.fillRect(SCREEN_BUTTON_X, 0, SCREEN_WIDTH, SCREEN_BUTTON_HEIGHT);
        g.setColor(new Color(COLOR_FRIENDLY_BACKGROUND));
        g.fillRect(SCREEN_BUTTON_X + 2, 2,
                   SCREEN_WIDTH - 4, SCREEN_BUTTON_HEIGHT - 4);
        g.setColor(new Color(0xffffff));
        g.drawString(">>", SCREEN_BUTTON_TEXT_X,
                     SCREEN_STATUS_BAR_TEXT_Y - 2);
        int i = 0;
        int x = MAP_FIRST_COLUMN_CENTER_X;
        for (int c = 0; c < MAP_COLUMNS; c++) {
            int y = ((c % 2 == 0) ? MAP_EVEN_ROW_LAYER : 0) + SCREEN_MAP_Y
                + HEX_HEIGHT / 2;
            for (int r = 0; r < MAP_ROWS; r++) {
                g.setColor(new Color(COLOR_OPEN));
                for (int hx = 0; hx <= HEX_WIDTH / 4; hx++) {
                    int hy = hx * 155 / 100;
                    g.fillRect(x - HEX_WIDTH / 2 + hx,
                               y - hy + 2,
                               (HEX_WIDTH / 2 - hx) * 2,
                               hy * 2 - 1);
                }
                int td = map[i + MAP_TERRAIN_LAYER] * TERRAIN_DATA_SIZE;
                for (int a = 0;
                     a < terrainData[td + TERRAIN_DRAW_AMOUNT_OFFSET];
                     a++) {
                    int terrainColor =
                        terrainData[td + drawRnd.nextInt(2)];
                    int xSpread
                        = terrainData[td + TERRAIN_DRAW_X_SPREAD_OFFSET];
                    int tx = x - xSpread / 2 + drawRnd.nextInt(xSpread);
                    int ty = y - HEX_HEIGHT / 2 + drawRnd.nextInt(HEX_HEIGHT);
                    int w = terrainData[td + TERRAIN_DRAW_WIDTH_OFFSET];
                    int h = terrainData[td + TERRAIN_DRAW_HEIGHT_OFFSET];
                    tx -= w / 2;
                    ty -= h / 2;
                    g.setColor(new Color(terrainColor));
                    g.fillRect(tx, ty, w, h);
                }
                int unitImage = -1;
                int markImage = -1;
                int markColor = -1;
                int unitColor = -1;
                int borderColor = COLOR_FRIENDLY_BORDER;
                boolean mirror = false;
                int f = map[i + MAP_FRIENDLY_UNIT_LAYER];
                if (f > 0) {
                    unitImage = f;
                    unitColor = COLOR_FRIENDLY_BACKGROUND;
                    mirror = true;
                    if (map[i + MAP_FRIENDLY_MOVED_LAYER] > 0) {
                        borderColor = COLOR_MOVED_BORDER;
                        unitColor = COLOR_MOVED_BORDER;
                    }
                }
                if (map[i + MAP_FRIENDLY_FOV_LAYER] > 0) {
                    int e = map[i + MAP_ENEMY_UNIT_LAYER];
                    if (e > 0) {
                        unitImage = e;
                        unitColor = COLOR_ENEMY_BACKGROUND;
                        borderColor = COLOR_ENEMY_BORDER;
                        if (map[i + MAP_ATTACKED_LAYER] > 0) {
                            borderColor = COLOR_MOVED_BORDER;
                        }
                    }
                }
                markImage = map[i + MAP_MARK_LAYER];
                if (markImage != MARK_NONE) {
                    int mi;
                    switch (markImage) {
                        case MARK_SELECTED:
                            markColor = COLOR_RED_2;
                            break;
                        case MARK_MOVE_TARGET:
                            markColor = COLOR_RED_1;
                            break;
                        case MARK_ATTACK_TARGET:
                            markColor = COLOR_RED_1;
                            break;
                    }
                }
                markImage *= BITMAP_DATA_SIZE;
                unitImage *= BITMAP_DATA_SIZE;
                if (unitImage >= 0) {
                    g.setColor(new Color(borderColor));
                    g.fillRect(x - BITMAP_WIDTH / 2 - UNIT_BORDER_WIDTH,
                               y - BITMAP_HEIGHT / 2 - UNIT_BORDER_WIDTH,
                               BITMAP_WIDTH + 2 * UNIT_BORDER_WIDTH,
                               BITMAP_HEIGHT + 2 * UNIT_BORDER_WIDTH);
                }
                for (int idr = 0; idr < BITMAP_DATA_HEIGHT; idr++) {
                    for (int idc = 0; idc < BITMAP_DATA_WIDTH; idc++) {
                        int color = -1;
                        if (unitImage >= 0) {
                            color = unitColor;
                            if (BITMAP_DATA.charAt(unitImage) == 'x') {
                                color = 0;
                            }
                        }
                        if (markImage >= 0
                            && BITMAP_DATA.charAt(markImage) == 'x') {
                            color = markColor;
                        }
                        if (color >= 0) {
                            int cx = idc;
                            if (mirror) {
                                cx = BITMAP_DATA_WIDTH - cx - 1;
                            }
                            g.setColor(new Color(color));
                            int x1 = x - BITMAP_WIDTH / 2
                                + cx * BITMAP_SCALE;
                            int y1 = y - BITMAP_HEIGHT / 2
                                + idr * BITMAP_SCALE;
                            g.fillRect(
                                x1, y1,
                                BITMAP_SCALE, BITMAP_SCALE);
                        }
                        markImage++;
                        unitImage++;
                    }
                }
                y += HEX_HEIGHT;
                i += MAP_DATA_SIZE;
            }
            x += MAP_COLUMN_DISTANCE;
        }
        pg.drawImage(bufferImage, 0, 0, this);
    }
    public boolean handleEvent(Event e) {
        if (e.id != Event.MOUSE_DOWN) {
            return true;
        }
        if (e.y < SCREEN_BUTTON_HEIGHT
            && e.x > SCREEN_BUTTON_X) {
            switch (state) {
                case WON_LEVEL:
                    level++;
                case FAIL_LEVEL:
                    state = NEW_LEVEL;
                    break;
                case WON_CAMPAIGN:
                    break;
                default:
                    turn++;
                    state = NEW_TURN;
                    if (turn > NR_TURNS) {
                        state = FAIL_LEVEL;
                    }
            }
        }
        if (state == NEW_LEVEL) {
            start();
            turn = 1;
            state = NEW_TURN;
        }
        int i = 0;
        int closestDistSquare = SCREEN_WIDTH * SCREEN_WIDTH
            + SCREEN_HEIGHT * SCREEN_HEIGHT;
        int x = MAP_FIRST_COLUMN_CENTER_X;
        int iClicked = 0;
        int iPrevSelected = -1;
        for (byte c = 0; c < MAP_COLUMNS; c++) {
            int y = ((c % 2 == 0) ? MAP_EVEN_ROW_LAYER : 0) + SCREEN_MAP_Y
                + HEX_HEIGHT / 2;
            for (byte r = 0; r < MAP_ROWS; r++) {
                int dx = x - e.x;
                int dy = y - e.y;
                int distSquare = dx * dx + dy * dy;
                if (distSquare < closestDistSquare
                   && e.y >= SCREEN_MAP_Y) {
                    iClicked = i;
                    closestDistSquare = distSquare;
                }
                if (map[i + MAP_MARK_LAYER] == MARK_SELECTED) {
                    iPrevSelected = i;
                }
                switch (state) {
                    case NEW_TURN:
                        map[i + MAP_FRIENDLY_FOV_LAYER] = 0;
                        if (map[i + MAP_FRIENDLY_UNIT_LAYER] > 0) {
                            map[i + MAP_FRIENDLY_FOV_LAYER] = FOV_RANGE;
                        }
                        map[i + MAP_MARK_LAYER] = MARK_NONE;
                        map[i + MAP_FRIENDLY_MOVED_LAYER] = 0;
                        map[i + MAP_ENEMY_MOVED_LAYER] = 0;
                        map[i + MAP_ATTACKED_LAYER] = 0;
                    case SELECT_UNIT_TO_MOVE:
                        map[i + MAP_MARK_LAYER] = MARK_NONE;
                        map[i + MAP_ZOC_LAYER] = 0;
                        if (map[i + MAP_ENEMY_UNIT_LAYER] > 0) {
                            map[i + MAP_ZOC_LAYER] = 2;
                        }
                    case SHOW_TARGETS:
                        map[i + MAP_MOVE_COST_LAYER] = 0;
                        map[i + MAP_RANGE_LAYER] = 0;
                        map[i + MAP_TMP_LAYER] = map[i + MAP_MARK_LAYER];
                        map[i + MAP_MARK_LAYER] = MARK_NONE;
                        break;
                }
                i += MAP_DATA_SIZE;
                y += HEX_HEIGHT;
            }
            x += MAP_COLUMN_DISTANCE;
        }
        int searchLayer = MAP_RANGE_LAYER;
        Random rnd = new Random();
        int searchUnitIndex = -1;
        byte searchUnit = -1;
       if ((state == SELECT_UNIT_TO_MOVE
            || state == SHOW_TARGETS)
           && map[iClicked + MAP_FRIENDLY_UNIT_LAYER] > 0
           && map[iClicked + MAP_FRIENDLY_MOVED_LAYER] == 0) {
           state = FIND_ENEMY_ZOC;
           searchUnit = map[iClicked + MAP_FRIENDLY_UNIT_LAYER];
           searchUnitIndex = (searchUnit - 1) * UNIT_SIZE;
       } else if (state == SHOW_TARGETS) {
           searchUnit = map[iPrevSelected + MAP_FRIENDLY_UNIT_LAYER];
           searchUnitIndex = (searchUnit - 1) * UNIT_SIZE;
           switch (map[iClicked + MAP_TMP_LAYER]) {
               case MARK_MOVE_TARGET:
                   map[iPrevSelected + MAP_FRIENDLY_UNIT_LAYER] = 0;
                   map[iClicked + MAP_FRIENDLY_UNIT_LAYER] = searchUnit;
                   map[iClicked + MAP_FRIENDLY_MOVED_LAYER] = 1;
                   map[iClicked + MAP_FRIENDLY_FOV_LAYER] = FOV_RANGE;
                   state = SELECT_UNIT_TO_MOVE;
                   break;
               case MARK_ATTACK_TARGET:
                   int terrain = map[iClicked + MAP_TERRAIN_LAYER];
                   int au = map[iClicked + MAP_ENEMY_UNIT_LAYER];
                   int attackedUnitIndex = (au - 1) * UNIT_SIZE;
                   int multiplier = terrainData[terrain * TERRAIN_DATA_SIZE
                                                + TERRAIN_DEFENSE_OFFSET];
                   if (map[iClicked + MAP_ATTACKED_LAYER] > 0
                       || terrain == CITY && searchUnit == ENGINEER) {
                       multiplier *= 2;
                   }
                   int dr = rnd.nextInt(20);
                   if (dr < multiplier) {
                       map[iClicked + MAP_ENEMY_UNIT_LAYER] = 0;
                   }
                   if (map[iClicked + MAP_ATTACKED_LAYER] == 0
                       && searchUnit != ARTILLERY) {
                       dr = rnd.nextInt(20);
                       if (dr < DEFENDER_FIRE_BACK) {
                       map[iPrevSelected + MAP_FRIENDLY_UNIT_LAYER] = 0;
                       }
                   }
                   map[iClicked + MAP_ATTACKED_LAYER] = 1;
                   map[iPrevSelected + MAP_FRIENDLY_MOVED_LAYER] = 1;
               default:
                   state = SELECT_UNIT_TO_MOVE;
           }
       }
        while ((state & SEARCH_STATE_BIT) > 0) {
            int nrSearchIterations = 1;
            int setFoundInLayer = -1;
            byte setFoundValue = 1;
            byte resetFoundValue = -2;
            int blockLayer = -1;
            int onlyKeepLayer = -1;
            int notKeepLayer = -1;
            boolean waterBlocks = false;
            switch (state) {
                case SELECT_ENEMY_UNIT_TO_MOVE:
                    iPrevSelected = -1;
                    break;
                case FIND_ENEMY_ZOC:
                case FIND_FRIENDLY_ZOC:
                    searchLayer = MAP_ZOC_LAYER;
                    break;
                case FIND_FRIENDLY_THREAT:
                    searchLayer = MAP_FRIENDLY_THREAT_LAYER;
                    nrSearchIterations = THREAT_SEARCH_ITERATIONS;
                    waterBlocks = true;
                    break;
                case FIND_ENEMY_THREAT:
                    searchLayer = MAP_ENEMY_THREAT_LAYER;
                    nrSearchIterations = THREAT_SEARCH_ITERATIONS;
                    waterBlocks = true;
                    break;
                case FIND_ENEMY_VALUE:
                    searchLayer = MAP_ENEMY_VALUE_LAYER;
                    nrSearchIterations = VALUE_SEARCH_ITERATIONS;
                    waterBlocks = true;
                    break;
                case FIND_MOVE_TARGETS:
                    searchLayer = MAP_MOVE_COST_LAYER;
                    nrSearchIterations
                        = unitData[searchUnitIndex + UNIT_MOVE_OFFSET];
                    map[iClicked + MAP_MOVE_COST_LAYER]
                        = unitData[searchUnitIndex + UNIT_MOVE_OFFSET];
                    setFoundInLayer = MAP_MARK_LAYER;
                    setFoundValue = MARK_MOVE_TARGET;
                    resetFoundValue = MARK_NONE;
                    blockLayer = MAP_ZOC_LAYER;
                    notKeepLayer = MAP_FRIENDLY_UNIT_LAYER;
                    break;
                case FIND_ENEMY_MOVE_TARGETS:
                    searchLayer = MAP_MOVE_COST_LAYER;
                    nrSearchIterations
                        = unitData[searchUnitIndex + UNIT_MOVE_OFFSET];
                    map[iPrevSelected + MAP_MOVE_COST_LAYER]
                        = unitData[searchUnitIndex + UNIT_MOVE_OFFSET];
                    setFoundInLayer = MAP_MARK_LAYER;
                    setFoundValue = MARK_MOVE_TARGET;
                    resetFoundValue = MARK_NONE;
                    blockLayer = MAP_ZOC_LAYER;
                    notKeepLayer = MAP_ENEMY_UNIT_LAYER;
                    break;
                case FIND_ATTACK_TARGETS:
                    searchLayer = MAP_RANGE_LAYER;
                    byte range = (byte) (unitData[searchUnitIndex
                                                  + UNIT_RANGE_OFFSET]);
                    nrSearchIterations = range;
                    setFoundInLayer = MAP_MARK_LAYER;
                    map[iClicked + MAP_RANGE_LAYER] = range;
                    setFoundValue = MARK_ATTACK_TARGET;
                    resetFoundValue = -2;
                    blockLayer = -1;
                    onlyKeepLayer = MAP_ENEMY_UNIT_LAYER;
                    break;
                case FIND_ENEMY_ATTACK_TARGETS:
                    searchLayer = MAP_RANGE_LAYER;
                    range = (byte) (unitData[searchUnitIndex
                                             + UNIT_RANGE_OFFSET]);
                    nrSearchIterations = range;
                    setFoundInLayer = MAP_MARK_LAYER;
                    map[iPrevSelected + MAP_RANGE_LAYER] = range;
                    setFoundValue = MARK_ATTACK_TARGET;
                    resetFoundValue = -2;
                    blockLayer = -1;
                    onlyKeepLayer = MAP_FRIENDLY_UNIT_LAYER;
                    break;
                case NEW_TURN:
                case FIND_FRIENDLY_FOV:
                    searchLayer = MAP_FRIENDLY_FOV_LAYER;
                    nrSearchIterations = FOV_RANGE;
                    break;
                default:
                    setFoundInLayer = -1;
            }
            for (int searchIteration = 0; searchIteration < nrSearchIterations;
                 searchIteration++) {
                for (i = 0; i < MAP_DATA_TOTAL_SIZE; i += MAP_DATA_SIZE) {
                    if (state == FIND_FRIENDLY_ZOC) {
                        map[i + MAP_TMP_LAYER] = 0;
                        map[i + MAP_RANGE_LAYER] = 0;
                        map[i + MAP_MOVE_COST_LAYER] = 0;
                        map[i + MAP_FRIENDLY_THREAT_LAYER] = 0;
                        map[i + MAP_ENEMY_THREAT_LAYER] = 0;
                        map[i + MAP_ENEMY_VALUE_LAYER] = 0;
                        byte fu = map[i + MAP_FRIENDLY_UNIT_LAYER];
                        byte eu = map[i + MAP_ENEMY_UNIT_LAYER];
                        if (fu > 0) {
                            map[i + MAP_TMP_LAYER] = 2;
                            map[i + MAP_FRIENDLY_THREAT_LAYER]
                                = UNIT_THREAT;
                        } else if (eu > 0) {
                            map[i + MAP_ENEMY_THREAT_LAYER]
                                = UNIT_THREAT;
                            if (eu == ARTILLERY) {
                                map[i + MAP_ENEMY_VALUE_LAYER]
                                    += ARTILLERY_DEFEND_VALUE;
                            }
                        }
                        if (map[i + MAP_TERRAIN_LAYER] == CITY) {
                            map[i + MAP_ENEMY_VALUE_LAYER] += CITY_VALUE;
                        }
                    } else {
                        map[i + MAP_TMP_LAYER] = map[i + searchLayer];
                    }
                }
                i = 0;
                for (int c = 0; c < MAP_COLUMNS; c++) {
                    for (int r = 0; r < MAP_ROWS; r++) {
                        byte v = map[i + MAP_TMP_LAYER];
                        if (v > 1) {
                            int[] adj = new int[] {-1, -1, -1, -1, -1, -1};
                            if (r > 0) {
                                adj[0] = i - MAP_DATA_SIZE;
                            }
                            if (r < MAP_ROWS - 1) {
                                adj[3] = i + MAP_DATA_SIZE;
                            }
                            if (c % 2 == 0) {
                                if (c < MAP_COLUMNS - 1) {
                                    adj[1] = i
                                        + MAP_ROWS * MAP_DATA_SIZE;
                                    if (r < MAP_ROWS - 1) {
                                        adj[2] = i
                                            + (MAP_ROWS+1) * MAP_DATA_SIZE;
                                    }
                                }
                                if (c > 0) {
                                    if (r < MAP_ROWS - 1) {
                                        adj[4] = i
                                            - (MAP_ROWS-1) * MAP_DATA_SIZE;
                                    }
                                    adj[5] = i
                                        - MAP_ROWS * MAP_DATA_SIZE;
                                }
                            } else {
                                if (c < MAP_COLUMNS - 1) {
                                    if (r > 0) {
                                        adj[1] = i
                                            + (MAP_ROWS-1) * MAP_DATA_SIZE;
                                    }
                                    adj[2] = i + MAP_ROWS * MAP_DATA_SIZE;
                                }
                                if (c > 0) {
                                    adj[4] = i - MAP_ROWS * MAP_DATA_SIZE;
                                    if (r > 0) {
                                        adj[5] = i
                                            - (MAP_ROWS+1) * MAP_DATA_SIZE;
                                    }
                                }
                            }
                            for (int d = 0; d < 6; d++) {
                                if (adj[d] >= 0) {
                                    byte ev = (byte) (v - 1);
                                    byte bv = 0;
                                    if (searchLayer == MAP_MOVE_COST_LAYER) {
                                        int terrain
                                            = map[adj[d] + MAP_TERRAIN_LAYER];
                                        if (terrain == WATER
                                            && map[adj[d]
                                                   + MAP_FRIENDLY_UNIT_LAYER]
                                            == ENGINEER) {
                                        } else if (searchUnit == ENGINEER
                                                   && terrain == WATER
                                                   && v == 5) {
                                            ev = 1;
                                        } else {
                                            int terrainCost
                                                = terrainData[
                                                    terrain * TERRAIN_DATA_SIZE
                                                    + TERRAIN_MOVE_COST_OFFSET]
                                                ;
                                            ev = (byte)
                                                (v - terrainCost);
                                        }
                                    } else if (waterBlocks
                                              && map[adj[d]
                                                    + MAP_TERRAIN_LAYER]
                                              == WATER) {
                                        ev = 0;
                                    }
                                    byte ov = map[adj[d] + MAP_TMP_LAYER];
                                    if (blockLayer >= 0) {
                                        bv = map[adj[d] + blockLayer];
                                        if (bv > 0 && ev > 1) {
                                            ev = 1;
                                        }
                                    }
                                    if (ev > ov && bv <= 1) {
                                        map[adj[d] + MAP_TMP_LAYER] = ev;
                                    }
                                }
                            }
                        }
                        i += MAP_DATA_SIZE;
                    }
                }
                for (i = 0; i < MAP_DATA_TOTAL_SIZE; i += MAP_DATA_SIZE) {
                    byte v = map[i + MAP_TMP_LAYER];
                    map[i + searchLayer] = v;
                }
            }
            for (i = 0; i < MAP_DATA_TOTAL_SIZE; i += MAP_DATA_SIZE) {
                byte v = map[i + searchLayer];
                if ((onlyKeepLayer >= 0
                     && map[i + onlyKeepLayer] == 0)
                    || (notKeepLayer >= 0
                        && map[i + notKeepLayer] > 0)) {
                    map[i + searchLayer] = 0;
                }
            }
            int bestValue = 0;
            for (i = 0; i < MAP_DATA_TOTAL_SIZE; i += MAP_DATA_SIZE) {
                if (setFoundInLayer >= 0) {
                    if (resetFoundValue >= -1) {
                        map[i + setFoundInLayer] = resetFoundValue;
                    }
                    if (map[i + searchLayer] > 0) {
                        map[i + setFoundInLayer] = setFoundValue;
                    }
                }
                if (state == SELECT_ENEMY_UNIT_TO_MOVE
                    && map[i + MAP_ENEMY_UNIT_LAYER] > 0
                    && map[i + MAP_ENEMY_MOVED_LAYER] == 0) {
                    iPrevSelected = i;
                    searchUnitIndex
                        = (map[iPrevSelected + MAP_ENEMY_UNIT_LAYER]
                           - 1)
                        * UNIT_SIZE;
                } else if (state == FIND_ENEMY_ATTACK_TARGETS
                           && map[i + MAP_MARK_LAYER] != MARK_NONE) {
                    int value
                        = map[i + MAP_ENEMY_VALUE_LAYER]
                        + map[i + MAP_FRIENDLY_THREAT_LAYER];
                    if (value >= bestValue) {
                        bestValue = value;
                        iClicked = i;
                    }
                    map[i + MAP_TMP_LAYER] = map[i + MAP_MARK_LAYER];
                    map[i + MAP_MARK_LAYER] = MARK_NONE;
                }
            }
            switch (state) {
                case NEW_TURN:
                    state = SELECT_UNIT_TO_MOVE;
                    if (turn > 1) {
                        state = SELECT_ENEMY_UNIT_TO_MOVE;
                    }
                    break;
                case SELECT_ENEMY_UNIT_TO_MOVE:
                    state = FIND_FRIENDLY_ZOC;
                    if (iPrevSelected == -1) {
                        state = SELECT_UNIT_TO_MOVE;
                    }
                    break;
                case FIND_FRIENDLY_ZOC:
                    state = FIND_FRIENDLY_THREAT;
                    break;
                case FIND_FRIENDLY_THREAT:
                    state = FIND_ENEMY_THREAT;
                    break;
                case FIND_ENEMY_THREAT:
                    state = FIND_ENEMY_VALUE;
                    break;
                case FIND_ENEMY_VALUE:
                    state = FIND_ENEMY_MOVE_TARGETS;
                    break;
                case FIND_ENEMY_MOVE_TARGETS:
                    state = FIND_ENEMY_ATTACK_TARGETS;
                    break;
                case FIND_ENEMY_ATTACK_TARGETS:
                    byte u = map[iPrevSelected + MAP_ENEMY_UNIT_LAYER];
                    switch (map[iClicked + MAP_TMP_LAYER]) {
                        case MARK_MOVE_TARGET:
                            map[iPrevSelected + MAP_ENEMY_UNIT_LAYER] = 0;
                            map[iClicked + MAP_ENEMY_UNIT_LAYER] = u;
                            break;
                        case MARK_ATTACK_TARGET:
                            int terrain = map[iClicked + MAP_TERRAIN_LAYER];
                            int au = map[iClicked + MAP_FRIENDLY_UNIT_LAYER];
                            int attackedUnitIndex = (au - 1) * UNIT_SIZE;
                            int multiplier = terrainData[
                                terrain * TERRAIN_DATA_SIZE
                                + TERRAIN_DEFENSE_OFFSET];
                            if (map[iClicked + MAP_ATTACKED_LAYER] > 0) {
                                multiplier *= 2;
                            }
                            int dr = rnd.nextInt(20);
                            if (dr < multiplier) {
                                map[iClicked + MAP_FRIENDLY_UNIT_LAYER] = 0;
                            }
                            if (map[iClicked + MAP_ATTACKED_LAYER] > 0
                                && u != ARTILLERY) {
                                dr = rnd.nextInt(20);
                                 if (dr < DEFENDER_FIRE_BACK) {
                                     map[iPrevSelected
                                         + MAP_ENEMY_UNIT_LAYER] = 0;
                                 }
                             }
                            break;
                    }
                    map[iClicked + MAP_ENEMY_MOVED_LAYER] = 1;
                    map[iPrevSelected + MAP_ENEMY_MOVED_LAYER] = 1;
                    state = SELECT_ENEMY_UNIT_TO_MOVE;
                    break;
                case FIND_ENEMY_ZOC:
                    state = FIND_MOVE_TARGETS;
                    break;
                case FIND_MOVE_TARGETS:
                    state = FIND_ATTACK_TARGETS;
                    break;
                case FIND_ATTACK_TARGETS:
                    map[iClicked + MAP_MARK_LAYER] = MARK_SELECTED;
                    state = FIND_FRIENDLY_FOV;
                    break;
                case FIND_FRIENDLY_FOV:
                    state = SHOW_TARGETS;
                    break;
            }
        }
        boolean victory = true;
        for (i = 0; i < MAP_DATA_TOTAL_SIZE; i += MAP_DATA_SIZE) {
            if (map[i + MAP_TERRAIN_LAYER] == CITY
                && map[i + MAP_FRIENDLY_UNIT_LAYER] == NO_UNIT) {
                victory = false;
            }
        }
        if (victory
) {
            state = WON_LEVEL;
            if (level == LAST_CAMPAIGN_SCENARIO) {
                state = WON_CAMPAIGN;
            }
        }
        repaint();
 return false;
    }
}
