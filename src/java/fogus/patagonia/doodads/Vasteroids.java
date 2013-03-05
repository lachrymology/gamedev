package fogus.patagonia.doodads;

import java.applet.Applet;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Event;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Vasteroids extends Applet implements Runnable {
	
	private boolean[] keys = new boolean[32768];
	private int mousex;
	private int mousey;

	// game states
	private static final int STATE_FIRST_TIME = 0;
	private static final int STATE_INIT = 1;
	private static final int STATE_ACTIVE = 2;
	private static final int STATE_GAME_OVER = 3;

	// entity types
	private static final int ENEMY0 = 0; // orbits the centre
	private static final int ENEMY1 = 1; // slow homing/dodging enemy
	private static final int ENEMY2 = 2; // turns in to an enemy generator
	private static final int ENEMY3 = 3; // fast homing/dodging enemy
	private static final int ENEMY4 = 4; // zig zagging homing enemy
	private static final int ENEMY5 = 5; // fast homing enemy, doesn\'t dodge
	private static final int ENEMY_GENERATOR = 6;
	private static final int PLAYER = 7;
	private static final int PARTICLE = 8;
	private static final int BULLET = 9;
	private static final int SCORE_TEXT = 10;
	private static final int SPAWN_EFFECT = 11;
	
	// enemyType variable indexes
	private static final int MAX_SPEED = 1;
	private static final int ACCELERATION = 2;
	private static final int SPAWN_QUANTITY = 3;
	private static final int SPAWN_DELAY = 4;
	private static final int THINK_TIME = 5;
	private static final int DODGE_ANGLE = 6;
	private static final int NEXT_SPAWN = 7;
	private static final double[][] enemyTypes = new double[][]{
			new double[]{ENEMY0, 2, 0.1, 5, 700, 0, 0, 0},
			new double[]{ENEMY1, 2.5, 0.1, 5, 900, 0, java.lang.Math.PI/6, 0},
			new double[]{ENEMY2, 2, 0.1, 1, 200, 1, 0, 0},
			new double[]{ENEMY3, 4, 0.1, 1, 2300, 0, java.lang.Math.PI/6, 0},
			new double[]{ENEMY4, 2, 0.2, 1, 120, 90, java.lang.Math.PI/6, 0},
			new double[]{ENEMY5, 4, 0.5, 1, 300, 200, 0, 0},
			new double[]{ENEMY_GENERATOR, 0, 0, 0, 0, 0, 0, 0}
		};
		
	// game parameters
	private static final double FRICTION = 0.99;
	private static final double BOUNCE = 0.9;
	private static final double PLAYER_ACC = 0.3;
	private static final double PLAYER_SHOT_SPEED = 10;
	private static final int PLAYER_SHOT_DELAY = 10;
	private static final double PLAYER_SIZE = 10;
	private static final int ARENA_RADIUS = 512;
	private static final int CENTRE_RADIUS = 50;
	private static final double ENEMY_SIZE = 40;
	private static final int SCREEN_WIDTH = 800;
	private static final int SCREEN_HEIGHT = 600;
	private static final int SPAWN_WARMUP_TIME = 60;
	private static final int FLASH_TIME = 60;
	private static final int DEATH_DELAY = 180;
	private static final int IMMUNE_TIME = 180;
	private static final int GENERATOR_DELAY = 300;
	private static final int PERSPECTIVE_FACTOR = 20;
	private static final int DIFFICULTY_INCREASE_PERIOD = 1800;
	private static final double DIFFICULTY_SPAWN_FACTOR = 0.9;

	// entity variable indexes
	private static final int TYPE = 0;
	private static final int DX2 = 1;
	private static final int DY2 = 2;
	private static final int DX = 3;
	private static final int DY = 4;
	private static final int X = 5;
	private static final int Y = 6;
	private static final int FACING = 7;
	private static final int LIFESPAN = 8;
	private static final int AGE = 9;
	private static final int GROWTH = 10;
	private static final int TARGET_FACING = 10;
	private static final int STATE = 11;
	
	public void start() {
		new Thread(this).start();
	}

	public void run() {
		
		double ratio;
		double len;
		double dist;
		double distSq;
		double distPlusVelSq;
		double theta;
		double x;
		double y;
		int i;
		int j;
		int k;
		Graphics2D g2;
		double[] bullet;
		double[] particle;
		double[] enemy;
		double[] enemyType;
		double[] entity;
		BufferedImage image;
		Color color;
		final BasicStroke[] strokes = new BasicStroke[13];
		
		boolean paused = false;
		boolean canPause = true;
		int deathTimer = 0;
		int immuneTimer = 0;
		int fireTimer = 0;
		int flashTimer = 0;
		
		int state = STATE_FIRST_TIME;
		
		double[] player = new double[12];
		final ArrayList<double[]> bullets = new ArrayList<double[]>();
		final ArrayList<double[]> enemies = new ArrayList<double[]>();
		final ArrayList<double[]> particles = new ArrayList<double[]>();
		final BufferedImage images[] = new BufferedImage[10];
		long levelTime = 0; // in frames
		int lives = 0;
		int score = 0;
		
		for (i = 0; i < strokes.length; i++) {
			strokes[i] = new BasicStroke(i+1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		}
		
//		setSize(800, 600); // for AppletViewer, remove when using an Applet
		
		Canvas canvas = new Canvas();
		canvas.setSize(800, 600);
		add(canvas);
		canvas.createBufferStrategy(2);
		BufferStrategy strategy = canvas.getBufferStrategy();
		
		// load image data for each entity type
		final String s = "\u0032\u0006\u0000\u00ff\u0000\u000f\u002d\u0005\u0019\u000f\u0005\u002d\u0019\u000f\u002d\u000f\u0005\u0032\u0005\u0000\u00ff\u00ff\u0019\u002d\u002d\u0019\u0019\u0005\u0005\u0019\u0019\u002d\u0032\u000e\u00ff\u0064\u0064\u0019\u0019\u000f\u002d\u0023\u002d\u0019\u0019\u002d\u0023\u002d\u000f\u0019\u0019\u0023\u0005\u000f\u0005\u0019\u0019\u0005\u000f\u0005\u0023\u0019\u0019\u0019\u0019\u0032\u0005\u00ff\u00ff\u0000\u002a\u0019\u0008\u0005\u0008\u002d\u002a\u0019\u0008\u0019\u0032\u0004\u00ff\u0000\u0000\u002a\u0019\u0008\u0005\u0008\u002d\u002a\u0019\u0032\u0008\u00ff\u0000\u00ff\u0019\u002d\u002d\u0019\u0019\u0005\u0005\u0019\u0019\u002d\u0019\u0005\u002d\u0019\u0005\u0019\u0032\u000e\u00ff\u00af\u00af\u0019\u0019\u000f\u002d\u0023\u002d\u0019\u0019\u002d\u0023\u002d\u000f\u0019\u0019\u0023\u0005\u000f\u0005\u0019\u0019\u0005\u000f\u0005\u0023\u0019\u0019\u0019\u0019\u0032\u0005\u00ff\u00ff\u00ff\u0005\u002d\u002d\u0019\u0005\u0005\u0019\u0019\u0005\u002d\u0018\u0002\u0000\u0000\u00ff\u0007\u000c\u000c\u000c\u000c\u0005\u00ff\u00ff\u00ff\u0005\u0005\u0007\u0005\u0007\u0007\u0005\u0007\u0005\u0005";
		for (i = k = 0; i < images.length; i++) {
			image = new BufferedImage(s.charAt(k), s.charAt(k++), BufferedImage.TYPE_4BYTE_ABGR_PRE);
			g2 = (Graphics2D)image.getGraphics();
			int[] xPoints = new int[s.charAt(k)], yPoints = new int[s.charAt(k++)];
			color = new Color(s.charAt(k++), s.charAt(k++), s.charAt(k++));
			for (j = 0; j < xPoints.length; j++) {
				xPoints[j] = s.charAt(k++);
				yPoints[j] = s.charAt(k++);
			}
			for (j = 0; j < 10; j++) {
				g2.setColor(j == 0? Color.WHITE: color);
				g2.setStroke(strokes[j == 0? 2: 13-j]);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, j == 0? 1.0f: 0.1f));
				g2.drawPolyline(xPoints, yPoints, xPoints.length);
			}
			g2.dispose();
			images[i] = image;
		}

		int tick = 0, fps = 0, acc = 0, acc2 = 0;
		long lastTime = System.nanoTime();

		// main game loop
		while (true) {
			long now = System.nanoTime();
			acc += now - lastTime;
			tick++;
			if (acc >= 1000000000L) {
				acc -= 1000000000L;
				fps = tick;
				tick = 0;
			}
			
			if (state == STATE_FIRST_TIME || state == STATE_GAME_OVER) {
				// if player presses any mouse button, then start the game
				if (keys[0] || keys[1] || keys[2]) {
					state = STATE_INIT;
				}
				
			} else {
				
				// initialise a new game
				if (state == STATE_INIT) {
					player = new double[12];
					player[TYPE] = PLAYER;
					player[Y] = 80;
					lives = 3;
					score = 0;
					levelTime = 0;
					immuneTimer = IMMUNE_TIME;
					for (i = 0; i < enemyTypes.length; i++) {
						enemyTypes[i][NEXT_SPAWN] = i == ENEMY0 || i == ENEMY1? 0: enemyTypes[i][SPAWN_DELAY];
					}
					
					bullets.clear();
					enemies.clear();
					particles.clear();
					state = STATE_ACTIVE;
				}
				
				
				if (!keys[112]) { // 112 = the P key
					canPause = true;
				} else if (canPause && keys[112]) {
					canPause = false;
					paused = !paused;
				}
				
				if (!paused) {
					
					acc2 += now - lastTime;
					while (acc2 >= 16666667) {
						acc2 -= 16666667;
						
						// UPDATE
						
						// update particles
						for (i = particles.size()-1; i >= 0; i--) {
							particle = particles.get(i);
							if (++particle[AGE] > particle[LIFESPAN]) {
								particles.remove(i);
							}
							// apply velocity
							particle[X] += particle[DX];
							particle[Y] += particle[DY];
							if (particle[TYPE] == PARTICLE) {
								// bounce off arena boundaries
								distSq = particle[X]*particle[X] + particle[Y]*particle[Y];
								distPlusVelSq = (particle[X]+particle[DX])*(particle[X]+particle[DX]) + (particle[Y]+particle[DY])*(particle[Y]+particle[DY]);
								if (distSq > ARENA_RADIUS * ARENA_RADIUS && distPlusVelSq > distSq) {
									bounce(particle, true);
									particle[FACING] = java.lang.Math.atan2(particle[DY], particle[DX]);
								} else if (distSq < CENTRE_RADIUS * CENTRE_RADIUS && distPlusVelSq < distSq) {
									bounce(particle, false);
									particle[FACING] = java.lang.Math.atan2(particle[DY], particle[DX]);
								}
							}
						}

						if (flashTimer > 0) flashTimer--;
						
						if (deathTimer > 0) {
							if (--deathTimer > 0) {
								continue;
							} else {
								lives--;
								immuneTimer = IMMUNE_TIME;
							}
						}
						if (lives <= 0) {
							state = STATE_GAME_OVER;
							continue;
						}
						if (immuneTimer > 0) immuneTimer--;
						if (fireTimer > 0) fireTimer--;
						levelTime++;

						// update player
						x = mousex - 400 - (int)player[X]/2;
						y = mousey - 300 - (int)player[Y]/2;
						player[FACING] = java.lang.Math.atan2(y, x);
						player[DX2] = 0;
						player[DY2] = 0;

						// bounce off arena boundaries
						distSq = player[X]*player[X] + player[Y]*player[Y];
						distPlusVelSq = (player[X]+player[DX])*(player[X]+player[DX]) + (player[Y]+player[DY])*(player[Y]+player[DY]);
						if (distSq > (ARENA_RADIUS - 20) * (ARENA_RADIUS - 20)) {
							if (distPlusVelSq > distSq) {
								bounce(player, true);
							}
						} else if (distSq < (CENTRE_RADIUS + 20) * (CENTRE_RADIUS + 20)) {
							if (distPlusVelSq < distSq) {
								bounce(player, false);
							}
							
						// if player presses mouse button 1, accelerate in direction of the mouse pointer
						} else if (keys[0] && (x != 0 || y != 0)) {
							ratio = PLAYER_ACC / java.lang.Math.sqrt(x*x + y*y);
							player[DX2] = x * ratio;
							player[DY2] = y * ratio;
						}
						
						// apply acceleration
						player[DX] += player[DX2];
						player[DY] += player[DY2];
						
						// \'friction\'
						player[DX] *= FRICTION;
						player[DY] *= FRICTION;
						
						// apply velocity
						player[X] += player[DX];
						player[Y] += player[DY];

						// if player presses mouse button 3, then fire
						if (keys[2] && fireTimer <= 0) {
							fireTimer = PLAYER_SHOT_DELAY;
							bullet = new double[12];
							bullet[TYPE] = BULLET;
							bullet[X] = player[X];
							bullet[Y] = player[Y];
							bullet[DX] = PLAYER_SHOT_SPEED * java.lang.Math.cos(player[FACING]);
							bullet[DY] = PLAYER_SHOT_SPEED * java.lang.Math.sin(player[FACING]);
							bullet[FACING] = java.lang.Math.atan2(bullet[DY], bullet[DX]);
							bullets.add(bullet);
						}
						
						// spawn more enemies if required
						for (i = 0; i < enemyTypes.length; i++) {
							enemyType = enemyTypes[i];
							if (enemyType[SPAWN_QUANTITY] > 0 && levelTime >= enemyType[NEXT_SPAWN]) {
								dist = CENTRE_RADIUS*2 + java.lang.Math.random() * (ARENA_RADIUS - CENTRE_RADIUS*3);
								theta = java.lang.Math.random() * java.lang.Math.PI * 2;
								x = dist * java.lang.Math.cos(theta);
								y = dist * java.lang.Math.sin(theta);
								for (j = 0; j < enemyType[SPAWN_QUANTITY]; j++) {
									enemy = new double[12];
									enemy[TYPE] = enemyType[TYPE];
									enemy[X] = x + 0.1*j;
									enemy[Y] = y + 0.1*j;
									enemy[AGE] = -SPAWN_WARMUP_TIME;
									enemies.add(enemy);
								}
								particle = new double[12];
								particle[TYPE] = SPAWN_EFFECT;
								particle[LIFESPAN] = SPAWN_WARMUP_TIME;
								particle[X] = x;
								particle[Y] = y;
								particles.add(particle);

								enemyTypes[i][NEXT_SPAWN] = levelTime + enemyTypes[i][SPAWN_DELAY] * java.lang.Math.pow(DIFFICULTY_SPAWN_FACTOR, (int)(levelTime/DIFFICULTY_INCREASE_PERIOD));
							}
						}
						
						// update bullets
						for (i = bullets.size()-1; i >= 0; i--) {
							bullet = bullets.get(i);
							// apply velocity
							bullet[X] += bullet[DX];
							bullet[Y] += bullet[DY];
							distSq = bullet[X]*bullet[X] + bullet[Y]*bullet[Y];
							if (distSq < CENTRE_RADIUS * CENTRE_RADIUS || distSq > ARENA_RADIUS * ARENA_RADIUS) {
								bullets.remove(i);
							}
						}
						
						// update enemies
						for (i = enemies.size()-1; i >= 0; i--) {
							enemy = enemies.get(i);
							if (++enemy[AGE] < 0) continue;
							boolean dodge = false;
							enemyType = enemyTypes[(int)enemy[TYPE]];
							enemy[DX2] = 0;
							enemy[DY2] = 0;
							
							if (enemy[TYPE] == ENEMY_GENERATOR) {
								// rotate
								enemy[FACING] = java.lang.Math.PI * 2 * (enemy[AGE]%(int)enemy[STATE]) / enemy[STATE];
								// generate new enemy if required
								if (enemy[AGE] >= enemy[GROWTH]) {
									entity = new double[12];
									entity[TYPE] = ENEMY1;
									entity[X] = enemy[X];
									entity[Y] = enemy[Y];
									entity[AGE] = -SPAWN_WARMUP_TIME;
									enemies.add(entity);
									particle = new double[12];
									particle[TYPE] = SPAWN_EFFECT;
									particle[LIFESPAN] = SPAWN_WARMUP_TIME;
									particle[X] = enemy[X];
									particle[Y] = enemy[Y];
									particles.add(particle);
									// speed up enemy generation time
									if (enemy[STATE] > SPAWN_WARMUP_TIME*2) {
										enemy[STATE] -= 20;
									}
									// set next generation time
									enemy[GROWTH] = enemy[AGE] + enemy[STATE];
								}
								
							} else if (enemy[TYPE] == ENEMY0) {
								// orbit the arena centre
								len = java.lang.Math.sqrt(enemy[X]*enemy[X] + enemy[Y]*enemy[Y]);
								enemy[DX] = enemyType[MAX_SPEED] * enemy[Y]/len;
								enemy[DY] = enemyType[MAX_SPEED] * -enemy[X]/len;
								
							} else if (enemy[TYPE] == ENEMY1 || enemy[TYPE] == ENEMY3) {
								// attempt to home in on player
								x = player[X]-enemy[X];
								y = player[Y]-enemy[Y];
								dist = java.lang.Math.sqrt(x*x + y*y);
								ratio = enemyType[ACCELERATION] / dist;
								theta = player[FACING] - java.lang.Math.atan2(-y, -x);
								if (enemyType[DODGE_ANGLE] > 0 && dist < 400 && java.lang.Math.abs(theta) < enemyType[DODGE_ANGLE]) {
									// player is close and facing enemy, attempt to dodge at 90 degrees, with double acceleration 
									enemy[DX2] = (theta >= 0? -y: y) * ratio * 2;
									enemy[DY2] = (theta < 0? -x: x) * ratio * 2;
									dodge = true;
								} else {
									// player is not close and facing enemy, home in on player
									enemy[DX2] = x * ratio;
									enemy[DY2] = y * ratio;
								}
								
							} else if (enemy[TYPE] == ENEMY2) {
								// attempt to tag arena centre, then escape to arena edge
								distSq = enemy[X]*enemy[X] + enemy[Y]*enemy[Y];
								dist = java.lang.Math.sqrt(distSq);
								// if not tagged centre, the move towards the centre
								if (enemy[STATE] == 0) {
									ratio = enemyType[ACCELERATION] / dist; 
									enemy[DX2] = -enemy[X] * ratio;
									enemy[DY2] = -enemy[Y] * ratio;
									// if close enough, then mark this enemy as tagged
									if (dist < ENEMY_SIZE + CENTRE_RADIUS) {
										enemy[STATE] = 1;
									}
								} else {
									// if already tagged, then attempt to reach arena edge
									if (distSq >= (ARENA_RADIUS - ENEMY_SIZE/2) * (ARENA_RADIUS - ENEMY_SIZE/2)) {
										// enemy has tagged and escaped - convert to enemy generator.
										enemy[TYPE] = ENEMY_GENERATOR;
										enemy[STATE] = GENERATOR_DELAY;
										enemy[GROWTH] = enemy[AGE] + enemy[STATE];
									} else {
										// move towards arena edge
										ratio = enemyType[ACCELERATION] / dist; 
										enemy[DX2] = enemy[X] * ratio;
										enemy[DY2] = enemy[Y] * ratio;
									}
								}
								
							} else if (enemy[TYPE] == ENEMY4 || enemy[TYPE] == ENEMY5) {
								// moves towards player, re-targetting every THINK_TIME frames.
								// if DODGE_ANGLE is non-zero, then enemy zig-zags at an offset angle.
								// if closer than 100 pixels then simply home in on the player.
								int timer = (int)enemy[AGE]%(int)enemyType[THINK_TIME];
								if (timer == 0) {
									x = player[X]-enemy[X];
									y = player[Y]-enemy[Y];
									dist = java.lang.Math.sqrt(x*x + y*y);
									enemy[TARGET_FACING] = java.lang.Math.atan2(y, x);
									if (enemyType[DODGE_ANGLE] != 0) {
										enemy[TARGET_FACING] += (dist < 100? 0: (enemyType[THINK_TIME] == 0 || ((int)enemy[AGE]%((int)enemyType[THINK_TIME]*2) == 0)? enemyType[DODGE_ANGLE]: -enemyType[DODGE_ANGLE]));
									}
								}
								enemy[DX2] = enemyType[ACCELERATION] * java.lang.Math.cos(enemy[TARGET_FACING]);
								enemy[DY2] = enemyType[ACCELERATION] * java.lang.Math.sin(enemy[TARGET_FACING]);
							}
							
							// use repulsive force to move enemy away from other enemies of the same type
							for (j = i+1; j < enemies.size(); j++) {
								entity = enemies.get(j);
								if (entity[TYPE] == enemy[TYPE]) {
									x = enemy[X]-entity[X];
									y = enemy[Y]-entity[Y];
									distSq = x*x + y*y;
									if (distSq < ENEMY_SIZE * ENEMY_SIZE) {
										dist = java.lang.Math.sqrt(distSq);
										ratio = 0.5 * (ENEMY_SIZE - dist) / dist;
										// accelerate away from other enemy
										enemy[DX2] += x * ratio;
										enemy[DY2] += y * ratio;
									}
								}
							}
							
							// bounce off arena boundaries
							distSq = enemy[X]*enemy[X] + enemy[Y]*enemy[Y];
							distPlusVelSq = (enemy[X]+enemy[DX])*(enemy[X]+enemy[DX]) + (enemy[Y]+enemy[DY])*(enemy[Y]+enemy[DY]);
							if (distSq > (ARENA_RADIUS - 20) * (ARENA_RADIUS - 20) && distPlusVelSq > distSq) {
								bounce(enemy, true);
							} else if (distSq < (CENTRE_RADIUS + 20) * (CENTRE_RADIUS + 20) && distPlusVelSq < distSq) {
								bounce(enemy, false);
							}

							// apply acceleration
							enemy[DX] += enemy[DX2];
							enemy[DY] += enemy[DY2];
							
							// cap velocity
							double maxSpeed = dodge? enemyType[MAX_SPEED] * 2: enemyType[MAX_SPEED];
							if (enemy[DX]*enemy[DX] + enemy[DY]*enemy[DY] > maxSpeed*maxSpeed) {
								len = java.lang.Math.sqrt(enemy[DX]*enemy[DX] + enemy[DY]*enemy[DY]);
								enemy[DX] *= maxSpeed/len;
								enemy[DY] *= maxSpeed/len;
							}
							
							// apply velocity
							enemy[X] += enemy[DX];
							enemy[Y] += enemy[DY];
							
							// face velocity direction
							if (enemy[DX] != 0 && enemy[DY] != 0) {
								enemy[FACING] = java.lang.Math.atan2(enemy[DY], enemy[DX]);
							}
						}
						
						// COLLISION
						
						// player-enemy
						if (immuneTimer <= 0) {
							for (i = 0; i < enemies.size(); i++) {
								enemy = enemies.get(i);
								if (enemy[AGE] < 0) continue;
								if ((player[X] - enemy[X]) * (player[X] - enemy[X]) + (player[Y] - enemy[Y]) * (player[Y] - enemy[Y]) < (PLAYER_SIZE/2 + ENEMY_SIZE/2) * (PLAYER_SIZE/2 + ENEMY_SIZE/2)) {
									deathTimer = DEATH_DELAY;
									flashTimer = FLASH_TIME;
									bullets.clear();
									enemies.clear();
									player[DX] = 0;
									player[DY] = 0;
									// explosion
									for (j = 0; j < 25; j++) {
										particle = new double[12];
										particle[TYPE] = PARTICLE;
										particle[LIFESPAN] = 100;
										particle[X] = player[X];
										particle[Y] = player[Y];
										particle[DX] = java.lang.Math.random() * 16 - 8;
										particle[DY] = java.lang.Math.random() * 16 - 8;
										particle[FACING] = java.lang.Math.atan2(particle[DY], particle[DX]);
										particle[GROWTH] = 0.02;
										particles.add(particle);
									}
								}
							}
						}
						
						// enemy-bullet
						for (i = bullets.size()-1; i >= 0; i--) {
							bullet = bullets.get(i);
							for (j = enemies.size()-1; j >= 0; j--) {
								enemy = enemies.get(j);
								if (enemy[AGE] < 0) continue;
								if ((bullet[X] - enemy[X]) * (bullet[X] - enemy[X]) + (bullet[Y] - enemy[Y]) * (bullet[Y] - enemy[Y]) < ENEMY_SIZE/2 * ENEMY_SIZE/2) {

									flashTimer = FLASH_TIME;
									bullets.remove(i);

									// if this is an enemy generator, then slow its generation rate
									if (enemy[TYPE] == ENEMY_GENERATOR) {
										if (enemy[STATE] < GENERATOR_DELAY) {
											enemy[STATE]+=100;
										}
										enemy[GROWTH] = enemy[AGE] + enemy[STATE];
									} else {
										
										// otherwise destroy the enemy and score points
										enemies.remove(j);
										
										dist = java.lang.Math.sqrt((player[X] - enemy[X]) * (player[X] - enemy[X]) + (player[Y] - enemy[Y]) * (player[Y] - enemy[Y]));
										ratio = dist < 200? (210-dist)/10: 1; // proximity bonus multiplier 
										k = 100*(int)ratio;
										score+=k;
										
										particle = new double[12];
										particle[TYPE] = SCORE_TEXT;
										particle[STATE] = k;
										particle[LIFESPAN] = 50;
										particle[X] = enemy[X];
										particle[Y] = enemy[Y];
										particle[DY] = -1;
										particles.add(particle);
									
										// explosion
										for (k = 0; k < 25; k++) {
											particle = new double[12];
											particle[TYPE] = PARTICLE;
											particle[LIFESPAN] = 100;
											particle[X] = enemy[X];
											particle[Y] = enemy[Y];
											particle[DX] = java.lang.Math.random() * 16 - 8;
											particle[DY] = java.lang.Math.random() * 16 - 8;
											particle[FACING] = java.lang.Math.atan2(particle[DY], particle[DX]);
											particle[GROWTH] = 0.02;
											particles.add(particle);
										}
									}
									break;
								}
							}
						}
					}
				}
			}
			lastTime = now;
			
			// RENDER
			g2 = (Graphics2D)strategy.getDrawGraphics();

			AffineTransform at = g2.getTransform();
			Composite ac = g2.getComposite();
			
			// background
			g2.setBackground(Color.BLACK);
			g2.clearRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
			g2.setClip(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
			
			// cycle colours over time
			ratio = (50 + flashTimer)/255.0;
			color = Color.getHSBColor(levelTime%1530/1530.0f, 1.0f, (float)ratio);
			
			// outer boundary + concentric circles with simple perspective transform
			for (i = PERSPECTIVE_FACTOR, j = 0, k = 6; i < 36; i+=2, k--) {
				g2.setColor(i == PERSPECTIVE_FACTOR? Color.WHITE: color);
				g2.setStroke(strokes[k < 0? 0: k]);
				g2.setTransform(at);
				g2.translate(SCREEN_WIDTH/2 - (player[X]/2 * PERSPECTIVE_FACTOR/i), SCREEN_HEIGHT/2 - (player[Y]/2 * PERSPECTIVE_FACTOR/i));
				j = (int)(ARENA_RADIUS * PERSPECTIVE_FACTOR/i);
				g2.drawOval(-j, -j, 2*j, 2*j);
			}
			// grid
			g2.setClip(new Ellipse2D.Double(-j, -j, 2*j, 2*j));
			for (i = -j; i < j; i+=40) {
				g2.drawLine(i, -j, i, j);
				g2.drawLine(-j, i, j, i);
			}

			// translate to ensure player is on screen
			g2.setTransform(at);
			g2.setClip(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
			g2.translate(SCREEN_WIDTH/2 - player[X]/2, SCREEN_HEIGHT/2 - player[Y]/2);
			
			AffineTransform at2 = g2.getTransform();
			
			// draw inner boundary
			color = Color.getHSBColor(levelTime%1530/1530.0f, 1.0f, (float)1.0f);
			g2.setPaint(new RadialGradientPaint(0, 0, 2*CENTRE_RADIUS, new float[]{0.0f, 1.0f}, new Color[]{color, new Color(0, 0, 0, 0)}));
			g2.fillOval(-CENTRE_RADIUS*2, -CENTRE_RADIUS*2, 2*CENTRE_RADIUS*2, 2*CENTRE_RADIUS*2); // inner boundary
			g2.setColor(Color.WHITE);
			g2.fillOval(-CENTRE_RADIUS, -CENTRE_RADIUS, 2*CENTRE_RADIUS, 2*CENTRE_RADIUS); // inner boundary

			// draw the entities
			for (i = 0; i < 4; i++) {
				ArrayList<double[]> entities = i == 0? null: i==1? bullets: i==2? enemies: particles;
				for (j = 0; j < (i == 0? 1: entities.size()); j++) {
					entity = i == 0? player: entities.get(j);
					// only draw entity if on screen
					if (java.lang.Math.abs(entity[X] - player[X]/2) < (SCREEN_WIDTH/2 + 50) && java.lang.Math.abs(entity[Y] - player[Y]/2) < (SCREEN_HEIGHT/2 + 50)) {
						g2.setTransform(at2);
						g2.setComposite(ac);
						g2.translate(entity[X], entity[Y]);
						g2.rotate(entity[FACING]);
						float alpha = 1.0f;
						switch (i) {
						case 0:
							alpha = deathTimer > 0 || lives <= 0? 0.0f: immuneTimer > 0? (levelTime%60)/60.0f: 1.0f;
							break;
						case 3: 
							if (entity[GROWTH] > 0) {
								g2.scale(1 + (entity[GROWTH] * entity[AGE]), 1 + (entity[GROWTH] * entity[AGE]));
							}
							alpha = 1.0f-(float)(entity[AGE]/entity[LIFESPAN]);
						}
						g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
						switch ((int)entity[TYPE]) {
						case SCORE_TEXT:
							g2.scale(3,3);
							g2.setColor(Color.WHITE);
							g2.drawString(String.valueOf((int)entity[STATE]), -10, 0);
							break;
						case SPAWN_EFFECT:
							g2.setColor(Color.BLUE);
							g2.fillRect((int)(-40),(int)(-40*alpha),(int)(80),(int)(80*alpha));
							g2.fillRect((int)(-40*alpha),(int)(-40),(int)(80*alpha),(int)(80));
							break;
						default:
							image = images[(int)entity[TYPE]];
							g2.drawImage(image, -image.getWidth()/2, -image.getHeight()/2, null);
						}
					}
				}
			}
			g2.setTransform(at);
			g2.setComposite(ac);

			// number of lives
			for (i = 0; i < lives; i++) {
				g2.setTransform(at);
				g2.translate(760 - i*50, 50);
				g2.rotate(-java.lang.Math.PI/2);
				g2.drawImage(images[PLAYER], -images[PLAYER].getWidth()/2, -images[PLAYER].getWidth()/2, null);	
			}
			// score
			g2.setTransform(at);
			g2.setColor(Color.LIGHT_GRAY);
			g2.scale(5,5);
			g2.drawString(String.valueOf(score), 6, 14);

			if (state == STATE_FIRST_TIME) {
				g2.drawString("CLICK TO START", 30, 60);	
			} else if (state == STATE_GAME_OVER) {
				g2.drawString("GAME OVER", 43, 60);
			} else if (paused) {
				g2.drawString("PAUSED", 55, 60);
			}
			
			g2.setTransform(at);
			g2.drawString("FPS: " + String.valueOf(fps), 20, 580);
			
			g2.dispose();
			strategy.show();

			try {
				Thread.sleep(1);
			} catch (Exception e) {}
			while (System.nanoTime() - lastTime < 16666667) {
				Thread.yield();
			}

			if (!isActive()) {
				return;
			}
		}
	}
	
	// bounce entity off arena centre/arena edge
	private void bounce(double[] entity, boolean inwards) {
		double len = java.lang.Math.sqrt(entity[X]*entity[X] + entity[Y]*entity[Y]);
		// unit vector normal to the \'plane\' i.e. perpendicular to circumference at approximate point of contact
		double normalx = (inwards? 1: -1) * entity[X]/len;
		double normaly = (inwards? 1: -1) * entity[Y]/len;
		double normaldot = normalx * entity[DX] + normaly * entity[DY];
		// velocity vector in direction of normal
		double velnormalx = normalx * normaldot;
		double velnormaly = normaly * normaldot;
		// velocity vector perpendicular to normal.
		double velperpx = entity[DX] - velnormalx;
		double velperpy = entity[DY] - velnormaly;
		// velocity in direction of normal switches direction; velocity in direction of perpendicular remains the same.
		entity[DX] = BOUNCE * (-velnormalx + velperpx);
		entity[DY] = BOUNCE * (-velnormaly + velperpy);
	}

	public boolean handleEvent(Event e) {
		switch (e.id) {
		case Event.KEY_PRESS:
		case Event.KEY_RELEASE:
			keys[e.key] = e.id == Event.KEY_PRESS;
			break;
		case Event.MOUSE_DOWN:
		case Event.MOUSE_UP:
			// mouse button state: key[0] = left button, key[1] = middle button, key[2] = right button
			keys[(e.modifiers & Event.META_MASK) != 0? 2: (e.modifiers & Event.ALT_MASK) != 0? 1: 0] = e.id == Event.MOUSE_DOWN;
			break;
		case Event.MOUSE_MOVE:
		case Event.MOUSE_DRAG:
			mousex = e.x;
			mousey = e.y;
			break;
		}
		return false;
	}

}