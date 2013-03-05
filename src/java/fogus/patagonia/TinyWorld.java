package fogus.patagonia;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Event;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * tiny_world
 * A rather small game written for the 2013 J4K contest: http://www.java4k.com
 * 
 * @author dapy
 * 
 * http://java4k.com/index.php?action=games&method=view&gid=463
 */
public class TinyWorld extends Applet implements Runnable {

	private static final int GAME_STATE_INIT = 1;
	private static final int GAME_STATE_ACTIVE = 2;
	private static final int GAME_STATE_WON = 3;
	private static final int GAME_STATE_LOST = 4;

	private static final int SCREEN_WIDTH = 800;
	private static final int SCREEN_HEIGHT = 600;
	
	private static final int TILE_WIDTH = 12;
	private static final int TILE_HEIGHT = 8;
	private static final int HEIGHT_INCREMENT = 3;
	private static final int TILE_SIZE = 10;
	private static final double X_RATIO = (double)TILE_WIDTH/TILE_SIZE;
	private static final double Y_RATIO = (double)TILE_HEIGHT/TILE_SIZE;
	private static final int X_RATIO_INT = (int)(X_RATIO * 10); // note: only used to avoid a couple of casts!
	private static final int Y_RATIO_INT = (int)(Y_RATIO * 10); // note: only used to avoid a couple of casts!

	private static final int MAP_SIZE = 128; // in tiles
	private static final int MAP_WIDTH = MAP_SIZE * TILE_WIDTH;
	private static final int MAP_HEIGHT = MAP_SIZE * TILE_HEIGHT;
	private static final int MAP_VERTICAL_OFFSET = 50; // prevents us from chopping off the top of the map when rendering the map image
	private static final int CROP_RADIUS = 2; // how many crop tiles surround a farm
	
	private static final int COST_BUILDING = 10;
	private static final int COST_SOLDIER_ORE = 10;
	private static final int COST_SOLDIER_WOOD = 5;
	private static final int MAX_RESOURCES = 99;
	
	// world variable indexes
	private static final int WORLD_ARRAY_SIZE = 6;
	private static final int CONTENT_TYPE = 0;
	private static final int CONTENT_ID = 1;
	private static final int HEIGHT_MAP = 2;
	private static final int TREE_MAP = 3;
	private static final int TILE_CENTRE_HEIGHT = 4;
	private static final int NEARBY_VILLAGE_ID = 5;

	private static final int EMPTY_TILE = -1;
	private static final int OWNER_NONE = -1;
	private static final int OWNER_PLAYER = 0;
	
	// entity/terrain types - also used as index to entity image, and to indicate tile content
	private static final int TYPE_WOODCUTTER_HUT = 0;
	private static final int TYPE_MINE = 1;
	private static final int TYPE_FARM = 2;
	private static final int TYPE_VILLAGE = 3;
	private static final int TYPE_TREE = 4;
	private static final int TYPE_ORE = 5;
	private static final int TYPE_CROPS = 6;
	private static final int TYPE_WORKER = 10;
	private static final int TYPE_HILL = 16;
	private static final int TYPE_WATER = 17;
	
	private static final int IMAGE_RESOURCE_INDEX = 4;
	private static final int IMAGE_FLAG_INDEX = 11;
	private static final int IMAGE_BOAT = 15;
	private static final int IMAGE_TOTAL = 16;
	private static final int IMAGE_WIDTH = 12;
	private static final int IMAGE_HEIGHT = 12;
	
	private static final int TASK_IDLE = 0;
	private static final int TASK_GATHER_RESOURCE = 1;
	private static final int TASK_DEPOSIT_RESOURCE = 2;
	private static final int TASK_INVADE_VILLAGE = 3;
	private static final int TASK_CREATE_WORKER = 4;

	private static final int STATE_ALIVE = 0;
	private static final int STATE_DEAD = 1;
	
	private static final int RESOURCE_WOOD = 0;
	private static final int RESOURCE_ORE = 1;
	private static final int RESOURCE_FOOD = 2;
	private static final int RESOURCE_SOLDIERS = 3;
	
	private static final int RESOURCE_LOG_PERIOD = 600; // in frames
	private static final int RESOURCE_LOG_ENTRIES = 5;
	
	// entity variable indexes - common to all entities
	private static final int ENTITY_TYPE = 0;
	private static final int ENTITY_ID = 1;
	private static final int TIMER = 2;
	private static final int TASK = 3;
	private static final int X = 4;
	private static final int Y = 5;
	private static final int STATE = 6;
	// common to all entities except crops
	private static final int VILLAGE_X = 7;
	private static final int VILLAGE_Y = 8;
	private static final int VILLAGE_ID = 9;
	private static final int OWNER = 10;
	// common to village and worker
	private static final int TARGET_X = 11;
	private static final int TARGET_Y = 12;
	// village-specific
	private static final int CREATE_BUILDING_TYPE = 13;
	private static final int CREATE_BUILDING_AI_TIMER = 14;
	private static final int INVADE_VILLAGE_AI_TIMER = 15;
	private static final int INVADE_VILLAGE_STATE = 16;
	private static final int TOTAL_RESOURCE_INDEX = 17;
	private static final int TOTAL_WOOD = TOTAL_RESOURCE_INDEX + RESOURCE_WOOD;
	private static final int TOTAL_ORE = TOTAL_RESOURCE_INDEX + RESOURCE_ORE;
	private static final int TOTAL_FOOD = TOTAL_RESOURCE_INDEX + RESOURCE_FOOD;
	private static final int TOTAL_SOLDIERS = TOTAL_RESOURCE_INDEX + RESOURCE_SOLDIERS;
	private static final int TOTAL_BUILDING_INDEX = 21;
	private static final int TOTAL_WOODCUTTER = TOTAL_BUILDING_INDEX + RESOURCE_WOOD;
	private static final int TOTAL_MINE = TOTAL_BUILDING_INDEX + RESOURCE_ORE;
	private static final int TOTAL_FARM = TOTAL_BUILDING_INDEX + RESOURCE_FOOD;
	private static final int RESOURCE_INDEX = 24;
	private static final int GAINED_RESOURCE_INDEX = RESOURCE_INDEX;
	private static final int GAINED_WOOD = GAINED_RESOURCE_INDEX + RESOURCE_WOOD;
	private static final int GAINED_ORE = GAINED_RESOURCE_INDEX + RESOURCE_ORE;
	private static final int GAINED_FOOD = GAINED_RESOURCE_INDEX + RESOURCE_FOOD;
	private static final int USED_RESOURCE_INDEX = RESOURCE_INDEX + 3;
	private static final int USED_WOOD = USED_RESOURCE_INDEX + RESOURCE_WOOD;
	private static final int USED_ORE = USED_RESOURCE_INDEX + RESOURCE_ORE;
	private static final int USED_FOOD = USED_RESOURCE_INDEX + RESOURCE_FOOD;
	private static final int RESOURSE_LOG_OFFSET = USED_FOOD + 1;
	private static final int GAINED_RESOURSE_LOG_OFFSET = RESOURSE_LOG_OFFSET;
	private static final int USED_RESOURSE_LOG_OFFSET = GAINED_RESOURSE_LOG_OFFSET + RESOURCE_LOG_ENTRIES * 3;
	private static final int VILLAGE_ARRAY_SIZE = USED_RESOURSE_LOG_OFFSET + RESOURCE_LOG_ENTRIES * 3;
	// worker-specific
	private static final int HOME_X = 13;
	private static final int HOME_Y = 14;
	private static final int HOME_TYPE = 15;
	private static final int HOME_ID = 16;
	private static final int HUNGER_TIMER = 17;
	private static final int RESOURCE_X = 18;
	private static final int RESOURCE_Y = 19;

