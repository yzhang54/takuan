package in.yulez.patch;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Patch {
	public static void main(String[] args) throws ParseException {
		if (args.length != 4) {
			System.err.println("Usage: <polluter> <victim> <cleanerJsonFilePath> <mvnProejctPath>");
			System.exit(1);
			return;
		}

		String polluter = args[0];
		String victim = args[1];
		String cleanerJsonFilePath = args[2];
		String mvnProejctPath = args[3];

		try {
			generateJson(polluter, victim, cleanerJsonFilePath, mvnProejctPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public static void generateJson(String polluter, String victim, String cleanerJsonFilePath, String mvnProejctPath) throws IOException, ParseException {


		
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


		JSONArray testOrder = new JSONArray();
		JSONArray cleanerTests = new JSONArray();


		JSONParser parser = new JSONParser();
		System.out.println(cleanerJsonFilePath);
		JSONObject json = (JSONObject) parser.parse(new FileReader(cleanerJsonFilePath));
		JSONObject firstEle= (JSONObject) ((JSONArray) json.get("cleaners")).get(0);
		String cleaner = (String) firstEle.get("testMethod");
		
		cleanerTests.add(cleaner);
		testOrder.add(cleaner);


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

		FileWriter minimizedOutput = new FileWriter(mvnProejctPath+
				".dtfixingtools/minimized/" + victim + "-" + hash + "-ERROR-dependencies.json");
		minimizedOutput.write(minimizedJson.toJSONString());
		minimizedOutput.close();

		flakyListsJson.put("dts", dts);

		FileWriter flakyLists = new FileWriter(mvnProejctPath+"/.dtfixingtools/detection-results/flaky-lists.json");
		flakyLists.write(flakyListsJson.toJSONString());
		flakyLists.close();
	}
}
