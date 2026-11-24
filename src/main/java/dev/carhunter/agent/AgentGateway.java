package dev.carhunter.agent;

public interface AgentGateway {
    String run(String role, String instructions, String context);
}
