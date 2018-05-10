package org.afm.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Trace {

    /**
     * Gets content of a file where the file name is defined via stack trace.
     * @param overwrite  overwrites the file
     * @param actual the actual expected content
     * @param id additional id for the file
     * @param showFile print the file to sysout
     * @param traceDepth depth for the stacktrace
     * @param testResourcePath suffix path for the file
     * @return file content
     */
    public static String getFileContentByTrace(boolean overwrite, String actual, String id, boolean showFile,
                                               int traceDepth, String testResourcePath) {

        StackTraceElement[] stk = Thread.currentThread().getStackTrace();
        String path = stk[traceDepth].getClassName() + "." + stk[traceDepth].getMethodName() + "-" + id + ".expected";
//		System.out.println(Arrays.asList(stk));
        System.out.println(path);
        File testResourcePathFile = new File(testResourcePath);
        if (!testResourcePathFile.exists()) {
            testResourcePathFile.mkdirs();
        }
        String fullPath = testResourcePath + "/" + path;
        File f = new File(fullPath);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        String content = null;
        try {
            content = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath())), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (overwrite && !content.equals(actual)) {
            try {
                FileWriter fw = new FileWriter(f);
                fw.write(actual);
                content = actual;
                fw.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (showFile) {
            System.out.println(actual);
        }
        if (overwrite) {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! content is overwritten");
        }
        return content;
    }

}
