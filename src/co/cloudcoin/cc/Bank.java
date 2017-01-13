package co.cloudcoin.cc;

import android.util.Log;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import android.content.Context;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.io.FileNotFoundException;
import java.io.BufferedWriter;
import java.io.FileWriter;


import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.Arrays;

import java.io.FileReader;
import android.os.Environment;
import java.io.FilenameFilter;
import java.util.Random;

public class Bank {

	static String RAIDA_AUTH_URL = "https://www.cloudcoin.co/servers.html";
	static String DIR_BASE = "CloudCoins";
	static String IMPORT_DIR_NAME = "Import";
	static String EXPORT_DIR_NAME = "Export";
	static String IMPORTED_DIR_NAME = "Imported";
	static String BANK_DIR_NAME = "Bank";
	static String TAG = "CLOUDCOIN";
	static int CONNECTION_TIMEOUT = 5000; // ms

	static int STAT_FAILED = 0;
	static int STAT_AUTHENTIC = 1;
	static int STAT_COUNTERFEIT = 2;
	static int STAT_FRACTURED = 3;
	
	static int STAT_VALUE_MOVED_TO_BANK = 4;

	private String importDirPath;
	private String exportDirPath;
	private String importedDirPath;
	private String bankDirPath;

	private Context ctx;

	private ArrayList<IncomeFile> loadedIncome; 

	static int[] denominations = {1, 5, 25, 100, 250};

	RAIDA raida;

	private int[] importStats;

	public Bank() {

		this.importDirPath = null;
		this.loadedIncome = new ArrayList<IncomeFile>();

		this.raida = new RAIDA();
		this.resetImportStats();
//		this.findImportDir();
		this.createDirectories();
	}


	public void setContext(Context ctx) {
		this.ctx = ctx;
	}

	public void resetImportStats() {
		importStats = new int[6];

		for (int i = 0; i < importStats.length; i++)
			importStats[i] = 0;
	}

	public String getImportDirPath() {
		return this.importDirPath;
	}

	public String getRelativeImportDirPath() {
		return DIR_BASE + "/" + IMPORT_DIR_NAME;
	}

	public String getBankDirPath() {
		return this.bankDirPath;
	}

	private String createDirectory(File path, String dirName) {
		String idPath;

		idPath = path + "/" + DIR_BASE + "/" + dirName;
		try {
			File idPathFile = new File(idPath);
			idPathFile.mkdirs();
		} catch (Exception e) {
			Log.e(TAG, "Can not create Import directory");
			return null;
		}

		return idPath;
	}

	private void createDirectories() {

		String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(state)) {
			Log.e(TAG, "SD card is not mounted");
			return;
		}

		File path = Environment.getExternalStorageDirectory();
		if (path == null) {
			Log.e(TAG, "Failed to get External directory");
			return;
		}

		importDirPath = createDirectory(path, IMPORT_DIR_NAME);
		exportDirPath = createDirectory(path, EXPORT_DIR_NAME);
		bankDirPath = createDirectory(path, BANK_DIR_NAME);

