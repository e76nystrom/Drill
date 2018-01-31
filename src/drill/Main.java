/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drill;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.Scanner;

/**
 *
 * @author Eric Nystrom
 */

public class Main
{

 /**
  * @param args the command line arguments
  */
 public static void main(String[] args)
 {
  String inputFile = "";
  boolean mirrorX = false;
  boolean mirrorY = false;
  double mirrorXSize = 0.0;
  double mirrorYSize = 0.0;
  boolean flipX = true;
  boolean dxf = false;
  boolean output = true;
  String side = "t";
  Pattern pSpec = Pattern.compile("-([a-zA-Z])");
  Pattern p1Spec = Pattern.compile("--([a-zA-Z]*)=?(\\S*)");
  Pattern probeSpec = Pattern.compile("(\\d+):?(\\d*)");
  Pattern mirrorSpec = Pattern.compile("([xXyY])");
  Pattern sideSpec = Pattern.compile("([tb])");
  String line = "";

  double depth = -0.095;
  double retract = 0.030;
  double safeZ = 1.000;
  double feed = 7.000;
  double change = 1.500;

  boolean probeFlag = false;
  boolean probeHoles = false;
  int probeX = 0;
  int probeY = 0;
  double probeRetract = 0.020;
  double probeDepth = -0.010;

  if (args.length == 0)
  {
   File f = new File("drill.txt");
   if (f.isFile())
   {
    try (BufferedReader in = new BufferedReader(new FileReader(f)))
    {
     while ((line = in.readLine()) != null)
     {
      line = line.trim();
      if (line.length() != 0)
      {
       line = line.replaceAll(" +", " ");
       if (!line.startsWith("/"))
       {
	break;
       }
      }
     }
     in.close();
    }
    catch (IOException e)
    {
     System.out.printf("command read error\n");
    }
   }
  }
  else
  {
   for (String arg : args)
   {
    line += arg + " ";
   }
  }

  System.out.printf("%s\n", line);
  Scanner sc = new Scanner(line);
  MatchResult m;
  while (sc.hasNext())
  {
   if (sc.hasNext(pSpec))
   {
    sc.next(pSpec);
    m = sc.match();
    int count = m.groupCount();
    if (count == 1)
    {
     char c = m.group(1).charAt(0);
     switch (c)
     {
     case 'x':			/* set actual x size */
      flipX = true;
      if (sc.hasNextDouble())
      {
       mirrorXSize = sc.nextDouble();
      }
      break;
     case 'y':			/* set actual y size */
      flipX = false;
      if (sc.hasNextDouble())
      {
       mirrorYSize = sc.nextDouble();
      }
      break;
     case 'm':			/* mirror on x or y */
      if (sc.hasNext(mirrorSpec))
      {
       sc.next(mirrorSpec);
       m = sc.match();
       if (m.groupCount() >= 1)
       {
	String tmp = m.group(1).toLowerCase();
	if (tmp.equals("x"))
	{
	 mirrorX = true;
	}
	else if (tmp.equals("y"))
	{
	 mirrorY = true;
	}
       }
      }
      break;
     case 'c':			/* generate dxf file */
      dxf = true;
      break;
     case 'p':			/* generate probe file */
      if (sc.hasNext(probeSpec))
      {
       sc.next(probeSpec);
       m = sc.match();
       count = m.groupCount();
       probeX = 0;
       probeY = 0;
       try
       {
	if (count >= 1)
	{
	 probeFlag = true;
	 probeX = Integer.valueOf(m.group(1));
	}
	if (count >= 2)
	{
	 probeY = Integer.valueOf(m.group(2));
	}
       }
       catch (NumberFormatException e)
       {
	probeFlag = false;
       }	
      }
      break;
     case 'h':			/* use hole information with probe file */
      probeHoles = true;
      break;
     case 's':			/* set board side */
      if (sc.hasNext(sideSpec))
      {
       sc.next(sideSpec);
       m = sc.match();
       if (m.groupCount() >= 1)
       {
	side = m.group(1);
       }
      }
      break;
     case 'n':			/* no gcode for drilling */
      output = false;
      break;
     default:
      break;
     }
    }
   }
   else if (sc.hasNext(p1Spec))
   {
    sc.next(p1Spec);
    m = sc.match();
    if (m.groupCount() == 2)
    {
     String option = m.group(1);
     try
     {
      double val = Double.valueOf(m.group(2));
      switch (option)
      {
      case "depth":
       depth = val;
       break;
      case "retract":
       retract = val;
       break;
      case "safez":
       safeZ = val;
       break;
      case "feed":
       feed = val;
       break;
      case "probeDepth":
       probeDepth = val;
       break;
      case "probeRetract":
       probeRetract = val;
       break;
      default:
       break;
      }
     }
     catch (NumberFormatException e)
     {
     }
    }
   }
   else if (sc.hasNext())
   {
    if (inputFile.length() != 0)
    {
     inputFile += " ";
    }
    inputFile += sc.next();
   }
  }

  System.out.printf("Processing %s\n", inputFile);
  System.out.printf("drill 03/18/2017\n");
  Drill drill = new Drill(inputFile, flipX, output, dxf);
  drill.setParameters(depth, retract, safeZ, feed, change);
  drill.setProbe(probeFlag, probeHoles, probeX, probeY, side,
		 probeDepth, probeRetract);
  drill.setMirror(mirrorX, mirrorXSize, mirrorY, mirrorYSize);
  drill.process();
 }
}
