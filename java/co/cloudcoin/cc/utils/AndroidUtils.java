package co.cloudcoin.cc.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by navrajsingh on 8/7/17.
 */

public class AndroidUtils {
    public static void WriteToFile(String data,String file,Context context) {
        File fileObj = new File(file);

        try {
            FileOutputStream f = new FileOutputStream(fileObj);
            PrintWriter pw = new PrintWriter(f);
            pw.print(data);
            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }
/*
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(file, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
*/
    }

    public static String ReadFromFile(Context context,String file) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput(file);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

    public static String[] SplitByNumber(String text, int number) {

        int inLength = text.length();
        int arLength = inLength / number;
        int left=inLength%number;
        if(left>0){++arLength;}
        String ar[] = new String[arLength];
        String tempText=text;
        for (int x = 0; x < arLength; ++x) {

            if(tempText.length()>number){
                ar[x]=tempText.substring(0, number);
                tempText=tempText.substring(number);
            }else{
                ar[x]=tempText;
            }

        }


        return ar;
    }
}
