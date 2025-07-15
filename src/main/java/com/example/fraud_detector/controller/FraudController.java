package com.example.fraud_detector.controller;

import com.example.fraud_detector.model.Transaction;
import com.example.fraud_detector.model.AnalysisResult;
import jakarta.servlet.http.HttpSession;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class FraudController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String ANALYZE_API_URL = "https://fraud-detector-agent-1.onrender.com/analyze";
    private final String HIJACK_CHECK_API_URL = "https://fraud-detector-agent-1.onrender.com/hijack-check";
    private final String FRAUD_HISTORY_PATH = "fraud_history.jsonl";

    @GetMapping("/new")
    public String showForm(HttpSession session, Model model) {
        model.addAttribute("sessionId", session.getId()); // ✅ inject to Thymeleaf
        return "form";
    }


    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(FRAUD_HISTORY_PATH));
            List<JSONObject> records = lines.stream()
                    .map(JSONObject::new)
                    .collect(Collectors.toList());
            model.addAttribute("transactions", records);
        } catch (IOException e) {
            model.addAttribute("error", "Could not load fraud history.");
        }
        return "dashboard";
    }

    @GetMapping("/get-session-id")
    @ResponseBody
    public Map<String, String> getSessionId(HttpSession session) {
        return Map.of("sessionId", session.getId());
    }

    @PostMapping("/analyze")
    public String analyzeTxn(
            @RequestParam String consumerId,
            @RequestParam double amount,
            @RequestParam String location,
            @RequestParam int hour,
            @RequestParam String deviceId,
            @RequestParam String knownLocationsInput,
            @RequestParam String knownDevicesInput,
            @RequestParam double averageTransactionAmount,
            @RequestParam(required = false) String recentFlagsInput,
            HttpSession session,
            Model model
    ) {
        try {
            // Parse input
            List<String> knownLocations = Arrays.asList(knownLocationsInput.split("\\s*,\\s*"));
            List<String> knownDevices = Arrays.asList(knownDevicesInput.split("\\s*,\\s*"));
            List<String> recentFlags = recentFlagsInput == null || recentFlagsInput.isBlank()
                    ? List.of()
                    : Arrays.asList(recentFlagsInput.split("\\s*,\\s*"));

            Transaction txn = new Transaction(
                    consumerId, amount, location, hour, deviceId,
                    knownLocations, knownDevices,
                    averageTransactionAmount, recentFlags
            );

            // Prepare HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 1. Call /analyze (main fraud check)
            HttpEntity<Transaction> request = new HttpEntity<>(txn, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(ANALYZE_API_URL, request, String.class);
            JSONObject resJson = new JSONObject(response.getBody());
            String explanation = resJson.optString("analysis", "No explanation");
            String verdict = resJson.optString("ai_verdict", "Review");

            // 2. Call /hijack-check (session-based behavior check)
            JSONObject hijackPayload = new JSONObject().put("session_id", session.getId());
            HttpEntity<String> hijackRequest = new HttpEntity<>(hijackPayload.toString(), headers);
            ResponseEntity<String> hijackResp = restTemplate.postForEntity(HIJACK_CHECK_API_URL, hijackRequest, String.class);
            String hijackResult = new JSONObject(hijackResp.getBody()).optString("hijack_analysis", "⚠️ No session data found.");

            // 3. Pass data to result page
            model.addAttribute("result", new AnalysisResult(txn, explanation, verdict));
            model.addAttribute("hijackResult", hijackResult);
            model.addAttribute("sessionLogged", !hijackResult.contains("No session data"));

            return "result";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error analyzing transaction: " + e.getMessage());
            return "form";
        }
    }

    @PostMapping("/validate")
    public String validateResult(@RequestParam String consumerId,
                                 @RequestParam boolean isFraud) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(FRAUD_HISTORY_PATH));
            List<String> updated = new ArrayList<>();

            for (String line : lines) {
                JSONObject obj = new JSONObject(line);
                if (obj.optString("consumer_id").equals(consumerId)) {
                    obj.put("user_feedback", "Validated");
                    obj.put("final_verdict", isFraud ? "Fraud" : "Safe");
                    obj.put("timestamp", Instant.now().toString());
                }
                updated.add(obj.toString());
            }

            Files.write(Paths.get(FRAUD_HISTORY_PATH), updated);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "redirect:/dashboard";
    }
}
