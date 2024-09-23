package cmd;
//Tenkaichi Interaction Editor v1.0 by ViveTheModder
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;

public class MainApp 
{
	static boolean[] additionalArgs = new boolean[3]; //order: pre, post, all
	static RandomAccessFile param;
	static int charaCnt = 161;
	static String[] charaNames = new String[charaCnt];
	static final String CSV_PATH = "./csv/";
	static final String DAT_PATH = "./dat/";
	static final String OUT_PATH = "./out/";
	static final String HELP_TEXT =
	"Read and write contents of voice_speaker.dat files, which handle Special Interactions in Budokai Tenkaichi 3.\n"
	+ "Here is a list of all the arguments that can be used. Use -h or -help to print this out again.\n\n"
	+ "* -r\nRead voice_speaker.dat files and write their contents in *.csv files with the same file names.\n"
	+ "* -delpre [chara_ID]\nDisable a pre-battle Special Quote against a given character.\n"
	+ "* -delpost [chara_ID]\nDisable a post-battle Special Quote against a given character.\n"
	+ "* -delpreall\nDisable all pre-battle Special Quotes.\n"
	+ "* -delpostall\nDisable all post-battle Special Quotes.\n"
	+ "* -delall\nDisable all Special Quotes.\n"
	+ "* -wpre [chara_ID] [ADX_ID] [1st_or_2nd]\nAssign a pre-battle Special Quote against a given character.\n"
	+ "* -wpost [chara_ID] [ADX_ID]\nAssign a post-battle Special Quote against a given character.\n"
	+ "* -wpreall [ADX_ID]\nAssign a pre-battle Special Quote against all characters.\n"
	+ "* -wpostall [ADX_ID]\nAssign a post-battle Special Quote against all characters.\n";
	
