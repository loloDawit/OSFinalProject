public class SuperBlock{
    public final int numberOfInodeBlocks = 64;
    public int totalBlocks; // the number of disk blocks
    public int inodeBlocks; // the number of inodes
    public int freeList;    // the block number of the free list's head.

    //constructor
    public SuperBlock( int diskSize ){
        byte[] superBlock = new byte[Disk.blockSize];
        int offset = 0;

        SysLib.rawread(offset, superBlock);
        totalBlocks = SysLib.bytes2int(superBlock, offset);
        offset += 4;
        inodeBlocks = SysLib.bytes2int(superBlock, offset);
        offset += 4;
        freeList = SysLib.bytes2int(superBlock, offset);

        if (this.totalBlocks == diskSize 
            && this.freeList >= 2
            && this.inodeBlocks > 0){//No remaining blocks
            return;
        }

        this.totalBlocks = diskSize;
        format(numberOfInodeBlocks);
    }


    //
    public void format(int numFiles){
        this.inodeBlocks = numFiles;
        byte[] data = new byte[Disk.blockSize];


        Inode iNode;
        for (short i = 0; i < numFiles; i++){
            iNode = new Inode();
            iNode.toDisk(i);
        }

        for (int i = totalBlocks - 1; i > (inodeBlocks/16 + 1); i--){
            SysLib.int2bytes(i, data, 0);
            int val = SysLib.bytes2int(data, 0);
            SysLib.rawwrite(i - 1, data);
        }

        freeList = (inodeBlocks/16) + 2;
        
        sync(); //synchronous the data in the disk
    }

    //Get a free block
    public int getFreeBlock(){
        byte[] data;

        if (freeList > 0){//There are some spots remaining
            data = new byte[Disk.blockSize];
            SysLib.rawread(freeList, data);//copy from the disk

            int freeBlock = SysLib.bytes2int(data, 0);

            if(freeBlock > totalBlocks){//No more spots
                return 0;
            }
            
            int lastFreeBlock = freeList;
            freeList = freeBlock;
            
            return lastFreeBlock;
        }

        return -1;
    }


    //Put the block back to the free list
    public boolean returnBlock(int blockNumber){
        boolean range = (blockNumber > 0 && blockNumber < this.totalBlocks);

        byte[] data;

        if (range){
            data = new byte[Disk.blockSize];
            SysLib.int2bytes(freeList, data, 0);
            this.freeList = blockNumber;

            SysLib.rawwrite(blockNumber, data);

            return true;
        }

        return false;
    }
    

    //Writes data back to the disk.
    public void sync(){
        byte[] data = new byte[Disk.blockSize];

        int offset = 0;
        SysLib.int2bytes(totalBlocks, data, offset);

        offset += 4;
        SysLib.int2bytes(inodeBlocks, data, offset);

        offset += 4;
        SysLib.int2bytes(freeList, data, offset);
        
        SysLib.rawwrite(0, data);
    }
}