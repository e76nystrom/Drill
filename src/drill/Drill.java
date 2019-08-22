/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drill;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import Dxf.Dxf;
import Nc.Nc;

//import org.jgap.*;
//import org.jgap.impl.*;
//import org.jgap.impl.salesman.*;

/*
 * Drill.java
 *
 * Created on Dec 8, 2008 at 4:53:25 PM
 *
 * @author Eric Nystrom
 *
 */

public class Drill
{
 ArrayList<Hole> list;
// ArrayList<Hole1> list1 = null;
 ArrayList<Slot> slotList = null;
 Double[] toolSize;
 Drill.ApertureList aperture;
 String inputFile;
 double xSize;
 double ySize;
 double curX = 0.0;
 double curY = 0.0;
 int xMax = 0;
 int yMax = 0;
 Dxf d;
 Nc nc0;
 Nc nc1;
 Image image;
 double scale = 1000.0;
 double gScale = 10000.0;
 double inpScale = 1000000;
 boolean output = true;
 boolean flipX = false;
 boolean mirrorX = false;
 boolean mirrorY = false;
 double mirrorXSize = 0.0;
 double mirrorYSize = 0.0;
 boolean dxf = false;
 boolean probeFlag = false;
 boolean probeHoles = false;
 double probeRetract = 0.020;
 double probeDepth = -0.010;
 String side;
 int probeX;
 int probeY;
 boolean metric;

 double depth;
 double retract;
 double safeZ;
 double feed;
 double change;

 public static final int BACKGROUND = new Color(0xe0, 0xff, 0xff).getRGB();
 public static final int GRID       = new Color(0xe0, 0x00, 0xff).getRGB();

 public Drill(String inputFile, boolean flipX, boolean output, boolean dxf,
	      boolean metric)
 {
  this.inputFile = inputFile;
  this.flipX = flipX;
  this.output = output;
  this.dxf = dxf;
  this.metric = metric;
 }

 public void setParameters(double depth, double retract, double safeZ,
			   double feed, double change)
 {
  this.depth = depth;
  this.retract = retract;
  this.safeZ = safeZ;
  this.feed = feed;
  this.change = change;
 }

 public void setProbe(boolean probeFlag, boolean probeHoles,
		      int probeX, int probeY, String side,
		      double probeDepth, double probeRetract)
 {
  this.probeFlag = probeFlag;
  this.probeHoles = probeHoles;
  this.probeX = probeX;
  this.probeY = probeY;
  this.side = side;
  this.probeDepth = probeDepth;
  this.probeRetract = probeRetract;
 }

 public void setMirror(boolean mirrorX, double mirrorXSize,
		       boolean mirrorY, double mirrorYSize)
 {
  this.mirrorX = mirrorX;
  this.mirrorXSize = mirrorXSize;
  this.mirrorY = mirrorY;
  this.mirrorYSize = mirrorYSize;
 }

