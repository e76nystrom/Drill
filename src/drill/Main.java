/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drill;

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
  if (args.length >= 1)
  {
   inputFile = args[0];
  }

  if (inputFile.length() == 0)
  {
//   inputFile = "c:\\Development\\Circuits\\Strain Gage\\Connector.drl";
//   inputFile = "c:\\Development\\Circuits\\Xilinx Control\\Xilinx.drl";
//   inputFile = "c:\\Development\\Circuits\\CNC Power\\VFD.drl";
   inputFile = "c:\\Development\\Circuits\\RS485\\Adapter1.drl";
  }

  System.out.printf("Processing %s\n",inputFile);
  new Drill(inputFile);
 }
}
