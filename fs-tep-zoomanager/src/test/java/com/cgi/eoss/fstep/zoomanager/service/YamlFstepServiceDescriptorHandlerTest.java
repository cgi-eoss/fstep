package com.cgi.eoss.fstep.zoomanager.service;

import com.cgi.eoss.fstep.model.FstepServiceDescriptor;
import com.cgi.eoss.fstep.zoomanager.ExampleServiceDescriptor;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class YamlFstepServiceDescriptorHandlerTest {

    private YamlFstepServiceDescriptorHandler handler;

    private FileSystem fs;

    @Before
    public void setUp() {
        this.handler = new YamlFstepServiceDescriptorHandler();
        this.fs = Jimfs.newFileSystem(Configuration.unix());
    }

    @Test
    public void readFile() throws Exception {
        FstepServiceDescriptor svc = handler.readFile(Paths.get(getClass().getResource("/TestService1.yaml").toURI()));
        assertThat(svc, is(ExampleServiceDescriptor.getExampleSvc()));
    }

    @Test
    public void writeFile() throws Exception {
        Path yaml = fs.getPath("test.yaml");
        FstepServiceDescriptor svc = ExampleServiceDescriptor.getExampleSvc();
        handler.writeFile(svc, yaml);

        List<String> generatedLines = Files.readAllLines(yaml);
        List<String> expectedLines = Files.readAllLines(Paths.get(getClass().getResource("/TestService1.yaml").toURI()));

        assertThat(generatedLines, is(expectedLines));
    }
    @Test
    public void read() throws Exception {
        FstepServiceDescriptor svc = handler.read(Files.newInputStream(Paths.get(getClass().getResource("/TestService1.yaml").toURI())));
        assertThat(svc, is(ExampleServiceDescriptor.getExampleSvc()));
    }

    @Test
    public void write() throws Exception {
        Path yaml = fs.getPath("test.yaml");
        FstepServiceDescriptor svc = ExampleServiceDescriptor.getExampleSvc();
        handler.write(svc, Files.newOutputStream(yaml));

        List<String> generatedLines = Files.readAllLines(yaml);
        List<String> expectedLines = Files.readAllLines(Paths.get(getClass().getResource("/TestService1.yaml").toURI()));

        assertThat(generatedLines, is(expectedLines));
    }

}