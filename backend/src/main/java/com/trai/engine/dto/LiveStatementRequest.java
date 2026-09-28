package com.trai.engine.dto;

public class LiveStatementRequest {
    private String speaker;
    private String statement;
    private String mediaSource;

    public LiveStatementRequest() {}

    public LiveStatementRequest(String speaker, String statement, String mediaSource) {
        this.speaker = speaker;
        this.statement = statement;
        this.mediaSource = mediaSource;
    }

    public String getSpeaker() { return speaker; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }

    public String getStatement() { return statement; }
    public void setStatement(String statement) { this.statement = statement; }

    public String getMediaSource() { return mediaSource; }
    public void setMediaSource(String mediaSource) { this.mediaSource = mediaSource; }
}
