package com.example.studymate.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class QuizParser {

    public static List<QuizQuestion> parse(String jsonString) throws Exception {
        if (jsonString == null) throw new IllegalArgumentException("JSON string is null");

        String cleaned = jsonString.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        JSONArray arr;
        if (cleaned.startsWith("[")) {
            arr = new JSONArray(cleaned);
        } else {
            JSONObject obj = new JSONObject(cleaned);
            arr = obj.optJSONArray("questions");
            if (arr == null) arr = obj.optJSONArray("quiz");
            if (arr == null) throw new IllegalArgumentException("No question array found in response");
        }

        List<QuizQuestion> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject qObj = arr.getJSONObject(i);
            String question = qObj.getString("question");
            JSONArray optArr = qObj.getJSONArray("options");
            List<String> options = new ArrayList<>();
            for (int j = 0; j < optArr.length(); j++) {
                options.add(optArr.getString(j));
            }
            int correctIndex = qObj.getInt("correctIndex");
            String explanation = qObj.optString("explanation", "");
            list.add(new QuizQuestion(question, options, correctIndex, explanation));
        }
        return list;
    }
}
