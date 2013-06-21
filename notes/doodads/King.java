package fogus.patagonia.doodads;

// 4King & Country
//
//An entry to the 2013 Java4K Competition
//by sixtyten
//------------------------------------------------------------
//
//The King has sent you on a voyage of discovery and conquest. 
//You have 100 years to colonise these lands and make your 
//fortune! Can you succeed?
//
//Featuring an "infinite" Perlin noise/fBM-generated terrain, 
//with procedurally randomised resource bonus distributions. 
//This game was inspired by Colonisation, an old favourite of 
//mine, and The Wager, an entry to the Ludum Dare 24 hour game 
//competition.
//
//Compiles/compresses to 4,095 bytes
//with jar, proguard, joga, pack200, 7Zip (gzip format), Deflopt
// http://java4k.com/index.php?action=games&method=view&gid=471#source

import java.applet.Applet;
import java.awt.Event;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
//import java.awt.RenderingHints;		// Not enough space for this ;(

//DEBUG
//import java.text.DecimalFormat;		// Comment in if you want to look at debug text output

@SuppressWarnings("serial")
public class King extends Applet implements Runnable {
	
	// Mouse & key input globals
	private static final int MOUSE_LMB = 0;
	private static final int MOUSE_RMB = 4;
	private static final int MOUSE_X = 16;
	private static final int MOUSE_Y = 17;
	private static final int MOUSE_DRAG = 18;
	private static final int INPUT_KEY = 19;
	private static final int INPUT_PREVIOUS = 1 << 12;
	
