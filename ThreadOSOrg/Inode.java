

public class Inode
{
  public static final int iNodeSize = 32;
  public static final int directSize = 11;
  public static final int NoError = 0;
  public static final int ErrorBlockRegistered = -1;
  public static final int ErrorPrecBlockUnused = -2;
  public static final int ErrorIndirectNull = -3;
  public int length;
  public short count;
  public short flag;
  public short[] direct = new short[11];
  public short indirect;
  
  Inode() {
    length = 0;
    count = 0;
    flag = 1;
    for (int i = 0; i < 11; i++)
      direct[i] = -1;
    indirect = -1;
  }
  
  Inode(short paramShort) {
    int i = 1 + paramShort / 16;
    byte[] arrayOfByte = new byte['Ȁ'];
    SysLib.rawread(i, arrayOfByte);
    int j = paramShort % 16 * 32;
    
    length = SysLib.bytes2int(arrayOfByte, j);
    j += 4;
    count = SysLib.bytes2short(arrayOfByte, j);
    j += 2;
    flag = SysLib.bytes2short(arrayOfByte, j);
    j += 2;
    for (int k = 0; k < 11; k++) {
      direct[k] = SysLib.bytes2short(arrayOfByte, j);
      j += 2;
    }
    indirect = SysLib.bytes2short(arrayOfByte, j);
    j += 2;
  }

  void toDisk(short paramShort)
  {
    byte[] arrayOfByte1 = new byte[32];
    int i = 0, j = 0;
    
    SysLib.int2bytes(length, arrayOfByte1, i);
    i += 4;
    SysLib.short2bytes(count, arrayOfByte1, i);
    i += 2;
    SysLib.short2bytes(flag, arrayOfByte1, i);
    i += 2;
    for (j = 0; j < 11; j++) {
      SysLib.short2bytes(direct[j], arrayOfByte1, i);
      i += 2;
    }
    SysLib.short2bytes(indirect, arrayOfByte1, i);
    i += 2;
    
    j = 1 + paramShort / 16;
    byte[] arrayOfByte2 = new byte['Ȁ'];
    SysLib.rawread(j, arrayOfByte2);
    i = paramShort % 16 * 32;
    

    System.arraycopy(arrayOfByte1, 0, arrayOfByte2, i, 32);
    SysLib.rawwrite(j, arrayOfByte2);
  }
 
  short findIndexBlock()
  {
    return indirect;
  }
  
  boolean registerIndexBlock(short paramShort) {
    for (int i = 0; i < 11; i++)
      if (direct[i] == -1)
        return false;
    if (indirect != -1)
      return false;
    indirect = paramShort;
    byte[] arrayOfByte = new byte['Ȁ'];
    for (int j = 0; j < 256; j++)
      SysLib.short2bytes((short)-1, arrayOfByte, j * 2);
    SysLib.rawwrite(paramShort, arrayOfByte);
    
    return true;
  }
  
  short findTargetBlock(int paramInt) {
    int i = paramInt / 512;
    if (i < 11) {
      return direct[i];
    }
    if (indirect < 0) {
      return -1;
    }
    byte[] arrayOfByte = new byte['Ȁ'];
    SysLib.rawread(indirect, arrayOfByte);
    int j = i - 11;
    return SysLib.bytes2short(arrayOfByte, j * 2);
  }
  

  int registerTargetBlock(int paramInt, short paramShort)
  {
    int i = paramInt / 512;
    if (i < 11) {
      if (direct[i] >= 0)
        return -1;
      if ((i > 0) && (direct[(i - 1)] == -1))
        return -2;
      direct[i] = paramShort;
      return 0;
    }
    
    if (indirect < 0) {
      return -3;
    }
    byte[] arrayOfByte = new byte['Ȁ'];
    SysLib.rawread(indirect, arrayOfByte);
    int j = i - 11;
    if (SysLib.bytes2short(arrayOfByte, j * 2) > 0) {
      SysLib.cerr("indexBlock, indirectNumber = " + j + " contents = " + SysLib.bytes2short(arrayOfByte, j * 2) + "\n");
      


      return -1;
    }
    SysLib.short2bytes(paramShort, arrayOfByte, j * 2);
    
    SysLib.rawwrite(indirect, arrayOfByte);
    return 0;
  }
  

  byte[] unregisterIndexBlock()
  {
    if (indirect >= 0) {
      byte[] arrayOfByte = new byte['Ȁ'];
      SysLib.rawread(indirect, arrayOfByte);
      indirect = -1;
      return arrayOfByte;
    }
    
    return null;
  }
}
