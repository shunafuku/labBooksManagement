package io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSVReader {

    /**
     * CSVファイルを読み込み、ArrayListとして返すメソッド
     * 
     * @param filePath CSVファイルのパス
     * @param delimiter 区切り文字（デフォルトはカンマ）
     * @return CSVの内容を表すArrayList
     * @throws IOException ファイル読み込み時のエラー
     */
    public static List<List<String>> readCSV(String filePath, String delimiter) throws IOException {
        List<List<String>> lines = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                List<String> values = Arrays.asList(line.split(delimiter));
                lines.add(values);
            }
        }
        
        return lines;
    }

    /**
     * CSVファイルを読み込み、ArrayListとして返すメソッド（デリミタはカンマ）
     * 
     * @param filePath CSVファイルのパス
     * @return CSVの内容を表すArrayList
     * @throws IOException ファイル読み込み時のエラー
     */
    public List<List<String>> readCSV(String filePath) throws IOException {
        return readCSV(filePath, ",");
    }

    // 使用例
    public static void main(String[] args) {
        String filePath = "data.csv";
        CSVReader csvReader = new CSVReader();
        try {
            List<List<String>> csvData = csvReader.readCSV(filePath);
            // 結果の表示
            csvData.forEach(System.out::println);
        } catch (IOException e) {
            System.err.println("ファイルの読み込み中にエラーが発生しました: " + e.getMessage());
        }
    }
}