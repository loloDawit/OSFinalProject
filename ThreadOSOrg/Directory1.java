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

   public int byte2int(byte data[], int offset){
   		return ((data[offset] & 0xff) << 24) + 
   						((data[offset+1] & 0xff) << 16) +
   						((data[offset+2] & 0xff) << 8) +
   						((data[offset+3]  & 0xff));
   }
   
   public char byte2char(byte data[], int offset){
   		return ((char)data[offset]) ;
   }

   public void bytes2directory( byte data[] ) {
      // assumes data[] received directory information from disk
      // initializes the Directory instance with this data[]
   		int offset = 0;
   		this.maxChars += byte2int(data, offset);
   		offset += 4;
   		for(int i = 0; i < fsizes.length; i++){
   			fsizes[i] = byte2int(data, offset);
   			offset += 4;
   		}
   		for(int i = 0; i < fnames.length; i++){
   			for(int filename = 0; filename < fnames[i].length; filename++){
   				fnames[i][filename] = byte2char(data, offset);
   				offset += 1;
   			}
   		}
   }

   public byte[] directory2bytes( ) {
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

   public short ialloc( String filename ) {
      short iNumber = namei(filename);
      if(iNumber != -1) return iNumber;//The file already exist
      // filename is the one of a file to be created.
   		for(int i = 0; i < filename.length(); i++){
   			fnames[fileLocation][i] = filename.charAt(i);
      }
      fsizes[fileLocation] = filename.length();
      // allocates a new inode number for this filename
   		fileLocation++;
   		return (fileLocation--);
   }

   public boolean ifree( short iNumber ) {
   		if(iNumber < 0 || iNumber > this.fileLocation) return false;
      // deallocates this inumber (inode number) by overwriting
   		for(; iNumber < this.fileLocation; iNumber++){
   			fnames[iNumber] = fnames[iNumber + 1];
   		}
   		this.fileLocation--;
      // the corresponding file will be deleted.''
   		return true;
   }

   	//Performs a sequential file search
   public short namei( String filename ) {
   		for(short i = 0; i < fileLocation; i++){
   			for(int j = 0; j < filename.length(); j++){
   				boolean same = true;
   				if(fnames[i][j] != filename.charAt(j)){
   					j = filename.length();
   					same = false;
   				}
   				if((j + 1) == filename.length() && same){//
   					return i;
   				}
   			}
   		}
      // returns the inumber corresponding to this filename
   		return -1;
   }
}
