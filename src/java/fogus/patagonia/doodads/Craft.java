// https://code.google.com/p/4kraft/

package fogus.patagonia.doodads;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Event;
import java.awt.Graphics2D;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.Vector;

@SuppressWarnings("serial")
public class Craft extends Applet implements Runnable {
	
	// =====================================================================================================================================
	// GAME WIDE CONSTANTS (these are optimized out)
	// =====================================================================================================================================
	
	private static final double MATH_PI              = 3.14159265;
	private static final double MATH_HALF_PI         = 1.57079633;
	
	private static final int KEY_LEFT                = 0;
	private static final int KEY_RIGHT               = 1;
	private static final int KEY_UP                  = 2;
	private static final int KEY_DOWN                = 3;
	private static final int KEY_UNPAUSED            = 4;
	private static final int MOUSE_LEFT              = 5;
	private static final int MOUSE_RIGHT             = 6;
	private static final int KEY_NUMBER_1            = 7;
	/* Values in the range of 7..15 are reserved for keys: 1, 2, 3, 4, 5, 6, 7, 8, 9 */
	private static final int KEY_SHIFT               = 16;
	private static final int KEY_Q                   = 17;
	private static final int KEY_PLUS                = 18;
	private static final int KEY_MINUS               = 19;
	
	private static final int MOUSE_X                 = 0;
	private static final int MOUSE_Y                 = 1;
	
	private static final int SCREEN_WIDTH            = 800;
	private static final int SCREEN_HEIGHT           = 600;
	private static final int MAP_SIZE                = 1600;
	private static final int NAVIGATION_BAR_SIZE     = 100;
	private static final int MAP_VELOCITY            = 10;
	private static final int GROUND_TEXTURE_SIZE     = 56;
	
	private static final int UNIT_TYPES              = 8;
	
	private static final int UNIT_PROP_TEAM          = 0;
	private static final int UNIT_PROP_TYPE          = 1;
	private static final int UNIT_PROP_HEALTH        = 2;
	private static final int UNIT_PROP_MAX_HEALTH    = 3;
	private static final int UNIT_PROP_COOLDOWN      = 4;
	private static final int UNIT_PROP_MAX_COOLDOWN  = 5;
	private static final int UNIT_PROP_X             = 6;
	private static final int UNIT_PROP_Y             = 7;
	private static final int UNIT_PROP_V             = 8;
	private static final int UNIT_PROP_SIZE          = 9;
	private static final int UNIT_PROP_DAMAGE        = 10;
	private static final int UNIT_PROP_IS_SELECTED   = 11;
	private static final int UNIT_PROP_TARGET_X      = 12;
	private static final int UNIT_PROP_TARGET_Y      = 13;
	private static final int UNIT_PROP_TARGET_UNIT   = 14;
	private static final int UNIT_PROP_FIRE_RANGE    = 15;
	private static final int UNIT_PROP_IS_FREELANCER = 16; // By this I mean the unit decided on his own to attack another one (not by firing back or following orders)
	private static final int UNIT_PROP_EXPLOSION_R   = 17;
	private static final int UNIT_PROP_FACING_THETA  = 18; // Angle where the unit is facing to (degree!)
	private static final int UNIT_PROP_WAS_HIT       = 19;
	private static final int UNIT_PROP_IS_MOVING     = 20;
	
	private static final int BULLET_PROP_X           = 0;
	private static final int BULLET_PROP_Y           = 1;
	private static final int BULLET_PROP_OWNER_UNIT  = 2;
	private static final int BULLET_PROP_TARGET_UNIT = 3;
	
	private static final int SCROLLING_BORDER        = 18;
	
	private static final int MAX_LEVEL               = 10;
	
	private static final int TARGET_ANIMATION_R      = 12;
	
	public void start() {
		new Thread( this ).start();
	}
	
