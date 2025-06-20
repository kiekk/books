package com.example.recipe1311.replicator;

import java.io.IOException;

public interface FileCopier {

    void copyFile(String srcDir, String destDir, String filename) throws IOException;
}