 public void process()
 {
  Pattern p = Pattern.compile("^T(\\d+)C?([\\d\\.]*)");
  Pattern p1 = Pattern.compile("^X([-\\+\\d\\.]+)Y([-\\+\\d\\.]+)");
  Matcher m;
  toolSize = new Double[20];
  list = new ArrayList<>();
  slotList = new ArrayList<>();
//  if (probeHoles)
//  {
//   list1 = new ArrayList<>();
//  }
  aperture = new Drill.ApertureList();

  if (!inputFile.contains("."))
  {
   inputFile += ".drl";
  }

  File fIn = new File(inputFile);

  if (fIn.isFile())
  {
   String project = inputFile.split("\\.")[0];
   if (dxf)
   {
    d = new Dxf();
    d.init(project + ".dxf");
   }
   String boardFile = project + ".gbr";
   boardSize(boardFile);
   if (mirrorXSize != 0)
   {
    xMax = (int) (mirrorXSize * gScale);
   }
   if (mirrorYSize != 0.0)
   {
    yMax = (int) (mirrorYSize * gScale);
   }
   SimpleDateFormat sdf = new SimpleDateFormat("EEE LLL dd HH:mm:ss yyyy");
   String date = sdf.format(new Date());
   if (output)
   {
    nc0 = new Nc(project + ".ngc");
    nc1 = new Nc(project + "1.ngc");
    nc0.out.printf("(%s %s)\n", fIn.getAbsolutePath(), date);
    nc1.out.printf("(%s %s)\n", fIn.getAbsolutePath(), date);
    nc0.header();
    nc1.header();

    nc0.out.printf("g54	(coordinate system)\n");
    nc0.out.printf("#1 = 0      (offset)\n");
    nc0.out.printf("#2 = 1      (mirror)\n");
    nc0.out.printf("#3 = %6.3f (depth)\n", depth);
    nc0.out.printf("#4 = %5.3f  (retract)\n", retract);
    nc0.setFeedRate(feed);
    nc0.out.printf("s25000\n");
    nc0.moveRapidZ(safeZ);
    nc0.moveRapid(0.25, 0.25);

    nc1.out.printf("g54	(coordinate system)\n");
    if (flipX)
    {
     double size = mirrorXSize != 0.0 ? mirrorXSize : xSize;
     nc1.out.printf("#1 = [%5.3f - 0.000] (offset)\n", size);
    }
    else
    {
     double size = mirrorYSize != 0.0 ? mirrorYSize : ySize;
     nc1.out.printf("#1 = [%5.3f - 0.000] (offset)\n", size);
    }
    nc1.out.printf("#2 = -1    (mirror)\n");
    nc1.out.printf("#3 = 0.100 (retract)\n");
    nc1.setFeedRate(15.0);
    nc1.moveRapidZ(safeZ);
   }

   image = new Image(xMax, yMax);
   String topName = project + "_t.gbr";
   pads(topName);

   int currentTool = -1;
   @SuppressWarnings("UnusedAssignment")
   int lastTool = -1;

   String line;
   try (BufferedReader in = new BufferedReader(new FileReader(fIn)))
   {
    while ((line = in.readLine()) != null)
    {
     line = line.trim();
//     System.out.printf("%s\n", line);
     m = p1.matcher(line);
     if (m.find())
//     if (line.indexOf("X") == 0) // T01C0.035
     {
//      String x = line.substring(1, 7);
//      String y = line.substring(9, 15);
      String x = m.group(1);
      String y = m.group(2);
       
      double xVal = Double.valueOf(x);
      double yVal = Double.valueOf(y);
      if (xVal > 10.0)
      {
       xVal /= scale;
      }
      if (yVal > 10.0)
      {
       yVal /= scale;
      }
//      System.out.printf("x %7.4f y %7.4f\n", xVal, yVal);

      Hole h = new Hole(xVal, yVal);
      list.add(h);

      if (line.length() > 16)
      {
       String x1 = line.substring(20, 26);
       String y1 = line.substring(28, 34);

       double x1Val = Double.valueOf(x1) / scale;
       double y1Val = Double.valueOf(y1) / scale;

       Slot s = new Slot(xVal, yVal, x1Val, y1Val,
			 (double) toolSize[currentTool]);
       slotList.add(s);

       if (mirrorX)
       {
	xVal = mirrorXSize - xVal;
	x1Val = mirrorXSize - x1Val;
       }
       if (mirrorY)
       {
	yVal = mirrorYSize - yVal;
	y1Val = mirrorYSize - y1Val;
       }
       image.g.setColor(Color.BLACK);
       double w = toolSize[currentTool];
       BasicStroke stroke = new BasicStroke((float) (w * scale),
					    BasicStroke.CAP_ROUND,
					    BasicStroke.JOIN_MITER);
       image.g.setStroke(stroke);
       Line2D s0;
       w = 0.0;
       if (xVal == x1Val)
       {
	s0 = new Line2D.Double((xVal) * scale, (yVal + w) * scale,
			       (xVal) * scale, (y1Val - w) * scale);
       }
       else
       {
	s0 = new Line2D.Double((xVal - w) * scale, (yVal) * scale,
			       (x1Val + w) * scale, (yVal) * scale);
       }
       image.g.draw(s0);
      }
//       if (probeHoles)
//       {
//	list1.add(new Hole1(xVal, yVal, (double) toolSize[currentTool]));
//       }
     }
     else
     {
      m = p.matcher(line);
//      if (line.indexOf("T") == 0)
      if (m.find())
      {
       int t = Integer.valueOf(m.group(1));
//       if (line.indexOf("C") == 3) // T01C0.035
       String size = m.group(2);
       if (size.length() != 0)
       {
	/* int t = Integer.valueOf(line.substring(1, 3)); */
	if (toolSize[t] == null)
	{
//	 toolSize[t] = Double.valueOf(line.substring(1, 3);
	 toolSize[t] = Double.valueOf(size);
	}
       }
       else			// T01
       {
	lastTool = currentTool;
//	currentTool = Integer.valueOf(line.substring(1, 3));
	currentTool = t;
	if (lastTool > 0)
	{
	 if (currentTool != lastTool)
	 {
//	 if (true)
	  process0(list, lastTool);
	  if (!slotList.isEmpty())
	  {
	   processSlots(slotList, lastTool);
	   slotList.clear();
	  }
//	 else
//	 {
//	  list.add(0, new Hole(xLoc, yLoc));
//	  process1(list, lastTool);
//	 }
	 }
	}
       }
      }
     }
    }
   }
   catch (IOException e)
   {
   }

   if (probeFlag)
   {
//    if (probeHoles)
//    {
//     if (mirrorX || mirrorY)
//     {
//      for (Hole1 h : list1)
//      {
//       if (mirrorX)
//       {
//	h.x = mirrorXSize - h.x;
//       }
//       if (mirrorY)
//       {
//	h.y = mirrorYSize - h.y;
//       }
//      }
//     }
//     Collections.sort(list1);
//   }

    int xPoints = probeX;
    int yPoints = probeY;
    double margin = 0.125;
    double xStep = (this.xSize - 2 * margin) / (xPoints - 1);
    double yStep = (this.ySize - 2 * margin) / (yPoints - 1);
    double x = margin;
    double y = margin;
    double y1 = this.ySize - margin;

    String probeFile = project + "_" + side + "p.ngc";
    File pOut = new File(probeFile);
    try
    {
     PrintWriter prb;
     
     prb = new PrintWriter(new BufferedWriter(new FileWriter(pOut)));

     prb.printf("(%s %s)\n", probeFile, date);
     prb.printf("(xSize %6.4f ySize %6.4f retract %6.4f depth %6.4f)\n",
		this.xSize, this.ySize, probeRetract, probeDepth);
     prb.printf("(xPoints %d yPoints %d margin %6.4f " +
 		"xStep %6.4f yStep %6.4f)\n",
		xPoints, yPoints, margin, xStep, yStep);
     prb.printf("g54	(coordinate system 1)\n");
     prb.printf("g20	(units inches)\n");
     prb.printf("g61	(exact path)\n");
     prb.printf("f1.0	(probe feed rate)\n\n");
     prb.printf("g0 z%6.4f	(safe z)\n", safeZ);

     prb.printf("g0 x%6.4f y%6.4f (start position)\n\n", x, y);
     prb.printf("m0	(pause to check position)\n");
     prb.printf("g0 z%6.4f\n", probeRetract);
     prb.printf("g38.2 z%6.4f\n", probeDepth);
     prb.printf("g10 L20 P0 z0 (zero z)\n");
     prb.printf("g0 z%6.4f\n\n", probeRetract);
		
     prb.printf("(PROBEOPEN %s_%s.prb)\n", project, side);

     if (probeHoles)
     {
      image.getData();
     }

     boolean fwd = true;
     int j0;
     for (int i = 0; i < xPoints; i++)
     {
      if (fwd)
      {
       y = margin;
       j0 = 0;
      }
      else
      {
       y = y1;
       j0 = yPoints - 1;
      }
      for (int j = 0; j < yPoints; j++)
      {
       if (!probeHoles)
       {
	prb.printf("g0 x%6.4f y%6.4f (%d %d x%6.4f y%6.4f)\n",
		   x, y, i, j0, x, y);
	image.probe(x, y, 0.010);
       }
       else
       {
	int xInt = (int) (x * scale);
	int yInt = (int) (y * scale);
        boolean found = image.check(xInt, yInt);
//	double yOffset;
//	boolean found = false;
//	for (Hole1 hole : list1)
//	{
//	 double dx = x - hole.x;
//	 double dy = y - hole.y;
//	 double dist = Math.hypot(dx, dy);
//	 if (dist < (hole.size + 0.025))
//	 {
//	  double r = hole.size + 0.025;
//	  yOffset = Math.sqrt(r * r - dx * dx);
//	  if (dy < 0)
//	  {
//	   yOffset = -yOffset;
//	  }
//	  yOffset += hole.y;
////          System.out.printf("%d %d dist %6.4f %6.4f dx %6.3f dy %6.3f " +
////			      "x %6.4f y %6.4f offset %6.3f\n",
////			      i, j0, dist, hole.size + 0.025, dx, dy,
////			      x, y + yOffset, yOffset);
//	  image.circle(Color.GREEN, x, y, 0.010);
//	  found = true;
//	  prb.printf("g0 x%6.4f y%6.4f (%d %d x%6.4f y%6.4f)\n",
//		     x, yOffset, i, j0, x, y);
//	  image.probe(x, yOffset, 0.010);
//	  break;
//	 }
//	}
	if (found)
	{
	 int yIntOffset = image.scanY(xInt, yInt, 100);
	 double yOffset = ((double) yIntOffset) / scale;
	 image.circle(Color.GREEN, x, y, 0.010);
	 prb.printf("g0 x%6.4f y%6.4f (%d %d x%6.4f y%6.4f)\n",
		    x, yOffset, i, j0, x, y);
	 image.probe(x, yOffset, 0.010);
	}
	else
	{
	 prb.printf("g0 x%6.4f y%6.4f (%d %d x%6.4f y%6.4f)\n",
		    x, y, i, j0, x, y);
	 image.probe(x, y, 0.010);
	}
       }
       prb.printf("g38.2 z%6.4f\n", probeDepth);
       prb.printf("g0 z%6.4f\n\n", probeRetract);
       if (fwd)
       {
	y += yStep;
	j0 += 1;
       }
       else
       {
	y -= yStep;
	j0 -= 1;
       }
      }
      fwd = !fwd;
      x += xStep;
     }
     prb.printf("(PROBECLOSE %s.prb)\n", project);
     prb.printf("g0 z%5.3f\n", safeZ);
     prb.printf("g0 x%5.3f y%5.3f\n", 0.0, 0.0);
     prb.printf("m2	(end of program)\n");
     prb.close();
    }
    catch (IOException e)
    {
    }
  }
   
   image.getData();
   image.grid();
   image.write(image.data, project);

   if (output)
   {
    nc0.out.printf("m5\n");
    nc0.moveRapidZ(safeZ);
    nc0.moveRapid(0.0, 0.0);
    nc0.close();

    nc1.moveRapidZ(safeZ);
    nc1.moveRapid(0.0, 0.0);
    nc1.close();
   }
   if (dxf)
   {
    d.end();
   }
  }
 }

