package cmd;
//Tenkaichi Interaction Editor v1.2 by ViveTheModder
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;

public class MainApp 
{
	static boolean[] additionalArgs = new boolean[3]; //order: pre, post, all
	static boolean isBT4file=false;
	static RandomAccessFile param;
	static int charaCnt = 161;
	static String[] charaNames = new String[charaCnt];
	static final int[] ARG_COUNTS = {4,3,2};
	static final String CSV_PATH = "./csv/";
	static final String DAT_PATH = "./dat/";
	static final String OUT_PATH = "./out/";
	static File charaNamesCSV = new File(CSV_PATH+"characters.csv");
	static final String HELP_TEXT =
	"Read and write contents of voice_speaker.dat files, which handle Special Interactions in Budokai Tenkaichi 3.\n"
	+ "Here is a list of all the arguments that can be used. Use -h or -help to print this out again.\n\n"
	+ "* -r\nRead voice_speaker.dat files and write their contents in *.csv files with the same file names.\n"
	+ "* -delpre [chara_ID]\nDisable a pre-battle Special Quote against a given character.\n"
	+ "* -delpost [chara_ID]\nDisable a post-battle Special Quote against a given character.\n"
	+ "* -delpreall\nDisable all pre-battle Special Quotes.\n"
	+ "* -delpostall\nDisable all post-battle Special Quotes.\n"
	+ "* -delall\nDisable all Special Quotes.\n"
	+ "* -wpre [chara_ID] [quote_ID] [1st_or_2nd]\nAssign a pre-battle Special Quote against a given character.\n"
	+ "* -wpost [chara_ID] [quote_ID]\nAssign a post-battle Special Quote against a given character.\n"
	+ "* -wpreall [quote_ID]\nAssign a pre-battle Special Quote against all characters.\n"
	+ "* -wpostall [quote_ID]\nAssign a post-battle Special Quote against all characters.\n";
	
	public static String readDAT() throws IOException
	{
		//change character count to 256 if the DAT file is from BT4 (given that file size is >704 bytes)
		if (!isBT4file && param.length()==1040) 
		{
			isBT4file=true; charaCnt=256;
			charaNames = new String[charaCnt];
			charaNamesCSV = new File(CSV_PATH+"characters-bt4.csv");
			setCharaNames(charaNamesCSV);
		}
		//no reset is performed, meaning every other DAT file in the older is assumed to be from BT4
		if (isBT4file && param.length()==704) return null;
		//default parameters for two Special Quotes (pre-battle quote ID, who talks first, post-battle quote ID)
		byte[] specialQuoteParams = new byte[3];
		String output="opp_ID,opp_name,in_quote_ID,in_quote_placement,out_quote_ID\n"; //initialized with CSV header
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
	public static void writeDAT(int charaID, int quoteID, boolean isSecond) throws IOException
	{
		//change character count to 256 if the DAT file is from BT4 (given that file size is >704 bytes)
		if (!isBT4file && param.length()==1040) 
		{
			isBT4file=true; charaCnt=256;
			charaNames = new String[charaCnt];
			charaNamesCSV = new File(CSV_PATH+"characters-bt4.csv");
			setCharaNames(charaNamesCSV);
		}
		//no reset is performed, meaning every other DAT file in the older is assumed to be from BT4
		if (isBT4file && param.length()==704) return;
				
		byte quotePlacement=0; int iterations=charaCnt, pos=-1, step=4;
		if (additionalArgs[0] || additionalArgs[2]) pos=0;
		if (additionalArgs[1]) pos=2;
		if (additionalArgs[2])
		{
			step=2; iterations=charaCnt*2; //for-loop would end too early if I didn't multiply
		}
		if (isSecond) quotePlacement=1;
		
		for (int i=0; i<iterations; i++)
		{
			param.seek(pos);
			if (additionalArgs[2])
			{
				charaID=666; //set character ID to impossible value to bypass next if condition
				param.writeByte((byte)quoteID);
				if (quoteID==0xFF) param.writeByte(0);
				else param.readByte();
				pos+=step; //increment position to skip every two bytes
			}
			if (i==charaID) 
			{
				param.writeByte((byte)quoteID);
				param.writeByte(quotePlacement);
				return;
			}
			
			//adjust position based on additional argument suffixes (2 for all, 4 for pre or post)
			pos+=step;
			//to prevent endless writing, here's my own EOF """exception"""
			if (pos>=charaCnt*4) return;
		}
	}
	public static void main(String[] args) throws IOException 
	{
		boolean hasArgError=false, hasReadArg=false, hasDelArg=false, hasWriteArg=false;
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
		//check for lack of arguments
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
		
		boolean isSecond=false; int quoteID=0, charaID=0;
		if (hasReadArg) setCharaNames(charaNamesCSV);
		if (hasDelArg)
		{
			if (args.length!=2 && !additionalArgs[2])
			{
				System.out.println("Missing, if not too many arguments!");
				System.exit(3);
			}
		}
		if (hasWriteArg)
		{
			for (int argCnt=0; argCnt<additionalArgs.length; argCnt++)
			{
				if (additionalArgs[argCnt])
				{
					if (args.length==ARG_COUNTS[argCnt]) hasArgError=false;
					else hasArgError=true;
				}
			}
			if (hasArgError) 
			{
				System.out.println("Missing, if not too many arguments!"); System.exit(4);
			}
			
			if (additionalArgs[0] && !additionalArgs[2] && args[3].equals("2nd")) isSecond=true;
			if (!additionalArgs[2]) charaID = Integer.parseInt(args[1]);
			if (!additionalArgs[2]) quoteID = Integer.parseInt(args[2]);
			else quoteID = Integer.parseInt(args[1]);
		}
		
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
				if (output.length()==0) 
				{
					System.out.println("nevermind, time to skip!");
					continue;
				}
				fw.write(output);
				fw.close();
			}
			if (hasDelArg)
			{
				charaID=0;
				if (args.length==2) charaID=Integer.parseInt(args[1]);
				System.out.print("Disabling Special Quotes from "+fileName+"... ");
				writeDAT(charaID,0xFF,false);	
			}
			if (hasWriteArg)
			{
				if (charaID<0 || charaID>charaCnt)
				{
					System.out.println("Invalid character ID! It must be between 0 and "+charaCnt+".");
					System.exit(5);
				}
				if (quoteID<0 || quoteID>93)
				{
					System.out.println("Invalid quote ID! It must be between 0 and 93, although it is not recommended to exceed 40.");
					System.exit(6);
				}
				System.out.print("Assigning Special Quotes to "+fileName+"... ");
				writeDAT(charaID,quoteID,isSecond);
			}
			endForDAT = System.currentTimeMillis();
			double timeForDAT= (endForDAT-startForDAT)/1000;
			time+=timeForDAT;
			System.out.println(timeForDAT + " s");
		}
		System.out.println(String.format("Time: %.3f s", time));
	}
}