	public void run() {
		
		// =====================================================================================================================================
		// GAME MODEL AND STATE (MODEL OF MVC)
		// =====================================================================================================================================
		
		int iteration    = -1;
		int subiteration = 0; // Used to slow down level selection before game and to time the target point animation
		int mapX         = 0;
		int mapY         = 0;
		int selectionX1  = 0, selectionY1 = 0, selectionX2 = 0, selectionY2 = 0;
		int winner       = 2;
		int level        = 1;
		int tx           = 0; // Target x coordinate
		int ty           = 0; // Target y coordinate
		int fps          = 30;
		
		Vector< int[] > units   = null; // All the units (ours and opponent's)
		Vector< int[] > bullets = null;
		
		// =====================================================================================================================================
		// GAME VARIABLES
		// =====================================================================================================================================
		int i, j, l, p, q, r, s, x, y, x2, y2, m;
		double theta;
		int[] unit, unit2, bullet, unitCounts = new int[ UNIT_TYPES * 2 ]; // unitCounts holds how many of each type of units each team has
		final BufferedImage groundTexture  = new BufferedImage( GROUND_TEXTURE_SIZE, GROUND_TEXTURE_SIZE, BufferedImage.TYPE_INT_RGB );
		final BufferedImage buffer         = new BufferedImage( SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_RGB );
		final Graphics2D    graphics       = (Graphics2D) buffer.getGraphics();
		final Graphics2D    appletGraphics = (Graphics2D) getGraphics();
		final Random        random         = new Random();
		
		long lastTime = 0;
		
		// Game cycle
		while ( true ) {
			if ( System.nanoTime() - lastTime > ( 1000000000/fps ) ) {
				lastTime = System.nanoTime();
				
				// =============================================================================================================================
				// GAME CONTROLLER AND LOGIC (CONTROLLER OF MVC)
				// =============================================================================================================================
				
				if ( k [ KEY_PLUS  ] && fps < 50 )
					fps++;
				if ( k [ KEY_MINUS ] && fps > 15 )
					fps--;
				
				if ( k[ KEY_UNPAUSED ] || iteration < 0 ) { // iteration < 0 => to init the game so the startup screen will be valid 
					
					if ( winner >= 0 || iteration < 0 ) {
						// Init new game
						// Generate new ground texture
						for ( i = 0; i < GROUND_TEXTURE_SIZE; i++ )
							for ( j = 0; j < GROUND_TEXTURE_SIZE; j++ )
								groundTexture.setRGB( i, j, ( ( 30*256 + 70 + random.nextInt( 50 ) ) << 8 ) + 30 );
						
						iteration   = 0;
						mapX        = 0;
						mapY        = ( MAP_SIZE - SCREEN_HEIGHT + NAVIGATION_BAR_SIZE ) / 2;
						selectionX1 = -1; selectionY1 = -1; selectionX2 = -1; selectionY2 = -1;
						if ( winner == 0 )
							if ( ++level > MAX_LEVEL )
								level = MAX_LEVEL;
						winner      = -1;
						
						// Create units
						units = new Vector< int[] >();
						for ( i = 0; i < 2; i++ ) { // Teams
							for ( j = 0; j < UNIT_TYPES; j++ ) { // Unit types (small j means smaller unit)
								p = i == 0 && ( j == 0 && level >= MAX_LEVEL-1 || j == 1 && level == MAX_LEVEL ) ? 0 :
									i == 0 ? ( level<9 ? level+1 : 17-level ) : level<8?level+1 + level/3 + level/5+ level/7 : 15;
								unitCounts[ i * UNIT_TYPES + j ] = p;
								for ( l = 0; l < p; l++ ) { // Unit count
									//                   { team, type, health  , maxhealth, coold, maxcoold, x                                    , y                                              , v   , size  , damage, selected, targetX, targetY, targetUnit, fireRange, freelancer, explosion r, facing angle, was hit, is moving }
									units.add( new int[] {    i,    j, 100+j*20,  100+j*20,     0,  40+j*10, i == 0 ? 20+j*40 : MAP_SIZE-20 - j*40, MAP_SIZE/2 + 72 - p*100/2 + l*100 - (j&0x03)*22, 10-j, 15+j*2, 10+j*2, 0       , -1     , 0      , -1        , j * 40   , 1         , 15+j*2     , i * 180     , 0      , 0         } );
								}
							}
						}
						
						bullets = new Vector< int[] >();
						
					}
					else {
						// Calculate next iteration
						iteration++;
						
						final int mx = c[ MOUSE_X ], my = c[ MOUSE_Y ];
						
						if ( k[ KEY_LEFT ] || mx < SCROLLING_BORDER && my < SCREEN_HEIGHT - NAVIGATION_BAR_SIZE )
							mapX -= MAP_VELOCITY;
						if ( k[ KEY_RIGHT ] || mx > SCREEN_WIDTH - SCROLLING_BORDER && my < SCREEN_HEIGHT - NAVIGATION_BAR_SIZE )
							mapX += MAP_VELOCITY;
						if ( k[ KEY_UP ] || my < SCROLLING_BORDER )
							mapY -= MAP_VELOCITY;
						if ( k[ KEY_DOWN ] || my > SCREEN_HEIGHT - NAVIGATION_BAR_SIZE - SCROLLING_BORDER && my < SCREEN_HEIGHT - NAVIGATION_BAR_SIZE )
							mapY += MAP_VELOCITY;
						
						if ( k[ KEY_Q ] ) {
							for ( i = 0; i < units.size(); i++ ) {
								unit = units.get( i );
								if ( ( unit = units.get( i ) )[ UNIT_PROP_HEALTH ] >= 0 )
									unit[ UNIT_PROP_IS_SELECTED ] = unit[ UNIT_PROP_TEAM ] == 0 ? 1 : 0;
							}
						}
						else
							for ( j = KEY_NUMBER_1; j < KEY_NUMBER_1 + 9; j++ )
								if ( k[ j ] ) {
									// If selection should be controlled by the pressed numbers...
									for ( i = 0; i < units.size(); i++ ) {
										unit = units.get( i );
										unit[ UNIT_PROP_IS_SELECTED ] = k[ KEY_NUMBER_1 + unit[ UNIT_PROP_TYPE ] ] && unit[ UNIT_PROP_TEAM ] == 0 && unit[ UNIT_PROP_HEALTH ] >= 0 ? 1 : 0;
									}
									break;
								}
						
						// Is mouse over minimap?
						final boolean mouseOnMinimap = my > SCREEN_HEIGHT - NAVIGATION_BAR_SIZE - 2 && my < SCREEN_HEIGHT - 1 && mx > 0 && mx < NAVIGATION_BAR_SIZE + 1;
						
						if ( selectionX1 < 0 && k[ MOUSE_LEFT ] && my < SCREEN_HEIGHT - NAVIGATION_BAR_SIZE - 2 ) {
							// Start area selection
							selectionX1 = mx + mapX;
							selectionY1 = my + mapY;
						}
						if ( selectionX1 >= 0 ) {
							if ( k[ MOUSE_LEFT ] ) {
								// Update selection area
								selectionX2 = mx + mapX;
								selectionY2 = my + mapY;
							}
							else {
								// Stop area selection
								for ( i = 0; i < units.size(); i++ ) {
									unit = units.get( i );
									if ( unit[ UNIT_PROP_HEALTH ] < 0 ) continue;
									x = unit[ UNIT_PROP_X ];
									y = unit[ UNIT_PROP_Y ];
									s = unit[ UNIT_PROP_SIZE ] / 2;
									l = selectionX2; p = selectionX1;
									if ( selectionX1 < selectionX2 ) { l = selectionX1; p = selectionX2; }
									q = selectionY2; r = selectionY1;
									if ( selectionY1 < selectionY2 ) { q = selectionY1; r = selectionY2; }
									unit[ UNIT_PROP_IS_SELECTED ] = l > x + s || p < x - s || q > y + s || r < y - s ? ( k[ KEY_SHIFT ] && unit[ UNIT_PROP_IS_SELECTED ] == 1 ? 1 : 0 ) : ( k[ KEY_SHIFT ] && unit[ UNIT_PROP_IS_SELECTED ] == 1 ? 0 : 1 );
								}
								selectionX1 = -1;
							}
						}
						
						if ( selectionX1 < 0 && mouseOnMinimap && k[ MOUSE_LEFT ] ) {
							// Mouse is over the navigation bar
							mapX = MAP_SIZE * ( mx - 1                                       ) / NAVIGATION_BAR_SIZE - SCREEN_WIDTH / 2;
							mapY = MAP_SIZE * ( my - SCREEN_HEIGHT + NAVIGATION_BAR_SIZE + 1 ) / NAVIGATION_BAR_SIZE - ( SCREEN_HEIGHT - NAVIGATION_BAR_SIZE - 2 ) / 2;
						}
						
						// We check the positions whether they are outside the valid domains
						if ( mapX < 0 )
							mapX = 0;
						if ( mapX > MAP_SIZE - SCREEN_WIDTH )
							mapX = MAP_SIZE - SCREEN_WIDTH;
						if ( mapY < 0 )
							mapY = 0;
						if ( mapY > MAP_SIZE - SCREEN_HEIGHT + NAVIGATION_BAR_SIZE + 2 )
							mapY = MAP_SIZE - SCREEN_HEIGHT + NAVIGATION_BAR_SIZE + 2;
						
						if ( k[ MOUSE_RIGHT ] && ( my < SCREEN_HEIGHT - NAVIGATION_BAR_SIZE - 2 || mouseOnMinimap ) ) {
							// Move or attack command
							x = mouseOnMinimap ? MAP_SIZE * ( mx - 1                                       ) / NAVIGATION_BAR_SIZE : mx + mapX;
							y = mouseOnMinimap ? MAP_SIZE * ( my - SCREEN_HEIGHT + NAVIGATION_BAR_SIZE + 1 ) / NAVIGATION_BAR_SIZE : my + mapY;
							for ( i = 0; i < units.size(); i++ ) {
								unit = units.get( i );
								if ( unit[ UNIT_PROP_IS_SELECTED ] > 0 && unit[ UNIT_PROP_TEAM ] == 0 ) {
									unit[ UNIT_PROP_IS_FREELANCER ] = k[ KEY_SHIFT ] ? 1 : 0;
									// Check if target is a unit..
									boolean isTargetUnit = false;
									for ( j = 0; j < units.size(); j++ ) {
										unit2 = units.get( j );
										if ( unit2[ UNIT_PROP_HEALTH ] < 0 ) continue;
										if ( x < unit2[ UNIT_PROP_X ] + unit2[ UNIT_PROP_SIZE ]/2 && x > unit2[ UNIT_PROP_X ] - unit2[ UNIT_PROP_SIZE ]/2 && y < unit2[ UNIT_PROP_Y ] + unit2[ UNIT_PROP_SIZE ]/2 && y > unit2[ UNIT_PROP_Y ] - unit2[ UNIT_PROP_SIZE ]/2 ) {
											isTargetUnit = true;
											unit[ UNIT_PROP_TARGET_UNIT ] = j;
											break;
										}
									}
									if ( !isTargetUnit ) {
										unit[ UNIT_PROP_TARGET_UNIT ] = -1;
										unit[ UNIT_PROP_TARGET_X    ] = x;
										unit[ UNIT_PROP_TARGET_Y    ] = y;
										tx = x; ty = y; subiteration = iteration + TARGET_ANIMATION_R/2;
									}
								}
							}
						}
						
						// Step the units
						for ( i = 0; i < units.size(); i++ ) {
							unit = units.get( i );
							r = unit[ UNIT_PROP_EXPLOSION_R ];
							if ( unit[ UNIT_PROP_HEALTH ] < 0 ) {
								unit[ UNIT_PROP_EXPLOSION_R ]--;
								if ( unit[ UNIT_PROP_EXPLOSION_R ] > 10 )
									unit[ UNIT_PROP_EXPLOSION_R ]--;
								if ( r >= 0 && unit[ UNIT_PROP_EXPLOSION_R ] < 0 ) // The unit has just died now
									unitCounts[ unit[ UNIT_PROP_TEAM ] * UNIT_TYPES + unit[ UNIT_PROP_TYPE ] ]--;
								continue;
							}
							if ( unit[ UNIT_PROP_COOLDOWN ] > 0 )
								unit[ UNIT_PROP_COOLDOWN ]--;
							x = unit[ UNIT_PROP_X ]; y = unit[ UNIT_PROP_Y ];
							
							if ( unit[ UNIT_PROP_TARGET_UNIT ] >= 0 ) {
								unit2 = units.get( unit[ UNIT_PROP_TARGET_UNIT ] );
								x2 = unit2[ UNIT_PROP_X ]; y2 = unit2[ UNIT_PROP_Y ];
								if ( (x-x2)*(x-x2) + (y-y2)*(y-y2) < unit[ UNIT_PROP_FIRE_RANGE ] * unit[ UNIT_PROP_FIRE_RANGE ] ) {
									// If inside fire range...
									if ( unit2[ UNIT_PROP_HEALTH ] < 0 || unit[ UNIT_PROP_TEAM ] == unit2[ UNIT_PROP_TEAM ] ) {
										unit[ UNIT_PROP_TARGET_UNIT   ] = -1; // Stop beating a dead horse or following our own unit without attacking
										unit[ UNIT_PROP_IS_FREELANCER ] = 1;
									}
									else
										if ( unit[ UNIT_PROP_COOLDOWN ] == 0 ) {
											unit[ UNIT_PROP_COOLDOWN ] = unit[ UNIT_PROP_MAX_COOLDOWN ];
											bullets.add( new int[] { x, y, i, unit[ UNIT_PROP_TARGET_UNIT ] } );
										}
									unit[ UNIT_PROP_TARGET_X ] = -1;
									// Turn to face our target
									unit[ UNIT_PROP_FACING_THETA ] = (int) ( java.lang.Math.atan2( x2 - x, y2 - y ) / -MATH_PI * 180 + 90 );
								}
								else {
									// Else move closer to it
									unit[ UNIT_PROP_TARGET_X ] = x2;
									unit[ UNIT_PROP_TARGET_Y ] = y2;
								}
							}
							
							unit[ UNIT_PROP_IS_MOVING ] = 0;
							if ( unit[ UNIT_PROP_TARGET_X ] >= 0 ) {
								p = unit[ UNIT_PROP_TARGET_X ] - x;
								q = unit[ UNIT_PROP_TARGET_Y ] - y;
								// If distance is less than V, don't step
								if ( p*p + q*q < unit[ UNIT_PROP_V ] * unit[ UNIT_PROP_V ] ) {
									// Stop moving so the unit can attack if enemy is nearby
									unit[ UNIT_PROP_TARGET_X ] = -1;
								}
								else {
									tries:
									for ( l = 0; l < 4; l++ ) {
										theta = java.lang.Math.atan2( p, q ) + ( l == 1 ? MATH_HALF_PI : l == 2 ? -MATH_HALF_PI : l == 3 ? MATH_PI : 0 );
										x2 = unit[ UNIT_PROP_X ] += unit[ UNIT_PROP_V ] * java.lang.Math.sin( theta );
										y2 = unit[ UNIT_PROP_Y ] += unit[ UNIT_PROP_V ] * java.lang.Math.cos( theta );
										// Collision detection: if collides, do not take this step
										for ( j = 0; j < units.size(); j++ ) {
											if ( j == i ) continue;
											unit2 = units.get( j );
											if ( unit2[ UNIT_PROP_HEALTH ] < 0 ) continue;
											if ( x2 + unit[ UNIT_PROP_SIZE ]/2 < unit2[ UNIT_PROP_X ] - unit2[ UNIT_PROP_SIZE ]/2 || x2 - unit[ UNIT_PROP_SIZE ]/2 > unit2[ UNIT_PROP_X ] + unit2[ UNIT_PROP_SIZE ]/2 
											  || y2 + unit[ UNIT_PROP_SIZE ]/2 < unit2[ UNIT_PROP_Y ] - unit2[ UNIT_PROP_SIZE ]/2 || y2 - unit[ UNIT_PROP_SIZE ]/2 > unit2[ UNIT_PROP_Y ] + unit2[ UNIT_PROP_SIZE ]/2 )
												;
											else {
												unit[ UNIT_PROP_X ] = x;
												unit[ UNIT_PROP_Y ] = y;
												continue tries;
											}
										}
										unit[ UNIT_PROP_IS_MOVING ] = 1;
										unit[ UNIT_PROP_FACING_THETA ] = (int) ( theta / -MATH_PI * 180 + 90 );
										break tries;
									}
								}
							}
							
							if ( unit[ UNIT_PROP_TARGET_X ] < 0 && unit[ UNIT_PROP_TARGET_UNIT ] < 0 || unit[ UNIT_PROP_IS_FREELANCER ] == 1 ) { // No target point and no target unit
								// Check if enemy is nearby and find ourself a target
								for ( j = 0; j < units.size(); j++ ) {
									unit2 = units.get( j );
									if ( unit2[ UNIT_PROP_HEALTH ] >= 0 && unit2[ UNIT_PROP_TEAM ] != unit[ UNIT_PROP_TEAM ] ) { // This also excludes attacking itself
										p = unit2[ UNIT_PROP_X ] - x;
										q = unit2[ UNIT_PROP_Y ] - y;
										if ( p*p + q*q < 2 * unit[ UNIT_PROP_FIRE_RANGE ] * unit[ UNIT_PROP_FIRE_RANGE ] ) {
											unit[ UNIT_PROP_TARGET_UNIT ] = j;
											unit[ UNIT_PROP_IS_FREELANCER ] = 0;
											break;
										}
									}
								}
								if ( unit[ UNIT_PROP_TARGET_UNIT ] < 0 && unit[ UNIT_PROP_IS_FREELANCER ] == 1 && unit[ UNIT_PROP_TEAM ] == 1 && iteration > 100 + ( unit[ UNIT_PROP_V ] << 5 ) ) {
									// Comp go attack!
									unit[ UNIT_PROP_TARGET_X      ] = 40;
									unit[ UNIT_PROP_TARGET_Y      ] = unit[ UNIT_PROP_Y ];
									unit[ UNIT_PROP_IS_FREELANCER ] = 1;
								}
							}
						}
						
						// Step the bullets
						for ( i = bullets.size() - 1; i >= 0 ; i-- ) { // Descending order is a must because bullets might get removed here
							bullet = bullets.get( i );
							x = bullet[ BULLET_PROP_X ]; y = bullet[ BULLET_PROP_Y ];
							unit2 = units.get( bullet[ BULLET_PROP_TARGET_UNIT ] );
							p = unit2[ UNIT_PROP_X ] - x;
							q = unit2[ UNIT_PROP_Y ] - y;
							// If distance is less than V, damage
							if ( p*p + q*q < 4 * unit2[ UNIT_PROP_V ] * unit2[ UNIT_PROP_V ] ) {
								unit2[ UNIT_PROP_HEALTH ] -= units.get( bullet[ BULLET_PROP_OWNER_UNIT ] )[ UNIT_PROP_DAMAGE ];
								unit2[ UNIT_PROP_WAS_HIT ] = 2;
								// Unit death:
								if ( unit2[ UNIT_PROP_HEALTH ] < 0 ) {
									unit2[ UNIT_PROP_IS_SELECTED ] = 0;
									// Check all units that targeted this (now dead) unit
									for ( j = 0; j < units.size(); j++ ) {
										if ( units.get( j )[ UNIT_PROP_TARGET_UNIT ] == bullet[ BULLET_PROP_TARGET_UNIT ] ) {
											// If meanwhile (while the bullet hit the target) the unit was given no order, it's mission is complete
											units.get( j )[ UNIT_PROP_TARGET_UNIT ] = -1;
											units.get( j )[ UNIT_PROP_TARGET_X ] = -1;     // To stop coming closer
											units.get( j )[ UNIT_PROP_IS_FREELANCER ] = 1;
										}
									}
								}
								else {
									// Unit was hit. If the unit has no order or is a freelancer, fire back
									if ( unit2[ UNIT_PROP_IS_FREELANCER ] == 1 || unit2[ UNIT_PROP_TARGET_X ] < 0 && unit2[ UNIT_PROP_TARGET_UNIT ] < 0 )
										unit2[ UNIT_PROP_TARGET_UNIT ] = bullet[ BULLET_PROP_OWNER_UNIT ];
								}
								bullets.remove( i );
							}
							else {
								theta = java.lang.Math.atan2( p, q );
								bullet[ BULLET_PROP_X ] += 2 * unit2[ UNIT_PROP_V ] * java.lang.Math.sin( theta );
								bullet[ BULLET_PROP_Y ] += 2 * unit2[ UNIT_PROP_V ] * java.lang.Math.cos( theta );
							}
						}
						
						// Game end condition
						for ( i = 0; i < 2; i++ ) {// teams
							for ( j = 0, s = 0; j < UNIT_TYPES; j++ )
								s += unitCounts[ i * UNIT_TYPES + j ];
							if ( s == 0 ) {
								winner = 1 - i; // In case of a "draw" (all units died) this will favour us
								tx = -1;
								k[ KEY_UNPAUSED ] = false;
							}
						}
					}
				}
				else
					if ( ( iteration == 0 || winner == 0 || winner == 1 ) && subiteration++ > 2 ) { // subiteration++ must be the last condition, we use the short evaluation here, and else it would change its value when game is paused during game => would increase target animation (whose timing is based on this variable)!
						subiteration = 0;
						if ( k[ KEY_UP ] ) {
							if ( ++level > MAX_LEVEL )
								level = MAX_LEVEL;
							else
								iteration = -1;  // Regenerate level
						}
						if ( k[ KEY_DOWN ] ) {
							if ( --level < 1 )
								level = 1;
							else
								iteration = -1;  // Regenerate level
						}
					}
				
				// =============================================================================================================================
				// GAME VIEW (VIEW OF MVC)
				// =============================================================================================================================
				
				Color BLACK;
				// Draw on buffer...
				graphics.translate( -mapX, -mapY );
				// Draw the battle field, game scene
				graphics.setPaint( new TexturePaint( groundTexture, new Rectangle2D.Float( 0, 0, GROUND_TEXTURE_SIZE, GROUND_TEXTURE_SIZE ) ) );
				graphics.fillRect( 0, 0, MAP_SIZE, MAP_SIZE );
				graphics.setColor( new Color( 255, 0, 0 ) );
				graphics.drawRect( 0, 0, MAP_SIZE-1, MAP_SIZE-1 );
				
				final Color WHITE = new Color( 255, 255, 255 );
				
				// Draw units
				for ( l = 0; l < 2; l++ ) // dead and alive units (I want explosion to be alive to be drawn over the dead ones)
					for ( i = 0; i < units.size(); i++ ) {
						unit = units.get( i );
						if ( l == 0 && unit[ UNIT_PROP_EXPLOSION_R ] >= 0 || l == 1 && unit[ UNIT_PROP_EXPLOSION_R ] < 0 ) continue;
						BLACK = unit[ UNIT_PROP_MAX_COOLDOWN ] - unit[ UNIT_PROP_COOLDOWN ] < 2 ? WHITE : new Color( 0, 0, 0 );
						
						q = unit[ UNIT_PROP_TYPE ];
						j = 255 - ( unit[ UNIT_PROP_HEALTH ] >= 0 ? unit[ UNIT_PROP_TYPE ] * 12 : 0 );
						final Color color = unit[ UNIT_PROP_WAS_HIT ] > 0 && unit[ UNIT_PROP_HEALTH ] >= 0? new Color( 150, unit[ UNIT_PROP_TEAM ] * 250, 0 ) : new Color( unit[ UNIT_PROP_TEAM ] == 0 ? j/2 : j, unit[ UNIT_PROP_TEAM ] == 0 ? j/2 : j/3, unit[ UNIT_PROP_TEAM ] == 0 ? j : j/3 );
						unit[ UNIT_PROP_WAS_HIT ]--;
						
						graphics.setColor( color );
						s = unit[ UNIT_PROP_SIZE ];
						x = unit[ UNIT_PROP_X ]; y = unit[ UNIT_PROP_Y ];
						if ( unit[ UNIT_PROP_HEALTH ] < 0 ) {
							random.setSeed( i );
							for ( p = 0; p < s*3; p++ ) {
								r     = random.nextInt( 3*s/2 ); // Radius
								theta = random.nextInt( 360 ) * MATH_PI / 180;
								graphics.fillOval( (int) ( x + r * java.lang.Math.cos( theta ) ), (int) ( y + r * java.lang.Math.sin( theta ) ), 3, 3 );
							}
							// Explosion
							for ( r = unit[ UNIT_PROP_EXPLOSION_R ]; r >= 0; r -= 5 ) {
								graphics.setColor( new Color( 220 - r, 220 - r, 60 - r ) );
								graphics.fillOval( x - r, y - r, r*2, r*2 );
							}
						}
						else {
							m = unit[ UNIT_PROP_IS_MOVING ] * ( iteration % 5 - 2 );
							final AffineTransform storedAffineTransform = graphics.getTransform(); // We store the transform, because we draw units rotated.
							graphics.translate( x, y );
							graphics.rotate( unit[ UNIT_PROP_FACING_THETA ] * MATH_PI / 180 );
							switch ( q ) {
							case 0: // Probe
								graphics.fillOval( -s/2, -s/4, s, s/2 );
								graphics.setColor( BLACK );
								graphics.drawLine( s/2-3, 0, s/2, -4+m );
								graphics.drawLine( s/2-3, 0, s/2,  4-m );
								break;
							case 1: // Reaper
								graphics.fillOval( -s/4, -s/2, s/2, s );
								graphics.setColor( BLACK );
								for ( j = 1; j < 5; j++ ) {
									graphics.drawLine( 0, -j+1, s/2-5+j, -j*2-1+m );
									graphics.drawLine( 0,  j-1, s/2-5+j,  j*2+1-m );
									graphics.drawLine( -s/2+5-j, -j+1+m, 0, -j*2-1 );
									graphics.drawLine( -s/2+5-j,  j-1-m, 0,  j*2+1 );
								}
								break;
							case 2: // Viking
								graphics.fillOval( -s/4, -s/2, s/2, s );
								graphics.setColor( BLACK );
								graphics.fillOval( -s/4, -s/4, s/2, s/2 );
								graphics.drawLine( 0, s/2, s/2, -s/4 );
								graphics.fillOval(   s/4+2, -s/4,  3*m, s/5 );
								graphics.fillOval(   s/4+2,  s/4, -3*m, s/5 );
								graphics.fillOval(  -s/4-4, -s/4, -3*m, s/5 );
								graphics.fillOval(  -s/4-4, +s/4,  3*m, s/5 );
								break;
							case 3: // Winger
								graphics.fillOval( -s/4-6, -s/2, s/2+6, s );
								graphics.setColor( BLACK );
								for ( j = 0; j < 5; j++ ) {
									graphics.drawLine( -j, -s/2, s/2-j, 0 );
									graphics.drawLine( s/2-j, 0, -j, s/2 );
									graphics.drawLine( 1, 0, -s/2,  2*j - 4+m );
								}
								break;
							case 4: // Roach
								graphics.setColor( BLACK );
								graphics.fillOval( 0, -s/4, s/2, s/2 );
								for ( j = -3; j <= 1; j++ ) {
									graphics.drawLine( j*3+m, -s/2  , j-2+m, 0 );
									graphics.drawLine( j*3+m,  s/2+1, j-2+m, 0 );
								}
								graphics.setColor( color );
								graphics.fillOval( -3*s/8-2, -3*s/8, 3*s/4, 3*s/4 );
								break;
							case 5: // Vulture
								graphics.setColor( BLACK );
								graphics.fillRect( -5*s/12, -5*s/12+1-m/2, s/4, 5*s/6-2+m );
								graphics.fillRect(   s/4-1, -s/3+1   -m/2, s/4, 2*s/3-2+m );
								graphics.setColor( color );
								graphics.fillPolygon( new int[] { -s/2, s/2+1, s/2+1, -s/2 }, new int[] { s/4, s/7, -s/7, -s/4 }, 4 );
								break;
							case 6: // Panzer
								graphics.setColor( BLACK );
								graphics.fillPolygon( new int[] { s/4 + m, s/4 + m, -s/2 + m, s/2 + m, -s/2 + m, s/4  + m }, new int[] { -s/2, s/2, -s/2, 0, s/2, -s/2 }, 6 );
								graphics.setColor( color );
								graphics.fillOval( -s/4-5, -s/4-3, s/2+7, s/2+4 );
								graphics.setColor( BLACK );
								graphics.fillOval( -s/4-1, -s/5-1, s/2+3, 2*s/5+2 );
								break;
							default: // Tank
								graphics.fillRect( -s/2, -s/2, s, s );
								graphics.setColor( BLACK );
								graphics.fillOval( -s/4, -s/4+1, s/2, s/2-1 );
								graphics.fillRect( 0, -2, s/2+5, 4 );
								graphics.drawRect( -s/2, -s/2, s, s/4 );
								graphics.drawRect( -s/4 + m, -s/2, s/2, s/4 );
								graphics.drawRect( -s/2,  s/4+1, s, s/4 );
								graphics.drawRect( -s/4 + m,  s/4+1, s/2, s/4 );
							}
							graphics.setTransform( storedAffineTransform );
						}
						if ( unit[ UNIT_PROP_IS_SELECTED ] > 0 ) {
							graphics.setColor( unit[ UNIT_PROP_TEAM ] == 0 ? new Color( 100, 230, 100 ) : new Color( 255, 10, 10 ) );
							graphics.drawOval( x - s*7/10 - 1, y - s*7/10 - 1, s*7/5, s*7/5 );
							// Unit health
							graphics.setColor( new Color( 240, 0, 0 ) );
							graphics.fillRect( x - s/2, y + s/2 + 2, s, 5 );
							graphics.setColor( new Color( 0, 250, 0 ) );
							graphics.fillRect( x - s/2, y + s/2 + 2, s * unit[ UNIT_PROP_HEALTH ] / unit[ UNIT_PROP_MAX_HEALTH ], 5 );
						}
					}
				BLACK = new Color( 0, 0, 0 );
				
				// Target point animation
				graphics.setColor( new Color( 0, 255, 0 ) );
				r = 2 * ( subiteration - iteration );
				if ( r > TARGET_ANIMATION_R - 5 )
					r += 2 * ( TARGET_ANIMATION_R - r )+1;
				if ( r > 0 ) {
					r=2*r/3;
					graphics.drawOval( tx - r, ty - r, 2*r, 2*r );
					graphics.fillOval( tx - 1, ty - 1, 3, 3 );
				}
				
				// Draw bullets
				graphics.setColor( WHITE );
				for ( i = 0; i < bullets.size(); i++ ) {
					bullet = bullets.get( i );
					j = units.get( bullet[ BULLET_PROP_OWNER_UNIT ] )[ UNIT_PROP_TYPE ] + 1;
					graphics.fillOval( bullet[ BULLET_PROP_X ] - j/2, bullet[ BULLET_PROP_Y ] -j/2, j, j );
				}
				
				if ( selectionX1 >= 0 ) {
					// Selection rectangle
					graphics.setColor( new Color( 100, 250, 100 ) );
					graphics.drawRect( selectionX1 < selectionX2 ? selectionX1 : selectionX2,
							selectionY1 < selectionY2 ? selectionY1 : selectionY2,
							java.lang.Math.abs( selectionX2 - selectionX1 ), java.lang.Math.abs( selectionY2 - selectionY1 ) );
				}
				
				graphics.translate( mapX, mapY );
				// Draw the minimap
				// Minimap border
				graphics.setColor( new Color( 255, 0, 0 ) );
				graphics.drawRect( 0, SCREEN_HEIGHT - NAVIGATION_BAR_SIZE - 2, NAVIGATION_BAR_SIZE+1, NAVIGATION_BAR_SIZE+1 );
				// We draw info panel border because color is already set
				graphics.drawRect( NAVIGATION_BAR_SIZE + 2, SCREEN_HEIGHT - NAVIGATION_BAR_SIZE - 2, SCREEN_WIDTH - NAVIGATION_BAR_SIZE - 3 , NAVIGATION_BAR_SIZE + 1 );
				// Minimap fill
				graphics.setPaint( new TexturePaint( groundTexture, new Rectangle2D.Float( 0, 0, (float) NAVIGATION_BAR_SIZE * GROUND_TEXTURE_SIZE / MAP_SIZE, (float) NAVIGATION_BAR_SIZE * GROUND_TEXTURE_SIZE / MAP_SIZE ) ) );
				graphics.fillRect( 1, SCREEN_HEIGHT - NAVIGATION_BAR_SIZE - 1, NAVIGATION_BAR_SIZE, NAVIGATION_BAR_SIZE );
				// Indicate units on minimap
				for ( i = 0; i < units.size(); i++ ) {
					unit = units.get( i );
					if ( unit[ UNIT_PROP_HEALTH ] < 0 ) continue;
					graphics.setColor( new Color( unit[ UNIT_PROP_TEAM ] == 0 ? 130 : 255, 130, unit[ UNIT_PROP_TEAM ] == 0 ? 255 : 130 ) );
					graphics.fillRect( 1 + unit[ UNIT_PROP_X ] * NAVIGATION_BAR_SIZE / MAP_SIZE , SCREEN_HEIGHT - NAVIGATION_BAR_SIZE - 1 + unit[ UNIT_PROP_Y ] * NAVIGATION_BAR_SIZE / MAP_SIZE, 2, 2 );
				}
				
				// Clean the text area
				graphics.setColor( BLACK );
				graphics.fillRect( NAVIGATION_BAR_SIZE + 3, SCREEN_HEIGHT - NAVIGATION_BAR_SIZE - 1, SCREEN_WIDTH - NAVIGATION_BAR_SIZE - 4, NAVIGATION_BAR_SIZE );
				
				if ( !k[ KEY_UNPAUSED ] )
					graphics.fillRect( SCREEN_WIDTH/2 - 195, ( SCREEN_HEIGHT - NAVIGATION_BAR_SIZE ) / 2 - 65, 390, 156 );
				
				// Screen location in minimap
				graphics.setColor( WHITE );
				graphics.drawRect( 1 + ( NAVIGATION_BAR_SIZE - NAVIGATION_BAR_SIZE * SCREEN_WIDTH / MAP_SIZE ) * mapX / (MAP_SIZE-SCREEN_WIDTH),
						SCREEN_HEIGHT - NAVIGATION_BAR_SIZE - 1 + ( NAVIGATION_BAR_SIZE - NAVIGATION_BAR_SIZE * (SCREEN_HEIGHT-NAVIGATION_BAR_SIZE-2) / MAP_SIZE ) * mapY / (MAP_SIZE-SCREEN_HEIGHT+NAVIGATION_BAR_SIZE+2),
						NAVIGATION_BAR_SIZE * SCREEN_WIDTH / MAP_SIZE - 1,
						NAVIGATION_BAR_SIZE * (SCREEN_HEIGHT-NAVIGATION_BAR_SIZE-2) / MAP_SIZE - 1 );
				
				// Game texts
				String someString = "LEVEL: ".concat( String.valueOf( level ) );
				graphics.drawString( iteration == 0 || winner == 0 || winner == 1 ? someString.concat( " - press W S to change" ) : someString, SCREEN_WIDTH/2 + NAVIGATION_BAR_SIZE/2 - 30, SCREEN_HEIGHT - NAVIGATION_BAR_SIZE + 20 );
				graphics.drawString( "Speed: ".concat( String.valueOf( fps ) ).concat( " (+/-)" ), SCREEN_WIDTH - 109, SCREEN_HEIGHT - NAVIGATION_BAR_SIZE + 20 );
				if ( winner >= 0 )
					graphics.drawString( winner == 0 ? "VICTORY!" : "DEFEAT!", SCREEN_WIDTH/2 - 22 + winner, ( SCREEN_HEIGHT - NAVIGATION_BAR_SIZE ) / 2 - 29 );
				if ( !k[ KEY_UNPAUSED ] ) {
					graphics.drawRect( SCREEN_WIDTH/2 - 195, ( SCREEN_HEIGHT - NAVIGATION_BAR_SIZE ) / 2 - 65, 390, 156 );
					graphics.drawString( "4 K R A F T", SCREEN_WIDTH/2 - 25, ( SCREEN_HEIGHT - NAVIGATION_BAR_SIZE ) / 2 - 46 );
					graphics.drawString( "Paused", SCREEN_WIDTH/2 - 19, ( SCREEN_HEIGHT - NAVIGATION_BAR_SIZE ) / 2 - 10 );
					for ( i = 0; i < 6; i++ ) {
						graphics.drawString(              i == 0 ? "W S A D or mouse to edge" : i == 1 ? "click on minimap"   : i == 2 ? "mouse left right"                         : i == 3 ? "numbers or Q"             : i == 4 ? "SHIFT + select"            : "SHIFT + move" , SCREEN_WIDTH/2 - 185, ( SCREEN_HEIGHT - NAVIGATION_BAR_SIZE ) / 2 + 8 + i*15 );
						graphics.drawString( "- ".concat( i == 0 ? "scroll"                   : i == 1 ? "center click point" : i == 2 ? "select / move / attack (on minimap too!)" : i == 3 ? "select same or all units" : i == 4 ? "extend / narrow selection" : "ATTACK-MOVE" ), SCREEN_WIDTH/2 -  35, ( SCREEN_HEIGHT - NAVIGATION_BAR_SIZE ) / 2 + 8 + i*15 );
					}
				}
				for ( i = 0; i < 2; i++ ) { // Team
					graphics.setColor( new Color( i == 0 ? 130 : 255, 130, i == 0 ? 255 : 130 ) );
					for ( j = 0; j < UNIT_TYPES; j++ ) {
						if ( unitCounts[ i * UNIT_TYPES + j ] > 0 )
							graphics.drawString( ( j == 0 ? "Probe" : j == 1 ? "Reaper" : j == 2 ? "Viking" : j == 3 ? "Winger" : j == 4 ? "Roache" : j == 5 ? "Vulture" : j == 6 ? "Panzer" : "Tank" ).concat( "s: " ).concat( String.valueOf( unitCounts[ i * UNIT_TYPES + j ] ) ), NAVIGATION_BAR_SIZE - 61 + ( UNIT_TYPES - j ) * 85, SCREEN_HEIGHT - NAVIGATION_BAR_SIZE + 50 + i * 30 );
					}
				}
				
				// Now show the buffer.
				appletGraphics.drawImage( buffer, 0, 0, null );
			}
			
			if ( !isActive() )
				return;
			try { Thread.sleep( 1 ); } catch ( Exception e ) {}
		}
	}
	
