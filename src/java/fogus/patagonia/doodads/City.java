package fogus.patagonia.doodads;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Event;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 *
 * @author Russ and Maggie
 */
public class City extends Applet implements Runnable {

    @Override
    public void start() {
        //enableEvents(8);
        //enableEvents(Event.MOUSE_MOVE);
        new Thread(this).start();

    }
    final int BOTTOM = 480;
    final int EDGE = 640;
    final int SQUAREHEIGHT = 24;
    final int SQUAREWIDTH = 32;
    final int SQUAREPERWINDOW = 20;
    final short CONTROLLENGTH = 8;
    final short[] CONTROLCOLOR = {0,
        12,
        1,
        11,
        6,
        // 7,
        2,
        9,
        7};
    final short[] CONTROLZONES = {3,
        0,
        4,
        1,
        0,
        //1,
        3,
        3,
        1};
    final short[] CONTROLOFFSETS = {
        10, 10,
        10, 420,
        10, 80,
        80, 80,
        80, 420,
        //10, 280, 
        80, 10,
        150, 10,
        150, 80};
    final short[] CONTROLZONETYPE = {zonedResidental,
        0,
        powerPlant,
        powerLines,
        0,
        //* noBuilding,
        zonedCommerical,
        zonedIndustrial,
        road};
    final char[] CONTROLLABELS = {'R', 'V', 'P', 'L', 'T', /*
         * 'B',
         */ 'C', 'I', 'S'};
    final short CONTROLSIZE = 45;
    static short controlSelected = -1;
    final short NOSELECTION = -1;
    final short RESCONTROL = 0;
    final short LANDVALUECONTROL = 1;
    final short POWERPLANTCONTROL = 2;
    final short POWERLINECONTROL = 3;
    final short VIEWTRAFFICCONTROL = 4;
    final short BULLDOZECONTROL = 5;
    final short COMCONTROL = 5;
    final short INDCONTROL = 6;
    final short ROADCONTROL = 7;
    final float MAXLANDVALUE = 70;
    //ScrollShorts 
    static short scrollDelay = 80;
    //static Point mLocation = new Point();
    static int mx, my;
    static boolean[] scroll = {false, false, false, false};
    static boolean leftClick = false;
    static boolean rightClick = false;
    static int VIEWPORTTOP = 32;
    static int VIEWPORTLEFT = 32;
    static int money = 1000;
    final String S = "aaaaaaaaaaaaaaaaaaaaaaabaaaaaabbaaaaaaaacccccccccccccccccccccccc" + //64
            "deebdeebafdggdfaadddddgbaffffabbaffffffachhhhhbcccijijiccckkckkc" + //64
            "ddfbddfbafdggdfaaffffdgbafddfabbafgdgdfackkkkkhcccjijijcckhhkhhc" + //64
            "ddfbddfbafddddfalllafdgbafddfabbafdgdgfackhkhkhccbjijijcckhhkhhc" + //64
            "ddfbddfbafddddfalmlafdgbaffffaffaffffffackkkkkhccccccccccckkbkkc" + //64
            "aaabaaabaaabbaaalllaaaabaaaaaaddaaaabbaacccccccccccccccccccccccc" + //64
            "bbbbbbbbkkbbbbkkkkbbbbkkbbbjbbbbaaaaaaaacccccccccccccccccccccccc" + //64
            "bbbbbbbbkbbeebbkkbbeebbkbbbbjbbbaannnnnacbkkkkbccmhkkhmcchkbbkhc" + //64
            "bjjbjbbjkbeeeebkkbeoiebkbbbbjbbbannnnnnncbkhhkbcckkggkkcchkbbkhc" + //64
            "jbbjbjjbkbeeeebkkbeioebkbbbjbbbbangnngnncbbhhbbcckkggkkcchkbbkhc" + //64
            "bbbbbbbbkbbeebbkkbbeebbkbbbjbbbbaaggaggacbbbbbbccmhkkhmcchkbbkhc" + //64
            "bbbbbbbbkkbbbbkkkkbbbbkkbbbbjbbbaaggaggacccccccccccccccccccccccc" + //64
            "ppppppppaaaaaaaaqqqqqqqqccccccccppppppppqqqqqqqqqqqqqqqqqqqqqqqq" + //64
            "ppiiooppaaaeaaaaqqqeeeqqcceeeeccpeippeipqkbkqkkqqbbbbkkqqbkbkbkq" + //64
            "pipoopipaaeaeaaaqqqqeqqqccecccccppppppppqbebkkkqqbeebbbqqbkbkbeq" + //64
            "pipoopipaaeeaaaaqqqqeqqqccecccccppppppppqkbkqkkqqbeebkkqqbkbkbkq" + //64
            "ppooiippaaeaeaaaqqqeeeqqcceeeeccpieppiepqqqqqqqqqbbbbbbqqbkbkbkq" + //64
            "ppppppppaaaaaaaaqqqqqqqqccccccccppppppppqqqqqqqqqqqqqqqqqqqqqqqq" + //64
            "bbbbbbbbrrrrrrkrpppepeppsasasasapippppppqqqqqqqqqqqqqqqqqqqqqqqq" + //64
            "bbbbbbbbrrrrrkrkpppepeppasasasaspeppppepqkkkkkeqqbbqbbqqqqbebkkq" + //64
            "bbbjjbbbrrrkrrrreeeeeeeesasasasappppppipqbbkbbeqqbbqbbqqqbegebkq" + //64
            "bbbjjbbbrrkrkrrrpppepeppasasasaspippppppqbbkbbeqqqkqkqqqqbegebkq" + //64
            "bbbbbbbbrrrrrrrreeeeeeeesasasasapeppppepqkkkkkeqqbkkkbqqqqbebkkq" + //64
            "bbbbbbbbrrrrrrrrpppepeppasasasasppppppipqqqqqqqqqqqqqqqqqqqqqqqq";// + //64
    int[] COLORS = {
        0xff79932a, //a0
        0xff938e93,//b1
        0xffc4ede1,//c2
        0xff7e4c0a,//d3
        0xff000000,//e4
        0xffb26b0e,//f5
        0xff503006,//g6
        0xffe8edeb,//h7
        0xffe51f24,//i8
        0xffe5fb24,//j9
        0xffb4b8b6,//k10
        0xff37913f,//l11
        0xff91378c,//m12
        0xff1d5d00,//n13
        0xffe85d00,//o14
        0x00f5932a,//p15
        0xffd5e10d,//q16
        0xff0e6ab8,//r17
        0xff9ebd40//s18
    };
    //StartX,StartY,EndX,EndY,Color,Movement?
    //Level init delay
    int delay = 0;
    final static int MAPWIDTH = 128;
    final static int MAPHEIGHT = 128;
    final short CELLSTOUPDATE = MAPWIDTH * 8;
    final static int[] RIGHTLEFTDOWNUP = {1, -1, MAPWIDTH, -MAPWIDTH};
    final static int ARRAYSIZE = MAPHEIGHT * MAPWIDTH;
    static float[] newLandValue;
    static float[] landValue;
    final static short plainGround = 2;
    final static short waterDeep = 1;
    static short[] buildType;
    final static short noBuilding = 0;
    final static short zonedResidental = 1;
    final static short zonedIndustrial = 4;
    final static short waterTile = 3;
    final static short treeTile = 5;
    final static short zonedCommerical = 2;
    final static short road = 15;
    //THINGS THAT CAN TRANSMIT ELECTRICITY // START AT 25
    final static short TRANSMITSPOWER = 25;
    final static short powerPlant = 25;
    final static short powerLines = 26;
    final static short builtResidental = 27;
    final static short roadWithPowerLINE = 28;
    final static short builtIndustrial = 29;
    final static short builtCommerical = 30;
    BufferedImage[] sprites;
    static short[] graphicArray;
    final static short[] resImage = {0, 1, 2, 3, 4};
    final static short[] comImage = {5, 6, 7, 13, 14, 15};
    final static short[] indImage = {21, 22, 23, 29, 30, 31};
    final static short watImage = 25;
    final static short roadImageLeftRight = 8;
    final static short roadImageUpDown = 11;
    final static short roadImageAllway = 24;
    final static short zonedResidentialImage = 17;
    final static short zonedCommericalImage = 19;
    final static short zonedIndustrialImage = 18;
    final static short treeImage = 12;
    final static short powerLineImage = 26;
    final static short TRAFFICUP = 28;
    final static short TRAFFICLEFT = 20;
    final static short NOPOWERIMAGE = 16;
    final static short groundImage = 27;
    final static short gANEEDSUPDATE = -1;
    final static short[] powerPlantImage = {9, 10};
    static short lastPowerPlantImage = 0;
    static short[] jobs;
    static boolean[] power;
    static boolean updatePower = false;
    final static short POWERPLANTOUPUT = 30;
    static short[] roads;
    static int numRoads = 0;
    final static short NOROAD = -1;
    static short lastDraw = 0;
    static short[] traffic;
    static int numComm = 0;
    static int numInd = 0;
    final static short MAXTRAFFIC = 20;
    static short demand[] = {20, 20, 20};
    static int[] sList = new int[ARRAYSIZE];
    static short[] sCheck = new short[ARRAYSIZE];
    static int net = 0;
    static int population = 0;
    static int i, j;
    static int offset;
    static short btype;
    static int neighbor;
    static Color[] defC = new Color[19];
    static final short SSWIDTH = 64, SSHEIGHT = 24;
    static final short numSprite = 8;
    static final short SPRITEWIDTH = 8, SPRITEHEIGHT = 6;
    static long nextFrameStartTime;
    static int squareIndex;
    static int gX;
    static int gY;
    static int ticks;
    static int indBegin;
    static int indEnd;
    static int currCell;
    static int hasRoad;
    static float added;
    static int cell;

