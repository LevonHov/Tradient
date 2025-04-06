package com.example.tradient.infrastructure.risk;

import com.example.tradient.config.ConfigurationFactory;
import com.example.tradient.domain.risk.ConservativeRiskProfile;
import com.example.tradient.domain.risk.RiskProfile;
import com.example.tradient.domain.risk.StandardRiskProfile;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating and managing risk profiles.
 * Provides access to predefined profiles and supports custom profile creation.
 */
public class RiskProfileFactory {
    
    private static final Map<String, RiskProfile> profiles = new HashMap<>();
    
    static {
        // Register standard profiles
        registerProfile(new StandardRiskProfile());
        registerProfile(new ConservativeRiskProfile());
        
        // Additional profiles could be registered here
    }
    
    /**
     * Private constructor to prevent instantiation
     */
    private RiskProfileFactory() {
        // Static factory
    }
    
    /**
     * Registers a risk profile in the factory.
     *
     * @param profile The risk profile to register
     */
    public static void registerProfile(RiskProfile profile) {
        profiles.put(profile.getName(), profile);
    }
    
    /**
     * Gets a risk profile by name.
     *
     * @param name The name of the risk profile
     * @return The risk profile, or null if not found
     */
    public static RiskProfile getProfile(String name) {
        return profiles.get(name);
    }
    
    /**
     * Gets the default risk profile.
     *
     * @return The default risk profile
     */
    public static RiskProfile getDefaultProfile() {
        String defaultProfileName = ConfigurationFactory.getString("risk.defaultProfile", "Standard Risk Profile");
        return profiles.getOrDefault(defaultProfileName, new StandardRiskProfile());
    }
    
    /**
     * Gets all available risk profiles.
     *
     * @return Map of profile names to profile objects
     */
    public static Map<String, RiskProfile> getAllProfiles() {
        return new HashMap<>(profiles);
    }
} 