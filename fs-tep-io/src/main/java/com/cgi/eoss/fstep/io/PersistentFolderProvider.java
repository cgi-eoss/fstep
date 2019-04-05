package com.cgi.eoss.fstep.io;

import java.io.IOException;

public interface PersistentFolderProvider {

	boolean userFolderExists(String path) throws IOException;
	
	void createUserFolderWithQuota(String path, String quota) throws IOException;

	void setQuota(String path, String quota) throws IOException;

	long getUsageInBytes(String path) throws IOException;

	void deleteUserFolder(String path) throws IOException;
}