 private void boardSize(String fileName)
 {
  Pattern p = Pattern.compile("[Xx]?([0-9]*)[Yy]?([0-9]*)");

  File f = new File(fileName);
  if (f.isFile())
  {
   try (BufferedReader in = new BufferedReader(new FileReader(f)))
   {
    String line;
    while ((line = in.readLine()) != null)
    {
     line = line.trim();
     if (line.startsWith("%"))
     {
      continue;
     }
     Matcher m = p.matcher(line);
     if (m.find())
     {
      String xStr = m.group(1);
      String yStr = m.group(2);
      if (xStr.length() != 0)
      {
       int x = Integer.parseInt(xStr);
       if (!metric)
       {
	if (x > 100000)
	{
	 x /= 100;
	}
       }
       else
       {
	x /= 2540;
       }
       if (x > xMax)
       {
	xMax = x;
       }
      }
      if (yStr.length() != 0)
      {
       int y = Integer.parseInt(yStr);
       if (!metric)
       {
	if (y > 100000)
	{
	 y /= 100;
	}
       }
       else
       {
	y /= 2540;
       }
       if (y > yMax)
       {
	yMax = y;
       }
      }
     }
    }
   }
   catch (IOException e)
   {
    System.out.println(e);
   }
   if ((xSize == 0.0)
   &&  (xMax != 0 ))
   {
    xSize = xMax / gScale;
   }
   if ((ySize == 0.0)
   &&  (yMax != 0))
   {
    ySize = yMax / gScale;
   }
   
   System.out.printf("Size x %5.3f y %5.3f\n", xSize, ySize);
  }
 }