	public static String readDAT() throws IOException
	{
		//default parameters for two Special Quotes (pre-battle ADX ID, who talks first, post-battle ADX ID)
		byte[] specialQuoteParams = new byte[3];
		String output="opp_ID,opp_name,in_adx_ID,in_adx_placement,out_adx_ID\n"; //initialized with CSV header
		for (int i=0; i<charaCnt; i++)
		{
			param.seek(i*4);
			for (int j=0; j<specialQuoteParams.length; j++)
				specialQuoteParams[j]=param.readByte();
			//skip entries without Special Quotes
			if (specialQuoteParams[0]==-1 && specialQuoteParams[2]==-1) continue;
			
			output+=charaNames[i]+",";
			for (int j=0; j<specialQuoteParams.length; j++)
			{
				if (j%2==0) output+=specialQuoteParams[j];
				else
				{
					if (specialQuoteParams[j]==0) output+="1st";
					else output+="2nd";
				}
				if (j<(specialQuoteParams.length-1)) output+=",";
			}
			output+="\n";
		}
		return output;
	}
	public static void setCharaNames(File names) throws IOException
	{
		Scanner sc = new Scanner(names);
		int rowCnt=0;
		while (sc.hasNextLine())
		{
			String name = sc.nextLine();
			charaNames[rowCnt]=name;
			rowCnt++;
		}
		sc.close();
	}
	public static void writeDAT(int charaID, int adxID, boolean isSecond) throws IOException
	{
		byte adxPlacement=0; int iterations=charaCnt, pos=-1, step=4;
		if (additionalArgs[0] || additionalArgs[2]) pos=0;
		if (additionalArgs[1]) pos=2;
		if (additionalArgs[2])
		{
			step=2; iterations=charaCnt*2; //for-loop would end too early if I didn't multiply
		}
		if (isSecond) adxPlacement=1;
		
		for (int i=0; i<iterations; i++)
		{
			param.seek(pos);
			if (i==charaID || additionalArgs[2]) 
			{
				param.writeByte((byte)adxID);
				param.writeByte(adxPlacement);
			}
			//skip every two bytes starting from zero (to only overwrite pre-battle quote params)
			if (additionalArgs[0] && i%2==0) additionalArgs[2]=false;
			else additionalArgs[2]=true;
			//skip every two bytes starting from two (to only overwrite post-battle quote params)
			if (additionalArgs[1] && i%2==0) additionalArgs[2]=false;
			else additionalArgs[2]=true;
			//adjust position based on additional argument suffixes (2 for all, 4 for pre or post)
			pos+=step;
			//to prevent endless writing, here's my own EOF """exception"""
			if (pos==charaCnt*4) return;
		}
	}
	public static void main(String[] args) throws IOException 
	{
		boolean hasReadArg=false, hasDelArg=false, hasWriteArg=false;
		double endForDAT, startForDAT, time=0;
		File folder = new File(DAT_PATH), outputCSV;
		File[] paths = folder.listFiles((dir, name) -> (name.toLowerCase().endsWith(".dat") && name.toLowerCase().contains("voice_speaker")));
		FileWriter fw; String output;
		RandomAccessFile[] datFiles = new RandomAccessFile[paths.length];
		
		if (datFiles.length==0)
		{
			System.out.println("No voice_speaker.dat files have been detected! Check file names and/or extensions.");
			System.exit(1);
		}
		//check for arguments
		if (args.length==0)
		{
			System.out.println("No arguments have been provided! Have fun reading all this:\n"); 
			System.out.println(HELP_TEXT); System.exit(2);
		}
		if (args[0].equals("-h"))
		{
			System.out.println(HELP_TEXT); System.exit(0);
		}
		if (args[0].equals("-r")) hasReadArg=true;
		if (args[0].startsWith("-del")) hasDelArg=true;
		if (args[0].startsWith("-w")) hasWriteArg=true;
		if (args[0].contains("pre")) additionalArgs[0]=true;
		if (args[0].contains("post")) additionalArgs[1]=true;
		if (args[0].contains("all")) additionalArgs[2]=true;
		if (hasReadArg) setCharaNames(new File(CSV_PATH+"characters.csv"));
		
		for (int datIndex=0; datIndex<datFiles.length; datIndex++)
		{
			startForDAT = System.currentTimeMillis();
			datFiles[datIndex] = new RandomAccessFile(paths[datIndex].getAbsolutePath(),"rw");
			param = datFiles[datIndex];
			String fileName = paths[datIndex].getName();
			if (hasReadArg)
			{
				System.out.print("Reading contents of "+fileName+"... ");
				outputCSV = new File(OUT_PATH+fileName.replace(".dat", ".csv"));
				fw = new FileWriter(outputCSV);
				output = readDAT();
				fw.write(output);
				fw.close();
			}
			if (hasDelArg)
			{
				int charaID=0;
				if (args.length==2) charaID=Integer.parseInt(args[1]);
				if (args.length!=2 && !additionalArgs[2])
				{
					System.out.println("Missing arguments, if not too many! Must only contain character ID.");
					System.exit(3);
				}
				System.out.print("Disabling Special Quotes from "+fileName+"... ");
				writeDAT(charaID,0xFF,false);	
			}
			if (hasWriteArg)
			{
				boolean isSecond=false; int adxID=0, charaID=0;
				if (args.length!=3 && additionalArgs[0])
				{
					System.out.println("Missing arguments, if not too many! Must only contain character ID, ADX ID and placement (1st or 2nd)."); 
					System.exit(4);
				}
				if (args.length!=2 && additionalArgs[1])
				{
					System.out.println("Missing arguments, if not too many! Must only contain character ID and ADX ID."); 
					System.exit(4);
				}
				if (args.length!=1 && additionalArgs[2])
				{
					System.out.println("Missing arguments, if not too many! Must only contain ADX ID."); 
					System.exit(4);
				}
				
				if (args[3].equals("2nd")) isSecond=true;
				charaID = Integer.parseInt(args[1]);
				adxID = Integer.parseInt(args[2]);
				System.out.print("Assigning Special Quotes to "+fileName+"... ");
				writeDAT(charaID,adxID,isSecond);
			}
			endForDAT = System.currentTimeMillis();
			double timeForDAT= (endForDAT-startForDAT)/1000;
			time+=timeForDAT;
			System.out.println(timeForDAT + " s");
		}
		System.out.println(String.format("Time: %.3f s", time));
	}
}