package com.cgi.eoss.fstep.io;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

public interface PersistentFolderMounter {

	List<Path> bindUserPersistentFolder(URI uri, Path bindingDir) throws IOException;

	boolean supportsPersistentFolder(URI uri) throws IOException;
	
}
