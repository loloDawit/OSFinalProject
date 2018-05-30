public class Inode {
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers
 
    public int length;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this
    public short flag;                             // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer
 
    Inode( ) {                                     // a default constructor
       length = 0;
       count = 0;
       flag = 1;
       for ( int i = 0; i < directSize; i++ )
          direct[i] = -1;
       indirect = -1;
    }
 
    Inode( short iNumber ) {                       // retrieving inode from disk
       // design it by yourself.
       // to do 
       // Find the corresponding inode from the disk by calculating disk block
       // next set the buffer size of a block to 512 bytes
       // next we have to read from the blockNumber, the inode information into the data buffer
       // Find the corresponding inode from the disk by calculating disk block
		int blockNumber = (iNumber / 16) +1 ;
		byte data [] = new byte[Disk.blockSize]; 

        SysLib.rawread(blockNumber, data);
        
		int offsetEntry = (iNumber % 16) * 32;    // go to the disk and get the location 

		this.length = SysLib.bytes2int(data, offsetEntry);
		offsetEntry += 4;   // int for length
		this.count = SysLib.bytes2short(data, offsetEntry);
		offsetEntry += 2;   // short for count 
		this.flag = SysLib.bytes2short(data, offsetEntry);
		offsetEntry += 2;   // short for flag 

		for(int i = 0; i < directSize; i++) {
			this.direct[i] = SysLib.bytes2short(data, offsetEntry);
			offsetEntry += 2;
		}

		this.indirect = SysLib.bytes2short(data, offsetEntry);

       
    }
 
    //int toDisk( short iNumber ) {                  // save to disk as the i-th inode
       // design it by yourself.
    //}
 }