/**
 * FileSystem Class 
 * This is the main file system class & provides user threads with the system calls 
 * that will allow them to format, to open, to read from , to write to, 
 * to update the seek pointer of, to close, to delete, and to get the 
 * size of their files.
 * 
 * Dawit Abera 
 */

public class FileSystem {
	private SuperBlock superblock;
	private Directory directory;
	private FileTable filetable;

	/**
	 * Contructor
	 *
	 * @param diskBlocks number of total blocks in file system
	 */
	public FileSystem(int diskBlocks) {
		// create a superblock, and format disk with 64 nodes by default
		this.superblock = new SuperBlock(diskBlocks);

		// create directory, and register "/" in directory entry 0
		this.directory = new Directory(superblock.numberOfInodeBlocks);

		this.filetable = new FileTable(directory);

		// directory reconstruction
		FileTableEntry dirEnt = open("/", "r");
		int dirSize = fsize(dirEnt);

		if (dirSize > 0) {
			byte[] dirData = new byte[dirSize];
			read(dirEnt, dirData);
			this.directory.bytes2directory(dirData);
		}
		close(dirEnt);
	}
	/**
	 * ThreadOS saves all disk contents into DISK 
	 */
	public void sync() {

		FileTableEntry localFileTableEntry = open("/", "w");
		byte[] arrayOfByte = directory.directory2bytes();
		write(localFileTableEntry, arrayOfByte);
		close(localFileTableEntry);
		superblock.sync();

	}

	/**
	 * format the filesystem. Formats the disk (Disk.java's data contents). 
	 * The parameter files specifies the maximum number of files to be created 
	 * (the number of inodes to be allocated) in your file system. 
	 *
	 * @param files number of inodes
	 * @return false if filetable is not empty, true otherwise
	 */
	public boolean format(int files) {
		if (filetable.fempty()) {
			superblock.format(files);
			return true;
		}
		return false;
	}

	/**
	 * Opens the file specified by the fileName string in the given mode 
	 * (where "r" = ready only, "w" = write only, "w+" = read/write, "a" = append). 
	 * The call allocates a new file descriptor, fd to this file. The file is 
	 * created if it does not exist in the mode "w", "w+" or "a". SysLib.open 
	 * must return a negative number as an error value if the file does not exist 
	 * in the mode "r". 
	 *
	 * @param filename of file
	 * @param mode     of file (r, w, w+, a)
	 * @return FileTableEntry object, or null if its going to be deleted, 
	 *         or invalid mode type i.e: read empty file?
	 */
	public FileTableEntry open(String filename, String mode) {
		FileTableEntry ftEnt = filetable.falloc(filename, mode);
		if(ftEnt != null) {
			if (mode.equals("a")) {
				ftEnt.seekPtr = fsize(ftEnt);  // set it to end of file
			} else if (mode.equals("w")) {
				ftEnt.seekPtr = 0;
				if (!deallocAllBlocks(ftEnt)) { // deallocate blocks
					SysLib.cerr("Error in FileSystem.java, write() method: deallocAllBlocks returned false");
				}
			} else if (mode.equals("r") || mode.equals("w+")) {
				ftEnt.seekPtr = 0;
			}
		}
		return ftEnt;
	}

	/**
	 * Closes the file corresponding to ftEnt (fd), commits all file
	 * transactions on this file, and unregisters ftEnt from the user
	 * file descriptor table of the calling thread's TCB.
	 *
	 * @param ftEnt FileTableEntry Object to be deleted
	 * @return 0 if success, else -1
	 */
	public boolean close(FileTableEntry ftEnt) {
		ftEnt.inode.count--;
		ftEnt.count--;
		ftEnt.inode.flag = 0;
		return filetable.ffree(ftEnt);
	}

	/**
	 * @param ftEnt
	 * @return the size in bytes of the file indicated by ftEnt, else -1 if object is null
	 */
	public int fsize(FileTableEntry ftEnt) {
		if (ftEnt != null) {
			return ftEnt.inode.length;
		}
		return -1;
	}