    /*
     *
     * static final void markPower(int cell) { //Walk through the grid if (cell
     * < 0 || cell >= ARRAYSIZE) { return; } if (power[cell] == 10) { return; //
     * Don't run on marked cells } power[cell] = 10; // Cell Marked
     * //ySstem.out.println("MarkPower "+cell);
     *
     * if (((cell % MAPWIDTH) + 1) <= MAPWIDTH) {
     *
     * if ((buildType[cell + 1] >= TRANSMITSPOWER) && power[cell + 1] < 5) {
     * markPower(cell + 1); } } if (((cell % MAPWIDTH) - 1) > 0) { if
     * ((buildType[cell - 1] >= TRANSMITSPOWER) && power[cell - 1] < 5) {
     * markPower(cell - 1); } } if (buildType[cell + MAPWIDTH] >= TRANSMITSPOWER
     * && power[cell + MAPWIDTH] < 5) { markPower(cell + MAPWIDTH); } if
     * (buildType[cell - MAPWIDTH] >= TRANSMITSPOWER && power[cell - MAPWIDTH] <
     * 5) { markPower(cell - MAPWIDTH); } power[cell] = POWERPLANTOUPUT;
     *
     *
     * }
     */
    /*
     * static final void clearZone(int cell) {
     *
     * if (buildType[cell] == noBuilding) { return; } boolean centralTile =
     * (edgeArray[cell] == BUILDING); buildType[cell] = noBuilding;
     * edgeArray[cell] = noBuilding; graphicArray[cell] = gANEEDSUPDATE; //If at
     * an edge find all central tiles and run on that do nothing for yourself
     * for (i = -1; i < 2; i++) { for (j = -1; j < 2; j++) { int neighbor = cell
     * + i + j * MAPWIDTH; //System.out.println("Checking "+neighbor +"
     * ("+i+","+j+")");
     *
     * if ((cell % MAPWIDTH + i) < 0 || (cell % MAPWIDTH + i) >= MAPWIDTH) {
     * System.out.println("Not touching " + neighbor); continue; }
     *
     * if (neighbor < 0 || neighbor >= ARRAYSIZE) { continue; } if (cell ==
     * neighbor) { continue; } if (edgeArray[neighbor] == BUILDING) {
     * System.out.println("CALLING AGAIN on " + neighbor); clearZone(neighbor);
     * } if (edgeArray[neighbor] == BUILDINGEDGE && centralTile) {
     * buildType[neighbor] = noBuilding; edgeArray[neighbor] = noBuilding;
     * graphicArray[neighbor] = gANEEDSUPDATE; } } }
     *
     * }
     */
    public void run() {
        boolean zoneOK;
        //init Colors and sprites

        for (i = 0; i < 19; i++) {
            defC[i] = new Color(COLORS[i]);
        }

        //Decompress spriteSheet ------------------------------------------------
        BufferedImage ss = new BufferedImage(SSWIDTH, SSHEIGHT, BufferedImage.TYPE_INT_ARGB);
        for (i = 0; i < SSHEIGHT; i++) {
            for (j = 0; j < SSWIDTH; j++) {
                ss.setRGB(j, i, COLORS[S.charAt(j + i * SSWIDTH) - 'a']);
            }
        }
        sprites = new BufferedImage[32];
        for (i = 0; i < 8; i++) {
            for (j = 0; j < 4; j++) {
                sprites[i + j * 8] = ss.getSubimage(i * SPRITEWIDTH, j * SPRITEHEIGHT, SPRITEWIDTH, SPRITEHEIGHT);
            }
        }
        // END Decompress spriteSheet

        //init map data ---------------------------------------
        jobs = new short[ARRAYSIZE];
        newLandValue = new float[ARRAYSIZE];
        landValue = new float[ARRAYSIZE];
        traffic = new short[ARRAYSIZE];
        buildType = new short[ARRAYSIZE];
        //roads = new short[ARRAYSIZE];
        power = new boolean[ARRAYSIZE];
        graphicArray = new short[ARRAYSIZE];
        for (i = 0; i < (ARRAYSIZE); i++) {
            graphicArray[i] = gANEEDSUPDATE;
            jobs[i] = 0;
            gX = i % MAPWIDTH;
            gY = i / MAPWIDTH;
            if (gX <= 4 || gX >= MAPWIDTH - 3 || gY <= 3 | gY >= MAPHEIGHT - 3) {
                buildType[i] = waterTile;

                continue;
            }
            landValue[i] = 20.f;

            buildType[i] = noBuilding;
            //roads[i] = NOROAD;
            //power[i] = 0;

//            edgeArray[i] = noBuilding;
            double t = java.lang.Math.random();
            if (t > .5) {

                buildType[i] = treeTile;

            } else if (t < .01f) {
                buildType[i] = waterTile;
            }
        }
        //END end map init -------------------------------------------

        int lastCellUpdated = 0;


        // Get image for doing double buffering
        BufferedImage image = new BufferedImage(EDGE, BOTTOM, 1);
        Graphics2D g = (Graphics2D) image.getGraphics();

        Graphics2D g2 = null;


        /**
         * Run the game loop as many times as possible until next frame time
         * has come. Then render.
         */
        // GAME LOOP
        nextFrameStartTime = System.nanoTime();
        while (true) {
            //System.out.println("FrameStart");
            //Game Logic





            scrollDelay--;
            neighbor = 0;
            // This indicates how many cells we should work on before we try to render again
            int CELLEND = (((lastCellUpdated + CELLSTOUPDATE) > ARRAYSIZE) ? ARRAYSIZE : lastCellUpdated + CELLSTOUPDATE);

            //clear power


            //System.out.println(lastCellUpdated + " " + CELLEND);
            for (cell = lastCellUpdated; cell < CELLEND; cell++) {
                //Update Graphic representation
                lastDraw++;
                btype = buildType[cell];
                if (lastDraw < 0) {
                    updatePower = true;
                    lastDraw = 0;
                }
                //First set terrain if its there
                // GRAPHIC ARRAY UPDATES
                if (graphicArray[cell] == gANEEDSUPDATE) {
                    if (btype == waterTile) {
                        graphicArray[cell] = watImage;
                    } else {
                        graphicArray[cell] = groundImage;
                    }
                    //Overwrite Terrain with building
                    if (btype == powerPlant) {
                        graphicArray[cell] = powerPlantImage[lastPowerPlantImage];
                        lastPowerPlantImage++;
                        lastPowerPlantImage %= 2;
                    }
                    if (btype == zonedResidental) {
                        graphicArray[cell] = zonedResidentialImage;
                    }
                    if (btype == builtResidental) {
                        graphicArray[cell] = resImage[lastDraw % 5];
                    }
                    if (btype == builtIndustrial) {
                        graphicArray[cell] = indImage[lastDraw % 6];
                    }
                    if (btype == builtCommerical) {
                        graphicArray[cell] = comImage[lastDraw % 6];
                    }

                    if (btype == zonedCommerical) {
                        graphicArray[cell] = zonedCommericalImage;
                    }
                    if (btype == zonedIndustrial) {
                        graphicArray[cell] = zonedIndustrialImage;
                    }
                    if (btype == treeTile) {
                        graphicArray[cell] = treeImage;
                    }
                    if (btype == road || btype == roadWithPowerLINE) {
                        boolean updown = false;
                        boolean leftright = false;
                        if (cell - 1 > 0 && buildType[cell - 1] == road) {
                            leftright = true;
                        }
                        if ((cell + 1) % MAPWIDTH != 0 && buildType[cell + 1] == road) {
                            leftright = true;
                        }
                        if (cell - MAPWIDTH > 0 && buildType[cell - MAPWIDTH] == road) {
                            updown = true;
                        }
                        if (cell + MAPWIDTH < ARRAYSIZE && buildType[cell + MAPWIDTH] == road) {
                            updown = true;
                        }

                        if (leftright && updown) {
                            graphicArray[cell] = roadImageAllway;
                        } else if (updown) {
                            graphicArray[cell] = roadImageUpDown;

                        } else if (leftright) {
                            graphicArray[cell] = roadImageLeftRight;

                        } else {
                            graphicArray[cell] = roadImageAllway;
                        }




                    }

                }
                //END Graphic ARRAY UPDATE











                //Mark Power
                //POWER PROPAGATION
                //visitedNodes.clear();
                
                // Do a power update if we need one -----------------
                /**
                 * sList functions as a stack with pointer start and j
                 * Start indicates the beginning of the stack and j the end since
                 * we never visit more cells than there are nodes in the array this should work
                 * BFS by appending to the stack. Once the stack is empty we are done
                 */
                if (btype == powerPlant && updatePower) {
                    updatePower = false;
                    for (i = 0; i < ARRAYSIZE; i++) {
                        power[i] = false; /// CLEAR POWER ARRAY
                    }
                    //power[cell] = POWERPLANTOUPUT;
                    power[cell] = true;
                    sList[0] = cell;
                    int start = 0;
                    j = 1; //end of stack
                    while (start != j) {
                        currCell = sList[start];
                        start++;
                        for (i = 0; i < 4; i++) {
                            neighbor = currCell + RIGHTLEFTDOWNUP[i];
                            //All building types greater than TRANSMITSPOWER are allowed to pass electricity
                            if (buildType[neighbor] >= TRANSMITSPOWER) {
                                if (!power[neighbor]) {
                                    power[neighbor] = true;
                                    sList[j] = neighbor;
                                    j++;
                                }
                            }
                        }

                    }
                }
                //End power update --------------------------------

                //Update Land Values ----------------------------------
                newLandValue[cell] = 0;
                if (btype == waterTile) {
                    newLandValue[cell] = 80;

                } else if (btype == treeTile) {
                    newLandValue[cell] = 50;

                } else if (btype == builtCommerical) {
                    newLandValue[cell] = 85;
                } else if (btype == builtResidental) {
                    newLandValue[cell] = 65;
                } else if (btype == builtIndustrial) {
                    newLandValue[cell] = 5;
                } else if (btype == powerPlant) {
                    newLandValue[cell] = -200;
                } else if (btype >= TRANSMITSPOWER && !power[cell]) {
                    newLandValue[cell] -= 30;

                } else {
                    newLandValue[cell] = landValue[cell];
                }
                //Start Precomputing stuff to for move-ins
                hasRoad = -1;
                added = 0;
                for (i = -1; i < 2; i++) {
                    for (j = -1; j < 2; j++) {
                        neighbor = cell + i + j * MAPWIDTH;
                        //if ((cell % MAPWIDTH + i) < 0 || (cell % MAPWIDTH + i) >= MAPWIDTH) {
                        //    continue;
                        //}
                        if (neighbor == cell) {
                            continue;
                        }
                        if (neighbor < 0 || neighbor >= ARRAYSIZE) {
                            continue;
                        }

                        newLandValue[cell] += landValue[neighbor];
                        added++;



                    }
                }
                added++;

                // Done with adding neighbors, Normalize
                newLandValue[cell] /= added;
                for (i = -2; i < 3 && hasRoad == -1; i++) {
                    for (j = -2; j < 3 && hasRoad == -1; j++) {
                        neighbor = cell + i + j * MAPWIDTH;
                        if ((cell % MAPWIDTH + i) < 0 || (cell % MAPWIDTH + i) >= MAPWIDTH || neighbor < 0 || neighbor >= ARRAYSIZE) {
                            continue;
                        }
                        if (buildType[neighbor] == road) {
                            hasRoad = neighbor;
                        }
                    }
                }


                // Does this square have power
                boolean hasPower = false;
                for (i = 0; i < 4; i++) {
                    neighbor = cell + RIGHTLEFTDOWNUP[i];
                    if (neighbor < 0 || neighbor >= ARRAYSIZE) {
                        continue;
                    }

                    if (power[neighbor]) {
                        hasPower = true;
                    }
                }


                if (hasRoad >= 0 && hasPower) {

                    // MOVE INS
                    if (btype == zonedIndustrial) {
                        if (demand[2] > 0) { // && cell has power) && has job
                            buildType[cell] = builtIndustrial;
                            jobs[cell] = 2;
                            demand[2]--;
                            demand[0]++;
                            numInd++;
                            graphicArray[cell] = gANEEDSUPDATE;

                        }
                    }



                    if (btype == zonedResidental) {
                        if (demand[0] > 0 && landValue[cell] > 40.0) { // && cell has power) && has job
                            //Find Job
                            // System.out.println("Starting Road Search");
                            for (i = 0; i < ARRAYSIZE; i++) {
                                sCheck[i] = -1;//Clear Backtrace
                            }
                            boolean foundJob = false;

                            int start = 0;
                            sList[start] = hasRoad;
                            int end = 1;
                            int jobnum = -1;
                            
                            //FINDING A JOB THis should probably be checked more often and traffic recalculated ...
                            while (start != end && !foundJob) {

                                int currCell = sList[start];



                                // System.out.println(currCell);
                                start++;

                                for (i = -2; i < 3; i++) {
                                    for (j = -2; j < 3; j++) {
                                        neighbor = currCell + i + j * MAPWIDTH;

                                        if (jobs[neighbor] > 0) {
                                            foundJob = true;
                                            jobs[neighbor]--;
                                            jobnum = currCell; //LAST ROAD TOUCHED
                                            i = j = 3;
                                        }
                                    }
                                }

                                for (i = 0; i < 4; i++) {
                                    neighbor = currCell + RIGHTLEFTDOWNUP[i];
                                    if (neighbor > 0 && (buildType[neighbor] == road || buildType[neighbor] == roadWithPowerLINE)) {
                                        if (traffic[neighbor] < MAXTRAFFIC && sCheck[neighbor] == -1) {
                                            sCheck[neighbor] = (short) i;// Direction of how we got to this square DP style
                                            sList[end] = neighbor;
                                            end++;
                                        }
                                    }

                                }

                            }

                            if (!foundJob) {
                                //System.out.println("No job Found");
                                //System.out.println(visitedNodes);
                                continue;
                            }
                            population += 5;

                            while (jobnum != hasRoad) //Until we are back at the start walk backwards along the marks and do traffic
                            {
                                //System.out.println(jobnum);
                                traffic[jobnum]++;
                                jobnum -= RIGHTLEFTDOWNUP[sCheck[jobnum]];

                            }

                            //for (i = 0; i < visitedNodes.size(); i++) {
                            //    traffic[visitedNodes.get(i)]++;
                            //}

                            buildType[cell] = builtResidental;
                            demand[0]--;
                            demand[1]++;

                            graphicArray[cell] = gANEEDSUPDATE;
                        }
                    }

                    if (btype == zonedCommerical) {
                        if (demand[1] > 0 && landValue[cell] > 50.0) { // && cell has power) && has job
                            buildType[cell] = builtCommerical;
                            jobs[cell] = 2;
                            demand[1]--;
                            demand[2]++;
                            numComm++;
                            graphicArray[cell] = gANEEDSUPDATE;

                        }
                    }
                }
                //DONE with MOVE INS



            }
            lastCellUpdated = CELLEND;
            
            //ALL CELLS UPDATED FLIP LAND VALUE ARRAYS
            if (CELLEND == ARRAYSIZE) {
                lastCellUpdated = 0;
                float[] temp = landValue;
                landValue = newLandValue;
                newLandValue = temp;



            }


            




            int mouseCellX = -1;;
            int mouseCellY = -1;
            int mouseCell;
            //Mouse Handeling 
            if (mx > SQUAREWIDTH * 7) {
                //SCROLL CONTROLS
                mouseCellX = mx / (SQUAREWIDTH) + VIEWPORTLEFT;
                mouseCellY = my / (SQUAREHEIGHT) + VIEWPORTTOP;
                mouseCell = (mouseCellX + mouseCellY * MAPWIDTH);
                //Handle Scroll Stuff
                scroll[0] = false;
                scroll[2] = false;
                //System.out.println("Mouse Location (" + mx + "," + my + ")");
                if (mx < (SCROLLWIDTH + 7 * SCROLLWIDTH)) {
                    scroll[0] = true;
                    scroll[1] = false;
                    // System.out.println("Scroll Left");
                } else if (mx > (EDGE - SCROLLWIDTH)) {
                    scroll[0] = true;
                    scroll[1] = true;
                }

                if (my < SCROLLHEIGHT) {
                    scroll[2] = true;
                    scroll[3] = false;
                } else if (my > (BOTTOM - SCROLLWIDTH)) {
                    scroll[2] = true;
                    scroll[3] = true;
                }


                //SCROLL
                if (scrollDelay < 0 && (scroll[0] || scroll[2])) {
                    if (scroll[0]) {
                        scrollDelay = 45;
                        if (scroll[1]) {
                            VIEWPORTLEFT++;
                            if (VIEWPORTLEFT > (MAPWIDTH - SQUAREPERWINDOW)) {
                                VIEWPORTLEFT = (MAPWIDTH - SQUAREPERWINDOW);
                            }
                        } else {
                            VIEWPORTLEFT--;
                            if (VIEWPORTLEFT < -5) {
                                VIEWPORTLEFT = -5;
                            }
                        }
                    }
                    if (scroll[2]) {
                        scrollDelay = 45;
                        if (scroll[3]) {
                            VIEWPORTTOP++;
                            if (VIEWPORTTOP > (MAPHEIGHT - SQUAREPERWINDOW)) {
                                VIEWPORTTOP = (MAPHEIGHT - SQUAREPERWINDOW);
                            }
                        } else {
                            VIEWPORTTOP--;
                            if (VIEWPORTTOP < 0) {
                                VIEWPORTTOP = 0;
                            }
                        }
                    }
                }
            } else {
                mouseCell = -1;
            }
            // Now lets check if this cell is acceptable for zoning
            zoneOK = true;
            //For 3by 3s
            
            if (controlSelected >= 0) {
                int indBegin = CONTROLZONES[controlSelected] / 2;
                int indEnd = CONTROLZONES[controlSelected] - indBegin;
                for (i = -indBegin; i < indEnd; i++) {

                    for (j = -indBegin; j < indEnd; j++) {
                        neighbor = mouseCell + i + j * MAPWIDTH;
                        if (neighbor < 0 || neighbor >= ARRAYSIZE) {
                            zoneOK = false;
                            continue;
                        }
                        if ((mouseCellX + i) < 0 || (mouseCellX + i) > MAPWIDTH) {
                            zoneOK = false;
                            continue;

                        }
                        if (buildType[neighbor] == road && controlSelected == POWERLINECONTROL) {
                            continue;

                        }
                        if (buildType[neighbor] == powerLines) {
                            continue;
                        }
                        if ((buildType[neighbor] != noBuilding && buildType[neighbor] != treeTile) || !zoneOK) {
                            zoneOK = false;
                        }
                    }
                }
            } else {
                zoneOK = false;
            }
            //if (controlSelected == POWERPLANTCONTROL && numPlants == MAXPLANTS) {
            //    zoneOK = false;
            //}
            /*
             * if (controlSelected == BULLDOZECONTROL) { zoneOK = true; }
             */


            //Mouse Click
            //System.out.println(mouseClick[0]+"  "+mouseClick[1]);
            if (rightClick) {
                //System.out.println("RIGHTCLICK");
                controlSelected = NOSELECTION;
                rightClick = leftClick = false;

            }
            if (leftClick) {
                //Click HANDELING
                //System.out.println("CLICK");
                rightClick = leftClick = false;


                if (mx < SQUAREWIDTH * 7) { //Clicking in the control Panel
                    for (short control = 0; control < CONTROLLENGTH; control++) {
                        if (mx > CONTROLOFFSETS[2 * control]
                                && mx < (CONTROLOFFSETS[2 * control] + CONTROLSIZE)
                                && my > (CONTROLOFFSETS[2 * control + 1])
                                && my < (CONTROLOFFSETS[2 * control + 1] + CONTROLSIZE)) {
                            controlSelected = control;
                            // System.out.println("ControlSelected " + control);
                        }
                    }
                } else {
                    if (zoneOK && CONTROLZONES[controlSelected] * CONTROLZONES[controlSelected] * 10 < money) {

                        /*
                         * if (controlSelected == BULLDOZECONTROL) { if
                         * (buildType[mouseCell] == powerPlant) { numPlants--; }
                         * // clearZone(mouseCell);
                         *
                         * continue; } System.out.println("Hit you two");
                         */
                        if (controlSelected == POWERLINECONTROL && buildType[mouseCell] == road) {
                            buildType[mouseCell] = roadWithPowerLINE;
                            graphicArray[mouseCell] = gANEEDSUPDATE;
                            continue;

                        }
                        //if (controlSelected == POWERPLANTCONTROL) {
                        //    numPlants++;
                        //}

                        if (controlSelected == ROADCONTROL) {
                            numRoads++;
                            for (i = 0; i < 4; i++) {
                                neighbor = mouseCell + RIGHTLEFTDOWNUP[i];
                                if (neighbor > 0 && buildType[neighbor] == road) {

                                    graphicArray[neighbor] = gANEEDSUPDATE;
                                }

                            }
                        }


                        indBegin = CONTROLZONES[controlSelected] / 2;
                        indEnd = CONTROLZONES[controlSelected] - indBegin;
                        for (i = -indBegin; i < indEnd; i++) {

                            for (j = -indBegin; j < indEnd; j++) {

                                neighbor = mouseCell + i + j * MAPWIDTH;
                                buildType[neighbor] = CONTROLZONETYPE[controlSelected];
                                graphicArray[neighbor] = gANEEDSUPDATE;
                                money -= 10;
                                //if (i != -indBegin && i != (indEnd - 1) && j != -indBegin && j != (indEnd - 1)) {
                                //   edgeArray[neighbor] = BUILDING;
                                //} else {
                                //    edgeArray[neighbor] = BUILDINGEDGE;
                                // }
                            }
                        }
                    }

                }




            }



            // IF it is time to redraw lets do that , otherwise lets try to process more cells
            if (nextFrameStartTime > System.nanoTime()) {
                ticks++;
                if (ticks % 3000 == 0) {
                    money += net = population * 2 + numInd * 3 + numComm * 10 - numRoads * 7;
                }
                // Draw Background
                g.setColor(Color.DARK_GRAY);

                g.fillRect(0, 0, EDGE, BOTTOM);

                //Now Render
                // 

                for (short squareX = 7; squareX < SQUAREPERWINDOW; squareX++) {
                    for (short squareY = 0; squareY < SQUAREPERWINDOW; squareY++) {
                        squareIndex = (squareY + VIEWPORTTOP) * MAPWIDTH + VIEWPORTLEFT + squareX;
                        gX = squareX * SQUAREWIDTH;
                        gY = squareY * SQUAREHEIGHT;
                        btype = buildType[squareIndex];
                        //Draw Squares
                        if (controlSelected == LANDVALUECONTROL) {
                            float lvHue = (landValue[squareIndex] / (MAXLANDVALUE * 4));
                            if (lvHue < 0) {
                                lvHue = 0;
                            }



                            g.setColor(Color.getHSBColor(lvHue, .7f, .7f));
                            g.fillRect(gX, gY, SQUAREWIDTH, SQUAREHEIGHT);
                        } else if (controlSelected == VIEWTRAFFICCONTROL) {
                            if (buildType[squareIndex] != road && buildType[squareIndex] != roadWithPowerLINE) {
                                continue;
                            }
                            float lvHue = 0.25f - ((float) traffic[squareIndex] / (MAXTRAFFIC * 4));
                            if (lvHue < 0) {
                                lvHue = 0;
                            }
                            g.setColor(Color.getHSBColor(lvHue, .7f, .7f));
                            g.fillRect(gX, gY, SQUAREWIDTH, SQUAREHEIGHT);
                        } else {

                            short grapInt = graphicArray[squareIndex];
                            if (grapInt == gANEEDSUPDATE) {
                                continue;
                            }
                            //if (grapx<7) System.out.println(grapx +" "+ grapy + " "+grapInt);




                            g.drawImage(sprites[grapInt], gX, gY, SQUAREWIDTH, SQUAREHEIGHT, null);
                            if (btype == powerLines || btype == roadWithPowerLINE) {
                                g.drawImage(sprites[powerLineImage], gX, gY, SQUAREWIDTH, SQUAREHEIGHT, null);
                            }
                            if ((ticks / 80 % 2 == 0)) {
                                if (buildType[squareIndex] > TRANSMITSPOWER && !power[squareIndex]) {
                                    g.drawImage(sprites[NOPOWERIMAGE], gX, gY, SQUAREWIDTH, SQUAREHEIGHT, null);
                                }
                                if (buildType[squareIndex] == road && traffic[squareIndex] >= MAXTRAFFIC) {
                                    g.drawImage(sprites[(graphicArray[squareIndex] == roadImageUpDown ? TRAFFICUP : TRAFFICLEFT)], gX, gY, SQUAREWIDTH, SQUAREHEIGHT, null);
                                }


                            }

                            g.setColor(defC[3]);
                            g.drawRect(gX, gY, SQUAREWIDTH, SQUAREHEIGHT);

                            //DEBUG STRINGS
                            g.setColor(Color.WHITE);
                            //g.drawString(Boolean.toString(power[((squareY + VIEWPORTTOP) * MAPWIDTH + VIEWPORTLEFT + squareX)]), squareX * SQUAREWIDTH, (squareY + 1) * SQUAREHEIGHT);
                            //g.drawString(Integer.toString(traffic[((squareY + VIEWPORTTOP) * MAPWIDTH + VIEWPORTLEFT + squareX)]), squareX * SQUAREWIDTH, (squareY + 1) * SQUAREHEIGHT);
                            //g.drawString(Float.toString(landValue[((squareY + VIEWPORTTOP) * MAPWIDTH + VIEWPORTLEFT + squareX)]), squareX * SQUAREWIDTH, (squareY + 1) * SQUAREHEIGHT);
                            //g.drawString(Integer.toString(edgeArray[((squareY + VIEWPORTTOP) * MAPWIDTH + VIEWPORTLEFT + squareX)]), squareX * SQUAREWIDTH, (squareY + 1) * SQUAREHEIGHT);
                            //g.drawString(Integer.toString((squareY + VIEWPORTTOP) * MAPWIDTH + VIEWPORTLEFT + squareX), squareX * SQUAREWIDTH, (squareY + 1) * SQUAREHEIGHT);
                            //g.drawString(Integer.toString(graphicArray[((squareY + VIEWPORTTOP) * MAPWIDTH + VIEWPORTLEFT + squareX)]), squareX * SQUAREWIDTH, (squareY + 1) * SQUAREHEIGHT);
                            //g.drawString(Integer.toString(jobs[((squareY + VIEWPORTTOP) * MAPWIDTH + VIEWPORTLEFT + squareX)]), squareX * SQUAREWIDTH, (squareY + 1) * SQUAREHEIGHT);

                        }
                    }
                }
                //Draw 
                for (short squareX = 7; squareX < SQUAREPERWINDOW; squareX++) {
                    for (short squareY = 0; squareY < SQUAREPERWINDOW; squareY++) {
                        int squareIndex = (squareY + VIEWPORTTOP) * MAPWIDTH + VIEWPORTLEFT + squareX;
                        if (squareIndex == mouseCell && controlSelected >= 0 && zoneOK) {
                            g.setColor(defC[CONTROLCOLOR[controlSelected]]);
                            int indSpread = CONTROLZONES[controlSelected] / 2;
                            g.fill3DRect((squareX - indSpread) * SQUAREWIDTH, (squareY - indSpread) * SQUAREHEIGHT, (CONTROLZONES[controlSelected]) * SQUAREWIDTH, CONTROLZONES[controlSelected] * SQUAREHEIGHT, zoneOK);


                        }
                    }
                }


                //Draw Controls
                g.setColor(Color.DARK_GRAY);

                g.fillRect(0, 0, SQUAREWIDTH * 7, BOTTOM);
                for (short control = 0; control < CONTROLLENGTH; control++) {
                    g.setColor(defC[CONTROLCOLOR[control]]);
                    g.fill3DRect(CONTROLOFFSETS[control * 2], CONTROLOFFSETS[control * 2 + 1], CONTROLSIZE, CONTROLSIZE, (controlSelected == control) ? false : true);
                    g.setColor(Color.BLACK);
                    g.drawString(Character.toString(CONTROLLABELS[control]), CONTROLOFFSETS[control * 2] + CONTROLSIZE / 2, CONTROLOFFSETS[control * 2 + 1] + CONTROLSIZE / 2);
                    g.setColor(Color.PINK);

                }
                g.setColor(Color.BLACK);

                g.drawString("R C I", 10, 180);
                g.setColor(defC[CONTROLCOLOR[RESCONTROL]]);
                g.fillRect(10, 170 - demand[0], 10, (int) (demand[0]));
                g.setColor(defC[CONTROLCOLOR[COMCONTROL]]);

                g.fillRect(30, 170 - demand[1], 10, (int) demand[1]);
                g.setColor(defC[CONTROLCOLOR[INDCONTROL]]);

                g.fillRect(50, 170 - demand[2], 10, (int) demand[2]);
                g.setColor(Color.WHITE);
                g.drawString("$:" + money, 20, 260);
                g.drawString("Pop:" + population, 20, 280);
                g.drawString("Rs:" + numRoads, 20, 300);
                g.drawString("Net$:" + net, 20, 320);
                g.fillRect(20, 360, (30) - (ticks % 3000) / 100, 10);
                //g.drawImage(ss, 0, 0,320,240, null);
                for (j = 0; j < MAPHEIGHT; j++) {
                    for (i = 0; i < MAPWIDTH; i++) {
                        neighbor = i + j * MAPWIDTH;
                        int color = COLORS[10];
                        btype = buildType[neighbor];
                        if (btype == zonedCommerical || btype == builtCommerical) {
                            color = COLORS[2];
                        }
                        if (btype == zonedResidental || btype == builtResidental) {
                            color = COLORS[0];
                        }
                        if (btype == zonedIndustrial || btype == builtIndustrial) {
                            color = COLORS[9];
                        }
                        if (btype == waterTile) {
                            color = COLORS[17];
                        }
                        if (btype == road || btype == roadWithPowerLINE || btype == powerPlant) {
                            color = COLORS[5];
                        }
                        image.setRGB(90 + i, 250 + j, color);
                    }
                }
                g.drawRect(97 + VIEWPORTLEFT, 250 + VIEWPORTTOP, 12, 20);


                // And draw
                if (g2 == null) {
                    g2 = (Graphics2D) getGraphics();
                    requestFocus();
                } else {
                    g2.drawImage(image, 0, 0, EDGE, BOTTOM, null);
                }

            }

            nextFrameStartTime = System.nanoTime() + 1666666666;
        }
    }
    final short SCROLLWIDTH = 30;
    final short SCROLLHEIGHT = 30;

    //@Override
    public boolean handleEvent(Event mEvent) {
        //Check for Scroll
        switch (mEvent.id) {

            case Event.MOUSE_MOVE:
                mx = mEvent.x;
                my = mEvent.y;
                break;
            case Event.MOUSE_DOWN:
                leftClick = true;
                rightClick = (mEvent.modifiers == Event.META_MASK);
                break;



        }
        return true;

    }
}
    