package io.spring.batch.helloworld.batch.callback;

import org.springframework.batch.item.file.FlatFileHeaderCallback;

import java.io.IOException;
import java.io.Writer;

public class StatementHeaderCallback implements FlatFileHeaderCallback {

    public void writeHeader(Writer writer) throws IOException {
        writer.write(String.format("%120s\n", "Customer Service Number"));
        writer.write(String.format("%120s\n", "(800) 867-5309"));
        writer.write(String.format("%120s\n", "Available 24/7"));
        writer.write("\n");
    }

}