 private void pads(String fileName)
 {
  int xLoc = 0;
  int yLoc = 0;
  Drill.ApertureList.Aperture curAp = null;
  Graphics2D g;

  Pattern pT = Pattern.compile("^%ADD([0-9]+)([A-Z]),([0-9\\.]+)X?([0-9\\.]*)");
  Pattern pG = Pattern.compile("^X?([0-9]*)Y?([0-9]*)D([0-9]+)");
  g = image.g;

  File f = new File(fileName);
  if (f.isFile())
  {
   try (BufferedReader in = new BufferedReader(new FileReader(f)))
   {
    String line;
    while ((line = in.readLine()) != null)
    {
     line = line.trim();
     Matcher m = pG.matcher(line);
     if (m.find())
     {
      String xVal = m.group(1);
      String yVal = m.group(2);
      if (xVal.length() != 0)
      {
       xLoc = Integer.valueOf(xVal);
      }
      if (yVal.length() != 0)
      {
       yLoc = Integer.valueOf(yVal);
      }
      int d0 = Integer.valueOf(m.group(3));
      if (d0 > 10)
      {
       curAp = aperture.get(d0);
      }
      else
      {
       switch (d0)
       {
       case 1:
	break;
       case 2:
	break;
       case 3:
	if (curAp != null)
	{
	 double x0 = xLoc / inpScale;
	 double y0 = yLoc / inpScale;
	 if (mirrorX)
	 {
	  x0 = mirrorXSize - x0;
	 }
	 if (mirrorY)
	 {
	  y0 = mirrorYSize - y0;
	 }
	 curAp.draw(g, x0, y0);
	}
	break;
       }
      }
     }
     else
     {
      m = pT.matcher(line);
      if (m.find())
      {
       int ap = Integer.valueOf(m.group(1));
       String type = m.group(2);
       switch (type)
       {
       case "C":
	double d0 = Double.valueOf(m.group(3));
	aperture.add(ap, d0);
	break;
       case "R":
	double w = Double.valueOf(m.group(3));
	double h = Double.valueOf(m.group(4));
	aperture.add(ap, w, h);
	break;
       case "O":
	w = Double.valueOf(m.group(3));
	h = Double.valueOf(m.group(4));
	aperture.add(ap, w, h, ApertureList.Aperture.OVAL);
	break;
       default:
	System.out.printf("invalid aperture %s %d\n", type, ap);
	w = Double.valueOf(m.group(3));
	h = Double.valueOf(m.group(4));
	aperture.add(ap, w, h);
	break;
       }
      }
     }
    }
   }
   catch (IOException e)
   {
    System.out.println(e);
   }
  }
 }

