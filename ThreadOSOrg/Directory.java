public class Directory {
   private static int maxChars = 30; // max characters of each file name

   // Directory entries
   private int fsizes[];        // each element stores a different file size.
   private char fnames[][];    // each element stores a different file name.

   private short fileLocation = 1;	//Start adding files from this location

   public Directory( int maxInumber ) { // directory constructor
      this.fsizes = new int[maxInumber];     // maxInumber = max files
      for ( int i = 0; i < maxInumber; i++ ){ 
         this.fsizes[i] = 0;                 // all file size initialized to 0
      }
      this.fnames = new char[maxInumber][maxChars];
      String root = "/";                // entry(inode) 0 is "/"
      fsizes[0] = root.length( );        // fsize[0] is the size of "/".
      root.getChars( 0, fsizes[0], fnames[0], 0 ); // fnames[0] includes "/"
   }

   public int bytes2short(byte[]data, int offset){
      return SysLib.bytes2short(data,  offset);
   }

//Complete::
// assumes data[] received directory information from disk
// initializes the Directory instance with this data[]
   public void bytes2directory( byte data[] ) {
   	int offset  = maxChars * 2 + 4;         
      int blkNumber = 0;           
      for (int i = 0; i < fsizes.length; ++i) {
         fsizes[i] = 0;
      } 
        
      for (int i = 0; i < (data.length / offset); ++i) {
         blkNumber = bytes2short(data, i * offset);
         fsizes[blkNumber] = bytes2short(data, i * offset + 2);
         for (int j = 0; j < fsizes[blkNumber]; ++j) {
             fnames[blkNumber][j] = (char)bytes2short(data, i * offset + j * 2 + 4);
         } 
      } 
   }

//Completed::
   public byte[] directory2bytes( ) {
      int offset = maxChars * 2 + 4;
      int size = (fsizes.length*4) + (fnames.length*maxChars*2); 

      byte data[] = new byte[size];

      for (int i = 0; i < fsizes.length; i++) {
         offset += 4; 
         SysLib.int2bytes(fsizes[i], data, 0);
      }
      // Loop through the file name and convert it to bytes 
      for (int i = 0; i < fnames.length; i++) {
         offset += maxChars * 2;
         // now convert the char array to a String
         String fname = new String(fnames[i]);

         // next, convert the string to byte array
         byte charData[] = fname.getBytes();

         // finally copy it to array
         System.arraycopy(charData, 0, data, offset, charData.length);
      }
        return data;
   }

   //Allocate space for a new file:: Complete
   public short ialloc( String filename ) {
      short nodeNumber = namei(filename);

      if (nodeNumber == -1) {//File Doesn't Exist
         for (short i = 1; i < fsizes.length; ++i) {//Find an empty node
             if (fsizes[i] == 0) {
                 fsizes[i] = filename.length();
                 filename.getChars(0, fsizes[i], fnames[i], 0);
               return i;
              } 
         } 
       } 
      return -1;
   }

   //Completed::
   // deallocates this inumber (inode number) by overwriting the file
    // the corresponding file will be deleted.
   public boolean ifree( short iNumber ) {
   	if(iNumber < 0 || iNumber > this.fsizes.length) return false;
      
      boolean exists = (fsizes[iNumber] > 0);
      fsizes[iNumber] = 0;
      
      return exists;
   }

   //Completed::
   //Performs a sequential file search when trying to find a file
   public short namei( String filename ) {
      for(short i = 0; i < fsizes.length; i++){
            if(fsizes[i] > 0){
               for(int j = 0; j < filename.length(); j++){
                  boolean same = true;
                  if(filename.charAt(j) != (fnames[i][j])){
                     j = filename.length();
                     same = false;
                  }
                  if((j + 1) == filename.length() && same){//
                     return i;
                  }
               }
            }
         }
      // returns the inumber corresponding to this filename
         return -1;
   }
}
