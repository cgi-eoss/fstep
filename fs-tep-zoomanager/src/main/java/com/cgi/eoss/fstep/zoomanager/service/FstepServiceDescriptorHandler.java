package com.cgi.eoss.fstep.zoomanager.service;

import com.cgi.eoss.fstep.model.FstepServiceDescriptor;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * <p>Utility to read and write {@link FstepServiceDescriptor} objects.</p>
 */
public interface FstepServiceDescriptorHandler {

    /**
     * <p>Deserialise the given FS-TEP service descriptor from the given file, according to the interface
     * implementation.</p>
     *
     * @param file The file to be read.
     * @return The FS-TEP service as described in the given file.
     */
    FstepServiceDescriptor readFile(Path file);

    /**
     * <p>Deserialise the given FS-TEP service descriptor from the given stream, according to the interface
     * implementation.</p>
     *
     * @param stream The byte stream representing a service descriptor.
     * @return The FS-TEP service as described in the given stream.
     */
    FstepServiceDescriptor read(InputStream stream);

    /**
     * <p>Serialise the given FS-TEP service descriptor to the given file, according to the interface implementation.</p>
     *
     * @param svc The FS-TEP service descriptor to be serialised.
     * @param file The target file to write.
     */
    void writeFile(FstepServiceDescriptor svc, Path file);

    /**
     * <p>Serialise the given FS-TEP service descriptor to the given stream, according to the interface
     * implementation.</p>
     *
     * @param svc The FS-TEP service descriptor to be serialised.
     * @param stream The destination for the byte stream.
     */
    void write(FstepServiceDescriptor svc, OutputStream stream);

}