 @SuppressWarnings("null")
 private void process0(ArrayList<Hole> list, int t)
 {
  if (output)
  {
   nc0.out.printf("m5\n");
   nc0.moveRapidZ(change, String.format("Tool %d %5.3fin %4.2fmm",
					t, toolSize[t], toolSize[t] * 25.4));
   nc0.pause();
   nc0.out.printf("m3\n");
   nc0.out.printf("g4 p 1.0\n");
   nc0.moveRapidZ(safeZ);
  }

  if (mirrorX || mirrorY)
  {
   for (Hole h : list)
   {
    if (mirrorX)
    {
     h.x = mirrorXSize - h.x;
    }
    if (mirrorY)
    {
     h.y = mirrorYSize - h.y;
    }
   }
  }
  
  if (output &&
      (t == 1))
  {
   Hole hmin = null;
   Hole hmax = null;
   double min = 999.0;
   double max = 0;
   if (flipX)
   {
    for (Hole h : list)
    {
     if (h != null)
     {
      if (h.y < min)
      {
       hmin = h;
       min = h.y;
      }
      else if (h.y == min)
      {
       if (h.x > hmin.x)
       {
	hmin = h;
       }
      }

      if (h.y > max)
      {
       hmax = h;
       max = h.y;
      }
      else if (h.y == max)
      {
       if (h.x > hmax.x)
       {
	hmax = h;
       }
      }
     }
    }

    nc1.out.printf("g0 x[#1 + #2 * %3.3f] y%3.3f\n", hmin.x, hmin.y);
    nc1.moveRapidZ("[#3]");
    nc1.out.printf("m0\n");
    nc1.out.printf("g0 x[#1 + #2 * %3.3f] y%3.3f\n", hmax.x, hmax.y);
    nc1.out.printf("m0\n");
   }
   else
   {
    for (Hole h : list)
    {
     if (h != null)
     {
      if (h.x < min)
      {
       hmin = h;
       min = h.x;
      }
      else if (h.x == min)
      {
       if (h.y > hmin.y)
       {
	hmin = h;
       }
      }

      if (h.x > max)
      {
       hmax = h;
       max = h.x;
      }
      else if (h.x == max)
      {
       if (h.y > hmax.y)
       {
	hmax = h;
       }
      }
     }
    }

    nc1.out.printf("g0 x%3.3f y[#1 + #2 * %3.3f]\n", hmin.x, hmin.y);
    nc1.out.printf("m0\n");
    nc1.out.printf("g0 x%3.3f y[#1 + #2 * %3.3f]\n", hmax.x, hmax.y);
    nc1.out.printf("m0\n");
   }
  }

  int holeCount = list.size();
  double total = 0.0;
  int k = 0;
  while (list.size() > 0)
  {
   double min = 999.0;
   int j = -1;
   for (int i = 0; i < list.size(); i++)
   {
    double dist = list.get(i).dist(curX, curY);
    if (dist < min)
    {
     j = i;
     min = dist;
    }
   }
   total += min;
   Hole h = list.remove(j);
   image.circle(h.x, h.y, toolSize[t]  / 2);
   if (dxf)
   {
    d.line(curX, curY, h.x, h.y);
   }
//   image.line(curX, curY, h.x, h.y);
   if (dxf)
   {
    d.text((h.x + curX) / 2.0 + .005, (h.y + curY) / 2.0 + 0.005,
	   0.010, Integer.toString(k));
   }
   curX = h.x;
   curY = h.y;

   if (output)
   {
    if (flipX)
    {
     nc0.out.printf("g0 x[#1 + #2 * %3.3f] y%3.3f\n", curX, curY);
    }
    else
    {
     nc0.out.printf("g0 x%3.3f y[#1 + #2 * %3.3f]\n", curX, curY);
    }
    nc0.moveLinearZ("[#3]");
    nc0.moveRapidZ("[#4]");
   }

   if (dxf)
   {
    d.circle(curX, curY, toolSize[t] / 2.0);
   }
//     System.out.printf("%3d %3.3f %3.3f\n", j, xLoc, yLoc);
   k++;
  }
  System.out.printf("Tool %d %5.3f Holes %3d Path %4.1f inches\n",
		    t, toolSize[t], holeCount, total);
  list.clear();
 }