		importedDirPath = createDirectory(path, IMPORT_DIR_NAME + "/" + IMPORTED_DIR_NAME);
	}


	public String getFileExtension(String f) {
		String ext = "";
		int i = f.lastIndexOf('.');

		if (i > 0 &&  i < f.length() - 1) {
			ext = f.substring(i + 1);
		}

		return ext;
	}


	public ArrayList<IncomeFile> selectAllFileNamesFolder(String path, String extension) {
		int fileType;
		ArrayList<IncomeFile> fileArray = new ArrayList<IncomeFile>();

		try {
			File f = new File(path);
			File[] files = f.listFiles();
			for (File inFile : files) {
				if (inFile.isFile()) {
					String currentExtension = getFileExtension(inFile.getName()).toLowerCase();
					if (currentExtension.equals(extension)) {
						if (extension.equals("jpeg") || extension.equals("jpg")) {
							fileType = IncomeFile.TYPE_JPEG;
						} else {
							fileType = IncomeFile.TYPE_STACK;
						}

						fileArray.add(new IncomeFile(inFile.getAbsolutePath(), fileType));
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Failed to read directory: " + path);
			return fileArray;
		}

		return fileArray;
	}
	
	public boolean examineImportDir() {
		String extension;
		int fileType;

		if (this.importDirPath == null)
			return false;

		try {
			File f = new File(this.importDirPath);
			File[] files = f.listFiles();
			for (File inFile : files) {
				if (inFile.isFile()) {
					extension = getFileExtension(inFile.getName()).toLowerCase();
					
					if (extension.equals("jpeg") || extension.equals("jpg")) {
						fileType = IncomeFile.TYPE_JPEG;
					} else {
						fileType = IncomeFile.TYPE_STACK;
					}

					loadedIncome.add(new IncomeFile(inFile.getAbsolutePath(), fileType));
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Failed to read Import directory");
			return false;
		}

		return true;
	}

	public void importLoadedItem(int idx, String importTag) {
		IncomeFile incomeFile;
		CloudCoin cc;
		String incomeJson;

		if (idx >= loadedIncome.size()) {
			Log.e(TAG, "Failed to import coin due to internal error");
			importStats[STAT_FAILED]++;
			return;
		}	

		incomeFile = loadedIncome.get(idx);
		incomeFile.fileTag = importTag;
		try {
			if (incomeFile.fileType == IncomeFile.TYPE_JPEG) {
				cc = new CloudCoin(incomeFile);
				cc.saveCoin(bankDirPath, "suspect");
			} else if (incomeFile.fileType == IncomeFile.TYPE_STACK) {
				incomeJson = CloudCoin.loadJSON(incomeFile.fileName);
				if (incomeJson == null) {
					importStats[STAT_FAILED]++;
					return;
				}
			
				try {
					JSONObject o = new JSONObject(incomeJson);
					JSONArray incomeJsonArray = o.getJSONArray("cloudcoin");

					for (int i = 0; i < incomeJsonArray.length(); i++) {
						JSONObject childJSONObject = incomeJsonArray.getJSONObject(i);
						int nn     = childJSONObject.getInt("nn");
						int sn     = childJSONObject.getInt("sn");
						JSONArray an = childJSONObject.getJSONArray("an");
						String ed     = childJSONObject.getString("ed");
						String aoid = childJSONObject.getString("aoid");
					
						cc = new CloudCoin(nn, sn, CloudCoin.toStringArray(an), ed, aoid, importTag);
						cc.saveCoin(bankDirPath, "suspect");
					}
				} catch (JSONException e) {
					Log.e(TAG, "Stack file " + incomeFile.fileName + " is corrupted");
					importStats[STAT_FAILED]++;
					return;
				}
			}

			moveFileToImported(incomeFile.fileName);
			detectAuthenticity();
		} catch (Exception e) {
			Log.e(TAG, "Failed to import coin " + incomeFile.fileName + ". Please check it");
			importStats[STAT_FAILED]++;
			e.printStackTrace();
			return;
		}
	}


	public void fixFracked() {
		CloudCoin[] coins;

		coins = loadCoinArray("fracked");
		for (int i = 0; i < coins.length; i++) {
			raida.fixCoin(coins[i]);

			try {
				coins[i].saveCoin(bankDirPath, coins[i].extension);
				deleteCoin(coins[i].fullFileName);
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(TAG, "Failed to save coin: " + coins[i].fullFileName);
			}
		}
	}

	public void moveFileToImported(String fileName) {
		File fsource, ftarget;
		String target;
	
		try {
			fsource = new File(fileName);
			target = importedDirPath + "/" + fsource.getName() + ".imported";

			ftarget = new File(target);
			fsource.renameTo(ftarget);
		} catch (Exception e) {
			Log.e(TAG, "Failed to move to imported " + fileName);
			return;
		}

	}

	public void deleteCoin(String path) {
		boolean deleted = false;

		File f  = new File(path);
		try {
			f.delete();
		} catch (Exception e) {
			Log.e(TAG, "Failed to delete coin " + path);
			e.printStackTrace();
		}
	}

	
/*
	private String getOneJSON(String jsonData) {
		int indexOfFirstSquareBracket = CloudCoin.ordinalIndexOf(jsonData, "[", 0);
		int indexOfLastSquareBracket = CloudCoin.ordinalIndexOf(jsonData, "]", 0);

		return jsonData.substring(indexOfFirstSquareBracket, indexOfLastSquareBracket);
	}
*/
	public int[] exportJson(int[] values, String tag) {
		int[] failed;
		CloudCoin cc;
		ArrayList<IncomeFile> bankFiles = selectAllFileNamesFolder(bankDirPath, "bank");
		ArrayList<IncomeFile> frackedFiles = selectAllFileNamesFolder(bankDirPath, "fracked");

		bankFiles.addAll(frackedFiles);

		failed = new int[values.length];

		int denomination;
		int totalSaved = 0;
		int coinCount = 0;
		for (int i = 0; i < values.length; i++) {
			failed[i] = 0;
			totalSaved += denominations[i] * values[i];
			coinCount += values[i];
		}

		String tJ, json = "{ \"cloudcoin\": [";
		ArrayList<String> coinsToDelete = new ArrayList<String>();
		int c = 0;

		for (int i =0; i < bankFiles.size(); i++ ) {
			IncomeFile fileToExport = bankFiles.get(i);
			denomination = getDenomination(fileToExport);

			for (int j = 0; j < values.length; j++) {
				if (denomination == denominations[j] && values[j] > 0) {
					if (c != 0)
						json += ",\n";

					try {
						fileToExport.fileTag = tag;
						tJ = CloudCoin.loadJSON(fileToExport.fileName);
						if (tJ == null) {
							Log.e(TAG, "Failed to export coin: " + bankFiles.get(i).fileName);
							failed[j]++;
						} else {
							JSONObject o = new JSONObject(tJ);
							JSONArray jArray = o.getJSONArray("cloudcoin");
							json += jArray.getJSONObject(0).toString();
						}
						
						coinsToDelete.add(fileToExport.fileName);
						c++;
					} catch (JSONException e) {
						Log.e(TAG, "Invalid json " + bankFiles.get(i).fileName);
						failed[j]++;
					} catch (Exception e) {
						Log.e(TAG, "Failed to export coin: " + bankFiles.get(i).fileName);
						failed[j]++;
					}

					values[j]--;
					break;
				}
			}
		}

		json += "]}";

		try {  
			JSONObject o = new JSONObject(json);
		} catch (JSONException e) {
			Log.e(TAG, "Invalid JSON was created");
			failed[0] = -1;
			return failed;
		}

		if (tag.isEmpty()) {
			Random rnd = new Random();
			tag = "" + rnd.nextInt(999);
		}
	
		String fileName = exportDirPath + "/" + totalSaved + ".CloudCoins." + tag + ".stack";
		BufferedWriter writer = null;
		try {   
			if (ifFileExists(fileName)) { 
				// no overwriting
				Log.v(TAG, "Filename " + fileName + " already exists");
				failed[0] = -1;
				return failed;
		
				/*
				Random rnd = new Random();
				int tagrand = rnd.nextInt(999);
				fileName = exportDirPath + "/" + totalSaved + ".CloudCoins." + tag + tagrand + ".stack";
				*/
			}

                        writer = new BufferedWriter(new FileWriter(fileName));
                        writer.write(json);
                } catch (IOException e){
                        Log.e(TAG, "Failed to save file " + fileName);
			failed[0] = -1;
			return failed;
                } finally {
                        try{
                                if (writer != null)
                                        writer.close();
                        } catch (IOException e){
                                Log.e(TAG, "Failed to close BufferedWriter");
				failed[0] = -1;
				return failed;
                        }
                }
		
		for (String ctd : coinsToDelete) {
			deleteCoin(ctd);
		}

		return failed;
	}

	public boolean ifFileExists(String filePathString ) {
		File f = new File(filePathString);
		if (f.exists() && !f.isDirectory()) 
			return true;

		return false;
	}

	public int[] exportJpeg(int[] values, String tag) {
		int[] failed;
		CloudCoin cc;
		ArrayList<IncomeFile> bankFiles = selectAllFileNamesFolder(bankDirPath, "bank");
		ArrayList<IncomeFile> frackedFiles = selectAllFileNamesFolder(bankDirPath, "fracked");

		bankFiles.addAll(frackedFiles);

		failed = new int[values.length];

		int denomination;
		int totalSaved = 0;
		int coinCount = 0;
		for (int i = 0; i < values.length; i++) {
			failed[i] = 0;
			totalSaved += denominations[i] * values[i];
			coinCount += values[i];
		}

		for (int i =0; i < bankFiles.size(); i++ ) {
			IncomeFile fileToExport = bankFiles.get(i);
			denomination = getDenomination(fileToExport);

			for (int j = 0; j < values.length; j++) {
				if (denomination == denominations[j] && values[j] > 0) {
					try {
						fileToExport.fileTag = tag;
						cc = new CloudCoin(fileToExport);
						cc.setJpeg(bankDirPath, ctx);
						cc.writeJpeg(exportDirPath);
						deleteCoin(fileToExport.fileName);
						// delete
					} catch (Exception e) {
						Log.e(TAG, "Failed to export coin: " + bankFiles.get(i).fileName);
						failed[j]++;
					}

					values[j]--;
					break;
				}
			}
		}

		return failed;
	}

	public void detectAuthenticity() throws Exception {
		CloudCoin cc;
		ArrayList<IncomeFile> incomeFiles = selectAllFileNamesFolder(bankDirPath, "suspect");

		for (int i = 0; i < incomeFiles.size(); i++) {
			try {
				cc = new CloudCoin(incomeFiles.get(i));	
				raida.detectCoin(cc);

				cc.saveCoin(bankDirPath, cc.extension);
				deleteCoin(incomeFiles.get(i).fileName);

				if (cc.extension.equals("bank")) {
					importStats[STAT_AUTHENTIC]++;
					importStats[STAT_VALUE_MOVED_TO_BANK] += cc.getDenomination();
				} else if (cc.extension.equals("fracked")) {
					importStats[STAT_FRACTURED]++;
				} else if (cc.extension.equals("counterfeit")) {
					importStats[STAT_COUNTERFEIT]++;
				} else {
					importStats[STAT_FAILED]++;
				}
			} catch (Exception e) {
				Log.e(TAG, "Failed to detect coin");
				throw new Exception();
			}
		}

	}


	public boolean renameFileExtension(String source, String newExtension){
		String target;
		String currentExtension = getFileExtension(source);

		if (currentExtension.equals("")){
			target = source + "." + newExtension;
		} else {
			target = source.replaceFirst(Pattern.quote("." + currentExtension) + "$", Matcher.quoteReplacement("." + newExtension));
		}

		return new File(source).renameTo(new File(target));
	}

	public int getImportStats(int type) {
		if (type >= importStats.length)
			return 0;

		return importStats[type];
	}

	public int getLoadedIncomeLength() {
		return this.loadedIncome.size();
	}

	/*
	public int countCoins(CloudCoin[] coins, int denomination ){
		int totalCount =  0;
		for (int i = 0 ; i < coins.length; i++) {
			if (coins[i].getDenomination() == denomination) {
				totalCount++;
			}
		}
		return totalCount;
	}*/

	private int getDenomination(IncomeFile incomeFile) {
		int denomination;
		File f = new File(incomeFile.fileName);

		String[] nameParts = f.getName().split("\\.");
		try {
			denomination = Integer.parseInt(nameParts[0]);
		} catch (Exception e) {
			denomination = -1;
		}

		return denomination;			
	}

	public int[] countCoins(String extension) {
		int denomination;
		int totalCount =  0;
		int[] returnCounts = new int[6]; //0. Total, 1.1s, 2,5s, 3.25s 4.100s, 5.250s
		
		ArrayList<IncomeFile> incomeFiles = selectAllFileNamesFolder(bankDirPath, extension);

		for (int i = 0; i < incomeFiles.size(); i++) {
			denomination = getDenomination(incomeFiles.get(i));
			switch (denomination) {
				case 1: returnCounts[0] += 1; returnCounts[1]++; break;
				case 5: returnCounts[0] += 5; returnCounts[2]++; break;
				case 25: returnCounts[0] += 25; returnCounts[3]++; break;
				case 100: returnCounts[0] += 100; returnCounts[4]++; break;
				case 250: returnCounts[0] += 250; returnCounts[5]++; break;
			}
		}

		return returnCounts;
	}



	public CloudCoin[] loadCoinArray(String extension) {
		CloudCoin[] loadedCoins = null;
		
		try {
			ArrayList<IncomeFile> incomeFiles = selectAllFileNamesFolder(bankDirPath, extension);

			loadedCoins = new CloudCoin[incomeFiles.size()];
			for (int i = 0; i < incomeFiles.size(); i++) {
				loadedCoins[i] = new CloudCoin(incomeFiles.get(i)); 
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "Failed to load coins for " + extension);
			return null;
		}

		return loadedCoins;
	}
}
