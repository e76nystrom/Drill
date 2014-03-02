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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

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
 Double[] toolSize;
 Drill.ApertureList aperture;
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

 public static final boolean DXF = false;

 public static final int BACKGROUND = new Color(0xe0,0xff,0xff).getRGB();
 public static final int GRID       = new Color(0xe0,0x00,0xff).getRGB();

 public Drill(String inputFile)
 {
  if (inputFile.indexOf(".") < 0)
  {
   inputFile += ".drl";
  }

  System.out.printf("drill 03/02/2014\n");
  toolSize = new Double[20];
  list = new ArrayList<>();
  aperture = new Drill.ApertureList();

  File fIn = new File(inputFile);

  if (fIn.isFile())
  {
   String project = inputFile.split("\\.")[0];
   if (DXF)
   {
    d = new Dxf(project + ".dxf");
   }
   String boardFile = project + ".gbr";
   boardSize(boardFile);
   Date date = new Date();
   nc0 = new Nc(project + ".ngc");
   nc1 = new Nc(project + "1.ngc");
   nc0.out.printf("(%s %s)\n",fIn.getAbsolutePath(),date.toString());
   nc1.out.printf("(%s %s)\n",fIn.getAbsolutePath(),date.toString());
   nc0.header();
   nc1.header();

   String line;
    
   image = new Image(xMax,yMax);
   String topName = project + "_t.gbr";
   pads(topName);

   nc0.out.printf("#1 = 0      (offset)\n");
   nc0.out.printf("#2 = 1      (mirror)\n");
   nc0.out.printf("#3 = -0.095 (depth)\n");
   nc0.out.printf("#4 = 0.030  (retract)\n");
   nc0.setFeedRate(7.0);
   nc0.out.printf("s25000\n");
   nc0.moveRapidZ(0.75);
   nc0.moveRapid(0.25,0.25);

   nc1.out.printf("#1 = [%5.3f - 0.000] (offset)\n",yMax / gScale);
   nc1.out.printf("#2 = -1    (mirror)\n");
   nc1.out.printf("#3 = 0.500 (retract)\n");
   nc1.setFeedRate(15.0);
   nc1.moveRapidZ("0.75");
   nc1.moveRapid(0.25,0.25);
   nc1.moveRapidZ("[#3]");

   int currentTool = -1;
   @SuppressWarnings("UnusedAssignment")
   int lastTool = -1;

   try (BufferedReader in = new BufferedReader(new FileReader(fIn)))
   {
    while ((line = in.readLine()) != null)
    {
     if (line.indexOf("X") == 0) // T01C0.035
     {
      String x = line.substring(1,7);
      String y = line.substring(9,15);
       
      double xVal = Double.valueOf(x) / scale;
      double yVal = Double.valueOf(y) / scale;
       
      image.circle(xVal,yVal,toolSize[currentTool]  / 2);
       
      Hole h = new Hole(xVal,yVal);
      list.add(h);
     }
     else if (line.indexOf("T") == 0)
     {
      if (line.indexOf("C") == 3) // T01C0.035
      {
       int t = Integer.valueOf(line.substring(1,3));
       if (toolSize[t] == null)
       {
	toolSize[t] = Double.valueOf(line.substring(4));
       }
      }
      else			// T01
      {
       lastTool = currentTool;
       currentTool = Integer.valueOf(line.substring(1,3));
       if (lastTool > 0)
       {
	if (currentTool != lastTool)
	{
//	 if (true)
	 process0(list,lastTool);
//	 else
//	 {
//	  list.add(0,new Hole(xLoc,yLoc));
//	  process1(list,lastTool);
//	 }
	}
       }
      }
     }
    }
   }
   catch (IOException e)
   {
   }

   image.getData();
   image.grid();
   image.write(image.data,project);

   nc0.out.printf("m5\n");
   nc0.moveRapidZ(1.5);
   nc0.moveRapid(0.0,0.0);
   nc0.close();

   nc1.moveRapid(0.0,0.0);
   nc1.close();
   if (DXF)
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
       if (x > xMax)
       {
	xMax = x;
       }
      }
      if (yStr.length() != 0)
      {
       int y = Integer.parseInt(yStr);
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
   System.out.printf("Size x %5.3f y %5.3f\n",xMax / gScale,yMax / gScale);
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
	 double x0 = xLoc / gScale;
	 double y0 = yLoc / gScale;
	 curAp.draw(g,x0,y0);
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
	aperture.add(ap,d0);
	break;
       case "R":
	double w = Double.valueOf(m.group(3));
	double h = Double.valueOf(m.group(4));
	aperture.add(ap,w,h);
	break;
       default:
	System.out.printf("invalid aperture %s %d\n",type,ap);
	w = Double.valueOf(m.group(3));
	h = Double.valueOf(m.group(4));
	aperture.add(ap,w,h);
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
  nc0.out.printf("m5\n");
  nc0.moveRapidZ(1.5,String.format("Tool %d %5.3fin %4.2fmm",
				   t,toolSize[t],toolSize[t] * 25.4));
  nc0.pause();
  nc0.out.printf("m3\n");
  nc0.out.printf("g4 p 1.0\n");
  nc0.moveRapidZ("0.5");

  int holes = list.size();

  if (t == 1)
  {
   Hole hmin = null;
   Hole hmax = null;
   double min = 999.0;
   double max = 0;
   for (int i = 0; i < holes; i++)
   {
    Hole h = (Hole) list.get(i);
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
   nc1.out.printf("g0 x%3.3f y[#1 + #2 * %3.3f]\n",hmin.x,hmin.y);
   nc1.out.printf("m0\n");
   nc1.out.printf("g0 x%3.3f y[#1 + #2 * %3.3f]\n",hmax.x,hmax.y);
   nc1.out.printf("m0\n");
  }

  double total = 0.0;
  int k = 0;
  while (list.size() > 0)
  {
   double min = 999.0;
   int j = -1;
   for (int i = 0; i < list.size(); i++)
   {
    double dist = list.get(i).dist(curX,curY);
    if (dist < min)
    {
     j = i;
     min = dist;
    }
   }
   total += min;
   Hole h = list.remove(j);
   if (DXF)
   {
    d.line(curX,curY,h.x,h.y);
   }
   image.line(curX,curY,h.x,h.y);
   if (DXF)
   {
    d.text((h.x + curX) / 2.0 + .005,(h.y + curY) / 2.0 + 0.005,
	   0.010,Integer.toString(k));
   }
   curX = h.x;
   curY = h.y;

   nc0.out.printf("g0 x%3.3f y[#1 + #2 * %3.3f]\n",curX,curY);
   nc0.moveLinearZ("[#3]");
   nc0.moveRapidZ("[#4]");

   if (DXF)
   {
    d.circle(curX,curY,toolSize[t] / 2.0);
   }
//     System.out.printf("%3d %3.3f %3.3f\n",j,xLoc,yLoc);
   k++;
  }
  System.out.printf("Tool %d %5.3f Holes %3d Path %4.1f inches\n",
		    t,toolSize[t],holes,total);
  list.clear();
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

 public class Image
 {
  BufferedImage image;
  Graphics2D g;
  int[] data;
  int w0;
  int h0;

  public Image(int w, int h)
  {
   int nScale = (int) (gScale / scale);
   w0 = w / nScale;
   h0 = h / nScale;
   image = new BufferedImage(w0,h0,BufferedImage.TYPE_INT_RGB);
   g = image.createGraphics();
   g.setColor(new Color(BACKGROUND));
   g.fillRect(0,0,w0,h0);
   g.setColor(Color.BLACK);
   data = new int[w0 * h0];
  }

  public void getData()
  {
   image.getRGB(0,0,w0,h0,data,0,w0);
  }

  public void setData()
  {
   image.setRGB(0,0,w0,h0,data,0,w0);
  }

  public void line(double x0, double y0, double x1, double y1)
  {
   g.setColor(Color.BLUE);
   BasicStroke stroke = new BasicStroke(1,BasicStroke.CAP_ROUND,
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
   BasicStroke stroke = new BasicStroke(1,BasicStroke.CAP_ROUND,
				       BasicStroke.JOIN_MITER);
   g.setStroke(stroke);

   double offset = r * scale;
   double d = 2 * offset;
   Ellipse2D shape = new Ellipse2D.Double((x * scale - offset),
					  (y * scale - offset),
					  d,d);
   g.fill(shape);
  }

  public void write(int[] data, String f)
  {
   int j = h0 * w0;
   for (int i = 0; i < h0; i++)
   {
    j -= w0;
    image.setRGB(0,i,w0,1,data,j,w0);
   }
   write(f);
  }

  public void write(String f)
  {
   File file = new File(f + ".bmp");

   try
   {
    ImageIO.write(image,"bmp",file);
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
    aperture[i] = new Aperture(i,v1);
   }
  }

  public void add(int i, double v1, double v2)
  {
   if (i < MAXAPERTURE)
   {
    aperture[i] = new Aperture(i,v1,v2);
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

   public void draw(Graphics2D g, double x0, double y0)
   {
    g.setColor(Color.GRAY);
    BasicStroke stroke = new BasicStroke(1,BasicStroke.CAP_ROUND,
					 BasicStroke.JOIN_MITER);
    g.setStroke(stroke);

    if (type == ROUND)
    {
     double offset = val1 / 2;
     double diam = val1 * scale;
     Ellipse2D shape = new Ellipse2D.Double((x0 - offset) * scale,
					    (y0 - offset) * scale,
					    diam,diam);
     g.fill(shape);
    }
    else if (type == SQUARE)
    {
     double xOfs = val1 / 2;
     double yOfs = val2 / 2;
     Rectangle2D shape = new Rectangle2D.Double(((x0 - xOfs) * scale),
						(y0 - yOfs) * scale,
						val1 * scale ,val2 * scale);
     g.fill(shape);
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