 @SuppressWarnings("null")
 private void processSlots(ArrayList<Slot> list, int t)
 {
  if (output)
  {
   nc0.out.printf("m5\n");
   nc0.moveRapidZ(change, String.format("Tool %d %5.3fin %4.2fmm endmill",
					t, toolSize[t], toolSize[t] * 25.4));
   nc0.pause();
   nc0.out.printf("m3\n");
   nc0.out.printf("g4 p 1.0\n");
   nc0.moveRapidZ(safeZ);
  }

  if (mirrorX || mirrorY)
  {
   for (Slot s : list)
   {
    if (mirrorX)
    {
     s.x0 = mirrorXSize - s.x0;
     s.x1 = mirrorXSize - s.x1;
    }
    if (mirrorY)
    {
     s.y0 = mirrorYSize - s.y0;
     s.y1 = mirrorYSize - s.y1;
    }
   }
  }

  for (Slot s : list)
  {
   curX = s.x0;
   curY = s.y0;
    
   if (output)
   {
    if (flipX)
    {
     nc0.out.printf("g0 x[#1 + #2 * %3.3f] y%3.3f\n", curX, curY);
    }
    else
    {
     nc0.out.printf("g0 x%3.3f y[#1 + #2 * %3.3f]\n", curX, curY);
    }
    nc0.moveLinearZ("[#3]");
    curX = s.x1;
    curY = s.y1;
    if (flipX)
    {
     nc0.out.printf("g1 x[#1 + #2 * %3.3f] y%3.3f\n", curX, curY);
    }
    else
    {
     nc0.out.printf("g1 x%3.3f y[#1 + #2 * %3.3f]\n", curX, curY);
    }
    nc0.moveRapidZ("[#4]");
   }
  }
 }

   
// void process1(ArrayList<Hole> list, int t)
// {
//  nc0.moveRapidZ(1.5,String.format("Tool %d %5.3fin %4.2fmm",
//				   t,toolSize[t],
//				   toolSize[t] * 25.4));
//  nc0.pause();
//
//  try
//  {
//   Configuration.reset();
//   DrillPath path = new DrillPath();
//   IChromosome optimal = path.findOptimalPath(null);
//   Gene[] g = optimal.getGenes();
//   System.out.printf("len %3d\n",g.length);
//   for (int i = 0; i < g.length; i++)
//   {
//    IntegerGene gi = (IntegerGene) g[i];
//    int k = gi.intValue();
//    Hole h = list.get(k);
//    System.out.printf("%2d %2d x %6.3f y %6.3f\n",i,k,h.x,h.y);
//
//    if (k != 0)
//    {
//     d.line(xLoc,yLoc,h.x,h.y);
//     d.text((h.x + xLoc) / 2.0 + .005,(h.y + yLoc) / 2.0 + 0.005,
//	    0.010,Integer.toString(k));
//     xLoc = h.x;
//     yLoc = h.y;
//
//     nc0.out.printf("g0 x%3.3f y[#1 + #2 * %3.3f]\n",xLoc,yLoc);
//     nc0.moveLinearZ("[#3]");
//     nc0.moveLinearZ("[#4]");
//
//     d.circle(xLoc,yLoc,toolSize[t] / 2.0);
//    }
//   }
//  }
//  catch (Exception ex)
//  {
//   System.out.println(ex);
//  }
//  list.clear();
// }

 public class Hole
 {
  public double x;
  public double y;

  public Hole(double xLoc, double yLoc)
  {
   x = xLoc;
   y = yLoc;
  }

  public double dist(double xLoc, double yLoc)
  {
   double dx = xLoc - x;
   double dy = yLoc - y;
   return(Math.sqrt(dx * dx + dy * dy));
  }
 }

 public class Hole1 implements Comparable<Drill.Hole1>
 {
  public double x;
  public double y;
  public double size;

  public Hole1(double xLoc, double yLoc, double size)
  {
   x = xLoc;
   y = yLoc;
   this.size = size / 2.0;
  }

  @Override public int compareTo(Hole1 h)
  {
   int compare;

   compare = (int) Math.signum(x - h.x);
   if (compare == 0)
   {
    compare = (int) Math.signum(y - h.y);
   }
   return(compare);
  }
 }

 public class Slot
 {
  public double x0;
  public double y0;
  public double x1;
  public double y1;
  public double size;

  public Slot(double x0, double y0, double x1, double y1, double size)
  {
   this.x0 = x0;
   this.y0 = y0;
   this.x1 = x1;
   this.y1 = y1;
   this.size = size;
  }
 }

 public class Image
 {
  BufferedImage image;
  Graphics2D g;
  int[] data;
  int w0;
  int h0;
  int black = Color.BLACK.getRGB();

  public Image(int w, int h)
  {
   int nScale = (int) (gScale / scale);
   w0 = w / nScale;
   h0 = h / nScale;
   image = new BufferedImage(w0, h0, BufferedImage.TYPE_INT_RGB);
   g = image.createGraphics();
   g.setColor(new Color(BACKGROUND));
   g.fillRect(0, 0, w0, h0);
   g.setColor(Color.BLACK);
   data = new int[w0 * h0];
  }

