package com.ammons.taskactivity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Service to lookup geographic location information from IP addresses. Uses ip-api.com free tier
 * (no API key required).
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since November 2025
 */
@Service
public class GeoIpService {

    private static final Logger logger = LoggerFactory.getLogger(GeoIpService.class);
    private static final String GEO_IP_API_URL = "http://ip-api.com/json/";

    private final RestTemplate restTemplate;

    public GeoIpService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Lookup geographic location for an IP address
     * 
     * @param ipAddress the IP address to lookup
     * @return formatted location string (e.g., "New York, US") or "Unknown Location" if lookup
     *         fails
     */
    public String lookupLocation(String ipAddress) {
        // Handle localhost/private IPs
        if (isLocalOrPrivateIp(ipAddress)) {
            return "Local Network";
        }

        try {
            String url = GEO_IP_API_URL + ipAddress;
            GeoIpResponse response = restTemplate.getForObject(url, GeoIpResponse.class);

            if (response != null && "success".equals(response.status)) {
                String location = buildLocationString(response);
                logger.debug("GeoIP lookup for {}: {}", ipAddress, location);
                return location;
            } else {
                logger.warn("GeoIP lookup failed for {}: {}", ipAddress,
                        response != null ? response.message : "null response");
                return "Unknown Location";
            }
        } catch (Exception e) {
            logger.error("Error looking up GeoIP for {}: {}", ipAddress, e.getMessage());
            return "Unknown Location";
        }
    }

    /**
     * Build formatted location string from API response
     */
    private String buildLocationString(GeoIpResponse response) {
        StringBuilder location = new StringBuilder();

        if (response.city != null && !response.city.isEmpty()) {
            location.append(response.city);
        }

        if (response.regionName != null && !response.regionName.isEmpty()) {
            if (location.length() > 0) {
                location.append(", ");
            }
            location.append(response.regionName);
        }

        if (response.country != null && !response.country.isEmpty()) {
            if (location.length() > 0) {
                location.append(", ");
            }
            location.append(response.country);
        }

        return location.length() > 0 ? location.toString() : "Unknown Location";
    }

    /**
     * Check if IP is localhost or private network
     */
    private boolean isLocalOrPrivateIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return true;
        }

        // IPv4 localhost and private ranges
        return ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")
                || ipAddress.equals("::1") || ipAddress.startsWith("192.168.")
                || ipAddress.startsWith("10.") || ipAddress.startsWith("172.16.")
                || ipAddress.startsWith("172.17.") || ipAddress.startsWith("172.18.")
                || ipAddress.startsWith("172.19.") || ipAddress.startsWith("172.20.")
                || ipAddress.startsWith("172.21.") || ipAddress.startsWith("172.22.")
                || ipAddress.startsWith("172.23.") || ipAddress.startsWith("172.24.")
                || ipAddress.startsWith("172.25.") || ipAddress.startsWith("172.26.")
                || ipAddress.startsWith("172.27.") || ipAddress.startsWith("172.28.")
                || ipAddress.startsWith("172.29.") || ipAddress.startsWith("172.30.")
                || ipAddress.startsWith("172.31.");
    }

    /**
     * Response object from ip-api.com
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GeoIpResponse {
        public String status; // "success" or "fail"
        public String message; // Error message if status is "fail"
        public String country; // Country name
        public String countryCode; // Two-letter country code
        public String region; // Region/state code
        public String regionName; // Region/state name
        public String city; // City name
        public String zip; // Zip code
        public Double lat; // Latitude
        public Double lon; // Longitude
        public String timezone; // Timezone
        public String isp; // ISP name
        public String org; // Organization name
        public String as; // AS number and name
    }
}
