// TODO port:1.20.1 stub - simplified ACsLib service marker
package fr.aym.acslib.api;

/**
 * Marker interface for ACsLib services. Simplified stub for 1.20.1 port.
 */
public interface ACsService {
    default String getName() { return "stub"; }
    default String getVersion() { return "0.0.0"; }
}
