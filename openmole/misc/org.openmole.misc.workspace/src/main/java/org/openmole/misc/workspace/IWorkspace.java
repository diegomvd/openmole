package org.openmole.misc.workspace;

import java.io.File;
import java.io.IOException;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

public interface IWorkspace {
	static final String ConfigurationFile = ".preferences";
	static final String GlobalGroup = "Global";
	static final String DefaultObjectRepoLocaltion = ".objectRepository.bin";
	static final String DefaultTmpLocation = ".tmp";
	
	static final ConfigurationLocation UniqueID = new ConfigurationLocation(GlobalGroup, "UniqueID");
	static final ConfigurationLocation ObjectRepoLocation = new ConfigurationLocation(GlobalGroup, "ObjectRepoLocation");
	static final ConfigurationLocation TmpLocation = new ConfigurationLocation(GlobalGroup, "TmpLocation");

	void setLocation(File location);
        File getLocation();

	File newDir(String prefix) throws InternalProcessingError;
        File newDir() throws InternalProcessingError;
	File newFile(String prefix, String suffix) throws InternalProcessingError;
        File newFile() throws InternalProcessingError;

        File getFile(String name) throws IOException;

        String getPreference(ConfigurationLocation location) throws InternalProcessingError;

        int getPreferenceAsInt(ConfigurationLocation location) throws InternalProcessingError;

        long getPreferenceAsLong(ConfigurationLocation location) throws InternalProcessingError;

        double getPreferenceAsDouble(ConfigurationLocation location) throws InternalProcessingError;

        long getPreferenceAsDurationInMs(ConfigurationLocation location) throws InternalProcessingError;

        int getPreferenceAsDurationInS(ConfigurationLocation location) throws InternalProcessingError;

	void setPreference(ConfigurationLocation configurationLocation, String value) throws InternalProcessingError;

        void removePreference(ConfigurationLocation configurationElement) throws InternalProcessingError;

        void providePassword(String password) throws InternalProcessingError, UserBadDataError;

        void resetPreferences() throws InternalProcessingError;

        boolean isPreferenceSet(ConfigurationLocation location) throws InternalProcessingError, UserBadDataError;
        
	void addToConfigurations(ConfigurationLocation location, ConfigurationElement element);
	void addToConfigurations(ConfigurationLocation location, String defaultValue);
	
        String getDefaultValue(ConfigurationLocation location);
}