	private static final int HUD_BUTTON_GAP = 16;
	private static final int HUD_BUTTON_SIZE = 40;
	private static final int HUD_BUTTON_OFFSET_X = SCREEN_WIDTH - HUD_BUTTON_SIZE - HUD_BUTTON_GAP;
	private static final int HUD_BUTTON_OFFSET_Y = (SCREEN_HEIGHT - 3*(HUD_BUTTON_SIZE + HUD_BUTTON_GAP))/2;
	
	private static final int HUD_RESOURCES_HEIGHT = 30;
	private static final int HUD_RESOURCES_WIDTH = 70;
	private static final int HUD_RESOURCES_GAP_X = 70;
	private static final int HUD_RESOURCES_BAR_WIDTH = HUD_RESOURCES_WIDTH/2 + 3 * (HUD_RESOURCES_WIDTH + HUD_RESOURCES_GAP_X);
	private static final int HUD_RESOURCES_OFFSET_X = (SCREEN_WIDTH - HUD_RESOURCES_BAR_WIDTH)/2;
	private static final int HUD_RESOURCES_OFFSET_Y = 5;
	
	private static final int DELAY_VILLAGE_RESOURCE = 1200;
	private static final int DELAY_VILLAGE_RESOURCE_HARD = 600;
	private static final int DELAY_WORKER_RESOURCE = 300;
	private static final int DELAY_HUNGER = 1200;
	private static final int DELAY_AI_BUILD = 180;
	private static final int DELAY_AI_MOVE_SHORT = 60;
	private static final int DELAY_AI_MOVE_LONG = 600;
	private static final int DELAY_HARVEST_CROP = 5000;

	private static final int MOUSE_LMB = 0;
	private static final int MOUSE_RMB = 4;
	private static final int MOUSE_X = 16;
	private static final int MOUSE_Y = 17;
	private static final int INPUT_KEY = 18;
	private static final int INPUT_PREVIOUS = 19;
	
	private static final int BUTTON_BUILD_WOODCUTTER_HUT = TYPE_WOODCUTTER_HUT;
	private static final int BUTTON_BUILD_MINE = TYPE_MINE;
	private static final int BUTTON_BUILD_FARM = TYPE_FARM;
	private static final int BUTTON_ZOOM_IN = 4;
	private static final int BUTTON_ZOOM_OUT = 5;
	
	private int[] input = new int[INPUT_PREVIOUS*2];
	
	public void start() {
		new Thread(this).start();
	}

	public void run() {
		
		Graphics2D g2;
		AffineTransform at;
		int[] entity;
		int[] entity0;
		int[] village;
		int i, j;
		int x, y;
		
		Color[] colours = new Color[]{
				new Color(0, 0, 0, 128),
				new Color(255, 0, 0, 128),
				new Color(255, 0, 255, 128),
				new Color(0, 0, 255, 128),
				new Color(0, 255, 255, 128)
		};
		
//		setSize(SCREEN_WIDTH, SCREEN_HEIGHT); // for AppletViewer, remove when using an Applet

		final BufferedImage[] images = new BufferedImage[IMAGE_TOTAL];
		
		// numberOfPolys, r, g, b, numberOfPoints, xpoints, ypoints,
		final String s = 
				  "dapdebdggkdgbjhbecegggjchgebeggkdkggj" // woodcutter hut
				+ "dndbebdggkdgbjhbecegggjchgebeggkdkggj" // mine
				+ "dfapebdggkdgbjhbecegggjchgebeggkdkggj" // farm
				+ "dmmjebdggkdgbjhbecegggjchgebeggkdkggj" // village
				+ "eajddgagiciadadgagikiddadgihkhiafadgagiii" // tree
				+ "baaaebggdlggj" // ore
				+ "bpnaeaggcmggk" // crops 1
				+ "bppaeaggcmggk" // crops 2
				+ "bmnaeaggcmggk" // crops 3
				+ "bkobeaggcmggk" // crops 4
				+ "chcbegfhfhgggeepehghigigg" // worker
				+ "cpaaebbhbhfbfjhbeakbkbaaa" // flag red
				+ "cpapebbhbhfbfjhbeakbkbaaa" // flag magenta
				+ "caapebbhbhfbfjhbeakbkbaaa" // flag blue
				+ "cappebbhbhfbfjhbeakbkbaaa" // flag cyan
				+ "bdbafdgfihijgif"; // boat
		
		// load image data for each entity type
		i = 0;
		for (int z = 0; i < IMAGE_TOTAL; i++) {
			images[i] = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_4BYTE_ABGR_PRE);
			g2 = (Graphics2D)images[i].getGraphics();
			int polys = s.charAt(z++)-'a';
			for (j = 0; j < polys; j++) {
				g2.setColor(new Color((s.charAt(z++)-'a')*16, (s.charAt(z++)-'a')*16, (s.charAt(z++)-'a')*16));
				int[] xPoints = new int[s.charAt(z)-'a'], yPoints = new int[s.charAt(z++)-'a'];
				for (int k = 0; k < xPoints.length; k++) {
					xPoints[k] = s.charAt(z++)-'a';
					yPoints[k] = s.charAt(z++)-'a';
				}
				g2.fillPolygon(xPoints, yPoints, xPoints.length);
			}
		}
		
		BufferedImage screen = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D appletGraphics = (Graphics2D)getGraphics();

		int gameState = GAME_STATE_INIT; 
		int levelTime = 0; // in frames
		
		int[][][] world = null;
		int[][] entities = new int[32767][];
		int[] layers = new int[32767];
		
		BufferedImage worldImage = null;
		BufferedImage mouseMapImage = null;
		
		int activeBuildButton = -1;
		int selectedVillageId = 0;
		int mouseOverVillageId = -1;
		int entitiesTotal = 0;
		int villagesTotal = 0;
		
		int tilex = -1, tiley = -1;
		int camerax = 0, cameray = 0;
		double viewScale = 2;
		boolean hardMode = false;
		
		long nextFrameTime = System.nanoTime();

