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
    private final String ANALYZE_API_URL = "http://localhost:8000/analyze";
    private final String HIJACK_CHECK_API_URL = "http://localhost:8000/hijack-check";

    @GetMapping("/new")
    public String showForm() {
        return "form";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        try {
            List<String> lines = Files.readAllLines(Paths.get("fraud_history.jsonl"));
            List<JSONObject> records = lines.stream()
                    .map(JSONObject::new)
                    .collect(Collectors.toList());
            model.addAttribute("transactions", records);
        } catch (IOException e) {
            model.addAttribute("error", "Could not load fraud history.");
        }
        return "dashboard"; // Thymeleaf will look for dashboard.html in templates
    }

    @GetMapping("/get-session-id")
    @ResponseBody
    public Map<String, String> getSessionId(HttpSession session) {
        return Map.of("sessionId", session.getId());
    }

    @PostMapping("/analyze")
    public String analyzeTxn(@RequestParam String consumerId,
                             @RequestParam double amount,
                             @RequestParam String location,
                             @RequestParam int hour,
                             @RequestParam String deviceId,
                             @RequestParam String knownLocationsInput,
                             @RequestParam String knownDevicesInput,
                             @RequestParam double averageTransactionAmount,
                             @RequestParam(required = false) String recentFlagsInput,
                             HttpSession session,
                             Model model) {
        try {
            Transaction txn = new Transaction(
                    consumerId, amount, location, hour, deviceId,
                    Arrays.asList(knownLocationsInput.split("\\s*,\\s*")),
                    Arrays.asList(knownDevicesInput.split("\\s*,\\s*")),
                    averageTransactionAmount,
                    recentFlagsInput == null || recentFlagsInput.isBlank()
                            ? List.of()
                            : List.of(recentFlagsInput.split("\\s*,\\s*"))
            );

            // 1. Call /analyze
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Transaction> request = new HttpEntity<>(txn, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(ANALYZE_API_URL, request, String.class);
            JSONObject resJson = new JSONObject(response.getBody());

            String explanation = resJson.optString("analysis", "No explanation");
            String verdict = resJson.optString("ai_verdict", "Review");

            // 2. Call /hijack-check
            JSONObject hijackPayload = new JSONObject().put("session_id", session.getId());
            HttpEntity<String> hijackRequest = new HttpEntity<>(hijackPayload.toString(), headers);
            String hijackResponse = restTemplate.postForEntity(HIJACK_CHECK_API_URL, hijackRequest, String.class).getBody();
            String hijackAnalysis = new JSONObject(hijackResponse).optString("hijack_analysis", "Unavailable");

            // 3. Add to model
            model.addAttribute("result", new AnalysisResult(txn, explanation, verdict));
            model.addAttribute("hijackResult", hijackAnalysis);

            return "result";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "form";
        }
    }

    @PostMapping("/validate")
    public String validateResult(@RequestParam String consumerId,
                                 @RequestParam boolean isFraud) {
        try {
            List<String> lines = Files.readAllLines(Paths.get("fraud_history.jsonl"));
            List<String> updated = new ArrayList<>();
            for (String line : lines) {
                JSONObject obj = new JSONObject(line);
                if (obj.getString("consumer_id").equals(consumerId)) {
                    obj.put("user_feedback", "Validated")
                            .put("final_verdict", isFraud ? "Fraud" : "Safe")
                            .put("timestamp", Instant.now().toString());
                }
                updated.add(obj.toString());
            }
            Files.write(Paths.get("fraud_history.jsonl"), updated);
            return "redirect:/dashboard";

        } catch (IOException e) {
            return "redirect:/dashboard";
        }

    }
}
