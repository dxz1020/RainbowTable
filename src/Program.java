import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

public class Program {
	private static int rows = 90000;
	private static int rows2 = 74208;//this value from printout R2.size() in Rainbow.java
	private static int chainLen = 236;
	private static long TOTAL_SHA=0;
	private static int success = 0;

	private static HashMap<String, byte[]> Rtable;
	private static HashMap<String, byte[]> Rtable2;
	private static byte[][]digests = new byte[5000][20];
	private static byte[][]messages = new byte[5000][3];

	public static void main(String[] args) {
		Rtable= new HashMap<String, byte[]>();
		Rtable2= new HashMap<String, byte[]>();
		readFromFileRaw("rainbow.data");
		readFromFileRaw2("rainbow2.data");
		readDigestFromFile("SAMPLE_INPUT.data");
		System.out.println(TOTAL_SHA);
		invert();
		writeOutput("output.data");
	}
	//Solver functions
	public static void invert(){
		System.out.println("Inverting 5000 digest");
		byte[] digest;
		byte[] result;

		for(int i =0; i< messages.length;i++){
			digest=digests[i];
			result=solve(digest);
			messages[i]=result;

			if(result !=null){
				success++;
			}
			else{
				result=solve2(digest);
				messages[i]=result;
				if(result !=null){
					success++;
				}
			}
		}
	}
	public static byte[] solve(byte[] digest){
		byte[] result = new byte[3];
		String key = "";
		for(int i= chainLen-1; i>=0; i--){
			key = invertHashReduce(digest, i);
			if(Rtable.containsKey(key)){
				result = invertChain(digest, Rtable.get(key));
				if(result != null){
					return result;
				}
			}
		}
		return null;
	}
	public static byte[] solve2(byte[] digest){
		byte[] result = new byte[3];
		String key = "";
		for(int i= chainLen-1; i>=0; i--){
			key = invertHashReduce2(digest, i);
			if(Rtable2.containsKey(key)){
				result = invertChain2(digest, Rtable2.get(key));
				if(result != null){
					return result;
				}
			}
		}
		return null;
	}
	
	public static String invertHashReduce(byte[] digest, int step){
		byte[] word = new byte[3];
		for(int i = step; i<chainLen ; i++){
			word = reduce(digest,i);
			digest = hash(word);
		}
		return bytesToHexString (word);
	}
	public static String invertHashReduce2(byte[] digest, int step){
		byte[] word = new byte[3];
		for(int i = step; i<chainLen ; i++){
			word = reduce2(digest,i);
			digest = hash(word);
		}
		return bytesToHexString (word);
	}
	public static byte[] invertChain(byte[] digest_to_match, byte[] word){
		byte[] digest;
		for(int i=0; i<chainLen;i++){
			digest = hash(word);
			if(Arrays.equals(digest, digest_to_match)){
				return word;
			}
			word = reduce(digest,i);
		}
		return null;
	}
	public static byte[] invertChain2(byte[] digest_to_match, byte[] word){
		byte[] digest;
		for(int i=0;i<chainLen;i++){
			digest = hash(word);
			if(Arrays.equals(digest, digest_to_match)){
				return word;
			}
			word = reduce2(digest, i);
		}
		return null;
	}