		// main game loop
		while (true) {
				
			// initialise a new game
			if (gameState == GAME_STATE_INIT || gameState > GAME_STATE_ACTIVE && input[INPUT_KEY] == 1) {
				world = new int[MAP_SIZE+1][MAP_SIZE+1][WORLD_ARRAY_SIZE];
				entitiesTotal = 0;
				viewScale = 2;
				levelTime = 0;
				activeBuildButton = -1;
				selectedVillageId = 0; // select first village (this is always the player's village)
				
				// make the world
				// 2 iterations of noise: height map, followed by trees
				for (int k = HEIGHT_MAP; k <= TREE_MAP; k++) {
					
					// add some features
					for (int z = 0; z < 400; z++) {
						// feature centre point
						x = (int)(java.lang.Math.random() * (MAP_SIZE+1));
						y = (int)(java.lang.Math.random() * (MAP_SIZE+1));
						// feature height and radius
						int size = (int)(java.lang.Math.random() * (MAP_SIZE/8));
						for (i = x-size; i <= x+size; i++) {
							for (j = y-size; j <= y+size; j++) {
								if (i >= 0 && i < MAP_SIZE+1 && j >= 0 && j < MAP_SIZE+1) {
									// feature impact is reduced based on distance from feature centre point
									world[i][j][k] += size - java.lang.Math.max(java.lang.Math.abs(x-i),  java.lang.Math.abs(y-j));
								}
							}
						}
					}

					// calculate average height
					int avg = 0;
					for (i = 0; i < MAP_SIZE+1; i++) {
						for (j = 0; j < MAP_SIZE+1; j++) {
							avg+=world[i][j][k];
						}
					}
					avg/=((MAP_SIZE+1)*(MAP_SIZE+1));
					
					// translate cell values around the average:
					// - any values less than the average are set to 0
					// - the remaining values increase in steps of 10, i.e. 0, 10, 20... 
					for (i = 0; i < MAP_SIZE+1; i++) {
						for (j = 0; j < MAP_SIZE+1; j++) {
							world[i][j][k] = java.lang.Math.max(0, 10 + (world[i][j][k] - avg)/10 * 10);
						}
					}
				}
				// terraform using the height map + tree map
				for (i = 0; i < MAP_SIZE; i++) {
					for (j = 0; j < MAP_SIZE; j++) {
						// if opposite corners have the same height, then make this as the centre height
						int centreHeight = 0;
						int h0 = world[i][j][HEIGHT_MAP];
						int h1 = world[i][j+1][HEIGHT_MAP];
						int h2 = world[i+1][j+1][HEIGHT_MAP];
						int h3 = world[i+1][j][HEIGHT_MAP];
						// try horizontal corners first
						if (h0 == h2) {
							centreHeight = h0;
						// try vertical corners
						} else if (h1 == h3) {
							centreHeight = h1;
						} else {
							// otherwise use the average
							centreHeight = (h0 + h1 + h2 + h3)/4;
						}
						world[i][j][TILE_CENTRE_HEIGHT] = centreHeight;
						world[i][j][CONTENT_TYPE] = EMPTY_TILE;
						if (centreHeight <= 0) {
							world[i][j][CONTENT_TYPE] = TYPE_WATER;
						} else if (!(h0 == h1 && h1 == h2 && h2 == h3)) {
							world[i][j][CONTENT_TYPE] = TYPE_HILL;
						} else if (world[i][j][TREE_MAP] > 10) {
							world[i][j][CONTENT_TYPE] = TYPE_TREE;
						} else if (centreHeight > 10 && java.lang.Math.random()*3000 < centreHeight) {
							world[i][j][CONTENT_TYPE] = TYPE_ORE;
						}
					}
				}

				// add villages 
				// divide map into a 3x3 grid, and attempt to add one village to each cell in this grid
				for (i = 0; i < 9; i++) {
					village = new int[VILLAGE_ARRAY_SIZE];
					for (j = 0; j < 256; j++) {
						x = 12 + i%3*38 + (int)(java.lang.Math.random()*28);
						y = 12 + i/3*38 + (int)(java.lang.Math.random()*28);
						if (world[y][x][CONTENT_TYPE] == EMPTY_TILE) {
							village[ENTITY_TYPE] = TYPE_VILLAGE;
							village[ENTITY_ID] = entitiesTotal;
							village[TIMER] = DELAY_VILLAGE_RESOURCE;
							village[X] = x * TILE_SIZE + TILE_SIZE/2;
							village[Y] = y * TILE_SIZE + TILE_SIZE/2;
							village[VILLAGE_X] = village[X];
							village[VILLAGE_Y] = village[Y];
							village[VILLAGE_ID] = entitiesTotal;
							village[TOTAL_WOOD] = 30;
							village[TOTAL_FOOD] = 10;
							village[CREATE_BUILDING_TYPE] = -1;
							// assign 4 corner villages to player/AI
							village[OWNER] = 
									entitiesTotal == 0? OWNER_PLAYER: 
									entitiesTotal == 2? 1: 
									entitiesTotal == 6? 2: 
									entitiesTotal == 8? 3: 
									OWNER_NONE;
							if (i == 0) {
								// camera centres on the player's village
								camerax = X_RATIO_INT*(village[X] - village[Y])/20;
								cameray = Y_RATIO_INT*(village[X] + village[Y])/20;
							}
							world[y][x][CONTENT_TYPE] = TYPE_VILLAGE;
							world[y][x][CONTENT_ID] = entitiesTotal;
							for (int x0 = x-2; x0 <= x+2; x0++) {
								for (int y0 = y-2; y0 <= y+2; y0++) {
									world[y0][x0][NEARBY_VILLAGE_ID] = entitiesTotal+1; // saves initialising this field to -1 for all cells
								}
							}
							layers[entitiesTotal] = entitiesTotal;
							entities[entitiesTotal++] = village;
							break;
						}
					} 

				}
				villagesTotal = entitiesTotal;

				// render world to an image
				worldImage = new BufferedImage(MAP_WIDTH, MAP_HEIGHT + MAP_VERTICAL_OFFSET*2, BufferedImage.TYPE_4BYTE_ABGR_PRE);
				g2 = (Graphics2D)worldImage.getGraphics();
				mouseMapImage = new BufferedImage(MAP_WIDTH, MAP_HEIGHT + MAP_VERTICAL_OFFSET*2, BufferedImage.TYPE_4BYTE_ABGR_PRE);
				Graphics2D g3 = (Graphics2D)mouseMapImage.getGraphics();
				at = g2.getTransform();
				for (i = 0; i < MAP_SIZE; i++) {
					for (j = 0; j < MAP_SIZE; j++) {
						g2.setTransform(at);
						g3.setTransform(at);
						x = TILE_WIDTH * (i - j + MAP_SIZE)/2;
						y = TILE_HEIGHT * (i + j)/2;
						g2.translate(x, y + MAP_VERTICAL_OFFSET);
						g3.translate(x, y + MAP_VERTICAL_OFFSET);
						
						int centreHeight = world[j][i][TILE_CENTRE_HEIGHT];

						// draw this tile as 4 separate triangles
						// Take 2 adjacent corners, make a triangle between these points and the centre.
						// The corners are looped in the following manner: top right (i=1,j=0); bottom right (i=1,j=1); bottom left (i=0,j=1); top left (i=0,j=0);
						for (int count = 0, i0 = 0, j0 = 0, i1 = 0, j1 = 0; count < 4; count++, i0 = i1, j0 = j1) {
							i1 = (3>>count)&1; // 1,1,0,0
							j1 = (6>>count)&1; // 0,1,1,0
							int h0 = world[j+j0][i+i0][HEIGHT_MAP];
							int h1 = world[j+j1][i+i1][HEIGHT_MAP];
							if (centreHeight > 0 || h0 > 0 || h1 > 0) {
								// normal ground or shoreline - increment triangle colour by 'light intensity'
								int c0 = (i1-j1)*(h0 - centreHeight) - (i0-j0)*(h1 - centreHeight);
								int c1 = (i0-j0)*(h0 - centreHeight) + (i1-j1)*(h1 - centreHeight);
								int c = (32 * (c1 + 10) + 6 * (c0 + 10))/10;
								if (centreHeight < 10 || h0 <= 0 || h1 <= 0) {
									// shoreline
									g2.setColor(new Color(150 + c, 130 + c, 80));
								} else {
									// normal ground
									g2.setColor(new Color(80 + c/2, 127 + c, 80));
								}
							} else {
								// water
								g2.setColor(new Color(10, 110, 210));
							}
							
							g2.fillPolygon(
									new int[]{
											TILE_WIDTH/2 * (i0-j0), 
											TILE_WIDTH/2 * (i1-j1), 
											0
									}, 
									new int[]{
											TILE_HEIGHT/2 * (i0+j0) - HEIGHT_INCREMENT * h0/10, 
											TILE_HEIGHT/2 * (i1+j1) - HEIGHT_INCREMENT * h1/10, 
											TILE_HEIGHT/2 - HEIGHT_INCREMENT * centreHeight/10
									}, 
									3);
							g3.setColor(new Color(0, i+1, j+1));
							g3.fillPolygon(
									new int[]{
											TILE_WIDTH/2 * (i0-j0), 
											TILE_WIDTH/2 * (i1-j1), 
											0
									}, 
									new int[]{
											TILE_HEIGHT/2 * (i0+j0) - HEIGHT_INCREMENT * h0/10, 
											TILE_HEIGHT/2 * (i1+j1) - HEIGHT_INCREMENT * h1/10, 
											TILE_HEIGHT/2 - HEIGHT_INCREMENT * centreHeight/10
									}, 
									3);
						}
						// draw tree/ore if we have one
						if (world[j][i][CONTENT_TYPE] == TYPE_TREE || world[j][i][CONTENT_TYPE] == TYPE_ORE) {
							g2.drawImage(images[world[j][i][CONTENT_TYPE]], -IMAGE_WIDTH/2, TILE_HEIGHT/2 - HEIGHT_INCREMENT * centreHeight/10 - IMAGE_HEIGHT/2, null);
						}
					}
				}
				gameState = GAME_STATE_ACTIVE;
			}

			// fixed frame rate at 60 fps
			while (System.nanoTime() > nextFrameTime) {
				nextFrameTime += 16666667;
				levelTime++;
				int resourceMonitorIndex = levelTime/RESOURCE_LOG_PERIOD%RESOURCE_LOG_ENTRIES;
				
				// UPDATE
				
				// move camera if mouse at edge of screen
				if (camerax >= -MAP_WIDTH/2 && input[MOUSE_X] < 10) camerax-=4;
				if (camerax <= MAP_WIDTH/2 && input[MOUSE_X] > (SCREEN_WIDTH-10)) camerax+=4;
				if (cameray >= 0 && input[MOUSE_Y] < 10) cameray-=4;
				if (cameray <= MAP_HEIGHT && input[MOUSE_Y] > (SCREEN_HEIGHT-10)) cameray+=4;

				tilex = -1;
				tiley = -1;
				if (gameState == GAME_STATE_ACTIVE) {
					
					if (input[INPUT_KEY] > input[INPUT_PREVIOUS+INPUT_KEY]) {
						hardMode = !hardMode; 
					}
					
					// transform screen coordinates into world coordinates
					if (input[MOUSE_X] < HUD_BUTTON_OFFSET_X && input[MOUSE_Y] > HUD_RESOURCES_HEIGHT) {
						x = (int)((input[MOUSE_X] - SCREEN_WIDTH/2)/viewScale + camerax + MAP_WIDTH/2);
						y = (int)((input[MOUSE_Y] - SCREEN_HEIGHT/2)/viewScale + cameray + MAP_VERTICAL_OFFSET);
						if (x > 0 && x < MAP_WIDTH && y > 0 && y < MAP_HEIGHT + MAP_VERTICAL_OFFSET*2) {
							i = mouseMapImage.getRGB(x, y);
							tilex = ((i >> 8 & 255) - 1);
							tiley = ((i & 255) - 1);
						}
					}
					
					// work out the mouseover village
					mouseOverVillageId = -1;
					if (tilex >= 0 && activeBuildButton == -1) {
						mouseOverVillageId = world[tiley][tilex][NEARBY_VILLAGE_ID]-1;
					}
				}

				village = entities[selectedVillageId];
	
				// clicked left mouse button
				if (input[MOUSE_LMB] > input[INPUT_PREVIOUS + MOUSE_LMB]) {
					if (input[MOUSE_X] >= HUD_BUTTON_OFFSET_X) {
						// clicked in button area
						int clicked = (input[MOUSE_Y] + (HUD_BUTTON_SIZE - HUD_BUTTON_OFFSET_Y + HUD_BUTTON_GAP + HUD_BUTTON_GAP/2))/(HUD_BUTTON_SIZE + HUD_BUTTON_GAP) - 1;
						switch (clicked) {
						case BUTTON_BUILD_WOODCUTTER_HUT:
						case BUTTON_BUILD_MINE:
						case BUTTON_BUILD_FARM:
							activeBuildButton = clicked == activeBuildButton? -1: clicked;
							break;
						case BUTTON_ZOOM_IN:
							if (viewScale < 4) viewScale *= 2;
							break;
						case BUTTON_ZOOM_OUT:
							if (viewScale > 0.5) viewScale /= 2;
							break;
						}
					} else if (tilex >= 0) {
						// left click on empty part of map - build active building
						if (activeBuildButton >= 0 && world[tiley][tilex][CONTENT_TYPE] == EMPTY_TILE && village[TOTAL_WOOD] >= COST_BUILDING) {
							village[TARGET_X] = tilex * TILE_SIZE + TILE_SIZE/2;
							village[TARGET_Y] = tiley * TILE_SIZE + TILE_SIZE/2;
							village[CREATE_BUILDING_TYPE] = activeBuildButton;
							activeBuildButton = -1; // deactivate build button
						}
						// left-click village - select clicked village
						if (mouseOverVillageId >= 0 && entities[mouseOverVillageId][OWNER] == OWNER_PLAYER) {
							selectedVillageId = mouseOverVillageId;
						}
					}
				}
				
				// right-click village - move soldiers from selected village to clicked village
				if (input[MOUSE_RMB] > input[INPUT_PREVIOUS + MOUSE_RMB] 
						&& mouseOverVillageId >= 0 && mouseOverVillageId != selectedVillageId) {
					village[TARGET_X] = entities[mouseOverVillageId][X];
					village[TARGET_Y] = entities[mouseOverVillageId][Y];
					village[INVADE_VILLAGE_STATE] = 1;
				}
				
				// remember the input state for next time
				for (i = 0; i < INPUT_PREVIOUS; i++) {
					input[INPUT_PREVIOUS + i] = input[i];
				}
				
				// update entities
				for (i = 0; i < entitiesTotal; i++) {
					entity = entities[i];
					if (entity[STATE] == STATE_ALIVE) {
						village = null;
						int entityTilex = entity[X]/TILE_SIZE;
						int entityTiley = entity[Y]/TILE_SIZE;
					
						// make sure entity owner matches village owner
						if (entity[ENTITY_TYPE] != TYPE_CROPS) {
							village = entities[entity[VILLAGE_ID]];
							entity[OWNER] = village[OWNER];
						}
						
						if (entity[ENTITY_TYPE] == TYPE_WORKER) {
							// is the worker hungry?
							if (entity[HUNGER_TIMER] > 0) {
								entity[HUNGER_TIMER]--;
							} else if (entity[HOME_TYPE] != TYPE_FARM){ // note - farmers never get hungry
								entity[HUNGER_TIMER] = DELAY_HUNGER;
								// try to eat some food
								// note - update resource log regardless of whether there is enough food, so player can see how much food is required  
								village[USED_RESOURSE_LOG_OFFSET + RESOURCE_FOOD * RESOURCE_LOG_ENTRIES + resourceMonitorIndex]++;
								if (village[TOTAL_FOOD] > 0) {
									village[TOTAL_FOOD]--;
								} else {
									// not enough food
									if (entity[HOME_TYPE] == TYPE_VILLAGE) {
										// soldier - dies
										entity[STATE] = STATE_DEAD;
										if (entity[TASK] == TASK_IDLE) {
											village[TOTAL_SOLDIERS]--;
										}
									} else {
										// otherwise - go home and wait for a while
										entity[TARGET_X] = entity[HOME_X];
										entity[TARGET_Y] = entity[HOME_Y];
										entity[TIMER] = DELAY_HUNGER;
										entity[TASK] = TASK_IDLE;
									}
								}
							}
							// try to move closer to target
							if (entity[TARGET_X] > -1) {
								x = entity[TARGET_X] - entity[X];
								y = entity[TARGET_Y] - entity[Y];
								if (x == 0 && y == 0) {
									// reached target
									entity[TARGET_X] = -1;
								} else {
									// not reached target - move towards it
									entity[X] += x == 0? 0: x < 1? -1: 1;
									entity[Y] += y == 0? 0: y < 1? -1: 1;
								}
							} else if (entity[TIMER] > 0) {
								entity[TIMER]--;
							} else {
								// not moving and idle - perform the appropriate task
								switch (entity[TASK]) {
								case TASK_IDLE:
									if (entity[HOME_TYPE] == TYPE_FARM) {
										// farmer - find any nearby crops ready for harvest
										out1:
										for (y = entityTiley-CROP_RADIUS; y <= entityTiley+CROP_RADIUS; y++) {
											for (x = entityTilex-CROP_RADIUS; x <= entityTilex+CROP_RADIUS; x++) {
												if (x >= 0 && x < MAP_SIZE && y >= 0 && y < MAP_SIZE && world[y][x][CONTENT_TYPE] == TYPE_CROPS) {
													entity0 = entities[world[y][x][CONTENT_ID]];
													if (entity0[TIMER] == 0) {
														// farm the crop
														entity[TARGET_X] = entity0[X];
														entity[TARGET_Y] = entity0[Y];
														entity[TIMER] = DELAY_WORKER_RESOURCE;
														entity[TASK] = TASK_GATHER_RESOURCE;
														break out1;
													}
												}
											}
										}
									} else if (entity[HOME_TYPE] != TYPE_VILLAGE) {
										// woodcutter/miner - move to the nearest appropriate resource
										entity[TARGET_X] = entity[RESOURCE_X];
										entity[TARGET_Y] = entity[RESOURCE_Y];
//										entity[TIMER] = DELAY_WORKER_RESOURCE;
										entity[TIMER] = (DELAY_WORKER_RESOURCE - 50) + 50 * village[TOTAL_BUILDING_INDEX + entity[HOME_TYPE]];
										entity[TASK] = TASK_GATHER_RESOURCE;
									}
									break;
								case TASK_GATHER_RESOURCE:
									if (entity[HOME_TYPE] == TYPE_FARM) {
										entities[world[entityTiley][entityTilex][CONTENT_ID]][TIMER] = DELAY_HARVEST_CROP + (int)(java.lang.Math.random()*DELAY_HARVEST_CROP);
									}
									entity[TARGET_X] = entity[VILLAGE_X];
									entity[TARGET_Y] = entity[VILLAGE_Y];
									entity[TASK] = TASK_DEPOSIT_RESOURCE;
									break;
								case TASK_DEPOSIT_RESOURCE:
									entity[TARGET_X] = entity[HOME_X];
									entity[TARGET_Y] = entity[HOME_Y];
									entity[TASK] = TASK_IDLE;
									if (village[TOTAL_RESOURCE_INDEX + entity[HOME_TYPE]] < MAX_RESOURCES) {
										village[TOTAL_RESOURCE_INDEX + entity[HOME_TYPE]]++;
									}
									village[GAINED_RESOURSE_LOG_OFFSET + entity[HOME_TYPE] * RESOURCE_LOG_ENTRIES + resourceMonitorIndex]++;
									break;
								case TASK_INVADE_VILLAGE:
									int[] invadedVillage = entities[world[entityTiley][entityTilex][CONTENT_ID]];
									// occupied enemy village - if defended, then kill this attacker plus one defender
									if (invadedVillage[OWNER] != entity[OWNER] && invadedVillage[TOTAL_SOLDIERS] > 0) {
										// kill attacker
										entity[STATE] = STATE_DEAD;
										// find and kill defender
										for (j = 0; j < entitiesTotal; j++) {
											entity0 = entities[j];
											if (entity0[STATE] == STATE_ALIVE && entity0[ENTITY_TYPE] == TYPE_WORKER 
													&& entity0[HOME_ID] == invadedVillage[ENTITY_ID] && entity0[TASK] == TASK_IDLE) {
												invadedVillage[TOTAL_SOLDIERS]--;
												entity0[STATE] = STATE_DEAD;
												break;
											}
										}
									} else {
										// unoccupied or friendly village
										invadedVillage[TOTAL_SOLDIERS]++;
										entity[HOME_X] = invadedVillage[X];
										entity[HOME_Y] = invadedVillage[Y];
										entity[HOME_ID] = invadedVillage[ENTITY_ID];
										entity[VILLAGE_X] = invadedVillage[X];
										entity[VILLAGE_Y] = invadedVillage[Y];
										entity[VILLAGE_ID] = invadedVillage[ENTITY_ID];
										entity[TASK] = TASK_IDLE;
										
										// make sure AI doesn't immediately move troops out of the invaded village 
										invadedVillage[INVADE_VILLAGE_AI_TIMER] = levelTime + DELAY_AI_MOVE_LONG;
										
										// if this soldier has taken over a new village, then check if the player has won/lost
										if (invadedVillage[OWNER] != entity[OWNER]) {
											invadedVillage[OWNER] = entity[OWNER];
											if (gameState == GAME_STATE_ACTIVE) {
												int playerVillageId = -1;
												int totalComputerVillages = 0;
												for (j = 0; j < villagesTotal; j++) {
													if (entities[j][OWNER] == OWNER_PLAYER) {
														playerVillageId = j;
													}
													if (entities[j][OWNER] > OWNER_PLAYER) {
														totalComputerVillages++;
													}
												}
												// player has no more villages => player loses
												if (playerVillageId == -1) {
													gameState = GAME_STATE_LOST;
												} else if (invadedVillage[ENTITY_ID] == selectedVillageId) {
													// player's selected village has been taken - select the next available village
													selectedVillageId = playerVillageId;
												}
												// computer has no more villages => player wins
												if (totalComputerVillages == 0) {
													gameState = GAME_STATE_WON;
												}
											}
										}
									}
								}
							}
						} else {
							if (entity[TIMER] > 0) {
								entity[TIMER]--;
							}
		
							if (entity[ENTITY_TYPE] == TYPE_VILLAGE && entity[OWNER] > OWNER_NONE) {
								// update running resource totals every second, based on the previous RESOURCE_LOG_PERIOD seconds of data
								if (levelTime%RESOURCE_LOG_PERIOD == 0) {
									for (j = 0; j < 6; j++) {
										entity[RESOURCE_INDEX + j] += entity[RESOURSE_LOG_OFFSET + j * RESOURCE_LOG_ENTRIES + (levelTime-1)/RESOURCE_LOG_PERIOD%RESOURCE_LOG_ENTRIES] 
												- entity[RESOURSE_LOG_OFFSET + j * RESOURCE_LOG_ENTRIES + resourceMonitorIndex];
										entity[RESOURSE_LOG_OFFSET + j * RESOURCE_LOG_ENTRIES + resourceMonitorIndex] = 0;
									}
								}
								if (entity[OWNER] > OWNER_PLAYER) {
//								if (entity[OWNER] > -1) { // make AI play against itself
									// village AI
									// decide if the AI should move troops somewhere
									if (entity[TOTAL_FOOD] > 0 && levelTime >= entity[INVADE_VILLAGE_AI_TIMER] && entity[TOTAL_SOLDIERS] > 1) {
										int bestScore = -1;
										int bestIndex = -1;
										for (j = 0; j < villagesTotal; j++) {
											if (j != i) {
												entity0 = entities[j];
//												int dist = java.lang.Math.max(java.lang.Math.abs(entity[X]-entity0[X]), java.lang.Math.abs(entity[Y]-entity0[Y]))/MAP_SIZE;
												int armyDifference = entity[TOTAL_SOLDIERS] - entity0[TOTAL_SOLDIERS];
												int score = entity[TOTAL_FOOD]/2 + armyDifference 
														- java.lang.Math.max(java.lang.Math.abs(entity[X]-entity0[X]), java.lang.Math.abs(entity[Y]-entity0[Y]))/MAP_SIZE;
												if (entity0[TOTAL_SOLDIERS] == 0) {
													// bonus if village is empty
													score += 2;
												}
												if (entity0[OWNER] != entity[OWNER]) {
													// bonus if village is an unowned, or an enemy
													score += 2;
												} else if (armyDifference <= 1) {
													// penalty if friendly village whose army size is close to or bigger than that of this village
													score -= 32;
												}
												if (score > bestScore) {
													bestIndex = j;
													bestScore = score;
												}
											}
										}
										// short delay before next troop move, may send multiple soldiers to target
										int delay = DELAY_AI_MOVE_SHORT;
										if (bestScore > 5) {
											entity0 = entities[bestIndex];
											entity[TARGET_X] = entity0[X];
											entity[TARGET_Y] = entity0[Y];
											entity[INVADE_VILLAGE_STATE] = 1;
											// target is empty => long delay before next troop move, will only send one soldier to target
											if (entity0[TOTAL_SOLDIERS] == 0) delay = DELAY_AI_MOVE_LONG;
										}
										// wait a while before checking/moving again
										entity[INVADE_VILLAGE_AI_TIMER] = levelTime + delay;
									}
									
									// decide if the AI should build something 
									// (note - move troops and build are mutually exclusive: they use the same fields for targeting!)
									if (entity[INVADE_VILLAGE_STATE] == 0 && entity[TOTAL_WOOD] >= COST_BUILDING 
											&& levelTime >= entity[CREATE_BUILDING_AI_TIMER]) {
										int bestScore = -1;
										int bestIndex = -1;
										for (j = 0; j < 3; j++) {
											// less likely to build if we have enough ore for a soldier
											int score = 0;
											if (entity[TOTAL_ORE] >= COST_SOLDIER_ORE) {
												score -= 32;
											}
											if (j <= RESOURCE_ORE) {
												// opening strategy for wood: AI1 => 2 woodcutter, AI2 => 3 woodcutter, AI3 => 4 woodcutter
												// opening strategy for ore: AI1 => 1 mine, AI2 => 1 mine, AI3 => 2 mine
												if (entity[TOTAL_BUILDING_INDEX+j] < (1+entity[OWNER])>>j) {
													score += 16>>j;
												}
												// mid game: build up to 8 each of woodcutter/mine 
												if (levelTime > 14000) {
													score+=8-entity[TOTAL_BUILDING_INDEX+j];
												}
											} else if (entity[USED_FOOD] > entity[GAINED_FOOD]) {
												// more likely to build a farm if the village has low food and a food deficit
												score += (8 - entity[TOTAL_FOOD])*12;
											}
											if (score > bestScore) {
												bestIndex = j;
												bestScore = score;
											}
										}
										if (bestScore > 0) {
											// find somewhere to build
											for (int dist = 3; dist < 20; dist+=2) {
												x = entityTilex + 2*((int)(java.lang.Math.random()*dist)-dist/2);
												y = entityTiley + 2*((int)(java.lang.Math.random()*dist)-dist/2);
												if (x >=0 && x < MAP_SIZE && y >= 0 && y < MAP_SIZE 
														&& world[y][x][CONTENT_TYPE] == EMPTY_TILE) {
													entity[TARGET_X] = x * TILE_SIZE + TILE_SIZE/2;
													entity[TARGET_Y] = y * TILE_SIZE + TILE_SIZE/2;
													entity[CREATE_BUILDING_TYPE] = bestIndex;
													break;
												}
											}
										}
										// wait a while before checking/building again
										entity[CREATE_BUILDING_AI_TIMER] = levelTime + DELAY_AI_BUILD;
									}
								}
								
								if (entity[TIMER] == 0) {
									// slow background increase in resources
									for (j = 0; j < 3; j++) {
										if (entity[TOTAL_RESOURCE_INDEX + j] < MAX_RESOURCES) {
											entity[TOTAL_RESOURCE_INDEX + j]++;
										}
										entity[GAINED_RESOURSE_LOG_OFFSET + j * RESOURCE_LOG_ENTRIES + resourceMonitorIndex]++;
									}
									entity[TIMER] = hardMode && entity[OWNER] > OWNER_PLAYER? DELAY_VILLAGE_RESOURCE_HARD: DELAY_VILLAGE_RESOURCE;
								}
								// if we have enough ore and wood in this village, create a soldier
								// (note - we must have enough wood for both soldier and building, so it is always possible to build) 
								if (entity[TOTAL_ORE] >= COST_SOLDIER_ORE && entity[TOTAL_WOOD] >= COST_SOLDIER_WOOD + COST_BUILDING) {
									entity[TOTAL_ORE] -= COST_SOLDIER_ORE;
									entity[TOTAL_WOOD] -= COST_SOLDIER_WOOD;
									entity[USED_RESOURSE_LOG_OFFSET + RESOURCE_ORE * RESOURCE_LOG_ENTRIES + resourceMonitorIndex] += COST_SOLDIER_ORE;
									entity[USED_RESOURSE_LOG_OFFSET + RESOURCE_WOOD * RESOURCE_LOG_ENTRIES + resourceMonitorIndex] += COST_SOLDIER_WOOD;
									entity[TASK] = TASK_CREATE_WORKER;
									entity[TOTAL_SOLDIERS]++;
								}
								// add building to the centre of the tile
								if (entity[CREATE_BUILDING_TYPE] >= 0) {
									entity[TOTAL_WOOD] -= COST_BUILDING;
									entity[USED_RESOURSE_LOG_OFFSET + RESOURCE_WOOD * RESOURCE_LOG_ENTRIES + resourceMonitorIndex] += COST_BUILDING;
									entity[TOTAL_BUILDING_INDEX + entity[CREATE_BUILDING_TYPE]]++;
//									entity0 = new int[]{
//											entity[CREATE_BUILDING_TYPE],
//											entitiesTotal, 
//											0, 
//											TASK_CREATE_WORKER, 
//											entity[TARGET_X],
//											entity[TARGET_Y],
//											STATE_ALIVE,
//											entity[X], 
//											entity[Y], 
//											i,
//											entity[OWNER]
//									};
									entity0 = new int[11];
									entity0[ENTITY_TYPE] = entity[CREATE_BUILDING_TYPE];
									entity0[ENTITY_ID] = entitiesTotal;
									entity0[TASK] = TASK_CREATE_WORKER;
									entity0[X] = entity[TARGET_X];
									entity0[Y] = entity[TARGET_Y];
									entity0[VILLAGE_X] = entity[X];
									entity0[VILLAGE_Y] = entity[Y];
									entity0[VILLAGE_ID] = i;
									entity0[OWNER] = entity[OWNER];
									
									int buildTilex = entity[TARGET_X]/TILE_SIZE;
									int buildTiley = entity[TARGET_Y]/TILE_SIZE;
									world[buildTiley][buildTilex][CONTENT_TYPE] = entity[CREATE_BUILDING_TYPE];
									world[buildTiley][buildTilex][CONTENT_ID] = entitiesTotal;
									layers[entitiesTotal] = entitiesTotal;
									entities[entitiesTotal++] = entity0;
									// if this is a farm, then add crops in the surrounding flat tiles that are not already occupied
									if (entity[CREATE_BUILDING_TYPE] == TYPE_FARM) {
										for (x = -CROP_RADIUS; x <= CROP_RADIUS; x++) {
											for (y = -CROP_RADIUS; y <= CROP_RADIUS; y++) {
												int x0 = (x+buildTilex);
												int y0 = (y+buildTiley);
												if (x0 >= 0 && x0 < MAP_SIZE && y0 >= 0 && y0 < MAP_SIZE 
														&& world[y0][x0][CONTENT_TYPE] == EMPTY_TILE 
														&& x*x + y*y <= CROP_RADIUS * CROP_RADIUS) {
//													entity0 = new int[]{
//															TYPE_CROPS, 
//															entitiesTotal,
//															(int)(java.lang.Math.random()*DELAY_HARVEST_CROP),
//															TASK_IDLE,
//															x0 * TILE_SIZE + TILE_SIZE/2,
//															y0 * TILE_SIZE + TILE_SIZE/2,
//															STATE_ALIVE,
//													};
													entity0 = new int[7];
													entity0[ENTITY_TYPE] = TYPE_CROPS;
													entity0[ENTITY_ID] = entitiesTotal;
													entity0[TIMER] = (int)(java.lang.Math.random()*DELAY_HARVEST_CROP);
													entity0[X] = x0 * TILE_SIZE + TILE_SIZE/2;
													entity0[Y] = y0 * TILE_SIZE + TILE_SIZE/2;
													
													world[y0][x0][CONTENT_TYPE] = TYPE_CROPS;
													world[y0][x0][CONTENT_ID] = entitiesTotal;
													layers[entitiesTotal] = entitiesTotal;
													entities[entitiesTotal++] = entity0;
												}
											}
										}
									}
									entity[CREATE_BUILDING_TYPE] = -1;
								}
								if (entity[INVADE_VILLAGE_STATE] > 0) {
									for (j = 0; j < entitiesTotal; j++) {
										entity0 = entities[j];
										if (entity0[STATE] == STATE_ALIVE && entity0[ENTITY_TYPE] == TYPE_WORKER 
												&& entity0[HOME_ID] == entity[ENTITY_ID] && entity0[TASK] == TASK_IDLE) {
											// move soldier to target village
											entity[TOTAL_SOLDIERS]--;
											entity0[TARGET_X] = entity[TARGET_X];
											entity0[TARGET_Y] = entity[TARGET_Y];
											entity0[TASK] = TASK_INVADE_VILLAGE;
											break;
										}
									}
									entity[INVADE_VILLAGE_STATE] = 0;
								}
							}
							if (entity[TASK] == TASK_CREATE_WORKER) {
								// reuse dead entity array slot if possible
								int entityId = -1;
								for (j = 0; j < entitiesTotal; j++) {
									if (entities[j][STATE] == STATE_DEAD) {
										entityId = j;
										break;
									}
								}
								if (entityId == -1) {
									layers[entitiesTotal] = entitiesTotal;
									entityId = entitiesTotal++;
								}
								
//								entity0 = new int[]{
//										TYPE_WORKER, 
//										entityId,
//										0, 
//										TASK_IDLE, 
//										entity[X],
//										entity[Y],
//										STATE_ALIVE,
//										entity[VILLAGE_X], 
//										entity[VILLAGE_Y], 
//										entity[VILLAGE_ID], 
//										entity[OWNER],
//										-1, 
//										-1,
//										entity[X], 
//										entity[Y], 
//										entity[ENTITY_TYPE], 
//										entity[ENTITY_ID], 
//										0,
//										0,
//										0
//								};
								entity0 = new int[20];
								entity0[ENTITY_TYPE] = TYPE_WORKER;
								entity0[ENTITY_ID] = entityId;
								entity0[X] = entity[X];
								entity0[Y] = entity[Y];
								entity0[VILLAGE_X] = entity[VILLAGE_X];
								entity0[VILLAGE_Y] = entity[VILLAGE_Y];
								entity0[VILLAGE_ID] = entity[VILLAGE_ID];
								entity0[OWNER] = entity[OWNER];
								entity0[TARGET_X] = -1;
								entity0[TARGET_Y] = -1;
								entity0[HOME_X] = entity[X];
								entity0[HOME_Y] = entity[Y];
								entity0[HOME_TYPE] = entity[ENTITY_TYPE];
								entity0[HOME_ID] = entity[ENTITY_ID];
								
								entities[entityId] = entity0;
								entity[TASK] = TASK_IDLE;
								
								// for woodcutters or miners, find the nearest target resource
								if (entity[ENTITY_TYPE] <= TYPE_MINE) {
									// square-spiral out from home tile, until resource is located 
									int x0 = entityTilex;
									int y0 = entityTiley;
									x = 0;
									y = 1;
									out1:
									for (int z = 2, sx = 0; z < MAP_SIZE*4; z++, sx=x, x=y, y=-sx) {
										for (j = 0; j < z/2; j++) {
											x0+=x;
											y0+=y;
											if (x0 >= 0 && x0 < MAP_SIZE && y0 >= 0 && y0 < MAP_SIZE && world[y0][x0][CONTENT_TYPE] == entity[ENTITY_TYPE]+4) {
												entity0[RESOURCE_X] = x0 * TILE_SIZE + TILE_SIZE/2;
												entity0[RESOURCE_Y] = (y0+1)*TILE_SIZE; // move to the bottom of tree/ore tile, so it doesn't look like the worker is hovering over the tree
												break out1;
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
			// RENDER
			g2 = (Graphics2D)screen.getGraphics();

			at = g2.getTransform();
			
			// background
			g2.setColor(Color.DARK_GRAY);
			g2.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
			g2.setClip(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

			g2.scale(viewScale, viewScale);

			g2.translate((int)(SCREEN_WIDTH/(2*viewScale)) - camerax, (int)(SCREEN_HEIGHT/(2*viewScale)) - cameray);
			g2.drawImage(worldImage, -MAP_WIDTH/2, -MAP_VERTICAL_OFFSET, null);

			// sort the entities for rendering using insertion sort
			for (i = 1; i < entitiesTotal; i++) {
				entity = entities[layers[i]];
				entity0 = null;
				j = i;
				while (j >= 1 && (entity0 = entities[layers[j-1]]) != null && (entity0[ENTITY_TYPE] != TYPE_CROPS 
						&& (entity[ENTITY_TYPE] == TYPE_CROPS || entity0[X] + entity0[Y] > entity[X] + entity[Y]))) {
					layers[j--] = entity0[ENTITY_ID];
				}
				layers[j] = entity[ENTITY_ID];
			}

			// draw entities
			for (i = 0; i < entitiesTotal; i++) {
				entity = entities[layers[i]];
				if (entity[STATE] == STATE_ALIVE) {
					x = X_RATIO_INT*(entity[X] - entity[Y])/20;
					y = Y_RATIO_INT*(entity[X] + entity[Y])/20 - HEIGHT_INCREMENT*world[entity[Y]/TILE_SIZE][entity[X]/TILE_SIZE][TILE_CENTRE_HEIGHT]/10;
					// don't draw worker if they are at home
					if (!(entity[ENTITY_TYPE] == TYPE_WORKER && entity[X] == entity[HOME_X] && entity[Y] == entity[HOME_Y])) {
						// draw a flag for owned villages
						if (entity[ENTITY_TYPE] == TYPE_VILLAGE && entity[OWNER] > OWNER_NONE) {
							g2.drawImage(images[IMAGE_FLAG_INDEX + entity[OWNER]], x-4, y-12, null);
						}
						// draw a flag for soldiers
						if (entity[ENTITY_TYPE] == TYPE_WORKER && entity[HOME_TYPE] == TYPE_VILLAGE) {
							g2.drawImage(images[IMAGE_FLAG_INDEX + entity[OWNER]], x-2, y-8, null);
						}
						j = entity[ENTITY_TYPE] == TYPE_CROPS? 
								entity[ENTITY_TYPE] + (entity[TIMER] + 3332)/3333: 
							entity[ENTITY_TYPE] == TYPE_WORKER && world[entity[Y]/TILE_SIZE][entity[X]/TILE_SIZE][CONTENT_TYPE] == TYPE_WATER? 
									IMAGE_BOAT:
							entity[ENTITY_TYPE];
						g2.drawImage(images[j], x - IMAGE_WIDTH/2, y - IMAGE_HEIGHT/2, null);
						// draw total soldiers above village
						if (entity[ENTITY_TYPE] == TYPE_VILLAGE) {
							g2.setColor(colours[entity[OWNER]+1]);
							g2.fillRect(x-8, y-30, 16, 16);
							g2.setColor(entity[ENTITY_ID] == selectedVillageId? Color.CYAN: Color.WHITE);
							g2.drawString(String.valueOf(entity[TOTAL_SOLDIERS]), x-(entity[TOTAL_SOLDIERS] < 10? 4: 7), y-18);
							// highlight mouse-over and selected villages
							if (entity[ENTITY_ID] == mouseOverVillageId || entity[ENTITY_ID] == selectedVillageId) {
								g2.fillRect(x-8, y-33, 16, 3);
							}
						}
					}
				}
			}
			
			// draw cursor
			if (tilex >=0) {
				g2.setColor(colours[0]);
				g2.translate(TILE_WIDTH*(tilex - tiley)/2, TILE_HEIGHT*(tilex + tiley)/2 - HEIGHT_INCREMENT * world[tiley][tilex][TILE_CENTRE_HEIGHT]/10);
				g2.fillPolygon(new int[]{0,TILE_WIDTH/2,0,-TILE_WIDTH/2}, new int[]{0,TILE_HEIGHT/2,TILE_HEIGHT,TILE_HEIGHT/2}, 4);
			}

			// HUD
			entity = entities[selectedVillageId];
			g2.setTransform(at);
			g2.setColor(Color.GRAY);
			g2.fillRect(HUD_RESOURCES_OFFSET_X, HUD_RESOURCES_OFFSET_Y, HUD_RESOURCES_BAR_WIDTH, HUD_RESOURCES_HEIGHT);
			g2.setColor(Color.BLACK);
			g2.drawRect(HUD_RESOURCES_OFFSET_X, HUD_RESOURCES_OFFSET_Y, HUD_RESOURCES_BAR_WIDTH, HUD_RESOURCES_HEIGHT);
			for (i = 0; i < 3; i++) {
				// resources
				g2.setTransform(at);
				g2.translate(HUD_RESOURCES_OFFSET_X + HUD_RESOURCES_WIDTH/2 + (HUD_RESOURCES_WIDTH + HUD_RESOURCES_GAP_X)*i, HUD_RESOURCES_OFFSET_Y);
				// - gained
				g2.setColor(Color.GREEN);
				g2.drawString(String.valueOf(entity[GAINED_RESOURCE_INDEX + i]), 75, 14);
				// - spent
				g2.setColor(Color.RED);
				g2.drawString(String.valueOf(entity[USED_RESOURCE_INDEX + i]), 75, 26);
				// - total
				g2.scale(2, 2);
				g2.drawImage(images[IMAGE_RESOURCE_INDEX + i], 5, 2, null);
				g2.setColor(Color.WHITE);
				g2.drawString(String.valueOf(entity[TOTAL_RESOURCE_INDEX + i]), 20, 12);
			}
			
			for (i = 0; i < 6; i++) {
				// buttons
				if (i != 3) {
					g2.setTransform(at);
					g2.translate(HUD_BUTTON_OFFSET_X, HUD_BUTTON_OFFSET_Y + (HUD_BUTTON_SIZE + HUD_BUTTON_GAP)*i);
					g2.setColor(Color.GRAY);
					g2.fillRect(0, 0, HUD_BUTTON_SIZE, HUD_BUTTON_SIZE);
					g2.setColor(i == activeBuildButton? Color.CYAN: Color.BLACK);
					g2.drawRect(0, 0, HUD_BUTTON_SIZE, HUD_BUTTON_SIZE);
					g2.setColor(Color.WHITE);
					g2.scale(2, 2);
					if (i == 4) {
						g2.drawString("+", 7, 14); // zoom in
					} else if (i == 5) {
						g2.drawString("-", 9, 13); // zoom out
					} else {
						g2.drawImage(images[i], 4, 5, null); // build something
					}
				}
			}
			
			g2.setTransform(at);
			
			g2.scale(2, 2);
			if (hardMode) {
				g2.drawString("hard", 346, 15);
			} else {
				g2.drawString("normal", 340, 15);
			}

			// game over
			if (gameState > GAME_STATE_ACTIVE) {
				g2.drawString("Any key restarts", 157, 170);
				g2.scale(2, 2);
				g2.scale(2, 2);
				if (gameState == GAME_STATE_WON) {
					g2.drawString("VICTORY", 23, 37);
				} else {
					g2.drawString("DEFEAT", 26, 37);
				}
			}

			appletGraphics.drawImage(screen, 0, 0, null);

			// wait until ready for the next frame
			while (System.nanoTime() < nextFrameTime);
		}
	}

	public boolean handleEvent(Event e) {
		switch (e.id) {
		case Event.MOUSE_DOWN:
		case Event.MOUSE_UP:
			input[e.modifiers] = e.id&1;
			break;
		case Event.KEY_PRESS:
		case Event.KEY_RELEASE:
		case Event.KEY_ACTION:
		case Event.KEY_ACTION_RELEASE:
			input[INPUT_KEY] = e.id&1;
			break;
		case Event.MOUSE_DRAG:
		case Event.MOUSE_MOVE:
			input[MOUSE_X] = e.x;
			input[MOUSE_Y] = e.y;
		}
		return false;
	}

}