	/**
	 * States of the keys we use,
	 * the Game pause state (where false=paused, true=not paused),
	 * mouse button states.
	 */
	private final boolean[] k = new boolean[ 64 ]; // 64 might be compressed better as it has just one "1" bit
	
	/** Mouse pointer coordinate values. */
	private final int[]     c = new int[ 2 ];
	
	
	/**
	 * Handles the keyboard and mouse events controlling the game.
	 */
	@Override
	public boolean handleEvent( final Event event ) {
		k[ event.key == 'a' ? KEY_LEFT : event.key == 'd' ? KEY_RIGHT : event.key == 'w'? KEY_UP : event.key == 's' ? KEY_DOWN
				: event.key >= '1' && event.key <= '9' ? KEY_NUMBER_1 + event.key - '1'
				: event.key == 'q' ? KEY_Q : event.key == '+' ? KEY_PLUS : event.key == '-' ? KEY_MINUS : 63 ]
				= event.id == Event.KEY_PRESS;
		
		if ( event.id == Event.KEY_PRESS && event.key == ' ' )
			k[ KEY_UNPAUSED ] = !k[ KEY_UNPAUSED ];
		
		k[ KEY_SHIFT ] = event.shiftDown();
		
		if ( event.id == Event.MOUSE_MOVE || event.id == Event.MOUSE_DRAG ) {
			c[ MOUSE_X ] = event.x;
			c[ MOUSE_Y ] = event.y;
		}
		
		if ( event.id == Event.MOUSE_DOWN || event.id == Event.MOUSE_UP )
			k[ event.metaDown() ? MOUSE_RIGHT : MOUSE_LEFT ] = event.id == Event.MOUSE_DOWN;
		
		return false;
	}
	
}
