package com.trai.engine.multiagent;

/**
 * Represents the output of a single agent (Agent 1 Collector or Agent 2 Validator).
 * Each agent queries Grok, Gemini, and ChatGPT in parallel.
 */
public class AgentOutput {

    private String agentId;
    private String role;
    private String grokResponse;
    private String geminiResponse;
    private String chatGptResponse;
    private int aggregatedScore;

    private AgentOutput() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final AgentOutput o = new AgentOutput();
        public Builder agentId(String v)         { o.agentId = v;          return this; }
        public Builder role(String v)            { o.role = v;             return this; }
        public Builder grokResponse(String v)    { o.grokResponse = v;     return this; }
        public Builder geminiResponse(String v)  { o.geminiResponse = v;   return this; }
        public Builder chatGptResponse(String v) { o.chatGptResponse = v;  return this; }
        public Builder aggregatedScore(int v)    { o.aggregatedScore = v;  return this; }
        public AgentOutput build() { return o; }
    }

    public String getAgentId()         { return agentId; }
    public String getRole()            { return role; }
    public String getGrokResponse()    { return grokResponse; }
    public String getGeminiResponse()  { return geminiResponse; }
    public String getChatGptResponse() { return chatGptResponse; }
    public int getAggregatedScore()    { return aggregatedScore; }

    public void setAgentId(String v)         { this.agentId = v; }
    public void setRole(String v)            { this.role = v; }
    public void setGrokResponse(String v)    { this.grokResponse = v; }
    public void setGeminiResponse(String v)  { this.geminiResponse = v; }
    public void setChatGptResponse(String v) { this.chatGptResponse = v; }
    public void setAggregatedScore(int v)    { this.aggregatedScore = v; }
}
