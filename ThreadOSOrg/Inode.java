/**
 * Inode Class 
 * 
 * The next block after the superblock is the inode blocks.
 * Each inode describes ONE file, and is 32 bytes
 *
 * It includes 12 pointers of the index block.
 * The FIRST 11 of these pointers point to DIRECT blocks.
 * The LAST pointer points to an INDIRECT block
 *
 * 16 inodes can be stored in ONE block
 * Each inode muse include:
 * 		1. the length of the corresponding file
 * 		2. the number of file (structure) table entries that point to this inode
 * 		3. the flags indicate (self determined):
 * 				0 = unused
 * 				1 = used
 * 			    2 = read
 * 			    3 = write
 * 			    4 = delete
 *
 * Aisha and Dawit 2018
 */

public class Inode {
	private final static int iNodeSize = 32;       // fix to 32 bytes
	private final static int directSize = 11;      // # direct pointers

	public int length;                             // file size in bytes
	public short count;                            // # file-table entries pointing to this
	public short flag;                             // 0 = unused, 1 = used, ...
	public short direct[] = new short[directSize]; // direct pointers
	public short indirect;                         // a indirect pointer, block number of another block

	/**
	 * Default constructor:Creates blank inodes
	 * When new file is created, it is given a new inode and 
	 * the contents is updated as the file is written to the disk
	 */
	public Inode() {
		length = 0;
		count = 0;
		flag = 1;
		for ( int i = 0; i < directSize; i++ )
			direct[i] = -1;						// indicates invalid block numbers
		indirect = -1;							// means no data blocks yet
	}

	/**
	 * Constructor
	 * Retrieves an existing inode from disk into the memory
	 * Creates memory representation the of file on disk using 
	 * the inode number, this constructor: reads the corresponding 
	 * disk block locates the corresponding inode info in the block &
	 * intializes a new inode with the information retrived
	 *
	 * @param iNumber inode number
	 */
	public Inode( short iNumber ) {
		// Find the corresponding inode from the disk by calculating disk block
		int blockNumber = 1 + (iNumber / 16);
		// sets the buffer size of a block 512 bytes
		byte data[] = new byte[Disk.blockSize];  
		SysLib.rawread(blockNumber, data);

		// find where we are in the blockNumber of 512 bytes
		int offset = (iNumber % 16) * 32;

		this.length = SysLib.bytes2int(data, offset);
		offset += 4;

		this.count = SysLib.bytes2short(data, offset);
		offset += 2;

		this.flag = SysLib.bytes2short(data, offset);
		offset += 2;

		for(int i =0; i < directSize; i++) {
			this.direct[i] = SysLib.bytes2short(data, offset);
			offset += 2;
		}

		this.indirect = SysLib.bytes2short(data, offset);
	}


	/**
	 * A write-back operation that saves this inode data
	 * to the iNumber-th inode in the disk.
	 *
	 * @param iNumber inode number
	 */
	public void toDisk( short iNumber ) {
		// find block number
		byte data[] = new byte[Disk.blockSize];
		int blockNumber = 1 + (iNumber / 16);
		SysLib.rawread(blockNumber, data);

		// find offset in the block
		int offset = (iNumber % 16) * 32;

		SysLib.int2bytes(this.length, data, offset);
		offset += 4;

		SysLib.short2bytes(this.count, data, offset);
		offset += 2;

		SysLib.short2bytes(this.flag, data, offset);
		offset += 2;

		for(int i = 0; i < directSize; i++) {
			SysLib.short2bytes(this.direct[i], data, offset);
			offset += 2;
		}

		SysLib.short2bytes(this.indirect, data, offset);
		SysLib.rawwrite(blockNumber, data);
	}

	/**
	 * getIndexBlockNumber: retrives the block number
	 * @return the indirect pointer (index == indirect), -1 if no indirect blocks used
	 */
	public short getIndexBlockNumber() {
		return this.indirect;
	}

	/**
	 * Formats the indirect block
	 *
	 * @param indexBlockNumber
	 * @return boolean false if not all direct poitners are used else true
	 */
	public boolean setIndexBlock(short indexBlockNumber) {
		// check if all direct pointers are used
		for(int i = 0; i < directSize; i++) {
			if(this.direct[i] == -1) {
				return false;
			}
		}

		// check if indirect pointer is UNUSED
		if(this.indirect != -1) {
			return true;
		}

		// set indirect pointer to indexBlock number
		this.indirect = indexBlockNumber;
		byte block[] = new byte[Disk.blockSize];

		// format the block to 512/2 = 256 pointers, set it to short -1 (2 bytes each)
		int offset = 0;
		short indexPtr = -1;
		for(int i = 0; i < 256; i++) {
			SysLib.short2bytes(indexPtr, block, offset);
			offset += 2;
		}
		SysLib.rawwrite(indexBlockNumber, block);
		return true;
	}

	/**
	 * findTargetBlock : finds the target block 
	 * @param offset where the data is in the block
	 * @return the block given the offset
	 */
	public short findTargetBlock(int offset) {
		int blockNumber = offset/Disk.blockSize;

		if(blockNumber >= directSize) {
			if(this.indirect == -1) {
				return -1;
			}
			// read from the indirect block to gain access to pointers
			byte buffer[] = new byte[Disk.blockSize];
			SysLib.rawread(this.indirect, buffer);

			// traverse the index block
			int offsetLeft = (blockNumber - directSize) * 2;

			return SysLib.bytes2short(buffer, offsetLeft);
		}
		else {
			return this.direct[blockNumber];
		}
	}


	/**
	 * Set blockNumber to a free indirect block pointer
	 * @param blockNumber free block number to assing
	 * @return boolean true if assigned else false
	 */
	public boolean setIndirectPointers(short blockNumber) {
		int offset = 0;
		// read from the block
		byte buffer[] = new byte[Disk.blockSize];
		SysLib.rawread(this.indirect, buffer);

		for(int i =0; i < 256; i++) {
			// read the offset, convert bytetoshort
			if(SysLib.bytes2short(buffer, offset) ==  -1) {
				SysLib.short2bytes(blockNumber, buffer, offset);
				SysLib.rawwrite(this.indirect, buffer);
				return true;
			}
			offset += 2;
		}
		return false; 
	}
	/**
	 * Sets the blockNumber to a free direct or indirect pointer
	 *
	 * @param blockNumber free block number to assign
	 * @return boolean true if assigned else false
	 */
	public boolean setDirectPointers(short blockNumber) {
		// check if we ran out of free blocks
	   if(blockNumber == -1) { 
		   return false;
	   }
	   // assining direct blocks
	   for(int i = 0; i < directSize; i++) {
		   if(this.direct[i] == -1) {
			   this.direct[i] = blockNumber;
			   return true;
		   }
	   }

	   return false;
   }
}
