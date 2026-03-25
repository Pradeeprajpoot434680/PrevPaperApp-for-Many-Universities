package com.prevpaper.upload.utils;

import org.springframework.web.multipart.MultipartFile;
import java.io.*;

public class InMemoryMultipartFile implements MultipartFile {
    private final byte[] content;
    private final String name;

    public InMemoryMultipartFile(byte[] content, String name) {
        this.content = content;
        this.name = name;
    }

    @Override
    public String getName() { return name; }

    @Override
    public String getOriginalFilename() { return name; }

    @Override
    public String getContentType() {
        // Simple logic to guess content type based on extension
        if (name.toLowerCase().endsWith(".pdf")) return "application/pdf";
        if (name.toLowerCase().endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }

    @Override
    public boolean isEmpty() { return content == null || content.length == 0; }

    @Override
    public long getSize() { return content.length; }

    @Override
    public byte[] getBytes() throws IOException { return content; }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(content);
        }
    }
}