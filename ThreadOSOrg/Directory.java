public class Directory {
    private static int maxChars = 30; // max characters of each file name

    // Directory entries
    private int fsize[]; // each element stores a different file size.
    private char fnames[][]; // each element stores a different file name.

    public Directory(int maxInumber) { // directory constructor
        fsizes = new int[maxInumber]; // maxInumber = max files
        for (int i = 0; i < maxInumber; i++)
            fsize[i] = 0; // all file size initialized to 0
        fnames = new char[maxInumber][maxChars];
        String root = "/"; // entry(inode) 0 is "/"
        fsize[0] = root.length(); // fsize[0] is the size of "/".
        root.getChars(0, fsizes[0], fnames[0], 0); // fnames[0] includes "/"
    }

    public void bytes2directory(byte data[]) {
        // assumes data[] received directory information from disk
        // initializes the Directory instance with this data[]
    }

    public byte[] directory2bytes() {
        // converts and return Directory information into a plain byte array
        // this byte array will be written back to disk
        // note: only meaningfull directory information should be converted
        // into bytes.
        int offset = 0;
        int size = (fsize.length*4) + (fnames.length*maxChars*2); 

        byte data[] = new byte[size];

        for (int i = 0; i < fsizes.length; i++) {

            offset +=4; 
            SysLib.int2bytes(fsizes[i], data, 0);

        }
        // Loop through the file name and convert it to bytes 
        
        for (int i = 0; i < fnames.length; i++) {
            offset += maxChars * 2
            // now convert the char array to a String
            String fname = new String(fnames[i]);

            // next, convert the string to byte array
            byte charData[] = fname.getBytes();

            // finally copy it to array
            System.arraycopy(charData, 0, data, offset, charData.length);
        }
        return data;

    }

    public short ialloc(String filename) {
        // filename is the one of a file to be created.
        // allocates a new inode number for this filename
    }

    public boolean ifree(short iNumber) {
        // deallocates this inumber (inode number)
        // the corresponding file will be deleted.
    }

    public short namei(String filename) {
        // returns the inumber corresponding to this filename
    }
}