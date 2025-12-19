package com.example.gateway.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway")
public class GatewayProps {

  private String upstreamBaseUrl;
  private Map<String, String> apiKeys; // key -> plan name
  private Map<String, Plan> plans;     // plan -> config

  public String getUpstreamBaseUrl() { return upstreamBaseUrl; }
  public void setUpstreamBaseUrl(String upstreamBaseUrl) { this.upstreamBaseUrl = upstreamBaseUrl; }

  public Map<String, String> getApiKeys() { return apiKeys; }
  public void setApiKeys(Map<String, String> apiKeys) { this.apiKeys = apiKeys; }

  public Map<String, Plan> getPlans() { return plans; }
  public void setPlans(Map<String, Plan> plans) { this.plans = plans; }

  public static class Plan {
    private int windowSeconds;
    private int defaultLimit;
    private Map<String, Integer> routes;

    public int getWindowSeconds() { return windowSeconds; }
    public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }

    public int getDefaultLimit() { return defaultLimit; }
    public void setDefaultLimit(int defaultLimit) { this.defaultLimit = defaultLimit; }

    public Map<String, Integer> getRoutes() { return routes; }
    public void setRoutes(Map<String, Integer> routes) { this.routes = routes; }
  }
}