  public void test()
  {
   try
   {
    PrintWriter tst;
   
    tst = new PrintWriter(new BufferedWriter(new FileWriter("test.txt")));
    int size = w0 * h0;
    System.out.printf("size %d\n", size);
    for (int i = 0; i < size; i++)
    {
     if (data[i] == black)
     {
      int x = i % w0;
      int y = i / w0;
      tst.printf("%8d (%6d %6d)\n", i, x, y);
     }
    }
   }
   catch (IOException e)
   {
   }
  }

  public boolean check(int x, int y)
  {
   int i = x + y * w0;
//   System.out.printf("(%6d, %6d) %10d %8x\n", x, y, i, data[i]);
   return(data[i] == black);
  }

  public int scanY(int x, int y, int dist)
  {
   int dir = dist > 0 ? 1 : -1;
   int y0 = y;
   for (int j = 0; j <= dist; j++)
   {
    int i = x + y0 * w0;
    if (data[i] != black)
    {
     return(y0 + 10 * dir);
    }
    y0 += dir;
   }
   return(y0);
  }

  public void getData()
  {
   image.getRGB(0, 0, w0, h0, data, 0, w0);
  }

  public void setData()
  {
   image.setRGB(0, 0, w0, h0, data, 0, w0);
  }

  public void line(double x0, double y0, double x1, double y1)
  {
   g.setColor(Color.BLUE);
   BasicStroke stroke = new BasicStroke(1, BasicStroke.CAP_ROUND,
				       BasicStroke.JOIN_MITER);
   g.setStroke(stroke);

   Line2D shape = new Line2D.Double((x0 * scale),
				    (y0 * scale),
				    (x1 * scale),
				    (y1 * scale));
   g.draw(shape);
  }

  public void circle(double x, double y, double r)
  {
   g.setColor(Color.BLACK);
   BasicStroke stroke = new BasicStroke(1, BasicStroke.CAP_ROUND,
				       BasicStroke.JOIN_MITER);
   g.setStroke(stroke);

   double offset = r * scale;
   double d = 2 * offset;
   Ellipse2D shape = new Ellipse2D.Double((x * scale - offset),
					  (y * scale - offset),
					  d, d);
   g.fill(shape);
  }

  public void circle(Color color, double x, double y, double r)
  {
   g.setColor(color);
   BasicStroke stroke = new BasicStroke(1, BasicStroke.CAP_ROUND,
				       BasicStroke.JOIN_MITER);
   g.setStroke(stroke);

   double offset = r * scale;
   double d = 2 * offset;
   Ellipse2D shape = new Ellipse2D.Double((x * scale - offset),
					  (y * scale - offset),
					  d, d);
   g.fill(shape);
  }

  public void probe(double x, double y, double r)
  {
   g.setColor(Color.RED);
   BasicStroke stroke = new BasicStroke(1, BasicStroke.CAP_ROUND,
				       BasicStroke.JOIN_MITER);
   g.setStroke(stroke);

   double offset = r * scale;
   double d = 2 * offset;
   Ellipse2D shape = new Ellipse2D.Double((x * scale - offset),
					  (y * scale - offset),
					  d, d);
   g.fill(shape);
  }
  
  public void write(int[] data, String f)
  {
   int j = h0 * w0;
   for (int i = 0; i < h0; i++)
   {
    j -= w0;
    image.setRGB(0, i, w0, 1, data, j, w0);
   }
   write(f);
  }

  public void write(String f)
  {
   String tmp = f + (side.equals("b") ? side : "") + ".png";
   File file = new File(tmp);

   try
   {
    ImageIO.write(image, "png", file);
   }
   catch (IOException e)
   {
    System.out.println(e);
   }
  }

  public void grid()
  {
   int y0 = 100;
   int y1 = h0 - 100;
   for (int x = 100; x < w0 - 100; x++)
   {
    int i0 = x + y0 * w0;
    if (data[i0] == BACKGROUND)
    {
     data[i0] = GRID;
    }
    i0 = x + y1 * w0;
    if (data[i0] == BACKGROUND)
    {
     data[i0] = GRID;
    }
   }

   int x0 = 100;
   int x1 = w0 - 100;
   for (int y = 100; y < h0 - 100; y++)
   {
    int i0 = x0 + y * w0;
    if (data[i0] == BACKGROUND)
    {
     data[i0] = GRID;
    }
    i0 = x1 + y * w0;
    if (data[i0] == BACKGROUND)
    {
     data[i0] = GRID;
    }
   }

   for (int x = 0; x < w0; x += 500)
   {
    for (int y = 0; y < h0; y++)
    {
     int i0 = x + y * w0;
     if (data[i0] == BACKGROUND)
     {
      data[i0] = GRID;
     }
    }
   }

   for (int y = 0; y < h0; y += 500)
   {
    int i0 = y * w0;
    for (int x = 0; x < w0; x++)
    {
     if (data[i0] == BACKGROUND)
     {
      data[i0] = GRID;
     }
     i0++;
    }
   }
  }
 }