	//File I/O
	public static void readFromFileRaw(String file){
		try{
			FileInputStream instream = new FileInputStream(file);
			System.out.println("Reading rainbow table");
			byte[][]keyBytes= new byte[rows][3];
			byte[][]valueBytes= new byte[rows][3];
			for(int i=0; i<rows;i++){				
				instream.read(keyBytes[i],0,3);
			}
			for(int i=0;i<rows;i++){
				instream.read(valueBytes[i],0,3);
			}
			for(int i=0;i<rows;i++){
				byte[]key=new byte[3];
				byte[]value=new byte[3];
				key=keyBytes[i];
				value=valueBytes[i];
				Rtable.put(bytesToHexString(key), value);
			}
			System.out.println("Finish Reading rainbow table");
			instream.close();
		}catch (Exception e){
			System.out.println("Exception: "+ e);
		}
	}
	public static void readFromFileRaw2(String file){
		try{
			FileInputStream instream = new FileInputStream(file);
			System.out.println("Reading rainbow table 2");
			byte[][]keyBytes= new byte[rows2][3];
			byte[][]valueBytes= new byte[rows2][3];
			for(int i=0; i<rows2;i++){
				instream.read(keyBytes[i],0,3);
			}
			for(int i=0;i<rows2;i++){
				instream.read(valueBytes[i],0,3);
			}
			for(int i=0;i<rows2;i++){
				byte[]key=new byte[3];
				byte[]value=new byte[3];
				key=keyBytes[i];
				value=valueBytes[i];
				Rtable2.put(bytesToHexString(key), value);
			}
			System.out.println("Finish Reading rainbow table 2");
			instream.close();
		}catch (Exception e){
			System.out.println("Exception: "+ e);
		}
	}
	public static void readDigestFromFile(String file){
		try{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;	
			int index=0;
			System.out.println("Reading Input File");
			while((line=br.readLine()) !=null){
				String hex= line.substring(2, 10) + line.substring(12,20);
				hex += line.substring(22,30) + line.substring(32,40);
				hex += line.substring(42,50);
				hex = hex.replaceAll("\\s", "0");
				digests[index] = hexStringToBytes(hex);
				index++;
			}
			br.close();
			System.out.println("Finish Reading Input");
		}catch(Exception e){
			System.out.println("Exception: "+ e);
		}
	}
	public static void writeOutput(String file){
		try{
			FileWriter W = new FileWriter("output.data");
			System.out.println("Writing output");
			for(int i=0;i<messages.length;i++){
				if(messages[i] == null){
					W.write("0\n");
				}else{
					W.write(bytesToHexString(messages[i])+"\n");
				}
			}

			System.out.println("\n\nsuccess: "+success);
			System.out.println("SHA1 tCount: "+ TOTAL_SHA);
			System.out.println("Accuracy: "+ ((double)success/5000.0) * 100.0 + "%");
			System.out.println("Speedup F: "+ (5000.0/TOTAL_SHA) * 8388608);

			W.write("\n\nThe total number of words found is: "+success+"\n");
			W.close();
			System.out.println("Finished writing output to "+file);
		}catch(Exception e){
			System.out.println("Exception: "+ e);
		}
	}

	//Hashing function
	public static byte[] hash(byte[] plaintext){
		byte digest[] = new byte[20]; //160 bit digest
		try{
			MessageDigest SHA = MessageDigest.getInstance("SHA1");
			digest= SHA.digest(plaintext);
			SHA.reset();
			TOTAL_SHA=TOTAL_SHA+1;
		}catch (Exception e){
			System.out.println("Exception: "+e);
		}
		return digest;
	}

	//reduce function
	public static byte[] reduce(byte[] digest, int len){
		byte variation_byte = (byte) len;
		byte[] word = new byte[3];
		word[0]=(byte)(digest[(len + 0) %20]+ variation_byte);
		word[1]=(byte)(digest[(len + 1) %20]+ variation_byte);
		word[2]=(byte)(digest[(len + 2) %20]+ variation_byte);
		return word;
	}
	public static byte[] reduce2(byte[] digest, int len){
		byte[] word = new byte[3];
		byte variation_byte = (byte) len;
		word[0]=(byte)(digest[(len + 1) %20]- variation_byte);
		word[1]=(byte)(digest[(len + 2) %20]- variation_byte);
		word[2]=(byte)(digest[(len + 3) %20]- variation_byte);
		return word;
	}

	//unused
	public static byte[] reduce3(byte[] digest, int len){
		byte[] word = new byte[3];
		byte variation_byte = (byte) len;
		word[0]=(byte)( (digest[1]+ variation_byte) % 256);
		word[1]=(byte)( (digest[2]+ variation_byte) % 256);
		word[2]=(byte)( (digest[3]+ variation_byte) % 256);
		return word;
	}
	//Helper function
	public static String bytesToHexString(byte[] bytes) {
		HexBinaryAdapter adapter = new HexBinaryAdapter();
		String str = adapter.marshal(bytes);
		return str;
	}
	public static byte[] hexStringToBytes(String hexString) {
		HexBinaryAdapter adapter = new HexBinaryAdapter();
		byte[] bytes = adapter.unmarshal(hexString);
		return bytes;
	}
}
