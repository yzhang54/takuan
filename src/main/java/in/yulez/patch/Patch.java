package in.yulez.patch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Patch {


	public static void generateJson(String polluter, String victim, String cleanerFilePath) throws IOException {

		
		JSONObject minimizedJson = new JSONObject();
		JSONObject expectedRun = new JSONObject();
		JSONObject time = new JSONObject();
		JSONArray pollutersArr = new JSONArray();
		JSONObject results = new JSONObject();
		String hash = "00000000000000000000000000000000";
		JSONObject pollutersJson = new JSONObject();
		JSONArray depsArr = new JSONArray();
		JSONObject cleanerDataJson = new JSONObject();
		JSONArray cleanersArr = new JSONArray();
		JSONObject cleanersJson = new JSONObject();
		JSONObject flakyListsJson = new JSONObject();
		JSONArray dts = new JSONArray();
		
		minimizedJson.put("time", time);
		minimizedJson.put("expectedRun", expectedRun);
		minimizedJson.put("expected", "ERROR");
		minimizedJson.put("dependentTest", victim);
		minimizedJson.put("polluters", pollutersArr);
		
		
		File file = new File(cleanerFilePath);
        Scanner sc=new Scanner(file);
        JSONArray testOrder = new JSONArray();
        JSONArray cleanerTests = new JSONArray();
        
        while(sc.hasNextLine()){
        	String cleaner= sc.nextLine();
        	cleanerTests.add(cleaner);
        	testOrder.add(cleaner);
        }
		sc.close();
		
		testOrder.add(polluter);
		testOrder.add(victim);
		
		
		expectedRun.put("id", "takuan-run");
		expectedRun.put("testOrder", testOrder);
		expectedRun.put("results", results);
		
		
		minimizedJson.put("hash", hash);
		minimizedJson.put("flakyClass", "OD");
		
	
		pollutersArr.add(pollutersJson);
		pollutersJson.put("index", 0);
		
		
		depsArr.add(polluter);
		pollutersJson.put("deps", depsArr);

		
		pollutersJson.put("cleanerData", cleanerDataJson);
		cleanerDataJson.put("dependentTest", victim);
		cleanerDataJson.put("expected", "ERROR");
		cleanerDataJson.put("isolationResult", "PASS");
		
		cleanersArr.add(cleanersJson);
		
		cleanerDataJson.put("cleaners", cleanersArr);
	
		cleanersJson.put("dependentTest", victim);
		cleanersJson.put("originalSize", 0);
		
		cleanersJson.put("cleanerTests", cleanerTests);

		FileWriter minimizedOutput = new FileWriter(".dtfixingtools/minimized/"+victim +"-"+ hash +"-ERROR-dependencies.json");
		minimizedOutput.write(minimizedJson.toJSONString());
		minimizedOutput.close();


		flakyListsJson.put("dts", dts);

		FileWriter flakyLists = new FileWriter("/.dtfixingtools/detection-results/flaky-lists.json");
		flakyLists.write(flakyListsJson.toJSONString());
		flakyLists.close();

	}




}
