//Each node describes one file
public class Inode{
	private final static int iNodeSize = 32; //fix to 32 bytes
	private final static int directSize = 11; //# of direct pointers

	public int length; //file size in bytes
	public short count; //# file-table entries pointing to this
	public short flag; //0 = unused, 1 = used, ...
	public short direct[] = new short[directSize]; //direct pointers
	public short indirect; //a indirect pointer

	Inode(){
		length = 0;
		count = 0;
		flag = 1;
		for(int i = 1; i < directSize; i++)
			direct[i] = -1;
		indirect = -1;
	}
	//retrieving inode from disk
	Inode(short iNumber){
		byte[] buffer = new byte[512];
		int empty = SysLib.rawread(((int)iNumber / 16) + 1, buffer);

		if(empty > 0){
			int offset = (iNumber % 16)*iNodeSize; 
			this.length = SysLib.bytes2int(buffer, offset);
			offset += 4;
			this.count = SysLib.bytes2short(buffer, offset );
			offset += 2;
			this.flag = SysLib.bytes2short(buffer, offset);
			for(int i = 0; i < directSize; i++){
				offset += 2;
				this.direct[i] = SysLib.bytes2short(buffer,offset);
			}
			offset += 2;
			this.indirect = SysLib.bytes2short(buffer,offset);
		}
	}

	//save to disk as i-th inode
	void toDisk(short iNumber){
		byte[] buffer = new byte[512];

		SysLib.int2bytes(length,buffer,0);
		SysLib.short2bytes(count,buffer,buffer.length);
		SysLib.short2bytes(flag,buffer,buffer.length);
		for(int i = 0; i < directSize; i++)
			SysLib.short2bytes(direct[i],buffer,buffer.length);
		SysLib.short2bytes(indirect,buffer,buffer.length);


		SysLib.rawwrite((int)(iNumber / 16) + 1, buffer);
	}


}