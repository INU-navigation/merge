package com.wegoup.dijkstra;


import android.content.Context;
import android.content.res.AssetManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class CSVReader {
    public CSVReader() {
    }

    public static List<List<String>> pathCSV(Context context, String fileName) throws IOException {
        List<List<String>> data = new ArrayList();
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        CSVParser csvParser = CSVFormat.DEFAULT.parse(reader);
        Iterator var7 = csvParser.iterator();

        while(var7.hasNext()) {
            CSVRecord csvRecord = (CSVRecord)var7.next();
            List<String> row = new ArrayList();
            Iterator var10 = csvRecord.iterator();

            while(var10.hasNext()) {
                String value = (String)var10.next();
                row.add(value);
            }

            data.add(row);
        }

        inputStream.close();
        return data;
    }
}