	/**
	 * Reads up to buffer.length bytes from the file indicated by fd, starting at the position 
	 * currently pointed to by the seek pointer. If bytes remaining between the current seek 
	 * pointer and the end of file are less than buffer.length, SysLib.read reads as many bytes 
	 * as possible, putting them into the beginning of buffer. It increments the seek pointer 
	 * by the number of bytes to have been read. The return value is the number of bytes that 
	 * have been read, or a negative value upon an error.
	 *
	 * @param ftEnt  FileTableEntry object
	 * @param buffer stores the read data from the file indicated by ftEnt
	 * @return a negative value (-1) upon an error,
	 * else the number of bytes that have been sucessfully read
	 */
	public int read(FileTableEntry ftEnt, byte buffer[]) {
		// check for cases:
		// case I : Beginning location of file to EOF is less than buffer, means we should fit everything in buffer[]
		// case II: Beginning location of file to EOF is greater than buffer, means fit up to buffer.length

		int offset=0;
		int bytesRead =0;
		int remainingBytes = (fsize(ftEnt)<buffer.length)? fsize(ftEnt):buffer.length;
		
		while (remainingBytes > 0 && ftEnt.seekPtr < fsize(ftEnt)) {
			short targetBlock = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
			byte[] readBuff = new byte[512];
			SysLib.rawread(targetBlock, readBuff);

			if (buffer.length > fsize(ftEnt)) {

				if (remainingBytes > 512) {
					System.arraycopy(readBuff, ftEnt.seekPtr % 512, buffer, offset, 512);
					remainingBytes -= 512;
					offset += 512;
					ftEnt.seekPtr += 512;
					bytesRead += 512;
				} else {
					System.arraycopy(readBuff, ftEnt.seekPtr % 512, buffer, offset, remainingBytes);
					bytesRead += remainingBytes;
					break;
				}
			} else if (buffer.length <= fsize(ftEnt)) {

				if (remainingBytes > 512) {
					System.arraycopy(readBuff, ftEnt.seekPtr % 512, buffer, offset, 512);
					remainingBytes -= 512;
					offset += 512;
					ftEnt.seekPtr += 512;
					bytesRead += 512;
				} else {
					System.arraycopy(readBuff, ftEnt.seekPtr % 512, buffer, offset, remainingBytes);
					bytesRead += remainingBytes;
					break;
				}

			}
		}
		ftEnt.seekPtr = 0;
		return bytesRead;
	}

	/**
	 * Writes the contents of buffer to the file indicated by fd, starting at the position 
	 * indicated by the seek pointer. The operation may overwrite existing data in the file 
	 * and/or append to the end of the file. SysLib.write increments the seek pointer by 
	 * the number of bytes to have been written. The return value is the number of bytes 
	 * that have been written, or a negative value upon an error.
	 *
	 * @param ftEnt
	 * @param buffer contains the data to be written to the file indicated by ftEnt
	 * @return (-1) upon an error, else, number of bytes that have been written
	 */
	public int write(FileTableEntry ftEnt, byte buffer[]) {
		if (ftEnt == null || ftEnt.mode.equals("r")) {
			return -1;
		}

		int buffRemainder = buffer.length;
		int writtenBytes = 0;
		byte destBuffer[] = new byte[512];

		while(buffRemainder > 0) {  // start writing to blocks and setting direct/indirect pointers

			short currBlock = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);

			if(currBlock == -1) { // no current block, allocate a block

				currBlock = (short) superblock.getFreeBlock();

				//if (ftEnt.inode.setDirectPointers(currBlock)) {
				// if (ftEnt.inode.registerIndexBlock(currBlock)) {
				// } else 
				if (ftEnt.inode.registerIndexBlock(currBlock)) {
					//else if (ftEnt.inode.setIndexBlock(currBlock)) {
					currBlock = (short) superblock.getFreeBlock();
					// ftEnt.inode.setIndirectPointers(currBlock);
				} else {
					// no more free blocks
					SysLib.cout("no more free blocks\n");
					break;
				}
			}
			else {
				SysLib.rawread(currBlock, destBuffer); // read from currBlock
			}

			int currPtr = ftEnt.seekPtr % 512;
			int remainingBytesInBlock = 512 - currPtr;

			if(remainingBytesInBlock > buffRemainder) { // write up to the bufferRemainder

				System.arraycopy(buffer, writtenBytes, destBuffer, currPtr, buffRemainder);
				SysLib.rawwrite(currBlock, destBuffer);
				writtenBytes += buffRemainder;
				buffRemainder -= buffRemainder;
				ftEnt.seekPtr += buffRemainder;
			}
			else { // write to the remainder in blocks
				System.arraycopy(buffer, writtenBytes, destBuffer, currPtr, remainingBytesInBlock);
				SysLib.rawwrite(currBlock, destBuffer);
				writtenBytes += remainingBytesInBlock;
				buffRemainder -= remainingBytesInBlock;
				ftEnt.seekPtr += remainingBytesInBlock;
			}
		} // end of while loop

		if(ftEnt.mode.equals("w+")) {
			int diffInSize = fsize(ftEnt) - writtenBytes;
			if(diffInSize < 0) {
				ftEnt.inode.length += Math.abs(diffInSize);
			}
		}
		else if(ftEnt.mode.equals("a")) {
			ftEnt.inode.length = fsize(ftEnt) + writtenBytes;
		}
		else {
			ftEnt.inode.length += writtenBytes;
		}

		ftEnt.inode.toDisk(ftEnt.iNumber);  // write back to disk

