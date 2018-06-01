import java.util.*;

public class FileTable {

   private Vector<FileTableEntry> table;         // the actual entity of this file table
   private Directory dir;        // the root directory 

   public FileTable( Directory directory ) { // constructor
      table = new Vector( );     // instantiate a file (structure) table
      dir = directory;           // receive a reference to the Director
   }                             // from the file system

   // major public methods
   public synchronized FileTableEntry falloc( String filename, String mode ) {
      // allocate a new file (structure) table entry for this file name
      // allocate/retrieve and register the corresponding inode using dir
      // increment this inode's count
      // immediately write back this inode to the disk
      // return a reference to this file (structure) table entry
   }

   public synchronized boolean ffree( FileTableEntry e ) {//receive a file table entry reference
      // save the corresponding inode to the disk
     Inode inode = e.getInode();
     inode.toDisk(inode.getNodeNumber());
      // free this file table entry by overwriting it
     int i = 0; 
     while((i < table.size()) && e.getNodeNumber() != table[i].getNodeNumber())
     {
       i++;
     }
     if(i + 1 == table.size()) return false;//The entry doesn't exit

     //Overwrites the table entry
     for(; i < table.size(); i++){
         table[i] = table[i+1];
     }

     return true;
   }

   public synchronized boolean fempty( ) {
      return table.isEmpty( );  // return if table is empty 
   }                            // should be called before starting a format
}