	private static final int[] keys = new int[INPUT_PREVIOUS*2];

	
	public void start() {
		
		new Thread(this).start();
	}
	
	
	public void run() {
		
		//#############################################################################
		// INITIALISE CONSTANTS
		//#############################################################################
		
		// Game states
		final int STATE_MOVING = 0;
		final int STATE_DOCKED = 1;
		final int STATE_GAMEOVER = 2;
		
		// Entity states
		final int IS_VISIBLE = 1;
		final int IS_ACTIVE = 2;

		// Key constants
		final int KEY_SPACE = INPUT_KEY + 32;
		final int KEY_D = INPUT_KEY + 100;
		final int KEY_S = INPUT_KEY + 115;
		final int KEY_W = INPUT_KEY + 119;
		
		// Magic number for pseudo-random number generation
		final int MAGIC_HASH = 0x45D9F3B;
		
		// Time-limit
		final int YEAR_START = 1496;
		final int YEAR_COUNT = 100;
		final int SECS_PER_YEAR = 12;
		
		// Game physics
		final float BOUNCE_COEFF = 0.05f;
		final float DRAG_COEFF = 0.9f;
		final float WIND_SPEED = 0.0001f;
		
		// View/camera parameters
		final float VIEW_ZOOM = 0.25f;

		// World map to plot into (= applet pixel size)
		final int WORLD_SIZE_POW = 9;
		final int WORLD_SIZE = 1 << WORLD_SIZE_POW;	
		
		// Mini-map
		final int MINIMAP_SIZE = 1 << 12;
		final int MINIMAP_OCTAVE = 5;
		final int MINIMAP_VIEW_SIZE_POW = 3;						// 2^n x 2^n pixels in mini-map for each whole full-size screen
		final int MINIMAP_PLOT_SIZE = WORLD_SIZE >> 2;	
		
		// Perlin noise parameters for world-map
		final int P_NUM_OCTAVES = 10;
		final int P_LOWEST_OCTAVE = 0; 
		final int P_HIGHEST_OCTAVE = P_LOWEST_OCTAVE + P_NUM_OCTAVES;
		final float P_PERSISTENCE = 0.5f;
		
		// Perlin noise gradients
		final int GRAD_NUM = 8;
		
		// Colour indices
		final int COLOUR_RED = 0;
		final int COLOUR_GREEN = 1;
		final int COLOUR_BLUE = 2;
		final int COLOUR_YELLOW = 3;
		final int COLOUR_WHITE = 4;
		final int COLOUR_BLACK = 5;
		final int COLOUR_BROWN = 6;
		
		// Rendering parameters
		final int TYPE_INT_RGB = 1;
		final float POS2PX_RATIO = (float)WORLD_SIZE / VIEW_ZOOM;
		final int MAX_FPS = 60;
		final int NANOSECS_PER_SEC = 1000000000;
		final int PALETTE_SIZE = 1 << 12;
		final int WAVE_FREQUENCY = 7;
		
		// Values for transitions from water -> sand -> grass -> forest -> mountain
		// Packed as unsigned UTF8 (0x00 - 0x7F)
		final String TERRAIN_ELEVATIONS = 
				"\u0055\u0056\u0058\u005D\u0065\u007F";
		
		final int LAND_TYPES = 6;
		
		// Terrain palette
		// HSB colour + S & B deltas packed into UTF8
		// as signed values, where 0x40 = 0
		final String TERRAIN_PALETTE = 
				"\u0066\u007F\u0072\u0033\u0049" +				// water
				"\u0049\u0059\u007F\u0046\u0039" +				// sand
				"\u0053\u006C\u0066\u004F\u0026" +				// grass
				"\u0053\u007C\u004C\u002C\u0039" +				// forest
				"\u0040\u0040\u0042\u0040\u0072" +				// mountain slope
				"\u0040\u0040\u0042\u0040\u0072";				// mountain peak
		
		final int TERRAIN_PALETTE_LENGTH = 5;
		
		// Ship parameters
		final float SHIP_MAX_ROT_SPEED = 0.025f;
		final int SHIP_SIZE = 5;
		final int SHIP_LENGTH = (int)(SHIP_SIZE / VIEW_ZOOM);
		final int SHIP_AXES_RATIO = 2;
		
		final int SHIP_NUM_SAILS = 2;
		final int SHIP_NUM_SAIL_POINTS = 5;
		
		// Player parameters
		final float PLAYER_MAX_DOCKING_RADIUS = 0.025f;
		final int PLAYER_MAX_CARGO = 8;
		final int PLAYER_MAX_RATIONS = 1 << 8;
		final int PLAYER_MAX_SETTLERS = 5;
		
		final int RATIONS_EATEN_PER_CREW = 2;
		final int RATION_EAT_DELAY = 1;
		
		// Town parameters
		final int TOWN_MAX_NUM_BITS = 12;		// 2^townListBits = max no. towns
		final int TOWN_MAX_NUM = 1 << TOWN_MAX_NUM_BITS;
		final float TOWN_HARVEST_RADIUS = 0.05f;
		final int TOWN_RESOURCE_MAX = 1 << 8;
		final int TOWN_PX_RADIUS = 1 << 4;
		final int TOWN_BOUNDING_RADIUS = TOWN_PX_RADIUS << 1; 
		
		final int TOWN_HARVEST_AREA_POW = 8;
		final int TOWN_BAR_SCALE_POW = 4;
		
		// Cost to build a new town
		final int TOWN_SETTLER_COST = 1;
		final int TOWN_WOOD_COST = 2;
		
		// Resource & population growth at towns
		final int TOWN_HARVEST_DELAY = SECS_PER_YEAR/2;				// in secs
		final int TOWN_SETTLER_BORN_COST = TOWN_RESOURCE_MAX;
		final int TOWN_MAX_SETTLERS = 5;
		
		// Resources
		final int RESOURCE_INDEX = TOWN_MAX_NUM >> 1;
		final int RESOURCE_NUM_TYPES = 3;
		final int RESOURCE_ABUNDANCE = 18;
		final float RESOURCE_RANGE = 0.025f;
		final int RESOURCE_BASE_COST = 1;
		final int RESOURCE_BLOCK_SIZE = TOWN_RESOURCE_MAX >> 3;
		
		// HUD
		final int HUD_BAR_HEIGHT = 16;
		final int HUD_BLOCK_SIZE = 16;
		
		// Graphics setup
		final BufferedImage screen = new BufferedImage(WORLD_SIZE, WORLD_SIZE, TYPE_INT_RGB);
		final int[] pixels = ((DataBufferInt)screen.getRaster().getDataBuffer()).getData();
		final Graphics2D g = screen.createGraphics();
		
		final Graphics2D appletGraphics = (Graphics2D)getGraphics();
		
		// Comment in to make things prettier...
		//g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Fonts
		final Font bigFont = Font.decode("SERIF 60");
		final Font smallFont = Font.decode("SERIF 30");		
		
		// Text to display
		final String messages = 		"SERF" +
										"PLEB" +
										"EARL" +
										"DUKE" +
										"LORD" +
										"gold" +
										"A.D." +
										"Game Over!" +
										"Your rank:" +
										"S: Restart";
										
		final int MESSAGE_LENGTH = 4;
		
		final int ENDING_MESSAGE_NUM = 3;
		final int ENDING_MESSAGE_FIRST = 28; 
		final int ENDING_MESSAGE_LENGTH = 10;
		
		final int ENDING_RANK_FIRST = 0;
		final int ENDING_RANK_NUM = 5;
		
		final int HUD_MESSAGE_FIRST = ENDING_RANK_NUM;
		
		
		//#############################################################################
		// EMPTY ARRAYS
		//#############################################################################

		// Maps
		final double[] world = new double[WORLD_SIZE*WORLD_SIZE];
		final int[] minimapOrigin = new int[2];
		
		// Towns
		final int[][] townUpgradeCost = new int[TOWN_MAX_SETTLERS-1][RESOURCE_NUM_TYPES-1];
		
		// Resources
		final int[] resourceTypeID = new int[RESOURCE_NUM_TYPES];
		final int[] resourceValue = new int[RESOURCE_NUM_TYPES];
		
		// Rendering
		final double[][] grad = new double[GRAD_NUM][2];
		final int[] rPalette = new int[PALETTE_SIZE];
		final int[] deadPalette = new int[PALETTE_SIZE];
		
		final double[] screenPos = new double[2];

		final int[] pxInd = new int[2]; 		
		final int[] lastPxInd = new int[2];
		final int[] deltaPxInd = new int[2];
		final int[][] plotRange = new int[WORLD_SIZE][2];	// Start and end of plotting for each pixel row (scanline)
		
		// Gradients of visible grid squares
		final int maxGrids = (int)((double)(1 << P_HIGHEST_OCTAVE) * VIEW_ZOOM + 3);
		final int[][] gi = new int[maxGrids][maxGrids];
		
		
		//#############################################################################
		// TEMPORARY VARIABLES
		//#############################################################################
		
		// Loop counters
		int i, j, k;
		
		// Temporary vectors
		double tx, ty;
		double ux, uy;
		double vx, vy;
		
		// Temporary doubles
		double d;
		double t, v;
		
		// Pixel indices
		int pi, qi, ri;
		int px, py;
		int qx, qy;
		int rx, ry;


		//#############################################################################
		// INITIALISE GRAPHICS
		//#############################################################################
		
		// Set window size
		//setSize(WORLD_SIZE, WORLD_SIZE);
		
		// Java2D colours for resources, towns & bonuses
		Color[] symbolColour = new Color[] {
				
				new Color(255, 0, 0, 255),				// RED
				new Color(0, 255, 0, 255),				// GREEN
				new Color(0, 0, 255, 255),				// BLUE
				new Color(255, 255, 0, 255),			// YELLOW
				new Color(255, 255, 255, 255),			// WHITE
				new Color(0, 0, 0, 128),				// BLACK
				new Color(64, 32, 8, 255),				// BROWN
		};
		
		// Create colour map thresholds
		double[] rColourMapThresholds = new double[LAND_TYPES+1];
		
		for (i = 1; i < LAND_TYPES+1; i++) {
			rColourMapThresholds[i] = (double)(TERRAIN_ELEVATIONS.charAt(i-1) << 1) / 254; 
		}
		
		// Initialise world map palette
		float[] hsb = new float[TERRAIN_PALETTE_LENGTH];
		
		for (i = 0; i < LAND_TYPES; i++) {
			
			// Index range for this land type
			px = (int)(rColourMapThresholds[i] * (double)PALETTE_SIZE);
			py = (int)(rColourMapThresholds[i+1] * (double)PALETTE_SIZE);
			
			for (pi = px; pi < py; pi++) {
				
				float col;

				// For calculating colour gradients
				t = (double)(pi - px) / (double)(py - px);
				
				// Extract HSB colour and delta values
				for (j = 0; j < TERRAIN_PALETTE_LENGTH; j++) {
					
					hsb[j] = (float)((TERRAIN_PALETTE.charAt(i*TERRAIN_PALETTE_LENGTH + j) - 64) << 2) / 254; //252.0f;
				}

				// Apply colour gradients
				if (i == 0) {			// Water: Cubic trigonometric interpolation

					d = -java.lang.Math.cos(WAVE_FREQUENCY * t*t*t * java.lang.Math.PI);
					col = (float)((d*d*d + 1) * t) * 0.5f;
				}
				else {					// Beach, grass, forest, mountains: Quadratic
					
					col = (float)(t*t);
				}
				
				// Calculate next colour
				rPalette[pi] = Color.HSBtoRGB(hsb[0], hsb[1] + col*hsb[3], hsb[2] + col*hsb[4]);
				deadPalette[pi] = Color.HSBtoRGB(0.0f, hsb[1] + col*hsb[3], hsb[2] + col*hsb[4]);
			}
		}

		
		// Colour 0 = BLACK for mini-map hidden areas
		rPalette[0] = 0x000000;

		// Initialise gradients as #GRAD_NUM evenly-spaced 2D vectors
		// Most importantly: NONE are axis-aligned, NONE are diagonal-aligned
		// (based on Ken Perlin's advice in his 2002 SigGraph paper)
		for (i = 0; i < GRAD_NUM; i++) {
			
			d = 2*java.lang.Math.PI * (double)(2*i + 1) / (double)(GRAD_NUM*2);
			
			grad[i][0] = java.lang.Math.sin(d);
			grad[i][1] = java.lang.Math.cos(d);
		}
		
		// Generate sail polys
		int[] shipSailPolyX = new int[SHIP_NUM_SAIL_POINTS];
		int[] shipSailPolyY = new int[SHIP_NUM_SAIL_POINTS];
		
		for (i = 0; i < SHIP_NUM_SAIL_POINTS; i++) {
			
			j = i - (SHIP_NUM_SAIL_POINTS >> 1);
			shipSailPolyX[i] = -j*j + (SHIP_LENGTH >> 2);
			shipSailPolyY[i] = j * (SHIP_LENGTH >> 2);
		}
		
		// Resource values
		for (i = 1; i < RESOURCE_NUM_TYPES+1; i++)
			resourceValue[i-1] = RESOURCE_BASE_COST * i*i;
		
		// Town upgrade costs
		// Generates following upgrade costs
		// Level 1 = 0 wood, 0 iron
		// Level 2 = 1 wood, 0 iron
		// Level 3 = 2 wood, 1 iron
		// Level 4 = 3 wood, 2 iron		
		for (i = 0; i < TOWN_MAX_SETTLERS-1; i++) {
			for (j = 0; j < RESOURCE_NUM_TYPES-1; j++) {
				townUpgradeCost[i][j] = (i-j) < 0 ? 0 : (i-j);
			}
		}
		
		
		//#############################################################################
		// INITIALISE VARIABLES WHEN GAME (RE)STARTS
		//#############################################################################
		
		while (true) {

			// Must be true to plot correctly first time!
			boolean gameRefresh = true;

			// Initialise random seed for Perlin noise 
			long pSeed = System.nanoTime();

			// Game state
			int gameState = STATE_MOVING;
			
			// Town parameters
			int townClosest = -1;
			double townMinDist = 1;
			
			// Seconds since this game began
			int year = YEAR_START;
			int secsElapsed = 0;
			int rationsLastEaten = 0;
			
			int townHarvestTime = 0;
			boolean harvestThemCrops;
			
			// Town arrays
			double[][] townPos = new double[TOWN_MAX_NUM][2];
			int[][] townResources = new int[TOWN_MAX_NUM][3];
			int[][] townHarvestRate = new int[TOWN_MAX_NUM][3];
			int[] townSettlers = new int[TOWN_MAX_NUM];
			int[] townState = new int[TOWN_MAX_NUM];					
			int[] townType = new int[TOWN_MAX_NUM];			

			// Object counts
			int townsSettled = 0;

			// Player physics
			double[] playerPos = new double[2];
			double[] playerVel = new double[2];
			double playerRot = 0;
			double playerRotSpeed = 0;
			double playerTargetRot = 0;

			// Player resources
			int playerGold = 0;
			int playerRations = PLAYER_MAX_RATIONS;
			int playerWood = 4;
			int playerIron = 1;
			int playerSettlers = 2;
			int playerDockedAtTown = -1;

			// Player states
			boolean playerAnchorDropped = false;
			boolean playerSettling = false;
			boolean playerCanSettle = false;
			
			// Mini-map
			double[] minimap = new double[MINIMAP_SIZE * MINIMAP_SIZE];
			
			// Set random resource type IDs (determine where resources are placed)
			for (k = 0; k < RESOURCE_NUM_TYPES; k++) {
				resourceTypeID[k] = (int)(java.lang.Math.random() * pSeed);
			}			
		

			//#############################################################################
			// GAME LOOP
			//#############################################################################

			while (gameState != STATE_GAMEOVER || keys[KEY_S] == 0) {

				//#############################################################################
				// UPDATE, RESET & STORE VARIABLES
				//#############################################################################

				// Store timestamp of this frame
				long lastTime = System.nanoTime();
				secsElapsed = (int)((lastTime - pSeed) / (long)NANOSECS_PER_SEC);

				// Store world map pixel plotting origin before player moves again
				lastPxInd[0] = (int)((playerPos[0] - VIEW_ZOOM/2) * POS2PX_RATIO); 
				lastPxInd[1] = (int)((playerPos[1] - VIEW_ZOOM/2) * POS2PX_RATIO);

				// Clear player actions
				playerRotSpeed = 0;
				playerSettling = false;
				

				//#############################################################################
				// PLAYER SAILING: INPUT & STATE HANDLING
				//#############################################################################
				
				if (gameState == STATE_MOVING) {
					
					if (playerAnchorDropped) {		// Settle
						
						playerSettling = true;
						
						// Create a new town!
						if (keys[MOUSE_LMB] > keys[INPUT_PREVIOUS + MOUSE_LMB] && playerCanSettle) {

							// Initialise town
							townPos[townsSettled][0] = (double)(keys[MOUSE_X] - WORLD_SIZE/2) / POS2PX_RATIO + playerPos[0];
							townPos[townsSettled][1] = (double)(keys[MOUSE_Y] - WORLD_SIZE/2) / POS2PX_RATIO + playerPos[1];
							townState[townsSettled] = IS_ACTIVE;
							townType[townsSettled] = 0;
							
							// Calculate resource generation depending on local area &
							// resource bonuses
							px = (int)(townPos[townsSettled][0] * POS2PX_RATIO) & (WORLD_SIZE-1);
							py = (int)(townPos[townsSettled][1] * POS2PX_RATIO) & (WORLD_SIZE-1);
							rx = (int)(TOWN_HARVEST_RADIUS * POS2PX_RATIO);
							
							// Loop through surrounding pixels...
							for (qy = -rx; qy < rx; qy++) {
								for (qx = -rx; qx < rx; qx++) {
									
									// If within harvest radius
									if (qx*qx + qy*qy <= rx*rx) {
										
										// World pixel index
										pi = ((py+qy) & (WORLD_SIZE-1))*WORLD_SIZE + ((px+qx) & (WORLD_SIZE-1));
										
										// Accumulate base resource amounts
										for (ri = 2; ri < 5; ri++) {
											
											// Food, wood or metal, depending on terrain...
											if (world[pi] > rColourMapThresholds[ri] &&
												world[pi] < rColourMapThresholds[ri+1]) {
												
												townHarvestRate[townsSettled][ri-2]++;
												break;
											}
										}
									}
								}
							}
							
							// Loop through all resources, finding any within harvest radius
							// Seriously inefficient, but hopefully good enough
							int[] resourceBonus = new int[RESOURCE_NUM_TYPES];
							
							for (i = RESOURCE_INDEX; i < TOWN_MAX_NUM; i++) {
								
								if ((townState[i] & IS_ACTIVE) > 0) {
									
									// Town-to-resource distance
									tx = townPos[townsSettled][0] - townPos[i][0];
									ty = townPos[townsSettled][1] - townPos[i][1];
									
									// Check if resource is within range
									if (tx*tx + ty*ty <= TOWN_HARVEST_RADIUS*TOWN_HARVEST_RADIUS) {
										
										// Count resource types
										resourceBonus[townType[i]-1]++;
									}
								}
							}
							
							// Calculate final harvest rates and apply bonuses
							// Land counted in 16 x 16px blocks
							// Each bonus halves this requirement
							for (i = 0; i < RESOURCE_NUM_TYPES; i++) {
								
								townHarvestRate[townsSettled][i] >>= (TOWN_HARVEST_AREA_POW - resourceBonus[i]);
							}
							
							// Settlers leave the ship and build their new home...
							playerSettlers -= TOWN_SETTLER_COST;
							playerWood -= TOWN_WOOD_COST;
							townSettlers[townsSettled] = TOWN_SETTLER_COST;
							
							// Auto-dock at new town
							playerDockedAtTown = townsSettled;
							gameState = STATE_DOCKED;
							
							townsSettled++;
						}
					} 

					// Settlers & crew use up precious rations...
					if (secsElapsed >= rationsLastEaten + RATION_EAT_DELAY) {
						
						playerRations -= playerSettlers*RATIONS_EATEN_PER_CREW + 1;
						rationsLastEaten = secsElapsed;
					}
					
				}

					
				//#############################################################################
				// PLAYER DOCKED: INPUT & STATE HANDLING
				//#############################################################################

				else if (gameState == STATE_DOCKED) {			// Docked at town
					
					// Work out if the player is loading, unloading or neither
					int shipLoadRate = 0;
					
					if (keys[MOUSE_LMB] > keys[INPUT_PREVIOUS + MOUSE_LMB]) {
						shipLoadRate++;
					} 
					else if (keys[MOUSE_RMB] > keys[INPUT_PREVIOUS + MOUSE_RMB]) {
						shipLoadRate--;
					}
					
					// Settlers
					if (keys[KEY_S] > 0 &&
							playerSettlers + shipLoadRate >= 0 &&
							playerSettlers + shipLoadRate <= PLAYER_MAX_SETTLERS &&
							townSettlers[playerDockedAtTown] - shipLoadRate > 0 &&
							townSettlers[playerDockedAtTown] - shipLoadRate <= TOWN_MAX_SETTLERS) {

						playerSettlers += shipLoadRate;
						townSettlers[playerDockedAtTown] -= shipLoadRate;
					} 

					if (playerWood + playerIron + shipLoadRate <= PLAYER_MAX_CARGO) {

						// Wood
						if (keys[KEY_W] > 0 &&
								playerWood + shipLoadRate >= 0 &&
								townResources[playerDockedAtTown][1] - shipLoadRate*RESOURCE_BLOCK_SIZE >= 0 &&
								townResources[playerDockedAtTown][1] - shipLoadRate*RESOURCE_BLOCK_SIZE <= TOWN_RESOURCE_MAX) {

							playerWood += shipLoadRate;
							townResources[playerDockedAtTown][1] -= shipLoadRate * RESOURCE_BLOCK_SIZE;
						} 

						// Iron
						if (keys[KEY_D] > 0 &&
								playerIron + shipLoadRate >= 0 &&
								townResources[playerDockedAtTown][2] - shipLoadRate*RESOURCE_BLOCK_SIZE >= 0 &&
								townResources[playerDockedAtTown][2] - shipLoadRate*RESOURCE_BLOCK_SIZE <= TOWN_RESOURCE_MAX) {

							playerIron += shipLoadRate;
							townResources[playerDockedAtTown][2] -= shipLoadRate * RESOURCE_BLOCK_SIZE;
						}
					}

					// Replenish stocks in town
					playerRations = PLAYER_MAX_RATIONS;
				} 
				

				//#############################################################################
				// STATE & MOVEMENT UPDATE
				//#############################################################################
				
				if (gameState != STATE_GAMEOVER) {
					
					// Update year counter
					year = YEAR_START + (secsElapsed / SECS_PER_YEAR);
					
					// No more rations! Player dies.
					if (playerRations <= 0 || year >= YEAR_START + YEAR_COUNT) {
						gameState = STATE_GAMEOVER;
					}					
					
					// Drop/raise anchor (toggle)
					// Automatically docks with closest town, if in range
					if (keys[KEY_SPACE] > keys[INPUT_PREVIOUS + KEY_SPACE]) { 
						
						playerAnchorDropped = !playerAnchorDropped; 
						
						if (playerAnchorDropped && townMinDist < PLAYER_MAX_DOCKING_RADIUS) { 
							
							// Dock at closest town
							playerDockedAtTown = townClosest; 
							gameState = STATE_DOCKED; 
						} 
						else {
							
							// Undock and set sail (or stay anchored at sea)
							playerDockedAtTown = -1;
							gameState = STATE_MOVING; 
						}
					}
					
					// Mouse control of ship
					if (keys[MOUSE_LMB] > keys[INPUT_PREVIOUS + MOUSE_LMB] || keys[MOUSE_DRAG] > 0) { 

						// Turn in chosen direction
						playerTargetRot = java.lang.Math.atan2((double)(keys[MOUSE_Y] - WORLD_SIZE/2), (double)(keys[MOUSE_X] - WORLD_SIZE/2)); // + (2*Math.PI)); // % (2*Math.PI); 
					}

					// Full-circle rotation in radians
					t = 2*java.lang.Math.PI;
					d = (playerTargetRot - playerRot) % t;

					d = d > java.lang.Math.PI ? d-t : d;
					d = d < -java.lang.Math.PI ? d+t : d;
					
					// Rotate player towards clicked direction
					if (d/d > SHIP_MAX_ROT_SPEED)
						playerRotSpeed = d < 0 ? -SHIP_MAX_ROT_SPEED : SHIP_MAX_ROT_SPEED;

					// Rotate actor
					playerRot = (playerRot + playerRotSpeed) % t;
					
					// Simulate wind acceleration via sails
					if (!playerAnchorDropped) {
						
						playerVel[0] += WIND_SPEED * java.lang.Math.cos(playerRot);
						playerVel[1] += WIND_SPEED * java.lang.Math.sin(playerRot);
					}

					// Simulate water drag
					playerVel[0] *= DRAG_COEFF;
					playerVel[1] *= DRAG_COEFF;

					// Move actor
					playerPos[0] += playerVel[0];
					playerPos[1] += playerVel[1];
					

					//#############################################################################
					// PLAYER UPDATE: COLLISION DETECTION
					//#############################################################################

					// Collision vector & damage
					tx = 0;
					ty = 0;

					// Lowest land pixel value
					t = rColourMapThresholds[1];
					
					// Get position of the actor's bounding box in world map pixels
					qx = (int)(playerPos[0] * POS2PX_RATIO); 
					qy = (int)(playerPos[1] * POS2PX_RATIO);

					// Bounding box half-length
					k = SHIP_LENGTH >> 1;

					// ########## COLLISION WITH LAND ##########
					// Stop ship moving on to land!
					// Check only pixels within bounding box & then check with
					// non-axis-aligned shape (more accurate)
					checkLandCollision: 
					{
						for (py = qy-k; py < qy+k+1; py++) {
							for (px = qx-k; px < qx+k+1; px++) {

								// Check if this pixel is a land pixel
								if (world[(py & (WORLD_SIZE-1))*WORLD_SIZE + (px & (WORLD_SIZE-1))] >= t) {

									// Get vector from ship centre to this pixel
									tx = px - qx;
									ty = py - qy;

									// Rotate to same axes as ship
									ux = tx*java.lang.Math.cos(playerRot) + ty*java.lang.Math.sin(playerRot);
									uy = ty*java.lang.Math.cos(playerRot) - tx*java.lang.Math.sin(playerRot);

									// Scale major to minor axis to convert ellipse- to circle-test
									ux /= SHIP_AXES_RATIO;
									v = SHIP_LENGTH >> 2;				// Ship minor radius

									// Now check with circle radii
									if (ux*ux + uy*uy < v*v) {
			
										// Convert collision vector from pixel to global position
										tx /= POS2PX_RATIO;
										ty /= POS2PX_RATIO;
			
										// If there's a collision, put the player back to their 
										// last (valid) position and rotation
										playerPos[0] -= playerVel[0];
										playerPos[1] -= playerVel[1];
										playerRot -= playerRotSpeed;
			
										// Bounce ship away from collision
										playerVel[0] += BOUNCE_COEFF * -tx;
										playerVel[1] += BOUNCE_COEFF * -ty;
			
										// Break after first collision detected
										break checkLandCollision;
									}
								}
							}
						}
					}
				}
				
				// Remember the input state for next time
				for (i = 0; i < INPUT_PREVIOUS; i++) {
					keys[INPUT_PREVIOUS + i] = keys[i];
				}				


				//#############################################################################
				// PERLIN-fBM PLOTTING SETUP
				//#############################################################################

				// Top-left corner of the screen in world coordinates
				screenPos[0] = playerPos[0] - VIEW_ZOOM/2; 
				screenPos[1] = playerPos[1] - VIEW_ZOOM/2;			

				// New pixel indices for world plotting
				pxInd[0] = (int)(screenPos[0] * POS2PX_RATIO); 
				pxInd[1] = (int)(screenPos[1] * POS2PX_RATIO);

				// Calculate change in pixel origin
				deltaPxInd[0] = pxInd[0] - lastPxInd[0]; 
				deltaPxInd[1] = pxInd[1] - lastPxInd[1];

				// Now wrap pixel offset to map size
				pxInd[0] = pxInd[0] & (WORLD_SIZE-1); 
				pxInd[1] = pxInd[1] & (WORLD_SIZE-1);

				// Calculate exactly which pixels need to be drawn & store offsets as scanlines
				for (int xy = 0; xy < 2; xy++) {

					// scanOffset: [0 = columns, 1 = rows][0 = start pixel, 1 = end pixel]

					// Plot whole screen by default
					int[][] scanOffset = { { 0, WORLD_SIZE }, { 0, WORLD_SIZE } };

					if (!gameRefresh) {

						// Get offset for plotting scanlines
						int pxOffset = -deltaPxInd[xy] & (WORLD_SIZE-1);

						// Update offsets depending on player movement, or lack thereof
						if (deltaPxInd[xy] > 0) { scanOffset[xy][0] = pxOffset; }
						else if (deltaPxInd[xy] < 0) { scanOffset[xy][1] = pxOffset + 1; }
						else { scanOffset[xy][1] = 0; }
					}

					// Store ranges for pixels to be plot on each row (scanline)
					for (int row = scanOffset[1][0]; row < scanOffset[1][1]; row++) {

						plotRange[row][0] = scanOffset[0][0];
						plotRange[row][1] = scanOffset[0][1];
					}
				}

				//#############################################################################
				// PERLIN-fBM NOISE
				//#############################################################################

				double pAmp = 1/P_PERSISTENCE;

				// Create Perlin-FBM noise landscape
				for (int octave = P_LOWEST_OCTAVE; octave < P_HIGHEST_OCTAVE; octave++) {

					// Set frequency of this octave 
					double pFreq = (double)(1 << octave);
					pAmp *= P_PERSISTENCE;

					// Calculate scale of area to plot
					double worldScale = pFreq * VIEW_ZOOM;
					int gridsOnScreen = (int)(worldScale + 3);	// 1 extra left, 1 extra right & 1 for luck ;)
					
					// Top left corner of screen
					double[] screenOrigin = { 	
							screenPos[0] * pFreq,
							screenPos[1] * pFreq };

					// Grid offset of screen origin
					int[] gridOrigin = { 		
							(screenOrigin[0] < 0) ? (int)(screenOrigin[0]-1) : (int)screenOrigin[0],
							(screenOrigin[1] < 0) ? (int)(screenOrigin[1]-1) : (int)screenOrigin[1] };

					// Fractional part of the screen origin, relative to grid
					// NOTE: Always positive
					double[] fracOrigin = { 	
							screenOrigin[0] - (double)gridOrigin[0],
							screenOrigin[1] - (double)gridOrigin[1] };
					
					// Save plotting offsets for mini-map
					if (octave == MINIMAP_OCTAVE) {
						
						// Calculate mini-map origin in world-map pixels
						minimapOrigin[0] = (int)(fracOrigin[0] * (double)(1 << (WORLD_SIZE_POW - MINIMAP_VIEW_SIZE_POW)));
						minimapOrigin[1] = (int)(fracOrigin[1] * (double)(1 << (WORLD_SIZE_POW - MINIMAP_VIEW_SIZE_POW)));
					}

					// NOTE:
					// Further speed optimisation possible for hashing (not implemented here)
					// Hashing function doesn't need to calculate all the values
					// Depends on what needs to be drawn, so # loops can be massively reduced
					// As with the main loop below

					// Get pseudo-random gradients for all corners of the grid squares on screen
					for (px = 0; px < gridsOnScreen; px++) {
						
						// Hash grid X coordinate
						qx = px + gridOrigin[0] + (int)pSeed;
						qx = ((qx >>> 16) ^ qx) * MAGIC_HASH;
						
						for (py = 0; py < gridsOnScreen; py++) {
						
							// Chained hashing with Y coordinate
							qy = qx + py + gridOrigin[1];
						    qy = ((qy >>> 16) ^ qy) * MAGIC_HASH;
						    
						    // Store hash for this grid point
						    gi[px][py] = ((qx >>> 16) ^ qy);
						}
					}

					// Calculate per-pixel world grid step size
					double pxStep = worldScale / (double)WORLD_SIZE;
					
					// Scaling constant
					d = java.lang.Math.sqrt(2);

					// Draw the world map
					for (py = 0; py < WORLD_SIZE; py++) {
						
						for (px = plotRange[py][0]; px < plotRange[py][1]; px++) {
							
							// Calculate plotting index
							int nx = (pxInd[0] + px) & (WORLD_SIZE-1);
							int ny = (pxInd[1] + py) & (WORLD_SIZE-1);
							pi = ny*WORLD_SIZE + nx;						
							
							// Grid coordinates for this pixel
							double x = fracOrigin[0] + (double)px * pxStep;					
							double y = fracOrigin[1] + (double)py * pxStep;

							// Whole & fractional parts of pixel position
							int gx = (int)x;
							int gy = (int)y;
							double fx = x - (double)gx;
							double fy = y - (double)gy;

							// Corners of grid numbered like this:
							//
							// 0 ----- 1
							// |       |
							// |       |
							// 3 ----- 2

							// Fetch gradients at the 4 corners of the grid
							qx = gi[gx][gy] & (GRAD_NUM-1);
							qy = gi[gx+1][gy] & (GRAD_NUM-1);
							rx = gi[gx+1][gy+1] & (GRAD_NUM-1);
							ry = gi[gx][gy+1] & (GRAD_NUM-1);
							
							double gx0 = grad[qx][0];
							double gx1 = grad[qy][0];
							double gx2 = grad[rx][0];
							double gx3 = grad[ry][0];

							double gy0 = grad[qx][1];
							double gy1 = grad[qy][1];
							double gy2 = grad[rx][1];
							double gy3 = grad[ry][1];							

							// Dot product of each vector with respective corner gradients
							double dp0 = fx*gx0 + fy*gy0;
							double dp1 = (fx-1)*gx1 + fy*gy1;
							double dp2 = (fx-1)*gx2 + (fy-1)*gy2;
							double dp3 = fx*gx3 + (fy-1)*gy3;
							
							// Interpolation in x dimension
							double Wx = fx * fx * fx * (fx * (fx * 6 - 15) + 10);
							double wx01 = dp0 + Wx*(dp1-dp0);
							double wx23 = dp3 + Wx*(dp2-dp3);
							
							// Interpolate x means in y to give final value
							double Wy = fy * fy * fy * (fy * (fy * 6 - 15) + 10);
							double val = wx01 + Wy*(wx23-wx01);

							// Generate "ridged" noise
							val = (val < 0) ? -val : val;

							// Transform values to useful range
							// NOTE: Can remove this line and the Math.sqrt(2) above to
							// save bytes, but terrain type thresholds will need to be
							// re-tweaked!
							val = (val + d/2) / d;

							// Add weighted contribution to noise map
							double wp = 1 / pAmp;
							world[pi] = (wp == 1) ? val : (world[pi]*wp + val) / (wp+1);

							// Add resources on last pass within appropriate elevations
							if (octave == P_HIGHEST_OCTAVE-1) {

								int gHash = gi[gx][gy];

								// Generate hopefully relatively collision-free resource ID
								// using high bits
								rx = RESOURCE_INDEX + (gHash >>> (32 - (TOWN_MAX_NUM_BITS-1)));								

								// Create new resource, if it hasn't been discovered already
								if (townState[rx] == 0) {
									for (k = 1; k < RESOURCE_NUM_TYPES+1; k++) {

										if (world[pi] > rColourMapThresholds[k+1] - RESOURCE_RANGE && world[pi] < rColourMapThresholds[k+1] + RESOURCE_RANGE) {
											if (((gHash ^ resourceTypeID[k-1]) << RESOURCE_ABUNDANCE) == 0) {

												// Add a new resource at the middle of this grid square
												townPos[rx][0] = ((double)gx + 0.5 + gridOrigin[0]) / pFreq;
												townPos[rx][1] = ((double)gy + 0.5 + gridOrigin[1]) / pFreq;

												// Mark resource as initialised
												townState[rx] = IS_ACTIVE;

												// Store resource type
												townType[rx] = k;
											}
										}
									}
								}
							}
						}
					}
				}

				//#############################################################################
				// COPY WORLD-MAP BUFFER TO SCREEN BUFFER
				//#############################################################################

				double[][] minimapGrid = new double[(1 << MINIMAP_VIEW_SIZE_POW) + 1][(1 << MINIMAP_VIEW_SIZE_POW) + 1];
				
				// Set current palette
				int[] activePalette = 
						(gameState == STATE_GAMEOVER) || (playerRations < (PLAYER_MAX_RATIONS >> 2) && (secsElapsed & 1) == 0) 
						? deadPalette : rPalette;

				// Draw everything to the screen
				for (py=0; py < WORLD_SIZE; py++) {
					for (px=0; px < WORLD_SIZE; px++) {

						// Screen coordinates
						i = py*WORLD_SIZE + px;

						// Convert to wacky world coordinates
						int nx = (pxInd[0] + px) & (WORLD_SIZE-1);
						int ny = (pxInd[1] + py) & (WORLD_SIZE-1);
						pi = ny*WORLD_SIZE + nx;

						// Transform value from world-map to palette index
						j = (int)(world[pi] * (double)PALETTE_SIZE) & (PALETTE_SIZE-1);
						
						// Accumulate world-map values in mini-map grid
						qx = (px + minimapOrigin[0]) >> (WORLD_SIZE_POW - MINIMAP_VIEW_SIZE_POW);
						qy = (py + minimapOrigin[1]) >> (WORLD_SIZE_POW - MINIMAP_VIEW_SIZE_POW);
						
						minimapGrid[qx][qy] += world[pi];

						// Plot pixel
						pixels[i] = activePalette[j];
					}
				}
				
				
				//#############################################################################
				// DRAW SPRITES
				//#############################################################################

				// ########## DRAW TOWNS ##########
				
				// Keep track of distance of player from closest town
				townMinDist = 1;
				
				// Harvest this frame?
				harvestThemCrops = (secsElapsed >= townHarvestTime);

				// Loop in reverse to draw towns last (on top of resources)
				for (i = TOWN_MAX_NUM-1; i >= 0; i--) {
					
					if ((townState[i] & IS_ACTIVE) > 0) {
						
						// UPDATE TOWN STATE
						if (townType[i] == 0 && gameState != STATE_GAMEOVER) { 

							// Harvest them resources...
							if (harvestThemCrops) {

								for (j = 0; j < RESOURCE_NUM_TYPES; j++) {

									// Keep accumulating resources up to max
									qx = townHarvestRate[i][j] * townSettlers[i];
									townResources[i][j] = townResources[i][j] + qx > TOWN_RESOURCE_MAX ? TOWN_RESOURCE_MAX : townResources[i][j] + qx;

									// Earn money for resources sent back to the King
									playerGold += qx * resourceValue[j];
								}
							}

							if (townSettlers[i] < TOWN_MAX_SETTLERS) {

								// Town upgrade?
								qx = townUpgradeCost[townSettlers[i]-1][0] * RESOURCE_BLOCK_SIZE;
								qy = townUpgradeCost[townSettlers[i]-1][1] * RESOURCE_BLOCK_SIZE;

								if (townResources[i][0] >= TOWN_SETTLER_BORN_COST &&
									townResources[i][1] >= qx &&
									townResources[i][2] >= qy) {

									// Deduct the upgrade cost
									townResources[i][0] -= TOWN_SETTLER_BORN_COST;
									townResources[i][1] -= qx;
									townResources[i][2] -= qy;

									// New settler born!
									townSettlers[i]++;
								}
							}
						}
						
						// DRAW ON-SCREEN TOWNS

						// Convert to screen (pixel) coordinates
						px = (int)((townPos[i][0] - screenPos[0]) * POS2PX_RATIO);
						py = (int)((townPos[i][1] - screenPos[1]) * POS2PX_RATIO);

						// Draw town if it's on-screen
						if (px + TOWN_BOUNDING_RADIUS >= 0 && 
							px - TOWN_BOUNDING_RADIUS < WORLD_SIZE && 
							py + TOWN_BOUNDING_RADIUS >= 0 && 
							py - TOWN_BOUNDING_RADIUS < WORLD_SIZE) {
							
							// Mark town as visible
							townState[i] |= IS_VISIBLE;

							// Translate to town position
							g.translate(px, py);	
							
							pi = -TOWN_PX_RADIUS/2;
							
							if (townType[i] == 0) {				// Town

								// Check if this is the closest town to the player
								tx = townPos[i][0] - playerPos[0];
								ty = townPos[i][1] - playerPos[1];

								d = tx*tx + ty*ty;				
								
								// If so, mark as closest
								if (d < townMinDist*townMinDist) {

									townMinDist = java.lang.Math.sqrt(d);
									townClosest = i;
								}

								// Draw town
								g.setColor(symbolColour[COLOUR_WHITE]);
								g.fillRect(pi, pi, TOWN_PX_RADIUS, TOWN_PX_RADIUS);

								// Draw border
								//g.setColor(symbolColour[COLOUR_BLACK]);
								//g.drawRect(pi, pi, TOWN_PX_RADIUS, TOWN_PX_RADIUS);

								// Draw town population
								qi = TOWN_PX_RADIUS >> 2;
								
								for (j = 0; j < townSettlers[i]; j++) {
									
									g.setColor(symbolColour[COLOUR_WHITE]);
									g.fillOval(-TOWN_PX_RADIUS + j*(qi+2), -TOWN_PX_RADIUS, qi, qi);
									
									//g.setColor(symbolColour[COLOUR_BLACK]);
									//g.drawOval(-TOWN_PX_RADIUS + j*(qi+2), -TOWN_PX_RADIUS, qi, qi);
								}

								// Draw commodity levels
								qi = TOWN_PX_RADIUS * 2/3;
								
								for (j = 0; j < RESOURCE_NUM_TYPES; j++) { 
									
									g.setColor(symbolColour[j]);
									g.fillRect(TOWN_PX_RADIUS, -TOWN_PX_RADIUS + j*qi, townResources[i][j] >> TOWN_BAR_SCALE_POW, qi);
								}
								
								// Draw harvest radius
								if (playerSettling || playerDockedAtTown == i) {

									// Show harvest range of town
									g.setColor(symbolColour[COLOUR_YELLOW]);
									//g.setColor(symbolColour[COLOUR_WHITE]);
									rx = (int)(TOWN_HARVEST_RADIUS * POS2PX_RATIO);
									g.drawOval(-rx, -rx, rx*2, rx*2);
								}
							}
							else {							// Resource

								// Draw as diamonds (rotated rectangles)
								t = java.lang.Math.PI / 4;
								g.rotate(t);
								
								// Draw resource at this location
								g.setColor(symbolColour[townType[i]-1]);
								g.fillRect(pi/2, pi/2, TOWN_PX_RADIUS/2, TOWN_PX_RADIUS/2);

								// Draw border
								//g.setColor(symbolColour[COLOUR_BLACK]);
								//g.drawRect(pi/2, pi/2, TOWN_PX_RADIUS/2, TOWN_PX_RADIUS/2);
								
								// Reset
								g.rotate(-t);
							}
							
							// Reset
							g.translate(-px, -py);	
						}
						else {
							
							// Mark town as not visible
							townState[i] &= ~IS_VISIBLE;
						}
					}
				}
				
				if (harvestThemCrops)
					townHarvestTime = secsElapsed + TOWN_HARVEST_DELAY;


				// DRAW ACTOR(s)

				// Player position: always centre of the screen
				px = (WORLD_SIZE-1) >> 1;
				py = (WORLD_SIZE-1) >> 1;

				// Translate to actor position & rotation
				g.translate(px, py);
				g.rotate(playerRot);

				// Ship major & minor radii
				qx = SHIP_LENGTH >> 1;
				qy = qx / SHIP_AXES_RATIO;

				// Draw hull
				g.setColor(symbolColour[COLOUR_BROWN]);
				g.fillOval(-qx, -qy, SHIP_LENGTH, SHIP_LENGTH / SHIP_AXES_RATIO);

				// Hull border
				//g.setColor(symbolColour[COLOUR_BLACK]);
				//g.drawOval(-qx, -qy, SHIP_LENGTH, SHIP_LENGTH / SHIP_AXES_RATIO);

				// Draw each sail
				for (j = 0; j < SHIP_NUM_SAILS; j++) {

					// Calculate offset of sail
					tx = -j * (SHIP_LENGTH >> 2);

					// Set up sail drawing
					g.translate(tx, 0);

					// Sail canvas
					g.setColor(symbolColour[COLOUR_WHITE]);
					g.fillPolygon(shipSailPolyX, shipSailPolyY, shipSailPolyX.length);

					// Reset
					g.translate(-tx, 0);

				}

				// Reset
				g.rotate(-playerRot);
				g.translate(-px, -py);

				
				// ########## DRAW SETTLEMENT AREAS ##########
				
				// If player trying to settle, show potential town location
				playerCanSettle = false;
				
				if (playerSettling) {
					
					// Convert to wacky world coordinates
					int nx = (pxInd[0] + keys[MOUSE_X]) & (WORLD_SIZE-1);
					int ny = (pxInd[1] + keys[MOUSE_Y]) & (WORLD_SIZE-1);
					pi = ny*WORLD_SIZE + nx;
					
					// Distance from mouse cursor to player's ship
					tx = (double)(keys[MOUSE_X] - WORLD_SIZE/2) / POS2PX_RATIO;
					ty = (double)(keys[MOUSE_Y] - WORLD_SIZE/2) / POS2PX_RATIO;
					
					d = tx*tx + ty*ty;
					t = (double)(WORLD_SIZE*WORLD_SIZE) / POS2PX_RATIO;
					
					// Check town doesn't share resources with any established town
					for (i = 0; i < townsSettled; i++) {
						
						// Only check with on-screen towns
						if ((townState[i] & IS_VISIBLE) > 0) {
							
							// Mouse position in world coordinates
							ux = ((double)keys[MOUSE_X] / POS2PX_RATIO) + screenPos[0];
							uy = ((double)keys[MOUSE_Y] / POS2PX_RATIO) + screenPos[1];
							
							// Distance from mouse to town position
							vx = townPos[i][0] - ux;
							vy = townPos[i][1] - uy;
							v = vx*vx + vy*vy;
							
							t = t < v ? t : v;
						}
					}
					
					// Settle-able on grass only, within range of player's ship, outside
					// range of other towns & if player has enough settlers and resources
					// on-board
					playerCanSettle = (	world[pi] > rColourMapThresholds[2] &&
										world[pi] < rColourMapThresholds[3] &&
										d < PLAYER_MAX_DOCKING_RADIUS*PLAYER_MAX_DOCKING_RADIUS &&
										t > TOWN_HARVEST_RADIUS*TOWN_HARVEST_RADIUS*4 &&
										playerSettlers >= TOWN_SETTLER_COST &&
										playerWood >= TOWN_WOOD_COST );
					
					// Show potential settling location & allowed range
					g.setColor(symbolColour[COLOUR_WHITE]);
					rx = (int)(PLAYER_MAX_DOCKING_RADIUS * POS2PX_RATIO);
					g.drawOval(WORLD_SIZE/2 - rx, WORLD_SIZE/2 - rx, rx*2, rx*2);					

					// Unsettle-able = Red, Settle-able = White
					if (!playerCanSettle) g.setColor(symbolColour[COLOUR_RED]);
					rx = (int)(TOWN_HARVEST_RADIUS * POS2PX_RATIO);
					g.drawOval(keys[MOUSE_X] - rx, keys[MOUSE_Y] - rx, rx*2, rx*2);
				}
				
				
				// ########## DRAW SHIP CARGO DISPLAY ##########
				
				// Draw amount of on-board ration supply remaining
				g.setColor(symbolColour[COLOUR_RED]);
				g.fillRect(HUD_BAR_HEIGHT, 50, playerRations >> 1, HUD_BAR_HEIGHT);
				
				g.setColor(symbolColour[COLOUR_WHITE]);
				g.drawRect(HUD_BAR_HEIGHT, 50, PLAYER_MAX_RATIONS >> 1, HUD_BAR_HEIGHT);
				
				// Draw wood & iron cargo amount
				for (j = 0; j < PLAYER_MAX_CARGO; j++) {
					
					if (j < playerWood + playerIron) {
						
						g.setColor(symbolColour[COLOUR_BLUE]);

						if (j < playerWood)
							g.setColor(symbolColour[COLOUR_GREEN]);
						
						g.fillRect(HUD_BAR_HEIGHT + j*HUD_BLOCK_SIZE, 75, HUD_BLOCK_SIZE, HUD_BAR_HEIGHT);
					}

					// Draw ship cargo spaces
					g.setColor(symbolColour[COLOUR_WHITE]);
					g.drawRect(HUD_BAR_HEIGHT + j*HUD_BLOCK_SIZE, 75, HUD_BLOCK_SIZE, HUD_BAR_HEIGHT);
				}

				// Draw settlers on-board
				for (i = 0; i < playerSettlers; i++) {
					g.fillOval(25*i + HUD_BAR_HEIGHT, 100, HUD_BAR_HEIGHT, HUD_BAR_HEIGHT);
				}

				
				//#############################################################################
				// UPDATE & PLOT MINI-MAP
				//#############################################################################

				// Draw rectangle using semi-transparent black as background
				// for unexplored regions
				// NOTE: To enable this, also remove un-comment the if statement 
				// in the main mini-map drawing loop below
				//g.setColor(symbolColour[COLOUR_BLACK]);
				//g.fillRect(WORLD_SIZE - MINIMAP_PLOT_SIZE, 0, MINIMAP_PLOT_SIZE, MINIMAP_PLOT_SIZE);
				
				// Scale player position to mini-map offset
				d = (double)MINIMAP_PLOT_SIZE / ((double)(MINIMAP_PLOT_SIZE >> MINIMAP_VIEW_SIZE_POW) * VIEW_ZOOM);
				tx = (playerPos[0] * d);
				ty = (playerPos[1] * d);
				
				// Get integer part of player position (corrected for negative rounding to +inf)
				rx = tx < 0 ? (int)tx - 1 : (int)tx;
				ry = ty < 0 ? (int)ty - 1 : (int)ty;

				// Update mini-map
				for (px = 1; px < (1 << MINIMAP_VIEW_SIZE_POW); px++) {
					for (py = 1; py < (1 << MINIMAP_VIEW_SIZE_POW); py++) {
						
						// Calculate offset into mini-map
						qx = (MINIMAP_SIZE >> 1) - (1 << (MINIMAP_VIEW_SIZE_POW-1)) + px - 2 + rx;
						qy = (MINIMAP_SIZE >> 1) - (1 << (MINIMAP_VIEW_SIZE_POW-1)) + py - 2 + ry;
						
						pi = qy*MINIMAP_SIZE + qx;
						
						// Mini-map pixel value = average of accumulated grid values
						minimap[pi] = minimapGrid[px][py] / (double)(1 << ((WORLD_SIZE_POW - MINIMAP_VIEW_SIZE_POW) << 1));
					}
				}
				
				// Draw mini-map
				for (px = 0; px < MINIMAP_PLOT_SIZE; px++) {
					for (py = 0; py < MINIMAP_PLOT_SIZE; py++) {
						
						// Offsets into minimap and screen arrays
						i = (MINIMAP_SIZE >> 1) - (MINIMAP_PLOT_SIZE >> 1);
						pi = (py + ry + i)*MINIMAP_SIZE + px + rx + i;
						qi = py*WORLD_SIZE + px + WORLD_SIZE - MINIMAP_PLOT_SIZE; //qx;
						
						// Transform value from mini-map to palette index
						j = (int)(minimap[pi] * (double)PALETTE_SIZE) & (PALETTE_SIZE-1);

						// Un-comment to allow transparent background to be drawn
						//if (j != 0)
							pixels[qi] = activePalette[j];
					}
				}
				
				// White dot where the player is
				//pi = ((MINIMAP_PLOT_SIZE >> 1) - 2)*WORLD_SIZE + (MINIMAP_PLOT_SIZE >> 1) + qx - 2;
				//pixels[pi] = 0xFFFFFF;
				
				
				//#############################################################################
				// TEXT DRAWING
				//#############################################################################

				// Default colour & font
				g.setFont(smallFont);
				
				// Display gold
				String score = String.valueOf(playerGold);
				px = score.length();
				qx = WORLD_SIZE/2 - 40;				
				
				g.setColor(symbolColour[COLOUR_YELLOW]);
				g.drawString(score, qx - px*7, 40);
				g.drawString(messages.substring(HUD_MESSAGE_FIRST*MESSAGE_LENGTH, (HUD_MESSAGE_FIRST+1)*MESSAGE_LENGTH), qx + px*8 + 8, 40);

				// Display year
				g.setColor(symbolColour[COLOUR_WHITE]);
				g.drawString(String.valueOf(year), HUD_BAR_HEIGHT, 40);
				g.drawString(messages.substring((HUD_MESSAGE_FIRST+1)*MESSAGE_LENGTH, (HUD_MESSAGE_FIRST+2)*MESSAGE_LENGTH), HUD_BAR_HEIGHT + 65, 40);
				
				// Game over, man!
				if (gameState == STATE_GAMEOVER) {
					
					// Print message lines
					for (i = 0; i < ENDING_MESSAGE_NUM; i++) {
						
						// Y offset
						j = WORLD_SIZE/2 + (i - ENDING_MESSAGE_NUM/2)*100 + 40;
						
						// Print next line
						g.drawString(messages.substring(
								i*ENDING_MESSAGE_LENGTH + ENDING_MESSAGE_FIRST, 
								(i+1)*ENDING_MESSAGE_LENGTH + ENDING_MESSAGE_FIRST), 
								WORLD_SIZE/2 - 70, j);
					}

					// Print rank
					// Rank based on log10 gold
					g.setFont(bigFont);
					g.setColor(symbolColour[COLOUR_BLACK]);

					i = px - 4; // score.length() - 4;
					i = i > ENDING_RANK_NUM-1 ? ENDING_RANK_NUM-1 : i;
					i = i < 0 ? ENDING_RANK_FIRST : ENDING_RANK_FIRST+i;
					
					g.drawString(messages.substring(i*MESSAGE_LENGTH, (i+1)*MESSAGE_LENGTH), WORLD_SIZE/2 - 75, WORLD_SIZE/2 + 100);
				}

				// Only a few decimal places please...
				//DecimalFormat df2 = new DecimalFormat("#.##");
				//DecimalFormat df4 = new DecimalFormat("#.####");

				// XXX: DEBUG - Player and camera info
				//g.drawString("x: " + String.valueOf(df2.format(actorPos[0][0])), 20, 45);
				//g.drawString("y: " + String.valueOf(df2.format(actorPos[0][1])), 20, 60);
				//g.drawString("rot: " + String.valueOf(df2.format(actorRot[0])), 20, 75);
				//g.drawString("zoom: " + String.valueOf(df2.format(VIEW_ZOOM)), 20, 105);
				//g.drawString("target rot: " + String.valueOf(df2.format(playerTargetRot)), 20, 120);

				// XXX: DEBUG - Gold, food, wood & iron
				//g.drawString("Gold: " + String.valueOf(playerGold), 300, 30);
				//g.drawString("Food: " + String.valueOf(playerRations), 200, 45);
				//g.drawString("Wood: " + String.valueOf(playerWood), 300, 60);
				//g.drawString("Iron: " + String.valueOf(playerIron), 300, 75);
				//g.drawString("Settlers: " + String.valueOf(playerSettlers), 300, 90);

				// XXX: DEBUG - Screen movement info
				//g.drawString("pix: " + String.valueOf(pxInd[0]), 20, 90);
				//g.drawString("piy: " + String.valueOf(pxInd[1]), 20, 105);
				//g.drawString("d(pix): " + String.valueOf(deltaPxInd[0]), 20, 120);
				//g.drawString("d(piy): " + String.valueOf(deltaPxInd[1]), 20, 135);

				// XXX: DEBUG - Number of octaves
				//g.drawString("Num. octaves: " + String.valueOf(P_NUM_OCTAVES), 20, 150);


				//#############################################################################
				// DISPLAY FRAME & WAIT TIL NEXT FRAME
				//#############################################################################

				// Draw the entire results on the screen.
				appletGraphics.drawImage(screen, 0, 0, null);

				// After first frame is drawn, avoid redrawing whole screen
				gameRefresh = false;

				// Lock frame rate to 60 MAX_FPS
				while (System.nanoTime() - lastTime < NANOSECS_PER_SEC / MAX_FPS);
			}
		}
	}
	

	@Override
	public boolean handleEvent(Event e) {
		
		int down = 0;
		
		switch (e.id) {
		case Event.KEY_PRESS:
		case Event.KEY_ACTION:
			down = 1;
		case Event.KEY_RELEASE:
		case Event.KEY_ACTION_RELEASE:
			keys[INPUT_KEY + e.key] = down;
			break;
		case Event.MOUSE_DOWN:
		case Event.MOUSE_UP:
			keys[e.modifiers] = e.id&1;
		case Event.MOUSE_DRAG:
			down = 1;
		case Event.MOUSE_MOVE:
			keys[MOUSE_DRAG] = down;
			keys[MOUSE_X] = e.x;
			keys[MOUSE_Y] = e.y;
		}
		return false;
	}
		
}