		return writtenBytes; // return number of bytes that have been written
	}

	/**
	 * Returns used blocks to the freeList
	 *
	 * @param ftEnt FileTableEntry object, contains info like inode pointers to dealloc blocks
	 * @return false if error, true if dealloc, if no blocks to dealloc, also returns true?
	 */
	private boolean deallocAllBlocks(FileTableEntry ftEnt) {
		if (ftEnt != null) {
			// free the direct blocks first
			for (int i = 0; i < ftEnt.inode.direct.length; i++) {
				if (ftEnt.inode.direct[i] != -1) {
					// this.superblock.returnFreeBlock(ftEnt.inode.direct[i]); // free this block
					 this.superblock.returnBlock(ftEnt.inode.direct[i]); // free this block
					ftEnt.inode.direct[i] = -1;
				} else {
					return true;
				}
			}
			// free the indirect blocks
			short indirectBlockNum = ftEnt.inode.findIndexBlock();//ftEnt.inode.getIndexBlockNumber();
			if (indirectBlockNum != -1) {
				byte buffer[] = new byte[512];
				SysLib.rawread(indirectBlockNum, buffer);

				int offset = 0;
				short indexPtr = -1;
				for (int i = 0; i < 256; i++) {
					int indirectBlockPtr = SysLib.bytes2short(buffer, offset);
					if (indirectBlockPtr != -1) {
						this.superblock.returnBlock(indirectBlockPtr);
						SysLib.short2bytes(indexPtr, buffer, offset);
						offset += 2;
					} else {
						break;
					}
				}
				SysLib.rawwrite(indirectBlockNum, buffer);
				return true;
			}
		}
		return false;
	}

	/**
	 * Deletes the file from disk specified by fileName. All blocks used by file
	 * are freed.
	 * <p>
	 * If the file is currently open, it is not deleted and operation will return -1
	 *
	 * @param fileName name of file to be deleted
	 * @return 0 if successfully deleted, else -1 if not deleted
	 */
	public boolean delete(String fileName) {
		// check if file exists first
		short iNumber = this.directory.namei(fileName);

		// check if file exists
		if (iNumber == -1) {
			SysLib.cerr("FileSystem.java in delete(): files doesn't exist");
			return false;
		}

		// open the file to utilize "only one writer", ensures only this thread
		FileTableEntry ftEnt = open(fileName, "w");
		deallocAllBlocks(ftEnt); // release all blocks
		close(ftEnt); // set flags, write to disk, remove from ftEnt
		directory.ifree(iNumber); // remove from directory

		return false;
	}

	private final int SEEK_SET = 0;
	private final int SEEK_CUR = 1;
	private final int SEEK_END = 2;

	/**
	 * Updates the seek pointer corresponding to ftEnt (fd) as follows:
	 * 			1. If whence == SEEK_SET, the files seek pointer is set to offset
	 * 			   bytes from the beginning of the file
	 * 			2. If whence == SEEK_CUR, the file's seek pointer is set to its
	 * 			   current value plus the offset. The offset can be pos or neg
	 * 			3. If whence == SEEK_END, the files seek pointer is set to the size
	 *             of the file plus the offset. The offsent can be neg or pos
	 * Note:
	 * check If user attempts to set the seek pointer to a negative number, in this case
	 * we must clamp it to ZERO.
	 * Note:
	 * checl If the user attempts to set the pointer to beyond the file size, in this case
	 * we must set the seek pointer to the end of the file
	 *
	 * @param ftEnt  FileTableEntry object
	 * @param offset amount to offset to current seek pointer
	 * @param whence SEEK flag
	 * @return the offset location of the seek pointer in the file
	 */
	public int seek(FileTableEntry ftEnt, int offset, int whence) {
		if (ftEnt == null) {
			return -1;
		}
		int updatedSeekPtr;
		switch (whence) {
			case SEEK_SET:
				// the files seek pointer is set to offset bytes from the beginning of the file
				if (!setSeekPtr(ftEnt, offset)) {
					ftEnt.seekPtr = offset;
				}
				break;
			case SEEK_CUR:
				//the file's seek pointer is set to its current value plus the offset. The offset can be pos or neg
				updatedSeekPtr = ftEnt.seekPtr + offset;
				if (!setSeekPtr(ftEnt, updatedSeekPtr)) {
					ftEnt.seekPtr = updatedSeekPtr;
				}
				break;
			case SEEK_END:
				//the files seek pointer is set to the size of the file plus the offset. The offsent can be neg or pos
				// what does this mean? won't this go past the file size?
				updatedSeekPtr = fsize(ftEnt) + offset;
				if (!(setSeekPtr(ftEnt, updatedSeekPtr))) {
					ftEnt.seekPtr = updatedSeekPtr;
				}
				break;
			default:
				return -1;   // other values, is an error
		}
		return ftEnt.seekPtr;
	}

	private boolean setSeekPtr(FileTableEntry ftEnt, int currVal) {
		if (currVal < 0) { // if negative, clamp it to 0
			ftEnt.seekPtr = 0;
			return true;
		} else if (currVal > fsize(ftEnt)) {   // greater than file length
			ftEnt.seekPtr = fsize(ftEnt);   // set it to EOF
			return true;
		}
		return false;
	}
}