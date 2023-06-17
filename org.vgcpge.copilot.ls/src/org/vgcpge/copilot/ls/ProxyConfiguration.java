package org.vgcpge.copilot.ls;

/**
 * @param userId, password are optional
 */
public record ProxyConfiguration(String host, int port, String userId, String password) {
}
