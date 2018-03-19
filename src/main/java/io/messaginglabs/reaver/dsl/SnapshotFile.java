package io.messaginglabs.reaver.dsl;

public interface SnapshotFile {

    String path();
    String filename();

    /**
     * Returns the length of the file
     */
    int length();

    /*
     * In general, a snapshot file might contains a lot of chosen instances, the begin
     * indicates the first instance applied to this snapshot file, the end indicates
     * the last instance applied.
     *
     * -1 means that it unable to know the begin and end instance id from the file.
     */
    long begin();
    long end();

}
