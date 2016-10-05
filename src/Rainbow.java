import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter; //Hex to Byte conversion tool

public class Rainbow {
	private static HashMap<String, byte[]> Rtable; //<start word, end word>
	private static HashMap<String, byte[]> Rtable2;
	private static ArrayList<String> key1;
	private static ArrayList<String> map1;
	private static ArrayList<String> key2;
	private static ArrayList<String> map2;
	private static ArrayList<Integer> collision;
	private static int chainLen = 236;
	private static int rows = 90000;

	public static void main(String arg[]){
		Rtable= new HashMap<String, byte[]>();
		Rtable2 = new HashMap<String, byte[]>();
		collision = new ArrayList<Integer>();
		key1=new ArrayList<String>();
		map1=new ArrayList<String>();
		key2=new ArrayList<String>();
		map2=new ArrayList<String>();
		generate();
		System.out.println("R1 collision: "+ collision.size()+"\n");
		writeToFileRaw();
		generate2();
		writeToFileRaw2();
	}
	//Hashing function
	public static byte[] hash(byte[] plaintext){
		byte digest[] = new byte[20]; //160 bit digest
		try{
			MessageDigest SHA = MessageDigest.getInstance("SHA1");
			digest= SHA.digest(plaintext);
			SHA.reset();
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
	//Generate RTable
	public static void generate(){
		byte[] wordStart, wordEnd;
		String wordEndKey;
		System.out.println("Generating table\n");
		int i = 0;
		while(Rtable.size()<rows){
			wordStart= intToBytes(i);
			wordEnd = generateChain(wordStart);
			wordEndKey = bytesToHexString(wordEnd);
			if(!Rtable.containsKey(wordEndKey)){
				Rtable.put(wordEndKey, wordStart);
				key1.add(wordEndKey);
				map1.add(bytesToHexString(wordStart));
			}
			else{
				collision.add(i);
			}
			i++;
		}
		System.out.println("table 1 built");
	}
	public static void generate2(){
		byte[] wordStart, wordEnd;
		String wordEndKey;
		System.out.println("Generating table2\n");

		for(int i=0;i<collision.size();i++){
			wordStart= intToBytes(collision.get(i));
			wordEnd = generateChain2(wordStart);
			wordEndKey = bytesToHexString(wordEnd);
			if(!Rtable2.containsKey(wordEndKey) && !Rtable.containsKey(wordEndKey)){
				Rtable2.put(wordEndKey, wordStart);
				key2.add(wordEndKey);
				map2.add(bytesToHexString(wordStart));
			}
		}
		System.out.println("R2 size after removing duplicates: "+Rtable2.size()+"\n");
		System.out.println("key2 size: "+key2.size()+"\n");
		System.out.println("map2 size: "+map2.size()+"\n");
	}
	public static byte[] generateChain(byte[] wordStart){
		byte[] digest = new byte[20]; //160 bit digest
		byte[] wordPtr = wordStart;
		for (int i=0;i<chainLen;i++){
			digest=hash(wordPtr);
			wordPtr=reduce(digest,i);
		}
		return wordPtr;
	}
	public static byte[] generateChain2(byte[] wordStart){
		byte[] digest = new byte[20]; //160 bit digest
		byte[] wordPtr = wordStart;
		for (int i=0;i<chainLen;i++){
			digest=hash(wordPtr);
			wordPtr=reduce2(digest,i);
		}
		return wordPtr;
	}
	//Helper Functions
	public static byte[] intToBytes(int n){
		byte plaintext[] = new byte[3];
		plaintext[0] = (byte) ((n >> 16) & 0xFF);
		plaintext[1] = (byte) ((n >> 8) & 0xFF);
		plaintext[2] = (byte) n;
		return plaintext;
	}
	public static void writeToFileRaw(){
		try{
			FileOutputStream stream = new FileOutputStream("rainbow.data");
			for(int i=0;i<key1.size();i++){
				String key= key1.get(i);
				byte[] keyByte = hexStringToBytes(key);
				stream.write(keyByte,0,3);
			}
			for(int i=0;i<map1.size();i++){
				String value=map1.get(i);
				byte[] valueByte = hexStringToBytes(value);
				stream.write(valueByte, 0, 3);
			}
			stream.close();
			System.out.println("Raw byte R1 written to rainbow.data");
		}catch (Exception e){
			System.out.println("Exception: "+ e);
		}
	}
	public static void writeToFileRaw2(){
		try{
			FileOutputStream stream = new FileOutputStream("rainbow2.data");
			for(int i=0;i<key2.size();i++){
				String key= key2.get(i);
				byte[] keyByte = hexStringToBytes(key);
				stream.write(keyByte,0,3);
			}
			for(int i=0;i<map2.size();i++){
				String value=map2.get(i);
				byte[] valueByte = hexStringToBytes(value);
				stream.write(valueByte, 0, 3);
			}
			stream.close();
			System.out.println("Raw byte R2 written to rainbow.data");
		}catch (Exception e){
			System.out.println("Exception: "+ e);
		}
	}

	public static byte[] hexStringToBytes(String hexString) {
		HexBinaryAdapter adapter = new HexBinaryAdapter();
		byte[] bytes = adapter.unmarshal(hexString);
		return bytes;
	}

	public static String bytesToHexString(byte[] bytes) {
		HexBinaryAdapter adapter = new HexBinaryAdapter();
		String str = adapter.marshal(bytes);
		return str;
	}
}
