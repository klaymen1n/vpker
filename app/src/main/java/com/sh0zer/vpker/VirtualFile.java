package com.sh0zer.vpker;

public class VirtualFile {
    private String name;
    private boolean isDirectory;
    public VirtualFile(String name, boolean isDirectory)
    {
        this.name = name;
        this.isDirectory = isDirectory;
    }
    public String getName(){
        return this.name;
    }
    public boolean isDirectory(){
        return this.isDirectory;
    }
}