 public class ApertureList
 {
  public Aperture[] aperture;
  public static final int MAXAPERTURE = 60;

  public ApertureList()
  {
   aperture = new Aperture[MAXAPERTURE];
  }

  public void add(Aperture a)
  {
   aperture[a.index] = a;
  }

  public void add(int i, double v1)
  {
   if (i < MAXAPERTURE)
   {
    aperture[i] = new Aperture(i, v1);
   }
  }

  public void add(int i, double v1, double v2)
  {
   if (i < MAXAPERTURE)
   {
    aperture[i] = new Aperture(i, v1, v2);
   }
  }

  public void add(int i, double v1, double v2, int type)
  {
   if (i < MAXAPERTURE)
   {
    aperture[i] = new Aperture(i, v1, v2, type);
   }
  }

  public Aperture get(int i)
  {
   if (i < MAXAPERTURE)
   {
    return(aperture[i]);
   }
   return(null);
  }

  public class Aperture
  {
   int type;			/* aperture type */
   int index;			/* aperturn number */
   double val1;			/* size for round or width for rectangular */
   double val2;			/* height for rectangular */

   public static final int ROUND = 1;
   public static final int SQUARE = 2;
   public static final int OVAL = 3;

   public Aperture(int i, double v1)
   {
    type = ROUND;
    index = i;
    val1 = v1;
    val2 = v1;
   }

   public Aperture(int i, double v1, double v2)
   {
    type = SQUARE;
    index = 1;

    val1 = v1;
    val2 = v2;
   }

   public Aperture(int i, double v1, double v2, int type)
   {
    this.type = type;
    index = 1;

    val1 = v1;
    val2 = v2;
   }

   public void draw(Graphics2D g, double x0, double y0)
   {
    g.setColor(Color.GRAY);
    BasicStroke stroke = new BasicStroke(1, BasicStroke.CAP_ROUND,
					 BasicStroke.JOIN_MITER);
    g.setStroke(stroke);

    switch (type)
    {
    case ROUND:
    {
     double offset = val1 / 2;
     double diam = val1 * scale;
     Ellipse2D shape = new Ellipse2D.Double((x0 - offset) * scale,
					    (y0 - offset) * scale,
					    diam, diam);
     g.fill(shape);
     break;
    }
    case SQUARE:
    {
     double xOfs = val1 / 2;
     double yOfs = val2 / 2;
     Rectangle2D shape = new Rectangle2D.Double(((x0 - xOfs) * scale),
						(y0 - yOfs) * scale,
						val1 * scale , val2 * scale);
     g.fill(shape);
     break;
    }
    case OVAL:
    {
     float wx0 = (float) ((val1 < val2 ? val1 : val2) * scale);
     double w = val1 / 2;
     double h = val2 / 2;
     stroke = new BasicStroke(wx0, BasicStroke.CAP_ROUND,
			      BasicStroke.JOIN_MITER);
     g.setStroke(stroke);
     Line2D s;
     if (h > w)
     {
      s = new Line2D.Double((x0) * scale, (y0 - h + w) * scale,
			    (x0) * scale, (y0 + h - w) * scale);
     }
     else
     {
      s = new Line2D.Double((x0 - w + h) * scale, (y0) * scale,
			    (x0 + w - h) * scale, (y0) * scale);
     }
     g.draw(s);
     break;
    }
    default:
     break;
    }
   }
  }
 }

// public class DrillPath extends Salesman
// {
//  public IChromosome createSampleChromosome(Object a_initial_data)
//  {
//   try
//   {
//    int listSize = list.size();
//    Gene[] genes = new Gene[listSize];
//    for (int i = 0; i < genes.length; i++)
//    {
//     genes[i] = new IntegerGene(getConfiguration(), 0, listSize - 1);
//     genes[i].setAllele(new Integer(i));
//    }
//
//    IChromosome sample = new Chromosome(getConfiguration(), genes);
//    return(sample);
//   }
//   catch (InvalidConfigurationException iex)
//   {
//    throw new IllegalStateException(iex.getMessage());
//   }
//  }
//
//  public double distance(Gene a_from, Gene a_to)
//  {
//   IntegerGene geneA = (IntegerGene) a_from;
//   IntegerGene geneB = (IntegerGene) a_to;
//   int a = geneA.intValue();
//   int b = geneB.intValue();
//   Hole h0 = list.get(a);
//   Hole h1 = list.get(b);
//   return(Math.hypot(h0.x - h1.x,h0.y - h1.y));
//  }
// }
}
